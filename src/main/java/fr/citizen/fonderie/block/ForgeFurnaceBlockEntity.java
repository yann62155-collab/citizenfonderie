package fr.citizen.fonderie.block;

import fr.citizen.fonderie.Config;
import fr.citizen.fonderie.ModRegistry;
import fr.citizen.fonderie.menu.ForgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * Cœur du four : N emplacements de minerai qui cuisent chacun indépendamment,
 * N emplacements de sortie (un sous chaque minerai) et un emplacement de combustible partagé.
 * Le combustible s'use proportionnellement au nombre de minerais en cours de fonte
 * (8 minerais en parallèle = 8× plus de charbon, comme 8 fours vanilla).
 *
 * Slots : [0, N) minerais, [N, 2N) sorties, 2N combustible.
 */
public class ForgeFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SCALE = 1000; // progression envoyée au client sur 0..1000

    private final ForgeTier tier;
    private final int n;
    private final ItemStackHandler items;
    private final int[] progress;
    private final int[] total;
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe>[] checks;
    private int burnTime;
    private int burnMax;
    private float storedXp;

    private final IItemHandler topHandler;
    private final IItemHandler bottomHandler;
    private final IItemHandler sideHandler;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < n) {
                return total[index] <= 0 ? 0 : Math.min(SCALE, progress[index] * SCALE / total[index]);
            }
            return burnMax <= 0 ? 0 : Mth.clamp(burnTime * SCALE / burnMax, 0, SCALE);
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return n + 1;
        }
    };

    @SuppressWarnings("unchecked")
    public ForgeFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.FOUR_BE.get(), pos, state);
        this.tier = state.getBlock() instanceof ForgeFurnaceBlock b ? b.tier() : ForgeTier.SIMPLE;
        this.n = tier.slots();
        this.items = createHandler(n, this::setChanged);
        this.progress = new int[n];
        this.total = new int[n];
        this.checks = new RecipeManager.CachedCheck[n];
        for (int i = 0; i < n; i++) {
            checks[i] = RecipeManager.createCheck(RecipeType.SMELTING);
        }
        this.topHandler = new RangedWrapper(items, 0, n);
        this.bottomHandler = new RangedWrapper(items, n, 2 * n);
        this.sideHandler = new RangedWrapper(items, 2 * n, 2 * n + 1);
    }

    /** Même règles côté serveur et côté client (évite les items qui « sautent » dans le GUI). */
    public static ItemStackHandler createHandler(int n, Runnable onChange) {
        return new ItemStackHandler(2 * n + 1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (slot < n) {
                    return isMinerai(stack);
                }
                if (slot < 2 * n) {
                    return false; // sorties : remplies uniquement par le four
                }
                return isFuel(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                onChange.run();
            }
        };
    }

    public static boolean isMinerai(ItemStack stack) {
        return !stack.isEmpty() && (Config.accepterToutesCuissons() || stack.is(ModRegistry.MINERAIS));
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    public ForgeTier getTier() {
        return tier;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public ContainerData getData() {
        return data;
    }

    public IItemHandler getHandlerForSide(@Nullable Direction side) {
        if (side == Direction.UP) {
            return topHandler;
        }
        if (side == Direction.DOWN) {
            return bottomHandler;
        }
        return sideHandler;
    }

    // ---------------------------------------------------------------- tick

    public static void serverTick(Level level, BlockPos pos, BlockState state, ForgeFurnaceBlockEntity be) {
        int n = be.n;
        boolean wasLit = be.burnTime > 0;
        boolean changed = false;

        @SuppressWarnings("unchecked")
        RecipeHolder<SmeltingRecipe>[] recipes = new RecipeHolder[n];
        int active = 0;
        for (int i = 0; i < n; i++) {
            ItemStack input = be.items.getStackInSlot(i);
            RecipeHolder<SmeltingRecipe> recipe = null;
            if (isMinerai(input)) {
                recipe = be.checks[i].getRecipeFor(new SingleRecipeInput(input), level).orElse(null);
            }
            if (recipe != null && be.canOutput(i, recipe.value().getResultItem(level.registryAccess()))) {
                recipes[i] = recipe;
                active++;
            } else if (be.progress[i] != 0) {
                be.progress[i] = 0;
                changed = true;
            }
        }

        // le feu consomme 1 par tick et par minerai en fonte
        if (be.burnTime > 0) {
            be.burnTime = Math.max(0, be.burnTime - Math.max(1, active));
        }

        // rallumer avec un nouveau combustible
        if (be.burnTime <= 0 && active > 0) {
            int fuelSlot = 2 * n;
            ItemStack fuel = be.items.getStackInSlot(fuelSlot);
            int fuelTime = fuel.getBurnTime(RecipeType.SMELTING);
            if (fuelTime > 0) {
                be.burnTime = fuelTime;
                be.burnMax = fuelTime;
                if (fuel.hasCraftingRemainingItem() && fuel.getCount() == 1) {
                    be.items.setStackInSlot(fuelSlot, fuel.getCraftingRemainingItem()); // seau de lave -> seau
                } else {
                    be.items.extractItem(fuelSlot, 1, false);
                }
                changed = true;
            }
        }

        boolean lit = be.burnTime > 0;
        for (int i = 0; i < n; i++) {
            RecipeHolder<SmeltingRecipe> recipe = recipes[i];
            if (recipe == null) {
                continue;
            }
            if (lit) {
                be.total[i] = Math.max(1, recipe.value().getCookingTime());
                be.progress[i]++;
                if (be.progress[i] >= be.total[i]) {
                    be.progress[i] = 0;
                    be.smelt(i, recipe, level);
                }
                changed = true;
            } else if (be.progress[i] > 0) {
                be.progress[i] = Math.max(0, be.progress[i] - 2); // refroidit comme un four vanilla
                changed = true;
            }
        }

        if (wasLit != lit) {
            be.setLitOnStructure(level, pos, state, lit);
            changed = true;
        }
        if (changed) {
            setChanged(level, pos, state);
        }
    }

    private boolean canOutput(int i, ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        ItemStack out = items.getStackInSlot(n + i);
        if (out.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(out, result)) {
            return false;
        }
        return out.getCount() + result.getCount() <= out.getMaxStackSize();
    }

    private void smelt(int i, RecipeHolder<SmeltingRecipe> recipe, Level level) {
        ItemStack result = recipe.value().getResultItem(level.registryAccess()).copy();
        ItemStack out = items.getStackInSlot(n + i);
        if (out.isEmpty()) {
            items.setStackInSlot(n + i, result);
        } else {
            out.grow(result.getCount());
            items.setStackInSlot(n + i, out);
        }
        items.getStackInSlot(i).shrink(1);
        items.setStackInSlot(i, items.getStackInSlot(i)); // notifie le changement
        storedXp += recipe.value().getExperience();
    }

    private void setLitOnStructure(Level level, BlockPos master, BlockState masterState, boolean lit) {
        if (!(masterState.getBlock() instanceof ForgeFurnaceBlock block)) {
            return;
        }
        Direction facing = masterState.getValue(ForgeFurnaceBlock.FACING);
        for (int part = 0; part < tier.blocks(); part++) {
            BlockPos p = block.partPos(master, facing, part);
            BlockState s = level.getBlockState(p);
            if (s.is(block) && s.getValue(ForgeFurnaceBlock.LIT) != lit) {
                level.setBlock(p, s.setValue(ForgeFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
            }
        }
    }

    /** Appelé quand le joueur retire des lingots : donne l'expérience accumulée. */
    public void awardXp(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel) || storedXp <= 0) {
            return;
        }
        int whole = Mth.floor(storedXp);
        float frac = Mth.frac(storedXp);
        if (frac > 0 && serverLevel.random.nextFloat() < frac) {
            whole++;
        }
        storedXp = 0;
        if (whole > 0) {
            ExperienceOrb.award(serverLevel, player.position(), whole);
        }
        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    // ---------------------------------------------------------------- sauvegarde

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.putIntArray("Progress", progress);
        tag.putIntArray("Total", total);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnMax", burnMax);
        tag.putFloat("Xp", storedXp);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            CompoundTag itemsTag = tag.getCompound("Items");
            itemsTag.putInt("Size", 2 * n + 1); // garde la bonne taille quoi qu'il arrive
            items.deserializeNBT(registries, itemsTag);
        }
        copy(tag.getIntArray("Progress"), progress);
        copy(tag.getIntArray("Total"), total);
        burnTime = tag.getInt("BurnTime");
        burnMax = tag.getInt("BurnMax");
        storedXp = tag.getFloat("Xp");
    }

    private static void copy(int[] from, int[] to) {
        System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    // ---------------------------------------------------------------- menu

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ForgeMenu(containerId, inventory, this);
    }
}

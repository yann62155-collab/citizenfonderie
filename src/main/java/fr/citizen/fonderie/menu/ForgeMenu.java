package fr.citizen.fonderie.menu;

import fr.citizen.fonderie.ModRegistry;
import fr.citizen.fonderie.block.ForgeFurnaceBlock;
import fr.citizen.fonderie.block.ForgeFurnaceBlockEntity;
import fr.citizen.fonderie.block.ForgeTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

public class ForgeMenu extends AbstractContainerMenu {
    public static final int INPUT_Y = 18;
    public static final int OUTPUT_Y = 56;
    public static final int FUEL_X = 80;
    public static final int FUEL_Y = 80;
    public static final int INV_Y = 114;

    private final ForgeTier tier;
    private final int n;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Block block;
    @Nullable
    private final ForgeFurnaceBlockEntity be;

    /** Côté serveur. */
    public ForgeMenu(int containerId, Inventory inventory, ForgeFurnaceBlockEntity be) {
        this(containerId, inventory, be.getTier(), be.getItems(), be.getData(),
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), be.getBlockState().getBlock(), be);
    }

    /** Côté client (données envoyées par le serveur à l'ouverture). */
    public static ForgeMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ForgeTier tier = ForgeTier.byOrdinal(buf.readVarInt());
        Block block = inventory.player.level().getBlockState(pos).getBlock();
        if (block instanceof ForgeFurnaceBlock forge) {
            tier = forge.tier();
        }
        int n = tier.slots();
        return new ForgeMenu(containerId, inventory, tier, ForgeFurnaceBlockEntity.createHandler(n, () -> {
        }), new SimpleContainerData(n + 1), ContainerLevelAccess.NULL, block, null);
    }

    private ForgeMenu(int containerId, Inventory inventory, ForgeTier tier, IItemHandler items, ContainerData data,
                      ContainerLevelAccess access, Block block, @Nullable ForgeFurnaceBlockEntity be) {
        super(ModRegistry.FOUR_MENU.get(), containerId);
        this.tier = tier;
        this.n = tier.slots();
        this.data = data;
        this.access = access;
        this.block = block;
        this.be = be;

        int startX = columnX(0);
        for (int i = 0; i < n; i++) {
            addSlot(new SlotItemHandler(items, i, startX + i * 18, INPUT_Y));
        }
        for (int i = 0; i < n; i++) {
            addSlot(new OutputSlot(items, n + i, startX + i * 18, OUTPUT_Y));
        }
        addSlot(new SlotItemHandler(items, 2 * n, FUEL_X, FUEL_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INV_Y + 58));
        }
        addDataSlots(data);
    }

    /** Colonnes centrées dans le GUI (176 px de large). */
    public int columnX(int column) {
        return (176 - n * 18) / 2 + 1 + column * 18;
    }

    public ForgeTier getTier() {
        return tier;
    }

    public int slotCount() {
        return n;
    }

    /** 0..1 */
    public float progress(int i) {
        return data.get(i) / (float) ForgeFurnaceBlockEntity.SCALE;
    }

    /** 0..1 */
    public float burn() {
        return data.get(n) / (float) ForgeFurnaceBlockEntity.SCALE;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int fuel = 2 * n;
        int invStart = fuel + 1;
        int invEnd = invStart + 27;
        int hotEnd = invEnd + 9;

        if (index < invStart) {
            if (!moveItemStackTo(stack, invStart, hotEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            if (ForgeFurnaceBlockEntity.isMinerai(stack)) {
                moved = moveItemStackTo(stack, 0, n, false);
            }
            if (!moved && ForgeFurnaceBlockEntity.isFuel(stack)) {
                moved = moveItemStackTo(stack, fuel, fuel + 1, false);
            }
            if (!moved) {
                moved = index < invEnd
                        ? moveItemStackTo(stack, invEnd, hotEnd, false)
                        : moveItemStackTo(stack, invStart, invEnd, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return copy;
    }

    private class OutputSlot extends SlotItemHandler {
        OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (be != null) {
                be.awardXp(player);
            }
            super.onTake(player, stack);
        }
    }
}

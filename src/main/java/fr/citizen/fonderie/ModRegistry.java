package fr.citizen.fonderie;

import fr.citizen.fonderie.block.ForgeFurnaceBlock;
import fr.citizen.fonderie.block.ForgeFurnaceBlockEntity;
import fr.citizen.fonderie.block.ForgeFurnaceItem;
import fr.citizen.fonderie.block.ForgeTier;
import fr.citizen.fonderie.menu.ForgeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CitizenFonderie.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CitizenFonderie.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CitizenFonderie.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CitizenFonderie.MODID);

    /** Items acceptés par les fours de forgeron (et refusés par les fours vanilla). */
    public static final TagKey<Item> MINERAIS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(CitizenFonderie.MODID, "minerais"));

    public static final DeferredBlock<ForgeFurnaceBlock> FOUR_SIMPLE = block(ForgeTier.SIMPLE, MapColor.STONE);
    public static final DeferredBlock<ForgeFurnaceBlock> FOUR_APPRENTI = block(ForgeTier.APPRENTI, MapColor.COLOR_RED);
    public static final DeferredBlock<ForgeFurnaceBlock> FOUR_MAITRE = block(ForgeTier.MAITRE, MapColor.COLOR_BLACK);

    public static final DeferredItem<ForgeFurnaceItem> FOUR_SIMPLE_ITEM = item(FOUR_SIMPLE);
    public static final DeferredItem<ForgeFurnaceItem> FOUR_APPRENTI_ITEM = item(FOUR_APPRENTI);
    public static final DeferredItem<ForgeFurnaceItem> FOUR_MAITRE_ITEM = item(FOUR_MAITRE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeFurnaceBlockEntity>> FOUR_BE =
            BLOCK_ENTITIES.register("four_forgeron", () -> BlockEntityType.Builder.of(ForgeFurnaceBlockEntity::new,
                    FOUR_SIMPLE.get(), FOUR_APPRENTI.get(), FOUR_MAITRE.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ForgeMenu>> FOUR_MENU =
            MENUS.register("four_forgeron", () -> IMenuTypeExtension.create(ForgeMenu::fromNetwork));

    private ModRegistry() {
    }

    private static DeferredBlock<ForgeFurnaceBlock> block(ForgeTier tier, MapColor color) {
        return BLOCKS.register(tier.id, () -> new ForgeFurnaceBlock(tier, BlockBehaviour.Properties.of()
                .mapColor(color)
                .requiresCorrectToolForDrops()
                .strength(3.5F)
                .sound(SoundType.STONE)
                .pushReaction(PushReaction.BLOCK)
                .lightLevel(state -> state.getValue(ForgeFurnaceBlock.LIT) ? 13 : 0)));
    }

    private static DeferredItem<ForgeFurnaceItem> item(DeferredBlock<ForgeFurnaceBlock> block) {
        return ITEMS.register(block.getId().getPath(), () -> new ForgeFurnaceItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
    }
}

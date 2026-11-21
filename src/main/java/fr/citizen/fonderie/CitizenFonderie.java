package fr.citizen.fonderie;

import fr.citizen.fonderie.block.ForgeFurnaceBlock;
import fr.citizen.fonderie.block.ForgeFurnaceBlockEntity;
import fr.citizen.fonderie.jade.JadeItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(CitizenFonderie.MODID)
public class CitizenFonderie {
    public static final String MODID = "citizenfonderie";

    public CitizenFonderie(IEventBus modBus, ModContainer container) {
        ModRegistry.register(modBus);
        JadeItems.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "citizenfonderie-common.toml");
        modBus.addListener(this::creativeTabs);
        modBus.addListener(this::capabilities);
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModRegistry.FOUR_SIMPLE_ITEM.get());
            event.accept(ModRegistry.FOUR_APPRENTI_ITEM.get());
            event.accept(ModRegistry.FOUR_MAITRE_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(JadeItems.KATANA.get());
            event.accept(JadeItems.KABUTO.get());
            event.accept(JadeItems.PLASTRON.get());
            event.accept(JadeItems.JAMBIERES.get());
            event.accept(JadeItems.BOTTES.get());
        }
    }

    /** Entonnoirs / tuyaux : dessus = minerais, côtés = combustible, dessous = lingots. Marche sur tous les blocs du four. */
    private void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
            if (!(state.getBlock() instanceof ForgeFurnaceBlock block)) {
                return null;
            }
            BlockPos master = block.masterPos(pos, state);
            return level.getBlockEntity(master) instanceof ForgeFurnaceBlockEntity forge ? forge.getHandlerForSide(side) : null;
        }, ModRegistry.FOUR_SIMPLE.get(), ModRegistry.FOUR_APPRENTI.get(), ModRegistry.FOUR_MAITRE.get());
    }
}

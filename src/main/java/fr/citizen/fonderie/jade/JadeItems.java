package fr.citizen.fonderie.jade;

import fr.citizen.fonderie.CitizenFonderie;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

/**
 * Équipement de samouraï en jade. Se forge avec le Lingot de Jade de Citizen Guilds
 * (tag citizenfonderie:lingots_jade), lui-même obtenu en fondant le minerai de jade des donjons.
 */
public final class JadeItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CitizenFonderie.MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, CitizenFonderie.MODID);

    public static final TagKey<Item> LINGOTS_JADE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(CitizenFonderie.MODID, "lingots_jade"));

    /** Un peu au-dessus de la netherite : c'est un butin de donjon. */
    public static final Tier JADE_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            2300, 9.0F, 4.0F, 20, () -> Ingredient.of(LINGOTS_JADE));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JADE_SAMOURAI =
            ARMOR_MATERIALS.register("jade_samourai", () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.HELMET, 3,
                            ArmorItem.Type.CHESTPLATE, 8,
                            ArmorItem.Type.LEGGINGS, 6,
                            ArmorItem.Type.BOOTS, 3,
                            ArmorItem.Type.BODY, 11),
                    20,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(LINGOTS_JADE),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(CitizenFonderie.MODID, "jade_samourai"))),
                    3.0F,
                    0.1F));

    public static final DeferredItem<SwordItem> KATANA = ITEMS.register("katana_jade",
            () -> new SwordItem(JADE_TIER, new Item.Properties()
                    .rarity(Rarity.EPIC)
                    .fireResistant()
                    .attributes(SwordItem.createAttributes(JADE_TIER, 3, -2.2F))));

    public static final DeferredItem<KabutoItem> KABUTO = ITEMS.register("kabuto_jade",
            () -> new KabutoItem(JADE_SAMOURAI, armorProps(ArmorItem.Type.HELMET)));
    public static final DeferredItem<ArmorItem> PLASTRON = ITEMS.register("plastron_jade",
            () -> new ArmorItem(JADE_SAMOURAI, ArmorItem.Type.CHESTPLATE, armorProps(ArmorItem.Type.CHESTPLATE)));
    public static final DeferredItem<ArmorItem> JAMBIERES = ITEMS.register("jambieres_jade",
            () -> new ArmorItem(JADE_SAMOURAI, ArmorItem.Type.LEGGINGS, armorProps(ArmorItem.Type.LEGGINGS)));
    public static final DeferredItem<ArmorItem> BOTTES = ITEMS.register("bottes_jade",
            () -> new ArmorItem(JADE_SAMOURAI, ArmorItem.Type.BOOTS, armorProps(ArmorItem.Type.BOOTS)));

    private JadeItems() {
    }

    private static Item.Properties armorProps(ArmorItem.Type type) {
        return new Item.Properties().durability(type.getDurability(40)).rarity(Rarity.EPIC).fireResistant();
    }

    public static void register(IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
        ITEMS.register(bus);
    }
}

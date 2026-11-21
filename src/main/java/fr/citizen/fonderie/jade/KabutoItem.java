package fr.citizen.fonderie.jade;

import fr.citizen.fonderie.CitizenFonderie;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Casque de samouraï : modèle 3D dédié (voir client/KabutoModel) et sa propre texture 64×64. */
public class KabutoItem extends ArmorItem {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            CitizenFonderie.MODID, "textures/models/armor/kabuto_jade.png");

    public KabutoItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot,
                                            ArmorMaterial.Layer layer, boolean innerModel) {
        return TEXTURE;
    }
}

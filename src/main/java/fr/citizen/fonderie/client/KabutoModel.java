package fr.citizen.fonderie.client;

import fr.citizen.fonderie.CitizenFonderie;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Kabuto (casque de samouraï) en 3D, accroché à la tête du joueur :
 * bol laqué à côtes (hachi), visière (mabizashi), protège-nuque en lamelles évasées (shikoro),
 * rabats latéraux (fukigaeshi) et cimier en cornes dorées (kuwagata) avec une fleur de jade.
 * Texture 64×64 : textures/models/armor/kabuto_jade.png
 */
public final class KabutoModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CitizenFonderie.MODID, "kabuto_jade"), "main");

    private KabutoModel() {
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        // bol (hachi) et son sommet
                        .texOffs(0, 0).addBox(-4.5F, -9.5F, -4.5F, 9, 3, 9)
                        .texOffs(0, 12).addBox(-3.5F, -10.5F, -3.5F, 7, 1, 7)
                        .texOffs(28, 12).addBox(-1.0F, -11.5F, -1.0F, 2, 1, 2)
                        // visière
                        .texOffs(36, 0).addBox(-4.5F, -7.0F, -6.0F, 9, 1, 2)
                        // protège-nuque : 3 lamelles de plus en plus larges
                        .texOffs(0, 20).addBox(-5.0F, -6.5F, -3.0F, 10, 2, 9)
                        .texOffs(0, 31).addBox(-5.5F, -4.5F, -2.0F, 11, 2, 8)
                        .texOffs(0, 41).addBox(-6.0F, -2.5F, -1.0F, 12, 2, 7)
                        // rabats latéraux
                        .texOffs(36, 12).addBox(4.5F, -7.0F, -5.0F, 2, 3, 2)
                        .texOffs(44, 12).addBox(-6.5F, -7.0F, -5.0F, 2, 3, 2),
                PartPose.ZERO);

        // cimier : fleur de jade au front + deux cornes dorées en V
        PartDefinition crest = head.addOrReplaceChild("kuwagata", CubeListBuilder.create()
                        .texOffs(38, 20).addBox(-1.5F, -1.5F, -0.5F, 3, 3, 1),
                PartPose.offset(0.0F, -8.0F, -6.2F));
        crest.addOrReplaceChild("corne_gauche", CubeListBuilder.create()
                        .texOffs(48, 20).addBox(-0.5F, -8.0F, -0.5F, 1, 8, 1),
                PartPose.offsetAndRotation(1.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.45F));
        crest.addOrReplaceChild("corne_droite", CubeListBuilder.create()
                        .texOffs(52, 20).addBox(-0.5F, -8.0F, -0.5F, 1, 8, 1),
                PartPose.offsetAndRotation(-1.0F, -0.5F, 0.0F, 0.0F, 0.0F, -0.45F));

        // le reste du corps est vide : ce modèle ne sert qu'au casque
        CubeListBuilder empty = CubeListBuilder.create();
        root.addOrReplaceChild("hat", empty, PartPose.ZERO);
        root.addOrReplaceChild("body", empty, PartPose.ZERO);
        root.addOrReplaceChild("right_arm", empty, PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", empty, PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", empty, PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", empty, PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}

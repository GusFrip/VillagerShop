package com.villagershop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.villagershop.ShopMod;
import com.villagershop.registry.ModVillagers;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

/**
 * Re-peint la tenue du Marchand par-dessus le rendu vanilla, ce qui masque le
 * badge de niveau (pierre/fer/…) UNIQUEMENT pour notre profession. Les autres
 * villageois ne sont pas affectés.
 */
public class ShopBadgeCoverLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath(ShopMod.MOD_ID, "textures/entity/villager/profession/shopkeeper.png");

    public ShopBadgeCoverLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Villager villager,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (villager.isInvisible() || villager.isBaby()) return;
        if (villager.getVillagerData().getProfession() != ModVillagers.SHOPKEEPER) return;
        renderColoredCutoutModel(getParentModel(), TEX, poseStack, buffer, packedLight, villager, -1);
    }
}

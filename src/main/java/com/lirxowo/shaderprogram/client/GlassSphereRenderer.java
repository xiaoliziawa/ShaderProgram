package com.lirxowo.shaderprogram.client;

import com.lirxowo.shaderprogram.entity.GlassSphereEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class GlassSphereRenderer extends EntityRenderer<GlassSphereEntity> {

    public GlassSphereRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(GlassSphereEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/experience_orb.png");
    }

    @Override
    public void render(GlassSphereEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // No-op: actual rendering happens at AFTER_WEATHER stage via ClientEvents
        // so the captured framebuffer includes water, clouds, and weather effects.
    }
}

package com.lirxowo.shaderprogram.client;

import com.lirxowo.shaderprogram.entity.BlackHoleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/experience_orb.png");
    }

    @Override
    public void render(BlackHoleEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 实际渲染在 AFTER_WEATHER 阶段通过 ClientEvents 执行
    }
}

package com.lirxowo.shaderprogram.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public class DissolveEffect {

    private static boolean enabled = false;
    private static float progress = 0.0f;

    // 以20tick/秒的速率，约1秒完成完整溶解
    private static final float SPEED = 0.01f;

    public static void toggle() {
        enabled = !enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static float getProgress() {
        return progress;
    }

    public static void tick() {
        if (enabled) {
            progress = Math.min(1.0f, progress + SPEED);
        } else {
            progress = Math.max(0.0f, progress - SPEED);
        }
    }

    // 方块溶解uniform（V键切换）
    private static boolean wasActive = false;

    public static void applyUniforms() {
        boolean isActive = progress > 0.001f;

        // 仅在完全空闲时跳过（当前未激活，上一帧也未激活）
        if (!isActive && !wasActive) return;
        wasActive = isActive;

        setUniform(GameRenderer.getRendertypeSolidShader(), progress);
        setUniform(GameRenderer.getRendertypeCutoutShader(), progress);
        setUniform(GameRenderer.getRendertypeCutoutMippedShader(), progress);
        setUniform(GameRenderer.getRendertypeTranslucentShader(), progress);
    }

    // 实体溶解uniform（逐实体，用于死亡动画）
    public static void setEntityDissolveProgress(float entityProgress) {
        setUniform(GameRenderer.getRendertypeEntitySolidShader(), entityProgress);
        setUniform(GameRenderer.getRendertypeEntityCutoutShader(), entityProgress);
        setUniform(GameRenderer.getRendertypeEntityCutoutNoCullShader(), entityProgress);
        setUniform(GameRenderer.getRendertypeEntityTranslucentShader(), entityProgress);
    }

    private static void setUniform(@Nullable ShaderInstance shader, float value) {
        if (shader == null) return;
        var uniform = shader.getUniform("DissolveProgress");
        if (uniform != null) {
            uniform.set(value);
        }
    }
}

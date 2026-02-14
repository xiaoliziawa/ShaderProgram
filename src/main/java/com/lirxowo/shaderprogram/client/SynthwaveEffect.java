package com.lirxowo.shaderprogram.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public class SynthwaveEffect {

    private static boolean enabled = false;

    // 波纹半径（方块单位），0 = 无效果
    private static float radius = 0.0f;

    // 最大半径，覆盖最远渲染距离（32区块 = 512方块 + 余量）
    private static final float MAX_RADIUS = 600.0f;

    // 扩展速度：方块/tick（20tick/秒 = 60方块/秒）
    private static final float SPEED = 3.0f;

    // 波纹起始点（玩家按V时的XZ坐标）
    private static float originX = 0.0f;
    private static float originZ = 0.0f;

    public static void toggle() {
        enabled = !enabled;
        // 无论开启还是关闭，都以玩家当前位置为波纹中心
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            originX = (float) mc.player.getX();
            originZ = (float) mc.player.getZ();
        }
        if (!enabled) {
            // 关闭时：从当前覆盖的最大范围开始收缩
            // radius 保持当前值，tick() 会逐渐减小
        } else {
            // 开启时：从0开始扩展
            radius = 0.0f;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static float getRadius() {
        return radius;
    }

    public static void tick() {
        if (enabled) {
            radius = Math.min(MAX_RADIUS, radius + SPEED);
        } else {
            radius = Math.max(0.0f, radius - SPEED);
        }
    }

    // --- 方块合成波uniform（V键切换）---
    private static boolean wasActive = false;

    public static void applyUniforms() {
        boolean isActive = radius > 0.01f;

        if (!isActive && !wasActive) return;
        wasActive = isActive;

        // chunkPos in shader = Position + ChunkOffset = camera-relative world pos
        // So we must pass wave origin in camera-relative coords too
        Minecraft mc = Minecraft.getInstance();
        float camX = 0f, camZ = 0f;
        if (mc.gameRenderer != null && mc.gameRenderer.getMainCamera() != null) {
            var camPos = mc.gameRenderer.getMainCamera().getPosition();
            camX = (float) camPos.x;
            camZ = (float) camPos.z;
        }
        float relOriginX = originX - camX;
        float relOriginZ = originZ - camZ;

        ShaderInstance[] shaders = {
                GameRenderer.getRendertypeSolidShader(),
                GameRenderer.getRendertypeCutoutShader(),
                GameRenderer.getRendertypeCutoutMippedShader(),
                GameRenderer.getRendertypeTranslucentShader()
        };

        for (ShaderInstance shader : shaders) {
            setFloatUniform(shader, "SynthwaveRadius", radius);
            setFloatUniform(shader, "WaveOriginX", relOriginX);
            setFloatUniform(shader, "WaveOriginZ", relOriginZ);
            setFloatUniform(shader, "CameraPosX", camX);
            setFloatUniform(shader, "CameraPosZ", camZ);
        }
    }

    // --- 实体溶解uniform（死亡动画，保持不变）---
    public static void setEntityDissolveProgress(float entityProgress) {
        setFloatUniform(GameRenderer.getRendertypeEntitySolidShader(), "DissolveProgress", entityProgress);
        setFloatUniform(GameRenderer.getRendertypeEntityCutoutShader(), "DissolveProgress", entityProgress);
        setFloatUniform(GameRenderer.getRendertypeEntityCutoutNoCullShader(), "DissolveProgress", entityProgress);
        setFloatUniform(GameRenderer.getRendertypeEntityTranslucentShader(), "DissolveProgress", entityProgress);
    }

    private static void setFloatUniform(@Nullable ShaderInstance shader, String name, float value) {
        if (shader == null) return;
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }
}

package com.lirxowo.shaderprogram.client;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public class TileEffect {

    private static final float[] TILE_OPTIONS = {1.0f, 2.0f, 4.0f, 8.0f};
    private static int currentIndex = 0;

    public static void cycle() {
        currentIndex = (currentIndex + 1) % TILE_OPTIONS.length;
    }

    public static float getTileCount() {
        return TILE_OPTIONS[currentIndex];
    }

    public static void applyUniforms() {
        float count = getTileCount();
        setUniform(GameRenderer.getRendertypeSolidShader(), count);
        setUniform(GameRenderer.getRendertypeCutoutShader(), count);
        setUniform(GameRenderer.getRendertypeCutoutMippedShader(), count);
        setUniform(GameRenderer.getRendertypeTranslucentShader(), count);
    }

    private static void setUniform(@Nullable ShaderInstance shader, float value) {
        if (shader == null) return;
        var uniform = shader.getUniform("TileCount");
        if (uniform != null) {
            uniform.set(value);
        }
    }
}

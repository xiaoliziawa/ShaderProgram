package com.lirxowo.shaderprogram.client;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import javax.annotation.Nullable;

public class CosmicTooltipShader {

    @Nullable
    public static ShaderInstance instance;

    @Nullable
    private static Uniform timeUniform;
    @Nullable
    private static Uniform screenSizeUniform;
    @Nullable
    private static Uniform cosmicUvsUniform;

    private static final float[] COSMIC_UV_DATA = new float[40];

    public static int renderTick;
    public static float partialTick;

    public static void onShaderLoaded(ShaderInstance shader) {
        instance = shader;
        timeUniform = shader.getUniform("time");
        screenSizeUniform = shader.getUniform("ScreenSize");
        cosmicUvsUniform = shader.getUniform("cosmicuvs");
    }

    public static void tick() {
        if (!Minecraft.getInstance().isPaused()) {
            renderTick++;
        }
    }

    public static void updateCosmicUVs() {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < 10; i++) {
            TextureAtlasSprite sprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(ResourceLocation.fromNamespaceAndPath("shaderprogram", "misc/cosmic_" + i));
            COSMIC_UV_DATA[i * 4] = sprite.getU0();
            COSMIC_UV_DATA[i * 4 + 1] = sprite.getV0();
            COSMIC_UV_DATA[i * 4 + 2] = sprite.getU1();
            COSMIC_UV_DATA[i * 4 + 3] = sprite.getV1();
        }
        if (cosmicUvsUniform != null) {
            cosmicUvsUniform.set(COSMIC_UV_DATA);
        }
    }

    public static void applyUniforms(float screenWidth, float screenHeight) {
        if (timeUniform != null) {
            timeUniform.set((float) renderTick + partialTick);
        }
        if (screenSizeUniform != null) {
            screenSizeUniform.set(screenWidth, screenHeight);
        }
        updateCosmicUVs();
    }
}

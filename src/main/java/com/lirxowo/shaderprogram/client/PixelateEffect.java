package com.lirxowo.shaderprogram.client;

import com.lirxowo.shaderprogram.Shaderprogram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

public class PixelateEffect {
    private static boolean active = false;
    private static float pixelSize = 0.0f;
    private static final float MAX_PIXEL_SIZE = 16.0f;
    private static final float GROW_SPEED = 0.8f;
    private static final float SHRINK_SPEED = 1.6f;
    private static PostChain postChain = null;
    private static boolean shrinking = false;

    public static boolean isActive() {
        return active;
    }

    public static void toggle() {
        if (!active) {
            activate();
        } else {
            shrinking = true;
        }
    }

    private static void activate() {
        active = true;
        shrinking = false;
        pixelSize = 1.0f;

        Minecraft mc = Minecraft.getInstance();

        try {
            ResourceLocation shaderLoc = ResourceLocation.fromNamespaceAndPath(
                    Shaderprogram.MODID, "shaders/post/pixelate.json");
            mc.gameRenderer.loadEffect(shaderLoc);
            postChain = mc.gameRenderer.currentEffect();
        } catch (Exception e) {
            Shaderprogram.LOGGER.error("Failed to load pixelate shader", e);
            active = false;
        }
    }

    private static void deactivate() {
        active = false;
        shrinking = false;
        pixelSize = 0.0f;
        postChain = null;

        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.shutdownEffect();
    }

    public static void tick() {
        if (!active) return;

        if (shrinking) {
            pixelSize -= SHRINK_SPEED;
            if (pixelSize <= 1.0f) {
                deactivate();
                return;
            }
        } else {
            if (pixelSize < MAX_PIXEL_SIZE) {
                pixelSize = Math.min(pixelSize + GROW_SPEED, MAX_PIXEL_SIZE);
            }
        }
    }

    public static void applyPostUniforms() {
        if (!active || postChain == null) return;

        PostChain current = Minecraft.getInstance().gameRenderer.currentEffect();
        if (current != postChain) {
            postChain = current;
            if (postChain == null) {
                active = false;
                pixelSize = 0.0f;
                return;
            }
        }

        postChain.setUniform("PixelSize", pixelSize);
    }
}

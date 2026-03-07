package com.lirxowo.shaderprogram.client;

import com.lirxowo.shaderprogram.Shaderprogram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

public class WarpEffect {
    private static boolean active = false;
    private static float warpTime = 0.0f;
    private static final float MAX_WARP_TIME = 2.0f;
    private static final float GROW_SPEED = 0.03f;
    private static final float SHRINK_SPEED = 0.06f;
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
        warpTime = 0.01f;

        Minecraft mc = Minecraft.getInstance();

        try {
            ResourceLocation shaderLoc = ResourceLocation.fromNamespaceAndPath(
                    Shaderprogram.MODID, "shaders/post/warp.json");
            mc.gameRenderer.loadEffect(shaderLoc);
            postChain = mc.gameRenderer.currentEffect();
        } catch (Exception e) {
            Shaderprogram.LOGGER.error("Failed to load warp shader", e);
            active = false;
        }
    }

    private static void deactivate() {
        active = false;
        shrinking = false;
        warpTime = 0.0f;
        postChain = null;

        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.shutdownEffect();
    }

    public static void tick() {
        if (!active) return;

        if (shrinking) {
            warpTime -= SHRINK_SPEED;
            if (warpTime <= 0.0f) {
                deactivate();
                return;
            }
        } else {
            if (warpTime < MAX_WARP_TIME) {
                warpTime = Math.min(warpTime + GROW_SPEED, MAX_WARP_TIME);
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
                warpTime = 0.0f;
                return;
            }
        }

        postChain.setUniform("WarpTime", warpTime);
    }
}

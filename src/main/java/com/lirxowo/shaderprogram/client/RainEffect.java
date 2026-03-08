package com.lirxowo.shaderprogram.client;

import com.lirxowo.shaderprogram.Shaderprogram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class RainEffect {
    private static boolean active = false;
    private static float rainTime = 0.0f;
    private static float rainIntensity = 0.0f;
    private static PostChain postChain = null;
    private static final float FADE_SPEED = 0.02f;

    public static boolean isActive() {
        return active;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (active) deactivate();
            return;
        }

        boolean isRaining = mc.level.isRaining();
        BlockPos playerHead = mc.player.blockPosition().above();
        boolean exposedToRain = isRaining && mc.level.isRainingAt(playerHead);
        float targetIntensity = exposedToRain ? mc.level.getRainLevel(1.0f) : 0.0f;

        if (exposedToRain && !active) {
            if (mc.gameRenderer.currentEffect() == null) {
                activate();
            }
        }

        if (active) {
            if (rainIntensity < targetIntensity) {
                rainIntensity = Math.min(rainIntensity + FADE_SPEED, targetIntensity);
            } else if (rainIntensity > targetIntensity) {
                rainIntensity = Math.max(rainIntensity - FADE_SPEED, targetIntensity);
            }

            rainTime += 0.05f;
            if (rainTime > 10000.0f) {
                rainTime -= 10000.0f;
            }

            if (!exposedToRain && rainIntensity <= 0.0f) {
                deactivate();
            }
        }
    }

    private static void activate() {
        active = true;
        rainIntensity = 0.0f;

        Minecraft mc = Minecraft.getInstance();
        try {
            ResourceLocation shaderLoc = ResourceLocation.fromNamespaceAndPath(
                    Shaderprogram.MODID, "shaders/post/rain.json");
            mc.gameRenderer.loadEffect(shaderLoc);
            postChain = mc.gameRenderer.currentEffect();
        } catch (Exception e) {
            Shaderprogram.LOGGER.error("Failed to load rain shader", e);
            active = false;
        }
    }

    public static void deactivate() {
        if (!active) return;
        active = false;
        rainIntensity = 0.0f;

        Minecraft mc = Minecraft.getInstance();
        PostChain current = mc.gameRenderer.currentEffect();
        if (current == postChain && postChain != null) {
            mc.gameRenderer.shutdownEffect();
        }
        postChain = null;
    }

    public static void applyPostUniforms() {
        if (!active || postChain == null) return;

        PostChain current = Minecraft.getInstance().gameRenderer.currentEffect();
        if (current != postChain) {
            active = false;
            postChain = null;
            rainIntensity = 0.0f;
            return;
        }

        postChain.setUniform("RainTime", rainTime);
        postChain.setUniform("RainIntensity", rainIntensity);
    }
}

package com.lirxowo.shaderprogram.client;

import com.lirxowo.shaderprogram.Shaderprogram;
import com.lirxowo.shaderprogram.network.TimeStopPacket;
import com.lirxowo.shaderprogram.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class TimeStopEffect {
    private static boolean active = false;
    private static float rewindTime = 0.0f;
    private static PostChain postChain = null;

    public static boolean isActive() {
        return active;
    }

    public static void toggle(Player player) {
        if (!active) {
            activate(player);
        } else {
            deactivate();
        }
    }

    private static void activate(Player player) {
        active = true;
        rewindTime = 0.0f;

        Minecraft mc = Minecraft.getInstance();

        try {
            ResourceLocation shaderLoc = ResourceLocation.fromNamespaceAndPath(
                    Shaderprogram.MODID, "shaders/post/the_world.json");
            mc.gameRenderer.loadEffect(shaderLoc);
            postChain = mc.gameRenderer.currentEffect();
        } catch (Exception e) {
            Shaderprogram.LOGGER.error("Failed to load time stop shader", e);
            active = false;
            return;
        }

        player.playSound(ModSounds.TIME_STOP.get(), 1.0f, 1.0f);

        PacketDistributor.sendToServer(new TimeStopPacket());
    }

    private static void deactivate() {
        active = false;
        rewindTime = 0.0f;
        postChain = null;

        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.shutdownEffect();

        PacketDistributor.sendToServer(new TimeStopPacket());
    }

    public static void tick() {
        if (!active) return;
        rewindTime += 0.045f;
    }

    public static void applyPostUniforms() {
        if (!active || postChain == null) return;

        PostChain current = Minecraft.getInstance().gameRenderer.currentEffect();
        if (current != postChain) {
            postChain = current;
            if (postChain == null) {
                active = false;
                rewindTime = 0.0f;
                return;
            }
        }

        postChain.setUniform("RewindTime", rewindTime);
    }
}

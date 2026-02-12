package com.lirxowo.shaderprogram.item;

import com.lirxowo.shaderprogram.client.TimeStopEffect;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TimeClockClientHandler {
    public static void toggle(Player player) {
        TimeStopEffect.toggle(player);
    }
}

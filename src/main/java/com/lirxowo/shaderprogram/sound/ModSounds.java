package com.lirxowo.shaderprogram.sound;

import com.lirxowo.shaderprogram.Shaderprogram;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Shaderprogram.MODID);

    public static final Supplier<SoundEvent> TIME_STOP = SOUND_EVENTS.register("time_stop",
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(Shaderprogram.MODID, "time_stop")));
}

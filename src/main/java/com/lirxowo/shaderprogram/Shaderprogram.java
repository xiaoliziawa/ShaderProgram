package com.lirxowo.shaderprogram;

import com.lirxowo.shaderprogram.entity.ModEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Shaderprogram.MODID)
public class Shaderprogram {
    public static final String MODID = "shaderprogram";

    public Shaderprogram(IEventBus modBus) {
        ModEntities.ENTITY_TYPES.register(modBus);
    }
}

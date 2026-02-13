package com.lirxowo.shaderprogram;

import com.lirxowo.shaderprogram.entity.ModEntities;
import com.lirxowo.shaderprogram.item.ModItems;
import com.lirxowo.shaderprogram.sound.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Shaderprogram.MODID)
public class Shaderprogram {
    public static final String MODID = "shaderprogram";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public Shaderprogram(IEventBus modBus) {
        ModEntities.ENTITY_TYPES.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.CREATIVE_TABS.register(modBus);
    }
}

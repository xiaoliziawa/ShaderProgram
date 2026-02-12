package com.lirxowo.shaderprogram;

import com.lirxowo.shaderprogram.entity.ModEntities;
import com.lirxowo.shaderprogram.item.ModItems;
import com.lirxowo.shaderprogram.network.TimeStopPacket;
import com.lirxowo.shaderprogram.sound.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
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

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToServer(
                    TimeStopPacket.TYPE,
                    TimeStopPacket.STREAM_CODEC,
                    TimeStopPacket::handle
            );
        }
    }
}

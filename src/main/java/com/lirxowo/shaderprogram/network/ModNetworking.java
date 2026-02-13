package com.lirxowo.shaderprogram.network;

import com.lirxowo.shaderprogram.Shaderprogram;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Shaderprogram.MODID)
public class ModNetworking {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                TimeStopPacket.TYPE,
                TimeStopPacket.STREAM_CODEC,
                TimeStopPacket::handle
        );
        registrar.playToClient(
                EnchantedBlockSyncPacket.TYPE,
                EnchantedBlockSyncPacket.STREAM_CODEC,
                EnchantedBlockSyncPacket::handle
        );
        registrar.playToClient(
                EnchantedBlockUpdatePacket.TYPE,
                EnchantedBlockUpdatePacket.STREAM_CODEC,
                EnchantedBlockUpdatePacket::handle
        );
    }
}

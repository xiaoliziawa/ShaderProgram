package com.lirxowo.shaderprogram.network;

import com.lirxowo.shaderprogram.Shaderprogram;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TimeStopPacket() implements CustomPacketPayload {

    public static final Type<TimeStopPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Shaderprogram.MODID, "time_stop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TimeStopPacket> STREAM_CODEC =
            StreamCodec.unit(new TimeStopPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TimeStopPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverPlayer.getServer();
            if (server != null) {
                ServerTickRateManager tickRateManager = server.tickRateManager();
                boolean nowFrozen = !tickRateManager.isFrozen();

                if (nowFrozen) {
                    if (tickRateManager.isSprinting()) {
                        tickRateManager.stopSprinting();
                    }
                    if (tickRateManager.isSteppingForward()) {
                        tickRateManager.stopStepping();
                    }
                }

                tickRateManager.setFrozen(nowFrozen);
            }
        }
    }
}

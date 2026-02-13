package com.lirxowo.shaderprogram.network;

import com.lirxowo.shaderprogram.Shaderprogram;
import com.lirxowo.shaderprogram.client.EnchantedBlockClientStore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record EnchantedBlockSyncPacket(Map<BlockPos, ItemEnchantments> enchantments) implements CustomPacketPayload {

    public static final Type<EnchantedBlockSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Shaderprogram.MODID, "enchanted_block_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantedBlockSyncPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public EnchantedBlockSyncPacket decode(RegistryFriendlyByteBuf buf) {
                    int size = buf.readVarInt();
                    Map<BlockPos, ItemEnchantments> map = new HashMap<>();
                    for (int i = 0; i < size; i++) {
                        BlockPos pos = BlockPos.of(buf.readLong());
                        ItemEnchantments enchants = ItemEnchantments.STREAM_CODEC.decode(buf);
                        map.put(pos, enchants);
                    }
                    return new EnchantedBlockSyncPacket(map);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, EnchantedBlockSyncPacket pkt) {
                    buf.writeVarInt(pkt.enchantments().size());
                    for (Map.Entry<BlockPos, ItemEnchantments> entry : pkt.enchantments().entrySet()) {
                        buf.writeLong(entry.getKey().asLong());
                        ItemEnchantments.STREAM_CODEC.encode(buf, entry.getValue());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EnchantedBlockSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> EnchantedBlockClientStore.replaceAll(packet.enchantments()));
    }
}

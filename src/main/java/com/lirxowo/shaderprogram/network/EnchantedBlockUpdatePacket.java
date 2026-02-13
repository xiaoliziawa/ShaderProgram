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

import javax.annotation.Nullable;

public record EnchantedBlockUpdatePacket(BlockPos pos, boolean add,
                                         @Nullable ItemEnchantments enchantments) implements CustomPacketPayload {

    public static final Type<EnchantedBlockUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Shaderprogram.MODID, "enchanted_block_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantedBlockUpdatePacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public EnchantedBlockUpdatePacket decode(RegistryFriendlyByteBuf buf) {
                    BlockPos pos = BlockPos.of(buf.readLong());
                    boolean add = buf.readBoolean();
                    ItemEnchantments enchants = null;
                    if (add) {
                        enchants = ItemEnchantments.STREAM_CODEC.decode(buf);
                    }
                    return new EnchantedBlockUpdatePacket(pos, add, enchants);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, EnchantedBlockUpdatePacket pkt) {
                    buf.writeLong(pkt.pos().asLong());
                    buf.writeBoolean(pkt.add());
                    if (pkt.add() && pkt.enchantments() != null) {
                        ItemEnchantments.STREAM_CODEC.encode(buf, pkt.enchantments());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EnchantedBlockUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.add() && packet.enchantments() != null) {
                EnchantedBlockClientStore.put(packet.pos(), packet.enchantments());
            } else {
                EnchantedBlockClientStore.remove(packet.pos());
            }
        });
    }
}

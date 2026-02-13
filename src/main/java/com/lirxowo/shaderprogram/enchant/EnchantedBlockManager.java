package com.lirxowo.shaderprogram.enchant;

import com.lirxowo.shaderprogram.network.EnchantedBlockSyncPacket;
import com.lirxowo.shaderprogram.network.EnchantedBlockUpdatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class EnchantedBlockManager {

    public static void addEnchantedBlock(ServerLevel level, BlockPos pos, ItemEnchantments enchantments) {
        EnchantedBlockData data = EnchantedBlockData.get(level);
        data.put(pos, enchantments);
        EnchantedBlockUpdatePacket packet = new EnchantedBlockUpdatePacket(pos.immutable(), true, enchantments);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    @Nullable
    public static ItemEnchantments removeEnchantedBlock(ServerLevel level, BlockPos pos) {
        EnchantedBlockData data = EnchantedBlockData.get(level);
        ItemEnchantments removed = data.remove(pos);
        if (removed != null) {
            EnchantedBlockUpdatePacket packet = new EnchantedBlockUpdatePacket(pos.immutable(), false, null);
            for (ServerPlayer player : level.players()) {
                PacketDistributor.sendToPlayer(player, packet);
            }
        }
        return removed;
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            EnchantedBlockData data = EnchantedBlockData.get(serverLevel);
            PacketDistributor.sendToPlayer(player, new EnchantedBlockSyncPacket(data.getAll()));
        }
    }
}

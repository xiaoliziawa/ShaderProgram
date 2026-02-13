package com.lirxowo.shaderprogram.enchant;

import com.lirxowo.shaderprogram.Shaderprogram;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Shaderprogram.MODID)
public class EnchantedBlockEvents {

    // Temporary storage: BreakEvent fires before BlockDropsEvent,
    // so we stash enchantments here for the drops handler to pick up.
    private static final Map<BlockPos, ItemEnchantments> pendingDropEnchants = new HashMap<>();

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack heldItem = player.getMainHandItem();
        ItemEnchantments enchants = heldItem.get(DataComponents.ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) return;

        BlockPos pos = event.getPos();
        EnchantedBlockManager.addEnchantedBlock(level, pos, enchants);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        EnchantedBlockData data = EnchantedBlockData.get(level);
        ItemEnchantments enchants = data.get(pos);
        if (enchants == null) return;

        // Stash for BlockDropsEvent, then remove + notify clients
        pendingDropEnchants.put(pos.immutable(), enchants);
        EnchantedBlockManager.removeEnchantedBlock(level, pos);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemEnchantments enchants = pendingDropEnchants.remove(event.getPos());
        if (enchants == null) return;

        for (ItemEntity itemEntity : event.getDrops()) {
            ItemStack stack = itemEntity.getItem();
            if (stack.getItem() instanceof BlockItem) {
                stack.set(DataComponents.ENCHANTMENTS, enchants);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EnchantedBlockManager.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EnchantedBlockManager.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            EnchantedBlockManager.syncToPlayer(player);
        }
    }
}

package com.lirxowo.shaderprogram.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnchantedBlockClientStore {

    private static final ConcurrentHashMap<BlockPos, ItemEnchantments> store = new ConcurrentHashMap<>();

    public static void replaceAll(Map<BlockPos, ItemEnchantments> data) {
        store.clear();
        store.putAll(data);
    }

    public static void put(BlockPos pos, ItemEnchantments enchants) {
        store.put(pos.immutable(), enchants);
    }

    public static void remove(BlockPos pos) {
        store.remove(pos);
    }

    public static Map<BlockPos, ItemEnchantments> getAll() {
        return Collections.unmodifiableMap(store);
    }

    public static void clear() {
        store.clear();
    }
}

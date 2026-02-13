package com.lirxowo.shaderprogram.enchant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EnchantedBlockData extends SavedData {

    private static final String DATA_NAME = "enchanted_blocks";
    private final Map<BlockPos, ItemEnchantments> enchantments = new HashMap<>();

    public EnchantedBlockData() {
    }

    public static EnchantedBlockData load(CompoundTag tag, HolderLookup.Provider registries) {
        EnchantedBlockData data = new EnchantedBlockData();
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            Tag enchTag = entry.get("enchantments");
            if (enchTag != null) {
                ItemEnchantments enchants = ItemEnchantments.CODEC.parse(ops, enchTag)
                        .resultOrPartial(s -> {})
                        .orElse(ItemEnchantments.EMPTY);
                if (!enchants.isEmpty()) {
                    data.enchantments.put(pos, enchants);
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, ItemEnchantments> entry : enchantments.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("pos", entry.getKey().asLong());
            ItemEnchantments.CODEC.encodeStart(ops, entry.getValue())
                    .resultOrPartial(s -> {})
                    .ifPresent(nbt -> entryTag.put("enchantments", nbt));
            list.add(entryTag);
        }
        tag.put("entries", list);
        return tag;
    }

    public void put(BlockPos pos, ItemEnchantments enchants) {
        enchantments.put(pos.immutable(), enchants);
        setDirty();
    }

    @Nullable
    public ItemEnchantments remove(BlockPos pos) {
        ItemEnchantments removed = enchantments.remove(pos);
        if (removed != null) setDirty();
        return removed;
    }

    @Nullable
    public ItemEnchantments get(BlockPos pos) {
        return enchantments.get(pos);
    }

    public Map<BlockPos, ItemEnchantments> getAll() {
        return Collections.unmodifiableMap(enchantments);
    }

    public static EnchantedBlockData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EnchantedBlockData::new, EnchantedBlockData::load),
                DATA_NAME
        );
    }
}

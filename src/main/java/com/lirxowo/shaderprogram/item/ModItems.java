package com.lirxowo.shaderprogram.item;

import com.lirxowo.shaderprogram.Shaderprogram;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Shaderprogram.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Shaderprogram.MODID);

    public static final Supplier<Item> TIME_CLOCK = ITEMS.register("time_clock",
            () -> new TimeClockItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<CreativeModeTab> SP_TAB = CREATIVE_TABS.register("shaderprogram_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("tab.shaderprogram.name"))
                    .icon(() -> TIME_CLOCK.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(TIME_CLOCK.get());
                    })
                    .build());
}

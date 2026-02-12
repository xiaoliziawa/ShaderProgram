package com.lirxowo.shaderprogram.entity;

import com.lirxowo.shaderprogram.Shaderprogram;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Shaderprogram.MODID);

    public static final Supplier<EntityType<GlassSphereEntity>> GLASS_SPHERE =
            ENTITY_TYPES.register("glass_sphere", () ->
                    EntityType.Builder.of(GlassSphereEntity::new, MobCategory.MISC)
                            .sized(5.0f, 5.0f)
                            .clientTrackingRange(10)
                            .build("glass_sphere")
            );
}

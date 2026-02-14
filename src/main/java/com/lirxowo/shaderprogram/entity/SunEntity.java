package com.lirxowo.shaderprogram.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SunEntity extends Entity {

    // 轨道参数
    private static final double ORBIT_RADIUS = 1.5;
    private static final double HOVER_HEIGHT = 4;   // 肩膀高度（相对于眼睛）
    private static final double ORBIT_SPEED = 0.04;
    private static final double BOB_SPEED = 0.03;
    private static final double BOB_AMPLITUDE = 0.25;
    private static final double FOLLOW_SPEED = 0.1;
    private static final double SEARCH_RANGE = 32.0;
    private static final double MIN_SEPARATION = 5.0;  // 与黑洞的最小间距
    private static final double REPEL_STRENGTH = 2.0;   // 斥力强度

    public SunEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();

        Player nearest = level().getNearestPlayer(this, SEARCH_RANGE);
        if (nearest == null) return;

        double time = tickCount * ORBIT_SPEED;

        // 与黑洞相位差 π，避免重叠
        double targetX = nearest.getX() + Math.cos(time + Math.PI) * ORBIT_RADIUS;
        double targetY = nearest.getY() + nearest.getEyeHeight() + HOVER_HEIGHT
                + Math.sin(tickCount * BOB_SPEED) * BOB_AMPLITUDE;
        double targetZ = nearest.getZ() + Math.sin(time + Math.PI) * ORBIT_RADIUS;

        double newX = getX() + (targetX - getX()) * FOLLOW_SPEED;
        double newY = getY() + (targetY - getY()) * FOLLOW_SPEED;
        double newZ = getZ() + (targetZ - getZ()) * FOLLOW_SPEED;

        // 与黑洞实体互相避让
        AABB searchArea = AABB.ofSize(nearest.position(), SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE);
        List<BlackHoleEntity> blackHoles = level().getEntitiesOfClass(BlackHoleEntity.class, searchArea);
        for (BlackHoleEntity bh : blackHoles) {
            double dx = newX - bh.getX();
            double dz = newZ - bh.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < MIN_SEPARATION && dist > 0.01) {
                double repel = (MIN_SEPARATION - dist) / dist * REPEL_STRENGTH;
                newX += dx * repel;
                newZ += dz * repel;
            }
        }

        setPos(newX, newY, newZ);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}

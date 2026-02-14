package com.lirxowo.shaderprogram.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BlackHoleEntity extends Entity {

    // 轨道参数
    private static final double ORBIT_RADIUS = 1.5;   // 环绕半径（blocks）
    private static final double HOVER_HEIGHT = 4;   // 肩膀高度（相对于眼睛）
    private static final double ORBIT_SPEED = 0.05;    // 公转角速度
    private static final double BOB_SPEED = 0.035;     // 上下浮动频率
    private static final double BOB_AMPLITUDE = 0.3;   // 上下浮动幅度
    private static final double FOLLOW_SPEED = 0.1;    // 跟随插值速度（越小越丝滑）
    private static final double SEARCH_RANGE = 32.0;   // 搜索玩家的范围
    private static final double MIN_SEPARATION = 5.0;  // 与太阳的最小间距
    private static final double REPEL_STRENGTH = 2.0;  // 斥力强度

    public BlackHoleEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();

        Player nearest = level().getNearestPlayer(this, SEARCH_RANGE);
        if (nearest == null) return;

        // 目标位置：玩家头顶 + 圆形轨道 + 上下浮动
        double time = tickCount * ORBIT_SPEED;

        double targetX = nearest.getX() + Math.cos(time) * ORBIT_RADIUS;
        double targetY = nearest.getY() + nearest.getEyeHeight() + HOVER_HEIGHT
                + Math.sin(tickCount * BOB_SPEED) * BOB_AMPLITUDE;
        double targetZ = nearest.getZ() + Math.sin(time) * ORBIT_RADIUS;

        // 平滑插值到目标位置
        double newX = getX() + (targetX - getX()) * FOLLOW_SPEED;
        double newY = getY() + (targetY - getY()) * FOLLOW_SPEED;
        double newZ = getZ() + (targetZ - getZ()) * FOLLOW_SPEED;

        // 与太阳实体互相避让
        AABB searchArea = AABB.ofSize(nearest.position(), SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE);
        List<SunEntity> suns = level().getEntitiesOfClass(SunEntity.class, searchArea);
        for (SunEntity sun : suns) {
            double dx = newX - sun.getX();
            double dz = newZ - sun.getZ();
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

package com.poisoncircle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;

/** Server-owned visual wall. It has no hitbox, collision, AI, or gameplay damage. */
public final class PoisonCircleVisualEntity extends Entity {
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(PoisonCircleVisualEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ANGLE = SynchedEntityData.defineId(PoisonCircleVisualEntity.class, EntityDataSerializers.FLOAT);

    public PoisonCircleVisualEntity(EntityType<? extends PoisonCircleVisualEntity> type, Level level) { super(type, level); noPhysics = true; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(WIDTH, 0.0F); builder.define(ANGLE, 0.0F); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    public void updateVisual(PoisonCircleSegmentLayout.Position position, float width) {
        setPos(position.x(), level().getMinBuildHeight(), position.z());
        entityData.set(WIDTH, width);
        entityData.set(ANGLE, (float) position.angle());
    }
    public float width() { return entityData.get(WIDTH); }
    public float angle() { return entityData.get(ANGLE); }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 1_048_576.0; }
    @Override public PushReaction getPistonPushReaction() { return PushReaction.IGNORE; }
    @Override public void tick() { }
}

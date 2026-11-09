package com.poisoncircle;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Server-authoritative aircraft carrier. Rendering is supplied by the client source set. */
public class AirbornePlaneEntity extends Entity {
    private Vec3 routeVelocity = Vec3.ZERO;
    private float routeYaw;
    private double initialSpeed;
    private int routeTicks;
    private boolean released;
    private int flightAge;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DOOR=net.minecraft.network.syncher.SynchedEntityData.defineId(AirbornePlaneEntity.class,net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    public void setDoorProgress(float value){entityData.set(DOOR,Math.max(0,Math.min(1,value)));}
    public float doorProgress(float partialTick){return Math.min(1,entityData.get(DOOR)+(entityData.get(DOOR)>0?partialTick/30F:0));}
    public void continueAfterDrop(){released=true;}
    public boolean hasReachedTarget(){return position().distanceToSqr(new Vec3(target.x,getY(),target.z))<.01&&Math.abs(getY()-cruiseAltitude)<.1;}
    private Vec3 target = Vec3.ZERO;
    private double cruiseAltitude = Double.NaN;
    public void setCruiseAltitude(double altitude) { cruiseAltitude = altitude; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean saveAsPassenger(ValueOutput output) { return false; }

    public AirbornePlaneEntity(EntityType<? extends AirbornePlaneEntity> type, Level level) { super(type, level); noPhysics = true; }
    public void configure(Vec3 velocity, float yaw, int ticks) { configure(velocity, yaw, ticks, Vec3.ZERO); }
    public void configure(Vec3 velocity, float yaw, int ticks, Vec3 target) { routeVelocity = velocity; initialSpeed=Math.hypot(velocity.x,velocity.z);routeYaw = yaw; routeTicks = Math.max(0, ticks); this.target = target; setYRot(yaw); yRotO = yaw; }
    public Vec3 target() { return target; }
    @Override public boolean canAddPassenger(Entity passenger) { return getPassengers().size() < 2; }
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> RIDER_A=net.minecraft.network.syncher.SynchedEntityData.defineId(AirbornePlaneEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> RIDER_B=net.minecraft.network.syncher.SynchedEntityData.defineId(AirbornePlaneEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<org.joml.Vector3fc> FLOOR_A=net.minecraft.network.syncher.SynchedEntityData.defineId(AirbornePlaneEntity.class,net.minecraft.network.syncher.EntityDataSerializers.VECTOR3);
    private static final net.minecraft.network.syncher.EntityDataAccessor<org.joml.Vector3fc> FLOOR_B=net.minecraft.network.syncher.SynchedEntityData.defineId(AirbornePlaneEntity.class,net.minecraft.network.syncher.EntityDataSerializers.VECTOR3);
    public Vec3 cabinPosition(Entity passenger) {
        boolean a=entityData.get(RIDER_A)==passenger.getId()||(entityData.get(RIDER_B)!=passenger.getId()&&getPassengers().indexOf(passenger)==0);
        org.joml.Vector3fc v=entityData.get(a?FLOOR_A:FLOOR_B);return new Vec3(v.x(),v.y(),v.z());
    }
    public void setCabinPosition(Entity passenger,double x,double y,double z) {
        boolean a=entityData.get(RIDER_A)==passenger.getId()||(entityData.get(RIDER_A)==-1&&entityData.get(RIDER_B)!=passenger.getId());
        entityData.set(a?RIDER_A:RIDER_B,passenger.getId());entityData.set(a?FLOOR_A:FLOOR_B,new org.joml.Vector3f((float)x,(float)y,(float)z));
    }
    @Override public Vec3 getPassengerRidingPosition(Entity passenger) {
        return position().add(cabinPosition(passenger).yRot((float)Math.toRadians(-getYRot())));
    }
    @Override protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        // Cabin coordinates are feet positions; vanilla would subtract the avatar's 0.6-block seat attachment.
        Vec3 feet = getPassengerRidingPosition(passenger);
        move.accept(passenger, feet.x, feet.y, feet.z);
    }
    public Vec3 routeVelocity() { return routeVelocity; }
    public float flightYaw() { return routeYaw; }
    public int routeTicks() { return routeTicks; }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide() && (routeTicks > 0 || !getPassengers().isEmpty())) {
            flightAge++;
            double descent=Double.isFinite(cruiseAltitude)?Math.max(-.4,Math.min(0,cruiseAltitude-getY())):0;
            double radians=Math.toRadians(routeYaw),dx=-Math.sin(radians),dz=Math.cos(radians);
            double speed=initialSpeed;
            double remaining=(target.x-getX())*dx+(target.z-getZ())*dz;
            double step=released?speed:AirborneDeparture.approachStep(remaining,speed,flightAge>=100&&Math.abs(cruiseAltitude-getY())<=.4);
            routeVelocity=new Vec3(dx*step,descent,dz*step);setDeltaMovement(routeVelocity);setPos(position().add(routeVelocity));routeTicks=Math.max(0,routeTicks-1);
        }
        else if (!level().isClientSide()) setDeltaMovement(Vec3.ZERO);
    }
    @Override protected net.minecraft.world.entity.InterpolationHandler createInterpolationHandler() {
        // Entity.commonTick advances this once; ticking it here again would double the aircraft motion.
        return net.minecraft.world.entity.LinearInterpolationHandler.create(this, 3);
    }
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { builder.define(DOOR,0F);builder.define(RIDER_A,-1);builder.define(RIDER_B,-1);builder.define(FLOOR_A,new org.joml.Vector3f((float)AirborneDeparture.seat(0).x(),(float)AirborneDeparture.SEAT_FEET_Y,(float)AirborneDeparture.seat(0).z()));builder.define(FLOOR_B,new org.joml.Vector3f((float)AirborneDeparture.seat(1).x(),(float)AirborneDeparture.SEAT_FEET_Y,(float)AirborneDeparture.seat(1).z())); }
    @Override protected void readAdditionalSaveData(ValueInput input) { }
    @Override protected void addAdditionalSaveData(ValueOutput output) { }
}

package com.poisoncircle.mixin;

import com.poisoncircle.AirborneGame;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class AirbornePlayerMovementMixin {
    @Shadow public ServerPlayer player;
    @Shadow public abstract void resetPosition();
    @Shadow private void handlePlayerPositionChange(double x,double y,double z,float yaw,float pitch,boolean onGround,boolean horizontalCollision){throw new AssertionError();}

    @Redirect(method="handleAcceptTeleportPacket",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;absSnapTo(DDDFF)V"))
    private void poisoncircle$keepAuthoritativeTeleportPosition(ServerPlayer target,double x,double y,double z,float yaw,float pitch){
        // Accept the teleport ID and clear vanilla's pending state without rewinding an already moving flight.
        if(!AirborneGame.controlsMovement(target.getUUID()))target.absSnapTo(x,y,z,yaw,pitch);
    }
    @Redirect(method="handleAcceptTeleportPacket",at=@At(value="INVOKE",target="Lnet/minecraft/server/network/ServerGamePacketListenerImpl;handlePlayerPositionChange(DDDFFZZ)V"))
    private void poisoncircle$keepAuthoritativeAckPosition(ServerGamePacketListenerImpl listener,double x,double y,double z,float yaw,float pitch,boolean onGround,boolean horizontalCollision){
        if(AirborneGame.controlsMovement(player.getUUID()))resetPosition();
        else handlePlayerPositionChange(x,y,z,yaw,pitch,onGround,horizontalCollision);
    }
    @Inject(method="handleMovePlayer",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
    private void poisoncircle$movement(ServerboundMovePlayerPacket packet,CallbackInfo ci){
        if(AirborneGame.controlsMovement(player.getUUID()))ci.cancel();
    }
    @Inject(method="handleMoveVehicle",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
    private void poisoncircle$vehicle(ServerboundMoveVehiclePacket packet,CallbackInfo ci){
        if(AirborneGame.controlsMovement(player.getUUID()))ci.cancel();
    }
}

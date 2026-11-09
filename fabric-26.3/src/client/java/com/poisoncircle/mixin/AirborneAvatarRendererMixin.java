package com.poisoncircle.mixin;

import com.poisoncircle.AirborneAvatarPose;
import com.poisoncircle.AirborneFlightClient;
import com.poisoncircle.AirbornePlaneEntity;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
abstract class AirborneAvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void poisoncircle$extractAirbornePose(net.minecraft.world.entity.Avatar entity, AvatarRenderState state, float partialTick, CallbackInfo callback) {
        String phase = AirborneFlightClient.phaseFor(state.id);
        if (!AirborneAvatarPose.controlled(phase)) return;
        boolean aboard = phase.equals("aboard");
        state.isPassenger = aboard;
        state.isCrouching = false;
        state.pose = net.minecraft.world.entity.Pose.STANDING;
        state.isVisuallySwimming = false;
        state.swimAmount = 0;
        state.isFallFlying = phase.equals("diving");
        state.fallFlyingTimeInTicks = state.isFallFlying ? 20 : 0;
        state.shouldApplyFlyingYRot = false;
        if (aboard) state.walkAnimationSpeed = 0;
        if ((aboard || phase.equals("departing")) && entity.getVehicle() instanceof AirbornePlaneEntity plane) {
            float headYaw = state.bodyRot + state.yRot;
            state.bodyRot = plane.getYRot(partialTick) + 180;
            state.yRot = Mth.clamp(Mth.wrapDegrees(headYaw - state.bodyRot), -85, 85);
        }
    }
}

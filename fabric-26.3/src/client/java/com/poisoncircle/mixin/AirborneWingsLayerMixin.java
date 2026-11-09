package com.poisoncircle.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.poisoncircle.AirborneFlightClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WingsLayer.class)
abstract class AirborneWingsLayerMixin {
    @Inject(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",at=@At("HEAD"),cancellable=true)
    private void poisoncircle$hideAirborneWings(PoseStack pose, SubmitNodeCollector collector, int light, HumanoidRenderState state, float yaw, float pitch, CallbackInfo ci) {
        if(state instanceof AvatarRenderState avatar){String phase=AirborneFlightClient.phaseFor(avatar.id);if(phase.equals("diving")||phase.equals("landing"))ci.cancel();}
    }
}

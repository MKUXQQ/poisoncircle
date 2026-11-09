package com.poisoncircle.mixin;

import com.poisoncircle.AirborneAvatarPose;
import com.poisoncircle.AirborneFlightClient;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerCapeModel.class)
abstract class AirborneCapeModelMixin {
    @Shadow @Final private ModelPart cape;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void poisoncircle$attachCapeToTorso(AvatarRenderState state, CallbackInfo callback) {
        if (!AirborneAvatarPose.controlled(AirborneFlightClient.phaseFor(state.id))) return;
        // The cape is a body child; restore its baked local rotation, retaining the torso/glide matrix.
        cape.setRotation(0, (float) Math.PI, 0);
    }
}

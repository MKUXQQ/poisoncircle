package com.poisoncircle.mixin;

import com.poisoncircle.AirborneFlightClient;
import com.poisoncircle.ScanDeviceClient;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
abstract class AirbornePlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void poisoncircle$airbornePose(AvatarRenderState state, CallbackInfo callback) {
        String phase = AirborneFlightClient.phaseFor(state.id);
        if (!com.poisoncircle.AirborneAvatarPose.controlled(phase)) return;
        PlayerModel model = (PlayerModel) (Object) this;
        if (phase.equals("aboard") || phase.equals("departing")) {
            float standing = phase.equals("aboard") ? 0 : AirborneFlightClient.standProgressFor(state.id);
            float seated = com.poisoncircle.AirborneAvatarPose.seatedLegPitch(standing);
            float swing = standing >= 1 ? (float) Math.sin(state.ageInTicks * .5F) * .45F : 0;
            model.rightLeg.xRot = seated + swing;
            model.leftLeg.xRot = seated - swing;
            model.rightLeg.yRot = model.leftLeg.yRot = 0;
            model.rightLeg.zRot = model.leftLeg.zRot = 0;
            model.rightArm.xRot = -.62831855F * (1 - standing) - swing * .6F;
            model.leftArm.xRot = -.62831855F * (1 - standing) + swing * .6F;
            model.rightArm.yRot = model.leftArm.yRot = 0;
            model.rightArm.zRot = model.leftArm.zRot = 0;
            model.body.setRotation(0, 0, 0);
            return;
        }
        if (ScanDeviceClient.isAnimating(state.id)) return;
        if (!phase.equals("diving") && !phase.equals("landing")) return;
        boolean landing = phase.equals("landing");
        model.rightArm.xRot = model.leftArm.xRot = landing ? -.7F : -.25F;
        model.rightArm.zRot = landing ? .35F : .9F;
        model.leftArm.zRot = -model.rightArm.zRot;
        model.rightLeg.xRot = model.leftLeg.xRot = com.poisoncircle.AirborneClientRules.legPitch(landing);
        model.rightLeg.yRot = model.leftLeg.yRot = 0;
        model.rightLeg.zRot = model.leftLeg.zRot = 0;
    }
}

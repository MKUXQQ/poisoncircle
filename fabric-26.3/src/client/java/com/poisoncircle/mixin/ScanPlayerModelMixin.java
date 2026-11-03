package com.poisoncircle.mixin;

import com.poisoncircle.ScanDeviceClient;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
abstract class ScanPlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void poisoncircle$scanPose(AvatarRenderState state, CallbackInfo callback) {
        if (!ScanDeviceClient.isAnimating(state.id)) return;
        PlayerModel model = (PlayerModel) (Object) this;
        float wrist = ScanDeviceClient.wrist(state.ageInTicks - (int) state.ageInTicks);
        model.rightArm.xRot = -1.22F + wrist;
        model.leftArm.xRot = -1.22F - wrist;
        model.rightArm.yRot = -0.20F;
        model.leftArm.yRot = 0.20F;
        model.rightArm.zRot = 0.10F;
        model.leftArm.zRot = -0.10F;
    }
}

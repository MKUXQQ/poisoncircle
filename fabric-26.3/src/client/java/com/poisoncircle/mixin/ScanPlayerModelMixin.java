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
        float age = ScanDeviceClient.age(state.id, state.ageInTicks - (int) state.ageInTicks);
        float rightTap = (float) Math.sin(age * 0.75F);
        float leftTap = (float) Math.sin(age * 0.75F + 2.1F);
        model.rightArm.xRot = -1.12F + rightTap * 0.23F;
        model.leftArm.xRot = -1.30F + leftTap * 0.20F;
        model.rightArm.yRot = -0.25F + rightTap * 0.08F;
        model.leftArm.yRot = 0.25F + leftTap * 0.08F;
        model.rightArm.zRot = 0.12F;
        model.leftArm.zRot = -0.12F;
    }
}

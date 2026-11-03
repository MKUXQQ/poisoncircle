package com.poisoncircle.mixin;

import com.poisoncircle.PoisonCircleClient;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class PoisonCircleCameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract float yRot();
    @Shadow public abstract float xRot();

    // Apply after the camera resets to the entity, before projection and culling are calculated.
    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void poisoncircle$shake(float partialTick, CallbackInfo callback) {
        float yaw = PoisonCircleClient.shakeOffset(true);
        float pitch = PoisonCircleClient.shakeOffset(false);
        if (yaw != 0 || pitch != 0) setRotation(yRot() + yaw, xRot() + pitch);
    }
}

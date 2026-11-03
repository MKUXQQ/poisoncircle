package com.poisoncircle.mixin;

import com.poisoncircle.ScanDeviceClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class ScanMouseLockMixin {
    @Inject(method = "turnPlayer(D)V", at = @At("HEAD"), cancellable = true)
    private void poisoncircle$lockLook(double elapsedTime, CallbackInfo callback) {
        if (ScanDeviceClient.shouldLockMovement()) callback.cancel();
    }
}

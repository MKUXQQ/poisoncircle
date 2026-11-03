package com.poisoncircle.mixin;

import com.poisoncircle.ScanDeviceClient;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
abstract class ScanInputLockMixin {
    @Shadow public Input keyPresses;
    @Shadow protected Vec2 moveVector;

    @Inject(method = "tick", at = @At("TAIL"))
    private void poisoncircle$lockMovement(CallbackInfo callback) {
        if (ScanDeviceClient.shouldLockMovement()) {
            keyPresses = Input.EMPTY;
            moveVector = Vec2.ZERO;
        }
    }
}

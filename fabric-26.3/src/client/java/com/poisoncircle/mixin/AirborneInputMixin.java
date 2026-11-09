package com.poisoncircle.mixin;

import com.poisoncircle.AirborneFlightClient;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
abstract class AirborneInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void poisoncircle$airborneMovement(CallbackInfo callback) {
        if (!AirborneFlightClient.suppressVanillaMovement()) return;
        keyPresses = Input.EMPTY;
        moveVector = Vec2.ZERO;
    }
}

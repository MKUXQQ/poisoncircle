package com.poisoncircle.mixin;

import com.poisoncircle.AirborneFlightClient;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class AirborneTravelMixin {
    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void poisoncircle$controlledTravel(Vec3 input, CallbackInfo callback) {
        if (AirborneFlightClient.moveControlledPlayer((LivingEntity) (Object) this)) callback.cancel();
    }
}

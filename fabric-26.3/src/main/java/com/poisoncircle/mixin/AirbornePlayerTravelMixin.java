package com.poisoncircle.mixin;

import com.poisoncircle.AirborneGame;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class AirbornePlayerTravelMixin {
    @Inject(method="travel(Lnet/minecraft/world/phys/Vec3;)V",at=@At("HEAD"),cancellable=true)
    private void poisoncircle$serverTravel(Vec3 input,CallbackInfo ci){
        if((Object)this instanceof ServerPlayer p&&AirborneGame.controlsMovement(p.getUUID()))ci.cancel();
    }
}

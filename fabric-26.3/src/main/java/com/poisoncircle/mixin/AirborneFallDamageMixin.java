package com.poisoncircle.mixin;

import com.poisoncircle.AirborneGame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class AirborneFallDamageMixin {
    @Inject(method="causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z",at=@At("HEAD"),cancellable=true)
    private void poisoncircle$fall(double distance,float multiplier,DamageSource source,CallbackInfoReturnable<Boolean> ci){
        if((Object)this instanceof ServerPlayer p&&AirborneGame.protectsFall(p.getUUID()))ci.setReturnValue(false);
    }
}

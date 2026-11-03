package com.poisoncircle.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.poisoncircle.ScanDeviceClient;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class ScanFirstPersonHandsMixin {
    @org.spongepowered.asm.mixin.gen.Invoker("renderMapHand")
    protected abstract void poisoncircle$renderMapHand(PoseStack poses, SubmitNodeCollector collector, int light,
                                                        HumanoidArm arm, PlayerRenderState player);

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void poisoncircle$scanHands(float partialTick, PoseStack poses, SubmitNodeCollector collector,
                                       PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands,
                                       CallbackInfo callback) {
        if (player.avatarRenderState == null || !ScanDeviceClient.isAnimating(player.avatarRenderState.id)) return;
        if (!player.avatarRenderState.isInvisible) {
            // Native map-hand transforms supply visible skinned arms, without a fake held item.
            for (HumanoidArm arm : HumanoidArm.values()) {
                float side = arm == HumanoidArm.RIGHT ? 1 : -1;
                float wrist = ScanDeviceClient.wrist(partialTick) * side;
                poses.pushPose();
                poses.translate(0, -0.40F + wrist * 0.25F, -0.72F);
                poses.rotateDegrees(Axis.YP, 90);
                poses.rotateDegrees(Axis.XP, wrist * 55);
                poisoncircle$renderMapHand(poses, collector, player.avatarRenderState.lightCoords, arm, player);
                poses.popPose();
            }
        }
        callback.cancel();
    }
}

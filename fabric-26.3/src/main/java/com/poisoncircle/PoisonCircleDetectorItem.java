package com.poisoncircle;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public final class PoisonCircleDetectorItem extends Item {
    public PoisonCircleDetectorItem(Properties properties) { super(properties); }
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && PoisonCircleMod.revealPreview(serverPlayer)
            && DetectorUseRules.shouldConsume(player.getAbilities().instabuild)) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}

package com.poisoncircle;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Client opens the panel; server commands enforce permissions. */
public final class LobbyControllerItem extends Item {
    public LobbyControllerItem(Properties properties) { super(properties); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return InteractionResult.SUCCESS;
    }
}

package com.poisoncircle;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;

public final class LobbyClient {
    private LobbyClient() { }
    public static void install() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() && player.getItemInHand(hand).is(PoisonCircleMod.LOBBY)) {
                Minecraft client = Minecraft.getInstance();
                client.execute(() -> client.gui.setScreen(new LobbyControlScreen()));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }
}

package com.poisoncircle;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ShrinkWarningClient {
    private static long receivedAt;
    private static boolean active;
    private static boolean installed;
    private ShrinkWarningClient() { }

    public static void install() {
        if (installed) return;
        installed = true;
        ClientPlayNetworking.registerGlobalReceiver(ShrinkWarningPayload.TYPE, (payload, context) -> {
            CircleAudioClient.warning();
            receivedAt = System.nanoTime();
            active = true;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> active = false);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "shrink_warning"), (gui, delta) -> {
            if (!active) return;
            if (!AirborneClientRules.warningVisible(System.nanoTime() - receivedAt)) { active = false; return; }
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null || client.gui.screen() != null) return;
            String text = "准备缩圈";
            int width = client.font.width(text);
            int left = (gui.guiWidth() - width) / 2, top = (int)(gui.guiHeight() * .22);
            gui.text(client.font, text, left, top, 0xFFFFD878, true);
        });
    }
}

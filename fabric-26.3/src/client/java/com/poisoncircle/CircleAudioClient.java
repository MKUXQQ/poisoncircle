package com.poisoncircle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Audio follows received server transitions, never the HUD's predicted timer. */
public final class CircleAudioClient {
    private static CircleAudioTransitions transitions = new CircleAudioTransitions();
    private static final Map<SoundEvent, SoundInstance> playing = new HashMap<>();
    private static ClientLevel level;
    private static boolean installed;

    private CircleAudioClient() { }

    public static void install() {
        if (installed) return;
        installed = true;
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear(client));
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, next) -> clear(client));
    }

    public static void apply(CircleSyncPayload payload) {
        Minecraft client = currentClient();
        if (client == null || !client.level.dimension().identifier().equals(payload.dimension())) return;
        if (transitions.update(payload.round(), payload.waiting(), payload.active(), payload.collapsed()))
            play(client, CircleSounds.CLOSING, "closing", .65F);
    }

    public static void warning() {
        Minecraft client = currentClient();
        if (client != null && transitions.warning()) play(client, CircleSounds.WARNING, "warning", .65F);
    }

    public static void damage() {
        Minecraft client = currentClient();
        if (client != null && transitions.damage(System.nanoTime())) play(client, CircleSounds.DAMAGE, "damage", .55F);
    }

    private static Minecraft currentClient() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != level) { clear(client); level = client.level; }
        return client.level == null || client.player == null ? null : client;
    }

    private static void play(Minecraft client, SoundEvent event, String file, float volume) {
        Identifier asset = Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "sounds/circle/" + file + ".ogg");
        if (client.getResourceManager().getResource(asset).isEmpty()) return;
        // One slot per cue bounds overlap without cutting a different kind of notification.
        SoundInstance previous = playing.remove(event);
        if (previous != null) client.getSoundManager().stop(previous);
        Iterator<SoundInstance> old = playing.values().iterator();
        while (old.hasNext()) if (!client.getSoundManager().isActive(old.next())) old.remove();
        SoundInstance sound = SimpleSoundInstance.forLocalAmbience(event, 1F, volume);
        playing.put(event, sound);
        client.getSoundManager().play(sound);
    }

    private static void clear(Minecraft client) {
        playing.values().forEach(client.getSoundManager()::stop);
        playing.clear();
        transitions = new CircleAudioTransitions();
        level = null;
    }
}

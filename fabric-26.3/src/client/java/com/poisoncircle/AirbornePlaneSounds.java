package com.poisoncircle;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Client-only sounds: one loop per loaded aircraft, plus observed transition edges. */
public final class AirbornePlaneSounds {
    private static final Map<AirbornePlaneEntity, Aircraft> AIRCRAFT = new IdentityHashMap<>();
    private static AirborneSoundTransitions localTransitions = new AirborneSoundTransitions();
    private static ClientLevel level;
    private static SoundInstance exit;
    private static boolean installed;

    private AirbornePlaneSounds() { }

    public static void install() {
        if (installed) return;
        installed = true;
        ClientTickEvents.END_CLIENT_TICK.register(AirbornePlaneSounds::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear(client));
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, next) -> clear(client));
        Identifier id = Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "airborne_sounds");
        ResourceLoader loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        loader.registerReloadListener(id, new SimpleReloadListener<Void>() {
            @Override protected Void prepare(PreparableReloadListener.SharedState state) { return null; }
            @Override protected void apply(Void ignored, PreparableReloadListener.SharedState state) {
                // Keep observed edges while replacing buffers/loop instances after the vanilla sound reload.
                stopSounds(Minecraft.getInstance());
            }
        });
        loader.addListenerOrdering(ResourceReloaderKeys.Client.SOUNDS, id);
    }

    private static void tick(Minecraft client) {
        if (client.level != level) { clear(client); level = client.level; }
        if (level == null || client.player == null) return;
        Set<AirbornePlaneEntity> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof AirbornePlaneEntity plane) || plane.isRemoved()) continue;
            seen.add(plane);
            Aircraft aircraft = AIRCRAFT.computeIfAbsent(plane, ignored -> new Aircraft());
            if (aircraft.engine == null) {
                aircraft.engine = new Engine(plane);
                client.getSoundManager().play(aircraft.engine);
            }
            if (aircraft.transitions.door(plane.doorProgress(0))) {
                Vec3 door = plane.position().add(new Vec3(0, -2.05, -9).yRot((float)Math.toRadians(-plane.getYRot())));
                aircraft.door = new SimpleSoundInstance(AirborneSounds.DOOR, SoundSource.BLOCKS, .55F, 1F,
                    RandomSource.create(), door.x, door.y, door.z);
                client.getSoundManager().play(aircraft.door);
            }
        }
        AIRCRAFT.entrySet().removeIf(entry -> {
            if (seen.contains(entry.getKey())) return false;
            entry.getValue().stop(client);
            return true;
        });
        if (localTransitions.phase(AirborneFlightClient.phase())) {
            if (exit != null) client.getSoundManager().stop(exit);
            exit = SimpleSoundInstance.forLocalAmbience(AirborneSounds.EXIT, 1F, .65F);
            client.getSoundManager().play(exit);
        }
    }

    private static void stopSounds(Minecraft client) {
        AIRCRAFT.values().forEach(aircraft -> aircraft.stop(client));
        if (exit != null) client.getSoundManager().stop(exit);
        exit = null;
    }

    private static void clear(Minecraft client) {
        stopSounds(client);
        AIRCRAFT.clear();
        localTransitions = new AirborneSoundTransitions();
        level = null;
    }

    private static final class Aircraft {
        final AirborneSoundTransitions transitions = new AirborneSoundTransitions();
        Engine engine;
        SoundInstance door;
        void stop(Minecraft client) {
            if (engine != null) { engine.finish(); client.getSoundManager().stop(engine); }
            if (door != null) client.getSoundManager().stop(door);
            engine = null;
            door = null;
        }
    }

    private static final class Engine extends AbstractTickableSoundInstance {
        private final AirbornePlaneEntity plane;
        Engine(AirbornePlaneEntity plane) {
            super(AirborneSounds.ENGINE, SoundSource.BLOCKS, RandomSource.create());
            this.plane = plane;
            looping = true;
            delay = 0;
            volume = .30F;
            pitch = 1F;
            relative = false;
            attenuation = SoundInstance.Attenuation.LINEAR;
            follow();
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {
            if (plane.isRemoved() || Minecraft.getInstance().level != plane.level()) { stop(); return; }
            follow();
        }
        private void follow() { x = plane.getX(); y = plane.getY(); z = plane.getZ(); }
        void finish() { stop(); }
    }
}

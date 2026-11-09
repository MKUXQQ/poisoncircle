package com.poisoncircle;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Custom aircraft events; resource packs supply the actual recordings. */
public final class AirborneSounds {
    public static final SoundEvent ENGINE = event("airborne_engine");
    public static final SoundEvent DOOR = event("airborne_door");
    public static final SoundEvent EXIT = event("airborne_exit");

    private AirborneSounds() { }
    public static void register() { }

    private static SoundEvent event(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}

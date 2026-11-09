package com.poisoncircle;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class CircleSounds {
    public static final SoundEvent WARNING = event("circle_warning");
    public static final SoundEvent CLOSING = event("circle_closing");
    public static final SoundEvent DAMAGE = event("circle_damage");

    private CircleSounds() { }
    public static void register() { }

    private static SoundEvent event(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}

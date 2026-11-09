package com.poisoncircle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded clientbound authoritative snapshot consumed by the airborne UI. */
public record AirborneStatePayload(String json) implements CustomPacketPayload {
    public static final Type<AirborneStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "airborne_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AirborneStatePayload> STREAM_CODEC = new StreamCodec<>() {
        public AirborneStatePayload decode(RegistryFriendlyByteBuf b) { return new AirborneStatePayload(b.readUtf(32768)); }
        public void encode(RegistryFriendlyByteBuf b, AirborneStatePayload p) { b.writeUtf(p.json(), 32768); }
    };
    public AirborneStatePayload { if (json == null || json.length() > 32768) throw new IllegalArgumentException("Airborne snapshot too large"); }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

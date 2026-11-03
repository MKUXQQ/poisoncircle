package com.poisoncircle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Sent only to the player who consumes a detector. */
public record DetectorRevealPayload(Identifier dimension, double x, double z, double radius, int targetRound) implements CustomPacketPayload {
    public static final Type<DetectorRevealPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "detector_reveal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DetectorRevealPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public DetectorRevealPayload decode(RegistryFriendlyByteBuf buffer) { return new DetectorRevealPayload(buffer.readIdentifier(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt()); }
        @Override public void encode(RegistryFriendlyByteBuf buffer, DetectorRevealPayload payload) {
            buffer.writeIdentifier(payload.dimension); buffer.writeDouble(payload.x); buffer.writeDouble(payload.z); buffer.writeDouble(payload.radius); buffer.writeVarInt(payload.targetRound);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

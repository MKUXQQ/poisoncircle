package com.poisoncircle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Sent only to the player who consumes a detector. */
public record DetectorRevealPayload(ResourceLocation dimension, double x, double z, double radius) implements CustomPacketPayload {
    public static final Type<DetectorRevealPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "detector_reveal"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DetectorRevealPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public DetectorRevealPayload decode(RegistryFriendlyByteBuf buffer) { return new DetectorRevealPayload(buffer.readResourceLocation(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()); }
        @Override public void encode(RegistryFriendlyByteBuf buffer, DetectorRevealPayload payload) {
            buffer.writeResourceLocation(payload.dimension); buffer.writeDouble(payload.x); buffer.writeDouble(payload.z); buffer.writeDouble(payload.radius);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

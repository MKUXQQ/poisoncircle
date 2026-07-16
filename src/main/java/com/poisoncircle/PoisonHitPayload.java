package com.poisoncircle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server confirms damage before the client starts the visual feedback. */
public record PoisonHitPayload() implements CustomPacketPayload {
    public static final PoisonHitPayload INSTANCE = new PoisonHitPayload();
    public static final Type<PoisonHitPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "poison_hit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PoisonHitPayload> STREAM_CODEC = StreamCodec.of((buffer, payload) -> { }, buffer -> INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

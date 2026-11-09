package com.poisoncircle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A server-triggered three-second screen warning; no text or timing supplied by the network. */
public record ShrinkWarningPayload() implements CustomPacketPayload {
    public static final Type<ShrinkWarningPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "shrink_warning"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShrinkWarningPayload> STREAM_CODEC = StreamCodec.unit(new ShrinkWarningPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

package com.poisoncircle;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ScanHoldPayload(BlockPos position, boolean holding) implements CustomPacketPayload {
    public static final Type<ScanHoldPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "scanner_hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanHoldPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ScanHoldPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ScanHoldPayload(buffer.readBlockPos(), buffer.readBoolean());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ScanHoldPayload payload) {
            buffer.writeBlockPos(payload.position());
            buffer.writeBoolean(payload.holding());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

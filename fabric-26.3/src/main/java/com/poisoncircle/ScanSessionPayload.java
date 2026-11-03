package com.poisoncircle;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authoritative scanner pose and progress; inactive packets release the client camera. */
public record ScanSessionPayload(int entityId, BlockPos position, boolean active, int progressTicks, float yaw, float pitch)
    implements CustomPacketPayload {
    public static final Type<ScanSessionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "scanner_session"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanSessionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ScanSessionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ScanSessionPayload(buffer.readVarInt(), buffer.readBlockPos(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readFloat(), buffer.readFloat());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, ScanSessionPayload payload) {
            buffer.writeVarInt(payload.entityId());
            buffer.writeBlockPos(payload.position());
            buffer.writeBoolean(payload.active());
            buffer.writeVarInt(payload.progressTicks());
            buffer.writeFloat(payload.yaw());
            buffer.writeFloat(payload.pitch());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

package com.poisoncircle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Public snapshot: current circle and immediate next circle only. */
public record CircleSyncPayload(Identifier dimension,
                                double currentX, double currentZ, double currentRadius,
                                double nextX, double nextZ, double nextRadius,
                                int round, int remainingSeconds, int remainingTicks, boolean waiting,
                                boolean active, boolean collapsed, boolean hasNext) implements CustomPacketPayload {
    public static final Type<CircleSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "circle_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CircleSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public CircleSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            return new CircleSyncPayload(buffer.readIdentifier(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, CircleSyncPayload payload) {
            buffer.writeIdentifier(payload.dimension); buffer.writeDouble(payload.currentX); buffer.writeDouble(payload.currentZ); buffer.writeDouble(payload.currentRadius);
            buffer.writeDouble(payload.nextX); buffer.writeDouble(payload.nextZ); buffer.writeDouble(payload.nextRadius);
            buffer.writeVarInt(payload.round); buffer.writeVarInt(payload.remainingSeconds); buffer.writeVarInt(payload.remainingTicks);
            buffer.writeBoolean(payload.waiting); buffer.writeBoolean(payload.active); buffer.writeBoolean(payload.collapsed); buffer.writeBoolean(payload.hasNext);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

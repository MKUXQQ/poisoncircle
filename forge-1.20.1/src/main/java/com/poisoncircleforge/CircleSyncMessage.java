package com.poisoncircleforge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record CircleSyncMessage(String dimension, double x, double z, double radius, double nextX, double nextZ, double nextRadius, int round, int remainingTicks, boolean waiting, boolean active) {
    static void encode(CircleSyncMessage m, FriendlyByteBuf b) { b.writeUtf(m.dimension); b.writeDouble(m.x); b.writeDouble(m.z); b.writeDouble(m.radius); b.writeDouble(m.nextX); b.writeDouble(m.nextZ); b.writeDouble(m.nextRadius); b.writeVarInt(m.round); b.writeVarInt(m.remainingTicks); b.writeBoolean(m.waiting); b.writeBoolean(m.active); }
    static CircleSyncMessage decode(FriendlyByteBuf b) { return new CircleSyncMessage(b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean()); }
    static void handle(CircleSyncMessage m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> PoisonCircleClient.apply(m)); c.get().setPacketHandled(true); }
}

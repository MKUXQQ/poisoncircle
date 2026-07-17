package com.poisoncircleforge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record DetectorRevealMessage(String dimension, double x, double z, double radius, boolean visible) {
    static void encode(DetectorRevealMessage m, FriendlyByteBuf b) { b.writeUtf(m.dimension); b.writeDouble(m.x); b.writeDouble(m.z); b.writeDouble(m.radius); b.writeBoolean(m.visible); }
    static DetectorRevealMessage decode(FriendlyByteBuf b) { return new DetectorRevealMessage(b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(), b.readBoolean()); }
    static void handle(DetectorRevealMessage m, Supplier<NetworkEvent.Context> c) { c.get().enqueueWork(() -> PoisonCircleClient.applyReveal(m)); c.get().setPacketHandled(true); }
}

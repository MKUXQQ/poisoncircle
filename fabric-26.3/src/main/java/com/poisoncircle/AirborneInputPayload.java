package com.poisoncircle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Player flight input; server validates participant, finiteness and range. */
public record AirborneInputPayload(float forward, float strafe, float yaw, float pitch, boolean jump) implements CustomPacketPayload {
    public static final Type<AirborneInputPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "airborne_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AirborneInputPayload> STREAM_CODEC = new StreamCodec<>() {
        public AirborneInputPayload decode(RegistryFriendlyByteBuf b) { return new AirborneInputPayload(b.readFloat(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean()); }
        public void encode(RegistryFriendlyByteBuf b, AirborneInputPayload p) { b.writeFloat(p.forward()); b.writeFloat(p.strafe()); b.writeFloat(p.yaw()); b.writeFloat(p.pitch()); b.writeBoolean(p.jump()); }
    };
    public AirborneInputPayload { if (!valid(forward) || !valid(strafe) || !valid(yaw) || !valid(pitch) || Math.abs(forward) > 1 || Math.abs(strafe) > 1 || pitch < -90 || pitch > 90) throw new IllegalArgumentException("Invalid airborne input"); }
    private static boolean valid(float x) { return Float.isFinite(x); }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

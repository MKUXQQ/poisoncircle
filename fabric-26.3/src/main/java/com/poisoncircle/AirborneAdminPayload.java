package com.poisoncircle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded serverbound administrative request. Data is compact JSON. */
public record AirborneAdminPayload(String action, String data) implements CustomPacketPayload {
    public static final Type<AirborneAdminPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "airborne_admin"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AirborneAdminPayload> STREAM_CODEC = new StreamCodec<>() {
        public AirborneAdminPayload decode(RegistryFriendlyByteBuf b) { return new AirborneAdminPayload(b.readUtf(32), b.readUtf(4096)); }
        public void encode(RegistryFriendlyByteBuf b, AirborneAdminPayload p) { b.writeUtf(p.action(), 32); b.writeUtf(p.data(), 4096); }
    };
    public AirborneAdminPayload { if (action == null || action.length() > 32 || data == null || data.length() > 4096) throw new IllegalArgumentException("Airborne payload too large"); }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

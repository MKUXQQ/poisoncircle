package com.poisoncircle;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Common registration and authoritative five-second scanning sessions. */
public final class ScanDevice {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID, "poison_circle_scanner");
    private static final ResourceKey<Block> BLOCK_KEY = ResourceKey.create(Registries.BLOCK, ID);
    private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, ID);
    public static final Block BLOCK = Registry.register(BuiltInRegistries.BLOCK, BLOCK_KEY,
        new Block(BlockBehaviour.Properties.of().setId(BLOCK_KEY).strength(3.0F).sound(SoundType.METAL).noOcclusion()));
    public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
        new BlockItem(BLOCK, new Item.Properties().setId(ITEM_KEY).useBlockDescriptionPrefix()));
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Set<UUID> MUST_RELEASE = new HashSet<>();

    private ScanDevice() { }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(ScanSessionPayload.TYPE, ScanSessionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ScanHoldPayload.TYPE, ScanHoldPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ScanHoldPayload.TYPE, (payload, context) ->
            receive(context.server(), context.player(), payload));
        ServerTickEvents.END_SERVER_TICK.register(ScanDevice::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            end(server, handler.player.getUUID());
            MUST_RELEASE.remove(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { SESSIONS.clear(); MUST_RELEASE.clear(); });
        // Consume block interaction, preventing held items from placing or firing while scanning.
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
            level.getBlockState(hit.getBlockPos()).is(BLOCK) && !player.isSpectator()
                ? InteractionResult.SUCCESS : InteractionResult.PASS);
    }

    private static void receive(MinecraftServer server, ServerPlayer player, ScanHoldPayload payload) {
        UUID id = player.getUUID();
        if (!payload.holding()) {
            end(server, id);
            MUST_RELEASE.remove(id);
            return;
        }
        long tick = server.getTickCount();
        Session session = SESSIONS.get(id);
        if (session != null) {
            // After teleporting the third-person crosshair need not hit the scanner.
            if (session.hold().expired(tick) || !validSession(player, session)) end(server, id);
            else session.hold().heartbeat(tick);
            return;
        }
        if (!MUST_RELEASE.add(id)) return;
        if (!validTarget(player, payload.position())) { reject(server, player, payload.position()); return; }
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK || !((BlockHitResult) hit).getBlockPos().equals(payload.position())) {
            reject(server, player, payload.position());
            return;
        }
        Vec3 standing = safeStandingPosition(player, payload.position());
        if (standing == null) { reject(server, player, payload.position()); return; }
        double dx = payload.position().getX() + 0.5 - standing.x;
        double dz = payload.position().getZ() + 0.5 - standing.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(payload.position().getY() + 0.75 - (standing.y + player.getEyeHeight()), Math.hypot(dx, dz)));
        if (!player.teleportTo(player.level(), standing.x, standing.y, standing.z, Set.of(), yaw, pitch, true)) {
            reject(server, player, payload.position());
            return;
        }
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        ScanHoldState hold = new ScanHoldState(tick);
        hold.tick(tick, true);
        session = new Session(player.getId(), player.level().dimension(), payload.position().immutable(), standing, yaw, pitch, hold);
        SESSIONS.put(id, session);
        broadcast(server, session, true);
    }

    private static boolean validTarget(ServerPlayer player, BlockPos position) {
        return player.isAlive() && !player.isSpectator() && player.isWithinBlockInteractionRange(position, 0)
            && player.level().hasChunkAt(position) && player.level().getBlockState(position).is(BLOCK);
    }

    private static Vec3 safeStandingPosition(ServerPlayer player, BlockPos scanner) {
        var candidates = new ArrayList<BlockPos>();
        for (Direction direction : Direction.Plane.HORIZONTAL) candidates.add(scanner.relative(direction));
        candidates.sort(Comparator.comparingDouble(pos -> Vec3.atBottomCenterOf(pos).distanceToSqr(player.position())));
        for (BlockPos feet : candidates) {
            Vec3 standing = Vec3.atBottomCenterOf(feet);
            if (safeStandingPosition(player, standing)) return standing;
        }
        return null;
    }

    private static boolean safeStandingPosition(ServerPlayer player, Vec3 standing) {
        BlockPos feet = BlockPos.containing(standing);
        var level = player.level();
        if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(feet.above())
            || !level.hasChunkAt(feet) || !level.hasChunkAt(feet.below())
            || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
            || !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) return false;
        // Always reserve standing headroom, including when the player approaches crouched.
        AABB body = new AABB(standing.x - 0.3, standing.y, standing.z - 0.3,
            standing.x + 0.3, standing.y + 1.8, standing.z + 0.3);
        return level.getWorldBorder().isWithinBounds(body) && level.noCollision(player, body);
    }

    private static boolean validSession(ServerPlayer player, Session session) {
        return player.getId() == session.entityId() && player.level().dimension().equals(session.dimension())
            && validTarget(player, session.position())
            && player.position().distanceToSqr(session.standing()) <= 0.04
            && safeStandingPosition(player, session.standing());
    }

    private static void reject(MinecraftServer server, ServerPlayer player, BlockPos position) {
        broadcast(server, new Session(player.getId(), player.level().dimension(), position.immutable(), player.position(),
            player.getYRot(), player.getXRot(), new ScanHoldState(server.getTickCount())), false);
    }

    private static void end(MinecraftServer server, UUID id) {
        Session session = SESSIONS.remove(id);
        if (session != null) broadcast(server, session, false);
    }

    private static void broadcast(MinecraftServer server, Session session, boolean active) {
        var payload = new ScanSessionPayload(session.entityId(), session.position(), active,
            session.hold().progressTicks(server.getTickCount()), session.yaw(), session.pitch());
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
            // Also release the operator after a dimension change.
            if ((viewer.level().dimension().equals(session.dimension()) || viewer.getId() == session.entityId())
                && ServerPlayNetworking.canSend(viewer, ScanSessionPayload.TYPE)) ServerPlayNetworking.send(viewer, payload);
        }
    }

    private static void tick(MinecraftServer server) {
        long tick = server.getTickCount();
        for (UUID id : Set.copyOf(SESSIONS.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            Session session = SESSIONS.get(id);
            if (player == null || session.hold().expired(tick) || !validSession(player, session)) {
                end(server, id);
                continue;
            }
            player.setYRot(session.yaw());
            player.setXRot(session.pitch());
            player.setYHeadRot(session.yaw());
            player.setYBodyRot(session.yaw());
            player.setDeltaMovement(Vec3.ZERO);
            if (session.hold().tick(tick, true)) {
                PoisonCircleMod.revealPreview(player);
                end(server, id);
            } else {
                if (session.hold().progressTicks(tick) % 4 == 0) broadcast(server, session, true);
                if (session.hold().progressTicks(tick) % 20 == 0) {
                    PoisonCircleMod.playScanSound(player, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT, 0.35F,
                        0.8F + session.hold().progressTicks(tick) / 100.0F);
                }
            }
        }
    }

    private record Session(int entityId, ResourceKey<Level> dimension, BlockPos position, Vec3 standing,
                           float yaw, float pitch, ScanHoldState hold) { }
}

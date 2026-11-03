package com.poisoncircle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
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

    private ScanDevice() { }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(ScanHoldPayload.TYPE, ScanHoldPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ScanHoldPayload.TYPE, (payload, context) ->
            receive(context.server(), context.player(), payload));
        ServerTickEvents.END_SERVER_TICK.register(ScanDevice::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SESSIONS.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
        // Consume block interaction, preventing held items from placing or firing while scanning.
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
            level.getBlockState(hit.getBlockPos()).is(BLOCK) && !player.isSpectator()
                ? InteractionResult.SUCCESS : InteractionResult.PASS);
    }

    private static void receive(MinecraftServer server, ServerPlayer player, ScanHoldPayload payload) {
        UUID id = player.getUUID();
        if (!payload.holding()) {
            SESSIONS.remove(id);
            return;
        }
        if (!validTarget(player, payload.position())) {
            SESSIONS.remove(id);
            return;
        }
        long tick = server.getTickCount();
        Session session = SESSIONS.get(id);
        if (session == null || !session.dimension().equals(player.level().dimension())
            || !session.position().equals(payload.position()) || session.hold().expired(tick)) {
            session = new Session(player.level().dimension(), payload.position().immutable(), new ScanHoldState(tick));
            SESSIONS.put(id, session);
        }
        session.hold().heartbeat(tick);
    }

    private static boolean validTarget(ServerPlayer player, BlockPos position) {
        if (!player.isAlive() || player.isSpectator() || !player.isWithinBlockInteractionRange(position, 0)
            || !player.level().hasChunkAt(position) || !player.level().getBlockState(position).is(BLOCK)) return false;
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        return hit.getType() == HitResult.Type.BLOCK && ((BlockHitResult) hit).getBlockPos().equals(position);
    }

    private static void tick(MinecraftServer server) {
        long tick = server.getTickCount();
        var iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (player == null || session.hold().expired(tick)
                || !player.level().dimension().equals(session.dimension()) || !validTarget(player, session.position())) {
                iterator.remove();
                continue;
            }
            if (session.hold().tick(tick, true)) {
                PoisonCircleMod.revealPreview(player);
            } else if (tick % 20 == 0 && session.hold().progressTicks(tick) < ScanHoldState.REQUIRED_TICKS) {
                PoisonCircleMod.playScanSound(player, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT, 0.35F,
                    0.8F + session.hold().progressTicks(tick) / 100.0F);
            }
        }
    }

    private record Session(ResourceKey<Level> dimension, BlockPos position, ScanHoldState hold) { }
}

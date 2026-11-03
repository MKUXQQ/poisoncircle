package com.poisoncircle;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class PoisonCircleMod implements net.fabricmc.api.ModInitializer {
    public static final String MOD_ID = "poisoncircle";
    private static final int MAX_SHRINKS = 5;
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.Item> DETECTOR_KEY = net.minecraft.resources.ResourceKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "poison_circle_detector"));
    public static final PoisonCircleDetectorItem DETECTOR = net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM, DETECTOR_KEY,
        new PoisonCircleDetectorItem(new net.minecraft.world.item.Item.Properties().setId(DETECTOR_KEY).stacksTo(1)));
    public static final ResourceKey<net.minecraft.world.item.Item> CONTROLLER_KEY = ResourceKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "poison_circle_controller"));
    public static final net.minecraft.world.item.Item CONTROLLER = net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM, CONTROLLER_KEY,
        new PoisonCircleControllerItem(new net.minecraft.world.item.Item.Properties().setId(CONTROLLER_KEY).stacksTo(1)));
    public static final ResourceKey<net.minecraft.world.item.Item> LOBBY_KEY = ResourceKey.create(Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "poison_circle_lobby"));
    public static final net.minecraft.world.item.Item LOBBY = net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM, LOBBY_KEY,
        new LobbyControllerItem(new net.minecraft.world.item.Item.Properties().setId(LOBBY_KEY).stacksTo(1)));
    private static final Map<LevelKey, Circle> CIRCLES = new HashMap<>();
    private static final Map<LevelKey, Vec3> CONFIGURED_CENTERS = new HashMap<>();
    private static final RoundDurationSettings ROUND_DURATIONS = new RoundDurationSettings(MAX_SHRINKS, 120);
    private static final RoundCommandSettings ROUND_COMMANDS = new RoundCommandSettings();
    private static final Map<String, JsonObject> PERSISTED_SETTINGS = new HashMap<>();
    private static final Set<UUID> MATCH_PLAYERS = new HashSet<>(), ELIMINATED_PLAYERS = new HashSet<>(), LOBBY_WITHDRAWN_PLAYERS = new HashSet<>();
    private static boolean MATCH_HAD_PARTICIPANTS;
    private static LobbySettings lobby;
    private static final Logger LOGGER = Logger.getLogger(MOD_ID);
    private static ServerSettingsStore settingsStore;
    private static boolean WORLD_COLLAPSED;
    private static double GLOBAL_BASE_DAMAGE = 2;
    private static double GLOBAL_DAMAGE_INCREMENT = 2;
    private static double INITIAL_RADIUS = 500, FINAL_RADIUS = 50;

    @Override public void onInitialize() {
        ScanDevice.register();
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB,
            net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "control"),
            net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.poisoncircle.control"))
                .icon(() -> new ItemStack(CONTROLLER))
                .displayItems((parameters, output) -> { output.accept(CONTROLLER); output.accept(DETECTOR); output.accept(LOBBY); output.accept(ScanDevice.ITEM); }).build());
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(CircleSyncPayload.TYPE, CircleSyncPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(PoisonHitPayload.TYPE, PoisonHitPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(DetectorRevealPayload.TYPE, DetectorRevealPayload.STREAM_CODEC);
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(PoisonCircleMod::loadSettings);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(PoisonCircleMod::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> reset());
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                enrollParticipant(player);
                if (MATCH_PLAYERS.contains(player.getUUID())) ELIMINATED_PLAYERS.add(player.getUUID());
            }
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                enrollParticipant(oldPlayer);
                if (MATCH_PLAYERS.contains(oldPlayer.getUUID())) ELIMINATED_PLAYERS.add(oldPlayer.getUUID());
            }
        });
    }
    private static void loadSettings(MinecraftServer server) {
        Path world = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize();
        String worldId = UUID.nameUUIDFromBytes(world.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        settingsStore = new ServerSettingsStore(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("poisoncircle/" + worldId + ".json"), LOGGER);
        PERSISTED_SETTINGS.clear(); PERSISTED_SETTINGS.putAll(settingsStore.load());
        ROUND_DURATIONS.reset(120); ROUND_COMMANDS.clear();
        GLOBAL_BASE_DAMAGE = 2; GLOBAL_DAMAGE_INCREMENT = 2; INITIAL_RADIUS = 500; FINAL_RADIUS = 50; CONFIGURED_CENTERS.clear();
        JsonObject global = PERSISTED_SETTINGS.get("global");
        if (global != null) try {
            if (global.has("wait") && global.has("shrink")) {
                int[] waits = parseInts(global.get("wait").getAsJsonArray(), MAX_SHRINKS, 0);
                int[] shrinks = parseInts(global.get("shrink").getAsJsonArray(), MAX_SHRINKS, 1);
                for (int i = 0; i < MAX_SHRINKS; i++) ROUND_DURATIONS.setTimes(i + 1, waits[i], shrinks[i]);
            }
            if (global.has("commands") && global.get("commands").isJsonObject()) for (var command : global.getAsJsonObject("commands").entrySet()) {
                int round = Integer.parseInt(command.getKey());
                if (round >= 1 && round <= MAX_SHRINKS && command.getValue().isJsonPrimitive() && command.getValue().getAsString().length() <= 2048) ROUND_COMMANDS.set(round, command.getValue().getAsString());
            }
            if (global.has("baseDamage") && global.has("damageIncrement")) {
                double base = global.get("baseDamage").getAsDouble(), increment = global.get("damageIncrement").getAsDouble();
                if (Double.isFinite(base) && base >= 0 && Double.isFinite(increment) && increment >= 0) { GLOBAL_BASE_DAMAGE = base; GLOBAL_DAMAGE_INCREMENT = increment; }
            }
            if (global.has("initialRadius") && global.has("finalRadius")) {
                double initial = global.get("initialRadius").getAsDouble(), end = global.get("finalRadius").getAsDouble();
                if (Double.isFinite(initial) && Double.isFinite(end) && end >= 1 && initial > end) { INITIAL_RADIUS = initial; FINAL_RADIUS = end; }
            }
        } catch (Exception e) { LOGGER.log(java.util.logging.Level.WARNING, "Ignoring invalid global poisoncircle settings", e); }
        lobby = null;
        JsonObject lobbyJson = PERSISTED_SETTINGS.get("lobby");
        if (lobbyJson != null) try {
            net.minecraft.resources.Identifier.parse(lobbyJson.get("dimension").getAsString());
            lobby = LobbySettings.fromJson(lobbyJson);
        } catch (Exception e) { LOGGER.log(java.util.logging.Level.WARNING, "Ignoring invalid poisoncircle lobby settings", e); }
        for (Map.Entry<String, JsonObject> entry : PERSISTED_SETTINGS.entrySet()) {
            if (entry.getKey().equals("global") || entry.getKey().equals("lobby")) continue;
            try {
                var dimension = net.minecraft.resources.Identifier.parse(entry.getKey());
                ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension);
                JsonObject json = entry.getValue();
                if (json.has("centerX") && json.has("centerZ")) {
                    double x = json.get("centerX").getAsDouble(), z = json.get("centerZ").getAsDouble();
                    if (Double.isFinite(x) && Double.isFinite(z)) CONFIGURED_CENTERS.put(new LevelKey(key), new Vec3(x, 0, z));
                }
            } catch (Exception e) { LOGGER.log(java.util.logging.Level.WARNING, "Ignoring invalid poisoncircle settings for " + entry.getKey(), e); }
        }
    }
    private static int[] parseInts(com.google.gson.JsonArray values, int count, int minimum) {
        if (values.size() != count) throw new IllegalArgumentException("Expected " + count + " values");
        int[] parsed = new int[count];
        for (int i = 0; i < count; i++) { parsed[i] = values.get(i).getAsBigDecimal().intValueExact(); if (parsed[i] < minimum || parsed[i] > 86400) throw new IllegalArgumentException("Duration out of range"); }
        return parsed;
    }
    private static void saveSettings() {
        if (settingsStore == null) return;
        JsonObject json = PERSISTED_SETTINGS.computeIfAbsent("global", ignored -> new JsonObject());
        json.add("wait", new com.google.gson.Gson().toJsonTree(ROUND_DURATIONS.copyWaitSeconds()));
        json.add("shrink", new com.google.gson.Gson().toJsonTree(ROUND_DURATIONS.copyShrinkSeconds()));
        json.addProperty("baseDamage", GLOBAL_BASE_DAMAGE); json.addProperty("damageIncrement", GLOBAL_DAMAGE_INCREMENT);
        json.addProperty("initialRadius", INITIAL_RADIUS); json.addProperty("finalRadius", FINAL_RADIUS);
        JsonObject commands = new JsonObject(); ROUND_COMMANDS.copy().forEach((round, command) -> commands.addProperty(Integer.toString(round), command)); json.add("commands", commands);
        for (Map.Entry<LevelKey, Vec3> entry : CONFIGURED_CENTERS.entrySet()) {
            JsonObject center = PERSISTED_SETTINGS.computeIfAbsent(entry.getKey().key().identifier().toString(), ignored -> new JsonObject());
            center.addProperty("centerX", entry.getValue().x); center.addProperty("centerZ", entry.getValue().z);
        }
        if (lobby != null) PERSISTED_SETTINGS.put("lobby", lobby.toJson());
        settingsStore.save(PERSISTED_SETTINGS);
    }
    private static void reset() {
        CIRCLES.clear(); CONFIGURED_CENTERS.clear(); MATCH_PLAYERS.clear(); ELIMINATED_PLAYERS.clear(); LOBBY_WITHDRAWN_PLAYERS.clear(); WORLD_COLLAPSED = false; MATCH_HAD_PARTICIPANTS = false; lobby = null;
        PERSISTED_SETTINGS.clear(); settingsStore = null; ROUND_DURATIONS.reset(120); ROUND_COMMANDS.clear();
        GLOBAL_BASE_DAMAGE = 2; GLOBAL_DAMAGE_INCREMENT = 2; INITIAL_RADIUS = 500; FINAL_RADIUS = 50;
    }
    private static void sendToPlayer(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, payload.type()))
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
    }
    private static void sendToAllPlayers(ServerLevel level, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) sendToPlayer(player, payload);
    }
    public static void registerCommands(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("poisoncircle").requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("start").executes(c -> start(c.getSource(), INITIAL_RADIUS, FINAL_RADIUS))
                .then(Commands.argument("initialRadius", DoubleArgumentType.doubleArg(1))
                    .then(Commands.argument("finalRadius", DoubleArgumentType.doubleArg(1)).executes(c -> start(c.getSource(),
                        DoubleArgumentType.getDouble(c, "initialRadius"), DoubleArgumentType.getDouble(c, "finalRadius"))))))
            .then(Commands.literal("center").executes(c -> setCenter(c.getSource(), c.getSource().getPosition()))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg()).suggests(PoisonCircleMod::suggestCurrentX)
                    .then(Commands.argument("y", DoubleArgumentType.doubleArg()).suggests(PoisonCircleMod::suggestCurrentY)
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg()).suggests(PoisonCircleMod::suggestCurrentZ)
                            .executes(c -> setCenter(c.getSource(), new Vec3(DoubleArgumentType.getDouble(c, "x"), DoubleArgumentType.getDouble(c, "y"), DoubleArgumentType.getDouble(c, "z"))))))))
            .then(Commands.literal("damage").then(Commands.argument("base", DoubleArgumentType.doubleArg(0))
                .executes(c -> setDamage(c.getSource(), DoubleArgumentType.getDouble(c, "base"), null))
                .then(Commands.argument("increment", DoubleArgumentType.doubleArg(0)).executes(c -> setDamage(c.getSource(), DoubleArgumentType.getDouble(c, "base"), DoubleArgumentType.getDouble(c, "increment"))))))
            .then(Commands.literal("time").then(Commands.argument("round", IntegerArgumentType.integer(1, MAX_SHRINKS))
                .then(Commands.argument("waitSeconds", IntegerArgumentType.integer(0))
                    .then(Commands.argument("shrinkSeconds", IntegerArgumentType.integer(1)).executes(c -> setRoundTime(c.getSource(),
                        IntegerArgumentType.getInteger(c, "round"), IntegerArgumentType.getInteger(c, "waitSeconds"), IntegerArgumentType.getInteger(c, "shrinkSeconds")))))))
            .then(Commands.literal("command").then(Commands.argument("round", IntegerArgumentType.integer(1, MAX_SHRINKS))
                .then(Commands.argument("command", StringArgumentType.greedyString()).executes(c -> setRoundCommand(c.getSource(),
                    IntegerArgumentType.getInteger(c, "round"), StringArgumentType.getString(c, "command"))))))
            .then(Commands.literal("lobby")
                .executes(c -> lobbyStatus(c.getSource()))
                .then(Commands.literal("status").executes(c -> lobbyStatus(c.getSource())))
                .then(Commands.literal("disable").executes(c -> disableLobby(c.getSource())))
                .then(Commands.literal("set").then(Commands.argument("radius", DoubleArgumentType.doubleArg(1, 29_999_984))
                    .executes(c -> setLobby(c.getSource(), c.getSource().getPosition(), DoubleArgumentType.getDouble(c, "radius")))))
                .then(Commands.literal("center")
                    .then(Commands.argument("x", DoubleArgumentType.doubleArg(-29_999_984, 29_999_984))
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg(-20_000_000, 20_000_000))
                            .then(Commands.argument("z", DoubleArgumentType.doubleArg(-29_999_984, 29_999_984))
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1, 29_999_984))
                                    .executes(c -> setLobby(c.getSource(), new Vec3(DoubleArgumentType.getDouble(c, "x"), DoubleArgumentType.getDouble(c, "y"), DoubleArgumentType.getDouble(c, "z")), DoubleArgumentType.getDouble(c, "radius")))))))))
            .then(Commands.literal("detector").executes(c -> giveDetector(c.getSource())))
            .then(Commands.literal("stop").executes(c -> stop(c.getSource()))));
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) enrollParticipant(player);
        withdrawLobbyPlayers(server);
        if (shouldStopForEliminations(server)) {
            stopAll(server);
            broadcast(server, "所有参赛玩家已死亡，毒圈已停止。");
            return;
        }
        if (WORLD_COLLAPSED) {
            for (ServerLevel level : server.getAllLevels()) applyWorldDamage(level);
            return;
        }
        for (Map.Entry<LevelKey, Circle> entry : CIRCLES.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey().key());
            if (level == null) continue;
            Circle circle = entry.getValue();
            circle.elapsed++;
            if (circle.elapsed % 20 == 0) applyCircleDamage(level, circle);
            if (circle.waiting) {
                if (circle.elapsed >= circle.waitDuration()) {
                    circle.waiting = false;
                    circle.elapsed = 0;
                    sendCircleSync(level, circle, true, false);
                    executeRoundCommand(server, circle.stage + 1);
                    broadcast(server, "毒圈第 " + (circle.stage + 1) + "/5 圈开始缩小！");
                }
            } else if (circle.elapsed >= circle.shrinkDuration()) {
                circle.finishShrink();
                if (circle.stage == MAX_SHRINKS) {
                    WORLD_COLLAPSED = true;
                    sendCircleSync(level, circle, false, true);
                    broadcast(server, "毒圈已完成第 5 次缩圈，所有世界现在都受到毒圈伤害！");
                    return;
                }
                circle.promotePreview(level, level.getRandom());
                circle.waiting = true;
                circle.elapsed = 0;
                sendCircleSync(level, circle, true, false);
                broadcast(server, "第 " + (circle.stage + 1) + "/5 圈安全区已刷新，" + circle.waitDuration() / 20 + " 秒后开始缩小。");
            }
            if (circle.elapsed % 5 == 0) sendCircleSync(level, circle, true, false);
        }
    }

    private static void enrollParticipant(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!MatchElimination.shouldEnroll(!CIRCLES.isEmpty() || WORLD_COLLAPSED,
            LOBBY_WITHDRAWN_PLAYERS.contains(id), inLobby(player), player.isCreative() || player.isSpectator())) return;
        MATCH_PLAYERS.add(id);
        MATCH_HAD_PARTICIPANTS = true;
        if (!player.isAlive()) ELIMINATED_PLAYERS.add(id);
    }
    private static boolean shouldStopForEliminations(MinecraftServer server) {
        if (!MATCH_HAD_PARTICIPANTS) return false;
        for (UUID id : MATCH_PLAYERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (MatchElimination.blocksStop(ELIMINATED_PLAYERS.contains(id), player != null,
                player != null && (player.isCreative() || player.isSpectator()), player != null && player.isAlive())) return false;
        }
        return MatchElimination.shouldStop(MATCH_HAD_PARTICIPANTS, false);
    }
    private static void stopAll(MinecraftServer server) {
        for (Map.Entry<LevelKey, Circle> entry : CIRCLES.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey().key());
            if (level != null) sendCircleSync(level, entry.getValue(), false, false);
        }
        CIRCLES.clear(); WORLD_COLLAPSED = false; MATCH_PLAYERS.clear(); ELIMINATED_PLAYERS.clear(); LOBBY_WITHDRAWN_PLAYERS.clear(); MATCH_HAD_PARTICIPANTS = false;
    }
    private static boolean inLobby(ServerPlayer player) {
        return lobby != null && lobby.contains(player.level().dimension().identifier().toString(), player.getX(), player.getZ());
    }
    private static void withdrawLobbyPlayers(MinecraftServer server) {
        if (CIRCLES.isEmpty() && !WORLD_COLLAPSED) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) if (inLobby(player)) {
            if (MATCH_PLAYERS.remove(player.getUUID())) LOBBY_WITHDRAWN_PLAYERS.add(player.getUUID());
        }
    }
    private static boolean immuneToPoison(ServerPlayer player) {
        return MatchElimination.immune(ELIMINATED_PLAYERS.contains(player.getUUID()), LOBBY_WITHDRAWN_PLAYERS.contains(player.getUUID()),
            inLobby(player), player.isAlive(), player.isCreative() || player.isSpectator());
    }
    private static void applyCircleDamage(ServerLevel level, Circle circle) {
        for (ServerPlayer player : level.players()) if (!immuneToPoison(player) && circle.outside(player.getX(), player.getZ())) applyDamage(level, player, circle.damage());
    }
    private static void applyWorldDamage(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) return;
        for (ServerPlayer player : level.players()) if (!immuneToPoison(player)) applyDamage(level, player, GLOBAL_BASE_DAMAGE + GLOBAL_DAMAGE_INCREMENT * MAX_SHRINKS);
    }
    private static void applyDamage(ServerLevel level, ServerPlayer player, double amount) {
        if (immuneToPoison(player) || amount <= 0) return;
        float remaining = DirectHealthDamage.remainingHealth(player.getHealth(), amount);
        player.setHealth(remaining);
        if (remaining <= 0.0F) player.die(level.damageSources().magic());
        sendToPlayer(player, PoisonHitPayload.INSTANCE);
    }

    private static void sendCircleSync(ServerLevel level, Circle circle, boolean active, boolean collapsed) {
        sendToAllPlayers(level, new CircleSyncPayload(level.dimension().identifier(), circle.currentX(), circle.currentZ(), circle.currentRadius(),
            circle.nextCenter.x, circle.nextCenter.z, circle.nextRadius, circle.stage + 1, Math.max(0, circle.remainingTicks() / 20),
            Math.max(0, circle.remainingTicks()),
            circle.waiting, active, collapsed, !collapsed && circle.stage < MAX_SHRINKS));
    }
    static boolean revealPreview(ServerPlayer player) {
        Circle circle = CIRCLES.get(new LevelKey(player.level().dimension()));
        if (circle == null || !circle.hasPreview()) {
            playScanSound(player, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS, 0.6F, 0.65F);
            return false;
        }
        sendToPlayer(player, new DetectorRevealPayload(player.level().dimension().identifier(), circle.previewX(), circle.previewZ(), circle.previewRadius(), circle.stage + 3));
        playScanSound(player, net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP), 0.6F, 1.25F);
        return true;
    }
    static void playScanSound(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float volume, float pitch) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(sound,
            net.minecraft.sounds.SoundSource.BLOCKS, player.getX(), player.getY(), player.getZ(), volume, pitch, player.level().getRandom().nextLong()));
    }
    private static void broadcast(MinecraftServer server, String message) { server.getPlayerList().broadcastSystemMessage(Component.literal(message), false); }

    private static int start(CommandSourceStack source, double initial, double end) {
        if (!Double.isFinite(initial) || !Double.isFinite(end) || end < 1 || end >= initial) { source.sendFailure(Component.literal("最终半径必须至少为 1 且小于有限的初始半径。")); return 0; }
        ServerLevel level = source.getLevel(); Vec3 center = CONFIGURED_CENTERS.getOrDefault(new LevelKey(level.dimension()), source.getPosition());
        if (!circleFits(level.getWorldBorder(), center.x, center.z, initial)) { source.sendFailure(Component.literal("初始毒圈会超出世界边界，请站到更靠内的位置或减小半径。")); return 0; }
        INITIAL_RADIUS = initial; FINAL_RADIUS = end;
        CONFIGURED_CENTERS.put(new LevelKey(level.dimension()), new Vec3(center.x, 0, center.z));
        saveSettings();
        if (!CIRCLES.isEmpty() || WORLD_COLLAPSED) stopAll(source.getServer());
        WORLD_COLLAPSED = false; MATCH_PLAYERS.clear(); ELIMINATED_PLAYERS.clear(); LOBBY_WITHDRAWN_PLAYERS.clear();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (inLobby(player)) LOBBY_WITHDRAWN_PLAYERS.add(player.getUUID());
            else if (!player.isCreative() && !player.isSpectator()) {
                MATCH_PLAYERS.add(player.getUUID());
                if (!player.isAlive()) ELIMINATED_PLAYERS.add(player.getUUID());
            }
        }
        MATCH_HAD_PARTICIPANTS = !MATCH_PLAYERS.isEmpty();
        Circle circle = new Circle(center, initial, end);
        circle.baseDamage = GLOBAL_BASE_DAMAGE; circle.damageIncrement = GLOBAL_DAMAGE_INCREMENT;
        circle.waiting = true;
        circle.initializePreview(level, level.getRandom());
        CIRCLES.put(new LevelKey(level.dimension()), circle);
        sendCircleSync(level, circle, true, false);
        source.sendSuccess(() -> Component.literal("毒圈已启动：第 1 圈固定在当前位置，下一圈位置已生成。"), true);
        return 1;
    }
    private static int setLobby(CommandSourceStack source, Vec3 center, double radius) {
        try { lobby = new LobbySettings(true, source.getLevel().dimension().identifier().toString(), center.x, center.y, center.z, radius); }
        catch (IllegalArgumentException exception) { source.sendFailure(Component.literal("大厅坐标及半径必须为有限值，半径范围为 1–29999984。")); return 0; }
        saveSettings(); withdrawLobbyPlayers(source.getServer());
        source.sendSuccess(() -> Component.literal("大厅已设置：" + lobby.dimension() + " XYZ " + lobby.x() + " / " + lobby.y() + " / " + lobby.z() + "，水平半径 " + lobby.radius() + " 格（全高度圆柱）。"), true);
        return 1;
    }
    private static int disableLobby(CommandSourceStack source) {
        if (lobby != null) lobby = lobby.disabled();
        saveSettings();
        source.sendSuccess(() -> Component.literal("大厅范围已停用；本局已退出的玩家保持免毒至下一局。"), true);
        return 1;
    }
    private static int lobbyStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(lobby == null ? "大厅尚未设置。" : "大厅" + (lobby.enabled() ? "启用" : "停用") + "：" + lobby.dimension() + " XYZ " + lobby.x() + " / " + lobby.y() + " / " + lobby.z() + "，水平半径 " + lobby.radius() + " 格（全高度圆柱）。"), false);
        return 1;
    }
    private static int setCenter(CommandSourceStack source, Vec3 center) {
        if (!Double.isFinite(center.x) || !Double.isFinite(center.z)) { source.sendFailure(Component.literal("中心必须为有限坐标。")); return 0; }
        LevelKey configuredKey = new LevelKey(source.getLevel().dimension());
        if (!CIRCLES.containsKey(configuredKey)) {
            CONFIGURED_CENTERS.put(configuredKey, new Vec3(center.x, 0, center.z));
            saveSettings();
            source.sendSuccess(() -> Component.literal("毒圈初始中心已保存，将用于下一次 /poisoncircle start。"), true);
            return 1;
        }
        Circle circle = CIRCLES.get(new LevelKey(source.getLevel().dimension()));
        if (circle == null) { source.sendFailure(Component.literal("当前维度没有活动毒圈。")); return 0; }
        if (circle.stage != 0 || circle.elapsed != 0) { source.sendFailure(Component.literal("第一圈已经开始，不能再修改初始中心。")); return 0; }
        if (!circleFits(source.getLevel().getWorldBorder(), center.x, center.z, circle.startRadius)) { source.sendFailure(Component.literal("该中心会让初始毒圈超出世界边界。")); return 0; }
        circle.startCenter = new Vec3(center.x, 0, center.z); circle.nextCenter = circle.startCenter;
        CONFIGURED_CENTERS.put(configuredKey, circle.startCenter);
        saveSettings();
        sendCircleSync(source.getLevel(), circle, true, false); source.sendSuccess(() -> Component.literal("毒圈初始中心已更新。"), true); return 1;
    }
    private static int setDamage(CommandSourceStack source, double base, Double increment) {
        if (!Double.isFinite(base) || base < 0 || (increment != null && (!Double.isFinite(increment) || increment < 0))) { source.sendFailure(Component.literal("伤害必须为有限的非负数。")); return 0; }
        Circle circle = CIRCLES.get(new LevelKey(source.getLevel().dimension()));
        GLOBAL_BASE_DAMAGE = base; if (increment != null) GLOBAL_DAMAGE_INCREMENT = increment;
        if (circle != null) { circle.baseDamage = GLOBAL_BASE_DAMAGE; circle.damageIncrement = GLOBAL_DAMAGE_INCREMENT; }
        saveSettings();
        source.sendSuccess(() -> Component.literal("毒圈伤害：基础 " + base + "，每圈增加 " + GLOBAL_DAMAGE_INCREMENT), true); return 1;
    }
    private static int setRoundTime(CommandSourceStack source, int round, int waitSeconds, int shrinkSeconds) {
        if (waitSeconds > 86400 || shrinkSeconds > 86400) { source.sendFailure(Component.literal("每阶段时长不能超过 86400 秒。")); return 0; }
        ROUND_DURATIONS.setTimes(round, waitSeconds, shrinkSeconds);
        saveSettings();
        for (Map.Entry<LevelKey, Circle> entry : CIRCLES.entrySet()) {
            Circle circle = entry.getValue();
            circle.phaseDurations[round - 1] = shrinkSeconds * 20;
            circle.waitDurations[round - 1] = waitSeconds * 20;
            ServerLevel level = source.getServer().getLevel(entry.getKey().key());
            if (level != null) sendCircleSync(level, circle, true, false);
        }
        source.sendSuccess(() -> Component.literal("第 " + round + " 圈等待 " + waitSeconds + " 秒，缩圈 " + shrinkSeconds + " 秒，立即生效。"), true);
        return 1;
    }
    private static int setRoundCommand(CommandSourceStack source, int round, String command) {
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        ROUND_COMMANDS.set(round, normalized);
        saveSettings();
        source.sendSuccess(() -> Component.literal("第 " + round + " 圈开始缩圈时将执行：" + normalized), true);
        return 1;
    }
    private static void executeRoundCommand(MinecraftServer server, int round) {
        String command = ROUND_COMMANDS.get(round);
        if (command != null && !command.isBlank()) server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }
    private static int giveDetector(CommandSourceStack source) {
        ServerPlayer player; try { player = source.getPlayerOrException(); } catch (Exception ignored) { source.sendFailure(Component.literal("此指令只能由玩家执行。")); return 0; }
        player.getInventory().placeItemBackInInventory(new ItemStack(DETECTOR), net.minecraft.util.Prediction.SERVER_ONLY); source.sendSuccess(() -> Component.literal("已获得毒圈探测器。"), false); return 1;
    }
    private static int stop(CommandSourceStack source) {
        if (CIRCLES.isEmpty() && !WORLD_COLLAPSED) return noCircle(source);
        stopAll(source.getServer()); source.sendSuccess(() -> Component.literal("毒圈已停止。"), true); return 1;
    }
    private static int noCircle(CommandSourceStack source) { source.sendFailure(Component.literal("当前维度没有活动毒圈。")); return 0; }
    private static boolean circleFits(WorldBorder border, double x, double z, double radius) { return x - radius >= border.getMinX() && x + radius <= border.getMaxX() && z - radius >= border.getMinZ() && z + radius <= border.getMaxZ(); }
    private static CompletableFuture<Suggestions> suggestCurrentX(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) { return SharedSuggestionProvider.suggest(List.of(String.format("%.2f", c.getSource().getPosition().x)), b); }
    private static CompletableFuture<Suggestions> suggestCurrentY(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) { return SharedSuggestionProvider.suggest(List.of(String.format("%.2f", c.getSource().getPosition().y)), b); }
    private static CompletableFuture<Suggestions> suggestCurrentZ(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) { return SharedSuggestionProvider.suggest(List.of(String.format("%.2f", c.getSource().getPosition().z)), b); }
    private record LevelKey(ResourceKey<Level> key) {}

    private static final class Circle {
        Vec3 startCenter, nextCenter;
        final double startRadius, endRadius;
        double nextRadius;
        CirclePreview preview;
        final int[] phaseDurations = new int[MAX_SHRINKS], waitDurations = new int[MAX_SHRINKS];
        int stage, elapsed;
        boolean waiting;
        double baseDamage = 2, damageIncrement = 2;
        Circle(Vec3 center, double initial, double end) {
            startCenter = new Vec3(center.x, 0, center.z); nextCenter = startCenter; startRadius = initial; endRadius = end; nextRadius = radiusForStage(1);
            System.arraycopy(ROUND_DURATIONS.copyShrinkTicks(), 0, phaseDurations, 0, MAX_SHRINKS);
            System.arraycopy(ROUND_DURATIONS.copyWaitTicks(), 0, waitDurations, 0, MAX_SHRINKS);
        }
        void initializePreview(ServerLevel level, RandomSource random) { preview = createPreview(level, random, nextCenter, nextRadius, stage + 2); }
        void promotePreview(ServerLevel level, RandomSource random) {
            if (preview != null) {
                CirclePreview announced = preview.promote();
                nextCenter = new Vec3(announced.x(), 0, announced.z());
                nextRadius = announced.radius();
            }
            preview = createPreview(level, random, nextCenter, nextRadius, stage + 2);
        }
        private CirclePreview createPreview(ServerLevel level, RandomSource random, Vec3 fromCenter, double fromRadius, int targetStage) {
            if (targetStage > MAX_SHRINKS) return null;
            double targetRadius = radiusForStage(targetStage);
            Vec3 target = randomLegalCenter(level, random, fromCenter, fromRadius, targetRadius);
            return new CirclePreview(target.x, target.z, targetRadius);
        }
        boolean hasPreview() { return preview != null; }
        double previewX() { return preview == null ? nextCenter.x : preview.x(); }
        double previewZ() { return preview == null ? nextCenter.z : preview.z(); }
        double previewRadius() { return preview == null ? nextRadius : preview.radius(); }
        void finishShrink() { startCenter = nextCenter; stage++; elapsed = 0; }
        double currentX() { return lerpCenter().x; }
        double currentZ() { return lerpCenter().z; }
        double currentRadius() { return startRadius() + (nextRadius - startRadius()) * progress(); }
        Vec3 lerpCenter() { double p = progress(); return new Vec3(startCenter.x + (nextCenter.x - startCenter.x) * p, 0, startCenter.z + (nextCenter.z - startCenter.z) * p); }
        double startRadius() { return radiusForStage(stage); }
        double radiusForStage(int completed) { return startRadius + (endRadius - startRadius) * Math.min(completed, MAX_SHRINKS) / MAX_SHRINKS; }
        double progress() { return waiting ? 0 : Math.min(1, (double) elapsed / shrinkDuration()); }
        int shrinkDuration() { return phaseDurations[Math.min(stage, MAX_SHRINKS - 1)]; }
        int waitDuration() { return waitDurations[Math.min(stage, MAX_SHRINKS - 1)]; }
        int remainingTicks() { return (waiting ? waitDuration() : shrinkDuration()) - elapsed; }
        boolean outside(double x, double z) { double dx = x - currentX(), dz = z - currentZ(), r = currentRadius(); return dx * dx + dz * dz > r * r; }
        double damage() { return baseDamage + damageIncrement * stage; }
    }
    private static Vec3 randomLegalCenter(ServerLevel level, RandomSource random, Vec3 current, double currentRadius, double nextRadius) {
        WorldBorder border = level.getWorldBorder();
        double maxOffset = Math.max(0, currentRadius - nextRadius);
        Vec3 bestFlat = null;
        Vec3 bestFallback = null;
        int bestFlatScore = Integer.MAX_VALUE;
        int bestFallbackScore = Integer.MAX_VALUE;
        int sampleDistance = Math.max(6, Math.min(24, (int) (nextRadius / 4)));
        candidateSearch: for (int i = 0; i < 160; i++) {
            double a = random.nextDouble() * Math.PI * 2, d = Math.sqrt(random.nextDouble()) * maxOffset;
            double x = current.x + Math.cos(a) * d, z = current.z + Math.sin(a) * d;
            if (!circleFits(border, x, z, nextRadius)) continue;
            int[] heights = new int[9];
            boolean hasFluid = false;
            int index = 0;
            for (int offsetX : new int[] {0, -sampleDistance, sampleDistance}) for (int offsetZ : new int[] {0, -sampleDistance, sampleDistance}) {
                int sampleX = (int) Math.floor(x) + offsetX;
                int sampleZ = (int) Math.floor(z) + offsetZ;
                // Terrain scoring must never synchronously generate chunks during circle updates.
                if (level.getChunkSource().getChunkNow(sampleX >> 4, sampleZ >> 4) == null) continue candidateSearch;
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sampleX, sampleZ);
                heights[index++] = height;
                hasFluid |= !level.getFluidState(new BlockPos(sampleX, height - 1, sampleZ)).isEmpty();
            }
            if (hasFluid) continue;
            int score = PoisonCircleTerrainQuality.score(heights, level.getSeaLevel());
            Vec3 candidate = new Vec3(x, 0, z);
            if (score < bestFallbackScore) { bestFallback = candidate; bestFallbackScore = score; }
            if (PoisonCircleTerrainQuality.accepts(heights, level.getSeaLevel(), false) && score < bestFlatScore) {
                bestFlat = candidate;
                bestFlatScore = score;
            }
        }
        if (bestFlat != null) return bestFlat;
        if (bestFallback != null) return bestFallback;
        double x = Math.max(border.getMinX() + nextRadius, Math.min(border.getMaxX() - nextRadius, current.x));
        double z = Math.max(border.getMinZ() + nextRadius, Math.min(border.getMaxZ() - nextRadius, current.z));
        return new Vec3(x, 0, z);
    }
}

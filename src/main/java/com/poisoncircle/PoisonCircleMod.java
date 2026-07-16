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
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mod(PoisonCircleMod.MOD_ID)
@EventBusSubscriber(modid = PoisonCircleMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PoisonCircleMod {
    public static final String MOD_ID = "poisoncircle";
    private static final int MAX_SHRINKS = 5;
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<PoisonCircleVisualEntity>> POISON_CIRCLE_VISUAL = ENTITY_TYPES.register("poison_circle_visual",
        () -> EntityType.Builder.of(PoisonCircleVisualEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).clientTrackingRange(512).updateInterval(1).build("poison_circle_visual"));
    public static final DeferredItem<PoisonCircleDetectorItem> DETECTOR = ITEMS.register("poison_circle_detector",
        () -> new PoisonCircleDetectorItem(new net.minecraft.world.item.Item.Properties().stacksTo(1)));
    private static final Map<LevelKey, Circle> CIRCLES = new HashMap<>();
    private static final int WALL_SEGMENT_COUNT = 96;
    private static final Map<LevelKey, List<PoisonCircleVisualEntity>> VISUALS = new HashMap<>();
    private static final Map<LevelKey, Vec3> CONFIGURED_CENTERS = new HashMap<>();
    private static final RoundDurationSettings ROUND_DURATIONS = new RoundDurationSettings(MAX_SHRINKS, 120);
    private static final RoundCommandSettings ROUND_COMMANDS = new RoundCommandSettings();
    private static boolean WORLD_COLLAPSED;
    private static double GLOBAL_BASE_DAMAGE = 2;
    private static double GLOBAL_DAMAGE_INCREMENT = 2;

    public PoisonCircleMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(PoisonCircleMod::registerPayloads);
        modEventBus.addListener(PoisonCircleMod::addCreative);
    }

    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(DETECTOR);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MOD_ID).versioned("4");
        registrar.playToClient(CircleSyncPayload.TYPE, CircleSyncPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> PoisonCircleClient.apply(payload)));
        registrar.playToClient(PoisonHitPayload.TYPE, PoisonHitPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(PoisonCircleClient::shakeFromPoisonHit));
        registrar.playToClient(DetectorRevealPayload.TYPE, DetectorRevealPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> PoisonCircleClient.applyDetectorReveal(payload)));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("poisoncircle").requires(source -> source.hasPermission(2))
            .then(Commands.literal("start").executes(c -> start(c.getSource(), 500, 50))
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
            .then(Commands.literal("detector").executes(c -> giveDetector(c.getSource())))
            .then(Commands.literal("status").executes(c -> status(c.getSource())))
            .then(Commands.literal("stop").executes(c -> stop(c.getSource()))));
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (WORLD_COLLAPSED) {
            discardAllVisuals();
            for (ServerLevel level : server.getAllLevels()) applyWorldDamage(level);
            int eligible = 0, living = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isCreative() || player.isSpectator()) continue;
                eligible++;
                if (player.isAlive()) living++;
            }
            if (LastCircleStopCondition.shouldStop(eligible, living)) {
                WORLD_COLLAPSED = false;
                broadcast(server, "第五圈结束：所有玩家已死亡，毒圈已停止。");
            }
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
                    discardAllVisuals();
                    sendCircleSync(level, circle, false, true);
                    CIRCLES.clear();
                    broadcast(server, "毒圈已完成第 5 次缩圈，所有世界现在都受到毒圈伤害！");
                    return;
                }
                circle.promotePreview(level, level.random);
                circle.waiting = true;
                circle.elapsed = 0;
                sendCircleSync(level, circle, true, false);
                broadcast(server, "第 " + (circle.stage + 1) + "/5 圈安全区已刷新，" + circle.waitDuration() / 20 + " 秒后开始缩小。");
            }
            updateVisual(level, circle);
            if (circle.elapsed % 5 == 0) sendCircleSync(level, circle, true, false);
        }
    }

    private static void applyCircleDamage(ServerLevel level, Circle circle) {
        for (ServerPlayer player : level.players()) if (!player.isCreative() && !player.isSpectator() && circle.outside(player.getX(), player.getZ())) applyDamage(level, player, circle.damage());
    }
    private static void applyWorldDamage(ServerLevel level) {
        for (ServerPlayer player : level.players()) if (!player.isCreative() && !player.isSpectator()) applyDamage(level, player, GLOBAL_BASE_DAMAGE + GLOBAL_DAMAGE_INCREMENT * MAX_SHRINKS);
    }
    private static void applyDamage(ServerLevel level, ServerPlayer player, double amount) {
        player.setHealth(DirectHealthDamage.remainingHealth(player.getHealth(), amount));
        PacketDistributor.sendToPlayer(player, PoisonHitPayload.INSTANCE);
    }

    private static void sendCircleSync(ServerLevel level, Circle circle, boolean active, boolean collapsed) {
        PacketDistributor.sendToAllPlayers(new CircleSyncPayload(level.dimension().location(), circle.currentX(), circle.currentZ(), circle.currentRadius(),
            circle.nextCenter.x, circle.nextCenter.z, circle.nextRadius, circle.stage + 1, Math.max(0, circle.remainingTicks() / 20),
            Math.max(0, circle.remainingTicks()),
            circle.waiting, active, collapsed, !collapsed && circle.stage < MAX_SHRINKS));
    }
    static boolean revealPreview(ServerPlayer player) {
        Circle circle = CIRCLES.get(new LevelKey(player.level().dimension()));
        if (circle == null || !circle.hasPreview()) return false;
        PacketDistributor.sendToPlayer(player, new DetectorRevealPayload(player.level().dimension().location(), circle.previewX(), circle.previewZ(), circle.previewRadius()));
        return true;
    }
    private static void broadcast(MinecraftServer server, String message) { server.getPlayerList().broadcastSystemMessage(Component.literal(message), false); }

    private static int start(CommandSourceStack source, double initial, double end) {
        if (end >= initial) { source.sendFailure(Component.literal("最终半径必须小于初始半径。")); return 0; }
        ServerLevel level = source.getLevel(); Vec3 center = CONFIGURED_CENTERS.getOrDefault(new LevelKey(level.dimension()), source.getPosition());
        if (!circleFits(level.getWorldBorder(), center.x, center.z, initial)) { source.sendFailure(Component.literal("初始毒圈会超出世界边界，请站到更靠内的位置或减小半径。")); return 0; }
        WORLD_COLLAPSED = false; GLOBAL_BASE_DAMAGE = 2; GLOBAL_DAMAGE_INCREMENT = 2;
        Circle circle = new Circle(center, initial, end);
        circle.waiting = true;
        circle.initializePreview(level, level.random);
        CIRCLES.put(new LevelKey(level.dimension()), circle);
        updateVisual(level, circle);
        sendCircleSync(level, circle, true, false);
        source.sendSuccess(() -> Component.literal("毒圈已启动：第 1 圈固定在当前位置，下一圈位置已生成。"), true);
        return 1;
    }
    private static int setCenter(CommandSourceStack source, Vec3 center) {
        LevelKey configuredKey = new LevelKey(source.getLevel().dimension());
        if (!CIRCLES.containsKey(configuredKey)) {
            CONFIGURED_CENTERS.put(configuredKey, new Vec3(center.x, 0, center.z));
            source.sendSuccess(() -> Component.literal("毒圈初始中心已保存，将用于下一次 /poisoncircle start。"), true);
            return 1;
        }
        Circle circle = CIRCLES.get(new LevelKey(source.getLevel().dimension()));
        if (circle == null) { source.sendFailure(Component.literal("当前维度没有活动毒圈。")); return 0; }
        if (circle.stage != 0 || circle.elapsed != 0) { source.sendFailure(Component.literal("第一圈已经开始，不能再修改初始中心。")); return 0; }
        if (!circleFits(source.getLevel().getWorldBorder(), center.x, center.z, circle.startRadius)) { source.sendFailure(Component.literal("该中心会让初始毒圈超出世界边界。")); return 0; }
        circle.startCenter = new Vec3(center.x, 0, center.z); circle.nextCenter = circle.startCenter;
        CONFIGURED_CENTERS.put(configuredKey, circle.startCenter);
        updateVisual(source.getLevel(), circle);
        sendCircleSync(source.getLevel(), circle, true, false); source.sendSuccess(() -> Component.literal("毒圈初始中心已更新。"), true); return 1;
    }
    private static int setDamage(CommandSourceStack source, double base, Double increment) {
        Circle circle = CIRCLES.get(new LevelKey(source.getLevel().dimension()));
        if (circle == null) { source.sendFailure(Component.literal("当前维度没有活动毒圈。")); return 0; }
        circle.baseDamage = base; if (increment != null) circle.damageIncrement = increment;
        GLOBAL_BASE_DAMAGE = circle.baseDamage; GLOBAL_DAMAGE_INCREMENT = circle.damageIncrement;
        source.sendSuccess(() -> Component.literal("毒圈伤害：基础 " + base + "，每圈增加 " + circle.damageIncrement), true); return 1;
    }
    private static int setRoundTime(CommandSourceStack source, int round, int waitSeconds, int shrinkSeconds) {
        ROUND_DURATIONS.setTimes(round, waitSeconds, shrinkSeconds);
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
        source.sendSuccess(() -> Component.literal("第 " + round + " 圈开始缩圈时将执行：" + normalized), true);
        return 1;
    }
    private static void executeRoundCommand(MinecraftServer server, int round) {
        String command = ROUND_COMMANDS.get(round);
        if (command != null && !command.isBlank()) server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput().withPermission(4), command);
    }
    private static int giveDetector(CommandSourceStack source) {
        ServerPlayer player; try { player = source.getPlayerOrException(); } catch (Exception ignored) { source.sendFailure(Component.literal("此指令只能由玩家执行。")); return 0; }
        player.getInventory().placeItemBackInInventory(new ItemStack(DETECTOR.get())); source.sendSuccess(() -> Component.literal("已获得毒圈探测器。"), false); return 1;
    }
    private static int status(CommandSourceStack source) {
        if (WORLD_COLLAPSED) { source.sendSuccess(() -> Component.literal("毒圈已消失：所有世界持续受到最高伤害。"), false); return 1; }
        Circle circle = CIRCLES.get(new LevelKey(source.getLevel().dimension())); if (circle == null) return noCircle(source);
        source.sendSuccess(() -> Component.literal("第 " + (circle.stage + 1) + "/5 圈，半径 " + Math.round(circle.currentRadius()) + "，" + (circle.waiting ? "等待" : "缩圈中") + "。"), false); return 1;
    }
    private static int stop(CommandSourceStack source) {
        Circle stopped = CIRCLES.remove(new LevelKey(source.getLevel().dimension()));
        if (stopped == null) return noCircle(source);
        discardVisual(source.getLevel());
        WORLD_COLLAPSED = false; sendCircleSync(source.getLevel(), stopped, false, false); source.sendSuccess(() -> Component.literal("毒圈已停止。"), true); return 1;
    }
    private static int noCircle(CommandSourceStack source) { source.sendFailure(Component.literal("当前维度没有活动毒圈。")); return 0; }
    private static boolean circleFits(WorldBorder border, double x, double z, double radius) { return x - radius >= border.getMinX() && x + radius <= border.getMaxX() && z - radius >= border.getMinZ() && z + radius <= border.getMaxZ(); }
    private static CompletableFuture<Suggestions> suggestCurrentX(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) { return SharedSuggestionProvider.suggest(List.of(String.format("%.2f", c.getSource().getPosition().x)), b); }
    private static CompletableFuture<Suggestions> suggestCurrentY(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) { return SharedSuggestionProvider.suggest(List.of(String.format("%.2f", c.getSource().getPosition().y)), b); }
    private static CompletableFuture<Suggestions> suggestCurrentZ(CommandContext<CommandSourceStack> c, SuggestionsBuilder b) { return SharedSuggestionProvider.suggest(List.of(String.format("%.2f", c.getSource().getPosition().z)), b); }
    private static void updateVisual(ServerLevel level, Circle circle) {
        discardVisual(level);
    }
    private static void discardVisual(ServerLevel level) {
        List<PoisonCircleVisualEntity> visuals = VISUALS.remove(new LevelKey(level.dimension()));
        if (visuals != null) for (PoisonCircleVisualEntity visual : visuals) if (!visual.isRemoved()) visual.discard();
    }
    private static void discardAllVisuals() {
        for (List<PoisonCircleVisualEntity> visuals : VISUALS.values()) for (PoisonCircleVisualEntity visual : visuals) if (!visual.isRemoved()) visual.discard();
        VISUALS.clear();
    }
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
        for (int i = 0; i < 160; i++) {
            double a = random.nextDouble() * Math.PI * 2, d = Math.sqrt(random.nextDouble()) * maxOffset;
            double x = current.x + Math.cos(a) * d, z = current.z + Math.sin(a) * d;
            if (!circleFits(border, x, z, nextRadius)) continue;
            int[] heights = new int[9];
            boolean hasFluid = false;
            int index = 0;
            for (int offsetX : new int[] {0, -sampleDistance, sampleDistance}) for (int offsetZ : new int[] {0, -sampleDistance, sampleDistance}) {
                int sampleX = (int) Math.floor(x) + offsetX;
                int sampleZ = (int) Math.floor(z) + offsetZ;
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

package com.poisoncircleforge;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.HashMap;

@Mod(PoisonCircleForge.MOD_ID)
public final class PoisonCircleForge {
    public static final String MOD_ID = "poisoncircleforge";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final RegistryObject<Item> DETECTOR = ITEMS.register("safe_zone_detector", () -> new SafeZoneDetectorItem(new Item.Properties().stacksTo(1)));
    private static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(new net.minecraft.resources.ResourceLocation(MOD_ID, "circle"), () -> "1", "1"::equals, "1"::equals);
    private static final int MAX_ROUNDS = 5;
    private static final Map<ResourceKey<Level>, Circle> CIRCLES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Vec3> CONFIGURED_CENTERS = new HashMap<>();
    private static final Map<Integer, String> ROUND_COMMANDS = new HashMap<>();

    public PoisonCircleForge() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        NETWORK.registerMessage(0, CircleSyncMessage.class, CircleSyncMessage::encode, CircleSyncMessage::decode, CircleSyncMessage::handle);
        NETWORK.registerMessage(1, DetectorRevealMessage.class, DetectorRevealMessage::encode, DetectorRevealMessage::decode, DetectorRevealMessage::handle);
        MinecraftForge.EVENT_BUS.register(PoisonCircleForge.class);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("poisoncircle").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("start").executes(c -> start(c.getSource(), 500, 50)).then(Commands.argument("initialRadius", DoubleArgumentType.doubleArg(1)).then(Commands.argument("finalRadius", DoubleArgumentType.doubleArg(1)).executes(c -> start(c.getSource(), DoubleArgumentType.getDouble(c, "initialRadius"), DoubleArgumentType.getDouble(c, "finalRadius"))))));
        root.then(Commands.literal("center").then(Commands.argument("x", DoubleArgumentType.doubleArg()).then(Commands.argument("y", DoubleArgumentType.doubleArg()).then(Commands.argument("z", DoubleArgumentType.doubleArg()).executes(c -> center(c.getSource(), new Vec3(DoubleArgumentType.getDouble(c, "x"), DoubleArgumentType.getDouble(c, "y"), DoubleArgumentType.getDouble(c, "z"))))))));
        root.then(Commands.literal("time").then(Commands.argument("round", IntegerArgumentType.integer(1, MAX_ROUNDS)).then(Commands.argument("waitSeconds", IntegerArgumentType.integer(0)).then(Commands.argument("shrinkSeconds", IntegerArgumentType.integer(1)).executes(c -> time(c.getSource(), IntegerArgumentType.getInteger(c, "round"), IntegerArgumentType.getInteger(c, "waitSeconds"), IntegerArgumentType.getInteger(c, "shrinkSeconds")))))));
        root.then(Commands.literal("damage").then(Commands.argument("base", DoubleArgumentType.doubleArg(0)).executes(c -> damage(c.getSource(), DoubleArgumentType.getDouble(c, "base"), -1)).then(Commands.argument("increment", DoubleArgumentType.doubleArg(0)).executes(c -> damage(c.getSource(), DoubleArgumentType.getDouble(c, "base"), DoubleArgumentType.getDouble(c, "increment"))))));
        root.then(Commands.literal("command").then(Commands.argument("round", IntegerArgumentType.integer(1, MAX_ROUNDS)).then(Commands.argument("command", StringArgumentType.greedyString()).executes(c -> setRoundCommand(c.getSource(), IntegerArgumentType.getInteger(c, "round"), StringArgumentType.getString(c, "command"))))));
        root.then(Commands.literal("detector").executes(c -> giveDetector(c.getSource())));
        root.then(Commands.literal("status").executes(c -> status(c.getSource())));
        root.then(Commands.literal("stop").executes(c -> stop(c.getSource())));
        event.getDispatcher().register(root);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        for (Map.Entry<ResourceKey<Level>, Circle> entry : new HashMap<>(CIRCLES).entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) continue;
            Circle circle = entry.getValue();
            if (circle.finished) { if (circle.elapsed++ % 20 == 0) damageOutside(level, circle); continue; }
            circle.elapsed++;
            if (circle.elapsed % 20 == 0) damageOutside(level, circle);
            if (circle.waiting && circle.elapsed >= circle.waitTicks()) {
                circle.waiting = false; circle.elapsed = 0;
                broadcast(server, "毒圈第 " + (circle.round + 1) + "/5 圈开始缩小！");
            } else if (!circle.waiting && circle.elapsed >= circle.shrinkTicks()) {
                clearReveals(level); circle.center = circle.target; circle.round++; circle.elapsed = 0;
                executeRoundCommand(server, circle.round);
                if (circle.round >= MAX_ROUNDS) { circle.round = MAX_ROUNDS; circle.finished = true; sync(level, circle); continue; }
                if (circle.round >= MAX_ROUNDS) { CIRCLES.remove(entry.getKey()); broadcast(server, "第五圈完成，毒圈已结束。"); continue; }
                circle.target = chooseCenter(level, level.random, circle.center, circle.radiusFor(circle.round), circle.radiusFor(circle.round + 1));
                circle.waiting = true;
                broadcast(server, "第 " + (circle.round + 1) + "/5 圈安全区已刷新，等待 " + circle.waitTicks() / 20 + " 秒后缩小。");
            }
            for (ServerPlayer player : level.players()) if (!player.isCreative() && !player.isSpectator()) {
                player.displayClientMessage(Component.literal("毒圈 第 " + (circle.round + 1) + "/5 圈 | " + (circle.waiting ? "距离缩圈: " : "缩圈剩余: ") + Math.max(0, circle.remainingTicks() / 20) + "秒"), true);
            }
            if (circle.elapsed % 2 == 0) sync(level, circle);
        }
    }

    private static int center(CommandSourceStack source, Vec3 position) {
        CONFIGURED_CENTERS.put(source.getLevel().dimension(), new Vec3(position.x, 0, position.z));
        source.sendSuccess(() -> Component.literal("毒圈初始中心已设置。"), true); return 1;
    }
    private static int start(CommandSourceStack source, double initial, double end) {
        if (end >= initial) { source.sendFailure(Component.literal("最终半径必须小于初始半径。")); return 0; }
        ServerLevel level = source.getLevel(); Vec3 center = CONFIGURED_CENTERS.getOrDefault(level.dimension(), source.getPosition());
        if (!fits(level.getWorldBorder(), center.x, center.z, initial)) { source.sendFailure(Component.literal("初始毒圈超出世界边界。")); return 0; }
        Circle circle = new Circle(center, initial, end); CIRCLES.put(level.dimension(), circle);
        sync(level, circle);
        source.sendSuccess(() -> Component.literal("毒圈已启动。"), true); return 1;
    }
    private static int time(CommandSourceStack source, int round, int wait, int shrink) {
        for (Circle circle : CIRCLES.values()) { circle.wait[round - 1] = wait * 20; circle.shrink[round - 1] = shrink * 20; }
        source.sendSuccess(() -> Component.literal("第 " + round + " 圈等待 " + wait + " 秒，缩圈 " + shrink + " 秒。"), true); return 1;
    }
    private static int damage(CommandSourceStack source, double base, double increment) {
        Circle circle = CIRCLES.get(source.getLevel().dimension()); if (circle == null) { source.sendFailure(Component.literal("当前维度没有毒圈。")); return 0; }
        circle.baseDamage = base; if (increment >= 0) circle.increment = increment;
        source.sendSuccess(() -> Component.literal("毒圈伤害已更新。"), true); return 1;
    }
    private static int setRoundCommand(CommandSourceStack source, int round, String command) {
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        ROUND_COMMANDS.put(round, normalized);
        source.sendSuccess(() -> Component.literal("第 " + round + " 圈缩完后执行：" + normalized), true);
        return 1;
    }
    private static void executeRoundCommand(MinecraftServer server, int round) {
        String command = ROUND_COMMANDS.get(round);
        if (command != null && !command.isBlank()) server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput().withPermission(4), command);
    }
    private static int status(CommandSourceStack source) {
        Circle circle = CIRCLES.get(source.getLevel().dimension()); if (circle == null) { source.sendFailure(Component.literal("当前维度没有毒圈。")); return 0; }
        source.sendSuccess(() -> Component.literal("第 " + (circle.round + 1) + "/5 圈，半径 " + Math.round(circle.currentRadius()) + "。"), false); return 1;
    }
    private static int giveDetector(CommandSourceStack source) { try { source.getPlayerOrException().getInventory().placeItemBackInInventory(new ItemStack(DETECTOR.get())); source.sendSuccess(() -> Component.literal("已获得安全区探测器。"), false); return 1; } catch (Exception e) { source.sendFailure(Component.literal("此指令只能由玩家执行。")); return 0; } }
    static boolean reveal(ServerPlayer player) { Circle c=CIRCLES.get(player.level().dimension()); if(c==null) return false; player.displayClientMessage(Component.literal("下一安全区：半径 " + Math.round(c.radiusFor(Math.min(MAX_ROUNDS,c.round+1))) + "，已在地图上以白色圆环显示。"), true); return true; }
    private static void clearReveals(ServerLevel level) {
        DetectorRevealMessage clear = new DetectorRevealMessage(level.dimension().location().toString(), 0, 0, 0, false);
        for (ServerPlayer p : level.players()) NETWORK.send(PacketDistributor.PLAYER.with(() -> p), clear);
    }
    static boolean revealBlue(ServerPlayer player) {
        Circle c = CIRCLES.get(player.level().dimension());
        if (c == null) return false;
        int nextStage = Math.min(MAX_ROUNDS, c.round + 1);
        double r = c.radiusFor(nextStage);
        Vec3 nextNext = chooseCenter((ServerLevel) player.level(), player.getRandom(), c.target, r, c.radiusFor(Math.min(MAX_ROUNDS, nextStage + 1)));
        DetectorRevealMessage reveal = new DetectorRevealMessage(player.level().dimension().location().toString(), nextNext.x, nextNext.z, c.radiusFor(Math.min(MAX_ROUNDS, nextStage + 1)), true);
        for (ServerPlayer p : player.getServer().getPlayerList().getPlayers()) if (p == player || (player.getTeam() != null && player.getTeam() == p.getTeam())) NETWORK.send(PacketDistributor.PLAYER.with(() -> p), reveal);
        player.displayClientMessage(Component.literal("安全区探测器：下下个圈已用青蓝色圆环显示。"), true);
        return true;
    }
    private static int stop(CommandSourceStack source) {
        Circle stopped = CIRCLES.remove(source.getLevel().dimension());
        if (stopped == null) return 0;
        ServerLevel level = source.getLevel();
        clearReveals(level);
        CircleSyncMessage clear = new CircleSyncMessage(level.dimension().location().toString(), 0, 0, 0, 0, 0, 0, 0, 0, true, false);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) NETWORK.send(PacketDistributor.PLAYER.with(() -> player), clear);
        source.sendSuccess(() -> Component.literal("毒圈已停止并清除边界。"), true);
        return 1;
    }
    private static void sync(ServerLevel level, Circle circle) {
        double nextRadius = circle.radiusFor(Math.min(MAX_ROUNDS, circle.round + 1));
        NETWORK.send(PacketDistributor.ALL.noArg(), new CircleSyncMessage(level.dimension().location().toString(), circle.currentX(), circle.currentZ(), circle.currentRadius(), circle.target.x, circle.target.z, nextRadius, circle.round + 1, Math.max(0, circle.remainingTicks()), circle.waiting, true));
    }
    private static void damageOutside(ServerLevel level, Circle circle) {
        for (ServerPlayer player : level.players()) if (player.isAlive() && !player.isCreative() && !player.isSpectator() && circle.outside(player.getX(), player.getZ())) {
            float damage = (float) (circle.baseDamage + circle.increment * circle.round);
            player.hurt(level.damageSources().magic(), damage);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, false, true));
        }
    }
    private static void broadcast(MinecraftServer server, String message) { server.getPlayerList().broadcastSystemMessage(Component.literal(message), false); }
    private static boolean fits(WorldBorder border, double x, double z, double radius) { return x - radius >= border.getMinX() && x + radius <= border.getMaxX() && z - radius >= border.getMinZ() && z + radius <= border.getMaxZ(); }
    private static Vec3 chooseCenter(ServerLevel level, RandomSource random, Vec3 current, double currentRadius, double nextRadius) {
        WorldBorder border = level.getWorldBorder(); Vec3 best = null; int bestScore = Integer.MAX_VALUE; double maxOffset = currentRadius - nextRadius;
        for (int attempt = 0; attempt < 120; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, distance = Math.sqrt(random.nextDouble()) * maxOffset;
            double x = current.x + Math.cos(angle) * distance, z = current.z + Math.sin(angle) * distance;
            if (!fits(border, x, z, nextRadius)) continue;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, total = 0; boolean fluid = false;
            for (int ox : new int[] {0, -16, 16}) for (int oz : new int[] {0, -16, 16}) {
                int sx = (int) x + ox, sz = (int) z + oz, height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
                min = Math.min(min, height); max = Math.max(max, height); total += height;
                fluid |= !level.getFluidState(new BlockPos(sx, height - 1, sz)).isEmpty();
            }
            int score = (max - min) * 1000 + Math.max(0, total / 9 - level.getSeaLevel());
            if (!fluid && score < bestScore) { best = new Vec3(x, 0, z); bestScore = score; if (max - min <= 4 && total / 9 <= level.getSeaLevel() + 32) return best; }
        }
        return best == null ? current : best;
    }
    private static final class Circle {
        Vec3 center, target; final double initial, end; int round, elapsed; boolean waiting = true, finished; double baseDamage = 2, increment = 2;
        final int[] wait = {2400, 2400, 2400, 2400, 2400}; final int[] shrink = {2400, 2400, 2400, 2400, 2400};
        Circle(Vec3 center, double initial, double end) { this.center = new Vec3(center.x, 0, center.z); this.target = center; this.initial = initial; this.end = end; }
        double radiusFor(int stage) { return initial + (end - initial) * Math.min(stage, MAX_ROUNDS) / MAX_ROUNDS; }
        double progress() { return waiting ? 0 : Math.min(1, (double) elapsed / shrinkTicks()); }
        double currentRadius() { return radiusFor(round) + (radiusFor(round + 1) - radiusFor(round)) * progress(); }
        double currentX() { return center.x + (target.x - center.x) * progress(); }
        double currentZ() { return center.z + (target.z - center.z) * progress(); }
        int waitTicks() { return wait[Math.min(round, MAX_ROUNDS - 1)]; } int shrinkTicks() { return shrink[Math.min(round, MAX_ROUNDS - 1)]; }
        int remainingTicks() { return (waiting ? waitTicks() : shrinkTicks()) - elapsed; }
        boolean outside(double x, double z) { double dx = x - currentX(), dz = z - currentZ(), r = currentRadius(); return dx * dx + dz * dz > r * r; }
    }
}

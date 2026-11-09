package com.poisoncircle;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraft.world.scores.PlayerTeam;
import org.slf4j.*;

/** Single server-thread-owned match; configuration and roster are locked until stopped. */
public final class AirborneGame {
    private static final Logger LOG=LoggerFactory.getLogger("poisoncircle-airborne");
    private static final Gson JSON=new Gson();
    private static final int MAX_GROUPS=8,MAX_POINTS=8,MAX_PLAYERS=32;
    private static final ResourceKey<EntityType<?>> PLANE_KEY=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID,"airborne_plane"));
    public static final EntityType<AirbornePlaneEntity> PLANE_TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,PLANE_KEY,EntityType.Builder.of(AirbornePlaneEntity::new,MobCategory.MISC).sized(2.5F,1.2F).clientTrackingRange(128).updateInterval(1).noSummon().build(PLANE_KEY));
    private static ResourceKey<Item> itemKey(String name){return ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(PoisonCircleMod.MOD_ID,name));}
    private static final ResourceKey<Item> MANAGER_KEY=itemKey("airborne_manager"),SETTER_KEY=itemKey("airborne_setter");
    public static final Item MANAGER=Registry.register(BuiltInRegistries.ITEM,MANAGER_KEY,new Item(new Item.Properties().setId(MANAGER_KEY).stacksTo(1)));
    public static final Item SETTER=Registry.register(BuiltInRegistries.ITEM,SETTER_KEY,new SetterItem(new Item.Properties().setId(SETTER_KEY).stacksTo(1)));
    private static final Map<String,Group> GROUPS=new LinkedHashMap<>();
    private static final Map<UUID,PlayerState> PLAYERS=new LinkedHashMap<>();
    private static final List<Route> ROUTES=new ArrayList<>();
    private static final Set<String> MATCH_TEAMS=new HashSet<>();
    private static final Map<UUID,Integer> LAST_ADMIN=new HashMap<>();
    private static MinecraftServer server;
    private static String activeMode="solo",loadError="";
    private static String selectedGroup="";
    private static Group draft=new Group("","solo");
    private static String selected(){return selectedGroup;}
    private static Group working(){return working(selected());}
    private static Group working(String id){return id.isEmpty()?draft:GROUPS.get(id);}
    private static double height=200,speed=.8;
    private static boolean active;
    private static int countdown;
    private AirborneGame(){}
    public static void register(){
        PayloadTypeRegistry.serverboundPlay().register(AirborneAdminPayload.TYPE,AirborneAdminPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AirborneInputPayload.TYPE,AirborneInputPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AirborneStatePayload.TYPE,AirborneStatePayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(AirborneAdminPayload.TYPE,(packet,c)->admin(c.server(),c.player(),packet));
        ServerPlayNetworking.registerGlobalReceiver(AirborneInputPayload.TYPE,(packet,c)->input(c.player(),packet));
        ServerTickEvents.END_SERVER_TICK.register(AirborneGame::tick);
        ServerLifecycleEvents.SERVER_STARTED.register(AirborneGame::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(AirborneGame::stop);
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{server=null;GROUPS.clear();});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,s)->abandon(handler.player));
        ServerPlayConnectionEvents.JOIN.register((handler,sender,s)->send(handler.player,""));
    }
    public static boolean isActive(){return active;}
    public static boolean isParticipant(UUID id){return active&&PLAYERS.containsKey(id);}
    public static boolean isAirborne(UUID id){PlayerState p=PLAYERS.get(id);return active&&p!=null&&p.phase!=Phase.LANDED&&p.phase!=Phase.DONE;}
    public static boolean controlsMovement(UUID id){PlayerState p=PLAYERS.get(id);return active&&p!=null&&(p.phase==Phase.ABOARD||p.phase==Phase.DIVING||p.phase==Phase.LANDING);}
    public static boolean protectsFall(UUID id){PlayerState p=PLAYERS.get(id);return p!=null&&(isAirborne(id)||(server!=null&&p.fallProtectionUntil>=server.getTickCount()));}
    public static Set<UUID> participatingIds(){return Set.copyOf(PLAYERS.keySet());}
    public static void markAirborneAbandoned(UUID id){PlayerState p=PLAYERS.get(id);if(p!=null){p.phase=Phase.DONE;PoisonCircleMod.markAirborneAbandoned(id);}}
    private static boolean operator(ServerPlayer p){return p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);}
    public static int start(CommandSourceStack source){
        try{
            require(!active,"已有空降比赛，请先停止");require(loadError.isEmpty(),loadError);MinecraftServer s=source.getServer();
            Group group=working();require(group!=null&&!group.points.isEmpty(),"请先选择分组并用设置器添加点位");
            List<ServerPlayer> roster=s.getPlayerList().getPlayers().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()).toList();
            require(!roster.isEmpty(),"没有生存/冒险模式参赛玩家");require(roster.size()<=MAX_PLAYERS,"每局最多32名玩家");
            List<UUID> ids=new ArrayList<>(roster.stream().map(ServerPlayer::getUUID).toList());if(activeMode.equals("duo"))Collections.shuffle(ids);List<List<UUID>> squads=AirborneRules.groups(ids,activeMode);
            List<Route> prepared=new ArrayList<>();
            for(int i=0;i<squads.size();i++){
                Point point=group.points.get(AirborneRules.cyclicPoint(i,group.points.size())).copy();ServerLevel level=level(s,point.dimension);
                float yaw=roster.get(0).getYRot()+i*17;double radians=Math.toRadians(yaw);Vec3 direction=new Vec3(-Math.sin(radians),0,Math.cos(radians));
                Vec3 origin=new Vec3(point.x,point.y,point.z).subtract(direction.scale(20));double terrain=point.y;
                for(int d=0;d<=180;d+=8){Vec3 sample=origin.add(direction.scale(d));validateXZ(level,sample.x,sample.z);loadAround(level,sample.x,sample.z);terrain=Math.max(terrain,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(int)Math.floor(sample.x),(int)Math.floor(sample.z)));}
                double cruise=point.y+height;require(cruise>terrain+16,"航线地形高于目标巡航高度，请提高飞行高度");double altitude=cruise+80;require(Double.isFinite(altitude)&&altitude<20_000_000,"高度超过游戏坐标安全范围");
                prepared.add(new Route(point,level,new Vec3(origin.x,altitude,origin.z),direction.scale(speed),yaw,squads.get(i)));
            }
            PLAYERS.clear();ROUTES.clear();ROUTES.addAll(prepared);server=s;for(ServerPlayer p:roster)PLAYERS.put(p.getUUID(),new PlayerState(p));
            try{int index=0;for(Route r:ROUTES){
                PlayerTeam team=null;String name=null;if(activeMode.equals("duo")){name="pc_air_"+UUID.randomUUID().toString().substring(0,8);team=s.getScoreboard().addPlayerTeam(name);MATCH_TEAMS.add(name);team.setDisplayName(Component.literal("双人队 "+(++index)));team.setAllowFriendlyFire(false);}
                for(UUID id:r.ids){PlayerState st=PLAYERS.get(id);st.target=r.point;st.route=r;st.matchTeam=name;ServerPlayer p=s.getPlayerList().getPlayer(id);if(team!=null)s.getScoreboard().addPlayerToTeam(p.getScoreboardName(),team);}
            }active=true;countdown=AirborneRules.COUNTDOWN_TICKS;PoisonCircleMod.beginAirborneMatch(s,ids);for(ServerPlayer p:roster)PoisonCircleMod.playScanSound(p,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT,.6F,1.2F);broadcast("比赛已锁定，5秒后登机");
            }catch(RuntimeException e){stop(s);throw e;}
            source.sendSuccess(()->Component.literal("空降比赛准备中："+roster.size()+"人，5秒后出发"),false);return 1;
        }catch(RuntimeException e){source.sendFailure(Component.literal(message(e)));return 0;}
    }
    private static void launch(){
        for(Route r:ROUTES){AirbornePlaneEntity plane=new AirbornePlaneEntity(PLANE_TYPE,r.level);plane.setPos(r.origin);
            plane.configure(r.velocity,r.yaw,Math.max(220,(int)Math.ceil(240.0/speed)),new Vec3(r.point.x,r.point.y,r.point.z));plane.setCruiseAltitude(r.point.y+height);r.plane=plane;r.departureAt=-1;require(r.level.addFreshEntity(plane),"无法生成运输机");
            for(UUID id:r.ids){PlayerState st=PLAYERS.get(id);ServerPlayer p=server.getPlayerList().getPlayer(id);if(p==null||!p.isAlive()||st.phase==Phase.DONE)continue;
                p.stopRiding();p.setNoGravity(true);p.fallDistance=0;
                require(p.teleportTo(r.level,r.origin.x,r.origin.y+1,r.origin.z,Set.of(),r.yaw,0,false),"无法传送玩家登机");require(p.startRiding(plane,true,true),"无法安排飞机座位");st.phase=Phase.ABOARD;st.boardedAt=server.getTickCount();var seat=AirborneDeparture.seat(r.ids.indexOf(id));plane.setCabinPosition(p,seat.x(),seat.y(),seat.z());p.setYRot(r.yaw+180);p.setXRot(0);st.velocity=plane.routeVelocity();p.setPos(plane.getPassengerRidingPosition(p));p.connection.resetPosition();}
        }broadcast("");
    }
    private static void input(ServerPlayer p,AirborneInputPayload in){PlayerState st=PLAYERS.get(p.getUUID());if(st==null||!controlsMovement(p.getUUID())||server==null)return;if(st.lastInput==server.getTickCount())return;st.lastInput=server.getTickCount();st.input=in;}
    private static void jump(ServerPlayer p,PlayerState st){Vec3 departure=p.position();p.stopRiding();require(!(p.getVehicle() instanceof AirbornePlaneEntity),"离机失败");require(p.teleportTo(p.level(),departure.x,departure.y,departure.z,Set.of(),p.getYRot(),p.getXRot(),false),"离机同步失败");p.connection.resetPosition();p.setNoGravity(true);p.fallDistance=0;st.phase=Phase.DIVING;p.setOnGround(false);Vec3 carried=st.route.velocity;st.velocity=new Vec3(carried.x,-.3,carried.z);st.input=new AirborneInputPayload(0,0,p.getYRot(),p.getXRot(),false);st.lastInput=server.getTickCount();}
    private static void tick(MinecraftServer s){
        if(s!=server)return;if(!active){if(s.getTickCount()%20==0)broadcast("");return;}try{
            for(PlayerState st:PLAYERS.values()){ServerPlayer p=s.getPlayerList().getPlayer(st.id);if(p==null){if(st.phase!=Phase.DONE)markAirborneAbandoned(st.id);continue;}if(st.phase==Phase.DONE)continue;
                if(!p.isAlive()||p.isCreative()||p.isSpectator()||(st.phase!=Phase.COUNTDOWN&&p.level()!=st.route.level)){finish(p,st,Phase.DONE);PoisonCircleMod.markAirborneAbandoned(st.id);}}
            if(countdown>0){countdown--;if(countdown%20==0)for(PlayerState st:PLAYERS.values()){ServerPlayer p=s.getPlayerList().getPlayer(st.id);if(p!=null&&st.phase==Phase.COUNTDOWN)PoisonCircleMod.playScanSound(p,net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT,.5F,countdown==0?1.8F:1F);}if(countdown==0)launch();if(s.getTickCount()%4==0)broadcast("");return;}
            for(PlayerState st:PLAYERS.values()){ServerPlayer p=s.getPlayerList().getPlayer(st.id);if(p==null||st.phase==Phase.DONE||st.phase==Phase.LANDED)continue;
                if(st.phase==Phase.ABOARD){if(st.route.plane==null||st.route.plane.isRemoved()||p.getVehicle()!=st.route.plane)jump(p,st);else{st.velocity=st.route.plane.routeVelocity();p.fallDistance=0;animateDeparture(p,st);}}
                if(st.phase==Phase.DIVING||st.phase==Phase.LANDING)move(p,st);}
            for(Route route:ROUTES)if(route.plane!=null&&!route.plane.isRemoved()){if(route.plane.getPassengers().isEmpty())route.emptyTicks++;else route.emptyTicks=0;if(route.emptyTicks>=20)route.plane.discard();else if(!route.plane.getPassengers().isEmpty()&&s.getTickCount()%20==0){validateXZ(route.level,route.plane.getX(),route.plane.getZ());loadAround(route.level,route.plane.getX(),route.plane.getZ());}}
            if(s.getTickCount()%4==0)broadcast("");
        }catch(RuntimeException e){LOG.error("Airborne match safely aborted",e);stop(s);}
    }
    private static void animateDeparture(ServerPlayer p,PlayerState st){
        Route route=st.route;AirbornePlaneEntity plane=route.plane;
        int age=server.getTickCount()-st.boardedAt;
        if(route.departureAt<0&&age>=AirborneRules.BOARDING_LOCK_TICKS&&(plane.hasReachedTarget()||age>=400))route.departureAt=server.getTickCount();
        if(route.departureAt<0)return;
        int elapsed=server.getTickCount()-route.departureAt;
        plane.setDoorProgress((float)AirborneDeparture.doorProgress(elapsed));
        var position=AirborneDeparture.position(elapsed,route.ids.indexOf(st.id));
        plane.setCabinPosition(p,position.x(),position.y(),position.z());
        p.setYRot(route.yaw+180);p.setXRot(0);
        p.setPos(plane.getPassengerRidingPosition(p));p.connection.resetPosition();
        // The server owns the whole sequence; no key press, input heartbeat or client acknowledgement is required.
        if(AirborneDeparture.release(elapsed)){jump(p,st);plane.continueAfterDrop();}
    }
    private static void move(ServerPlayer p,PlayerState st){
        int cx=p.getBlockX()>>4,cz=p.getBlockZ()>>4;if(cx!=st.chunkX||cz!=st.chunkZ){loadAround(p.level(),p.getX(),p.getZ());st.chunkX=cx;st.chunkZ=cz;}AirborneInputPayload in=st.input;if(in==null||server.getTickCount()-st.lastInput>12)in=new AirborneInputPayload(0,0,p.getYRot(),p.getXRot(),false);
        var v=AirborneRules.glide(new AirborneRules.Vec(st.velocity.x,st.velocity.y,st.velocity.z),in.forward(),in.strafe(),in.yaw(),in.pitch());Vec3 wanted=new Vec3(v.x(),++st.flightTicks>1200?Math.min(-.8,v.y()):v.y(),v.z());
        boolean near=!p.level().noCollision(p,p.getBoundingBox().expandTowards(0,-8,0));st.phase=!p.level().noCollision(p,p.getBoundingBox().expandTowards(0,-50,0))?Phase.LANDING:Phase.DIVING;
        if(near)wanted=new Vec3(wanted.x*.6,Math.max(-.4,wanted.y),wanted.z*.6);st.velocity=wanted;if(near)st.velocity=new Vec3(st.velocity.x,Math.max(-.4,st.velocity.y),st.velocity.z);
        p.setNoGravity(true);p.fallDistance=0;p.setYRot(in.yaw());p.setXRot(in.pitch());p.setDeltaMovement(st.velocity);p.move(MoverType.SELF,st.velocity);p.connection.resetPosition();
        if(p.onGround()||p.isInWater())finish(p,st,Phase.LANDED);
    }
    private static void finish(ServerPlayer p,PlayerState st,Phase phase){if(p.getVehicle() instanceof AirbornePlaneEntity)p.stopRiding();p.setNoGravity(st.oldNoGravity);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);st.phase=phase;st.velocity=Vec3.ZERO;st.fallProtectionUntil=server.getTickCount()+20;}
    private static void abandon(ServerPlayer p){LAST_ADMIN.remove(p.getUUID());PlayerState st=PLAYERS.get(p.getUUID());if(st==null||server==null)return;finish(p,st,Phase.DONE);markAirborneAbandoned(st.id);restoreTeam(st);}
    public static void stop(MinecraftServer s){
        if(s==null||server!=s)return;for(PlayerState st:PLAYERS.values()){ServerPlayer p=s.getPlayerList().getPlayer(st.id);if(p!=null){boolean flying=isAirborne(st.id);finish(p,st,Phase.DONE);if(flying&&p.isAlive()){ServerLevel origin=s.getLevel(st.originDimension);if(origin!=null)p.teleportTo(origin,st.origin.x,st.origin.y,st.origin.z,Set.of(),st.originYaw,st.originPitch,false);}}restoreTeam(st);}
        for(Route route:ROUTES)if(route.plane!=null)route.plane.discard();for(String name:MATCH_TEAMS){PlayerTeam t=s.getScoreboard().getPlayerTeam(name);if(t!=null)s.getScoreboard().removePlayerTeam(t);}MATCH_TEAMS.clear();ROUTES.clear();PLAYERS.clear();active=false;countdown=0;broadcast("比赛已停止");
    }
    private static void restoreTeam(PlayerState st){if(server==null||st.matchTeam==null)return;var board=server.getScoreboard();PlayerTeam current=board.getPlayersTeam(st.scoreName);if(current!=null&&MATCH_TEAMS.contains(current.getName()))board.removePlayerFromTeam(st.scoreName,current);if(st.oldTeam!=null){PlayerTeam old=board.getPlayerTeam(st.oldTeam);if(old!=null)board.addPlayerToTeam(st.scoreName,old);}}
    private static void admin(MinecraftServer s,ServerPlayer p,AirborneAdminPayload packet){
        if(!operator(p))return;try{require(server==s,"服务器尚未就绪");JsonElement parsed=packet.data().isBlank()?new JsonObject():JsonParser.parseString(packet.data());require(parsed.isJsonObject(),"请求必须为对象");JsonObject d=parsed.getAsJsonObject();String action=packet.action();
            if(action.equals("sync")){send(p,"");return;}if(action.equals("stop")){stop(s);send(p,"比赛已停止");return;}
            if(action.equals("start")){require(start(p.createCommandSourceStack())==1,"无法开局，请检查点位和参赛玩家");send(p,"比赛已锁定");return;}
            require(!active,"比赛期间不能修改配置");require(loadError.isEmpty(),loadError);JsonObject before=config();
            try{switch(action){
                case "open_points"->{if(d.has("group")){String id=text(d,"group");require(id.isEmpty()||GROUPS.containsKey(id),"分组不存在");}send(p,"");return;}
                case "select_mode_group"->activeMode=mode(d,null);
                case "pack_group"->packGroup();
                case "delete_near_point"->{require(p.getMainHandItem().is(SETTER)||p.getOffhandItem().is(SETTER),"请手持空降区域设置器");double x=number(d,"x"),y=number(d,"y"),z=number(d,"z");require(p.getEyePosition().distanceToSqr(new Vec3(x+.5,y+.5,z+.5))<=64,"点位超出操作距离");Group current=group(d);Point q=current.points.stream().filter(a->a.dimension.equals(p.level().dimension().identifier().toString())&&Math.abs(a.x-(x+.5))<.6&&Math.abs(a.z-(z+.5))<.6&&Math.abs(a.y-(y+1))<2).findFirst().orElseThrow(()->new IllegalArgumentException("此处没有当前组点位"));current.points.remove(q);}
                case "select_group"->{String id=text(d,"group");require(id.isEmpty()||GROUPS.containsKey(id),"分组不存在");selectedGroup=id;}
                case "create_group"->{String label=name(d);var existing=GROUPS.entrySet().stream().filter(e->e.getValue().name.equals(label)&&e.getValue().points.isEmpty()).findFirst();if(existing.isPresent())selectedGroup=existing.get().getKey();else{require(GROUPS.size()<MAX_GROUPS,"最多8个分组");String id=UUID.randomUUID().toString();GROUPS.put(id,new Group(label,activeMode));selectedGroup=id;}}
                case "rename_group"->{require(!text(d,"group").isEmpty(),"草稿不是分组");group(d).name=name(d);}
                case "delete_group"->{String id=text(d,"group");require(GROUPS.remove(id)!=null,"分组不存在");if(selectedGroup.equals(id))selectedGroup="";}
                case "mode"->activeMode=mode(d,null);
                case "flight_settings"->{double h=number(d,"height"),v=number(d,"speed");require(AirborneRules.validConfig(h,v),"高度须为正有限数，速度0.2–2");height=h;speed=v;}
                case "rename_point"->point(d).name=name(d);
                case "delete_point"->{Point q=point(d);group(d).points.remove(q);}
                case "teleport_point"->{safeTeleport(p,point(d));send(p,"已传送到点位");return;}
                default->throw new IllegalArgumentException("未知管理操作");
            }persist();}catch(IOException|RuntimeException e){applyConfig(before);throw new IllegalArgumentException("修改未保存："+message(e),e);}broadcast("");send(p,action.equals("pack_group")?"当前组已保存，已切换到空白草稿；右键地面设置下一组":"设置已保存");
        }catch(RuntimeException e){send(p,message(e));}
    }
    private static void packGroup(){applyConfig(AirborneGroupRules.packGroup(config(),MAX_GROUPS));}
    private static void safeTeleport(ServerPlayer p,Point q){ServerLevel l=level(server,q.dimension);validatePosition(l,q.x,q.y,q.z);loadAround(l,q.x,q.z);AABB box=p.getBoundingBox().move(q.x-p.getX(),q.y-p.getY(),q.z-p.getZ());require(l.noCollision(p,box)&&!l.noCollision(p,box.move(0,-.1,0)),"点位不是安全站位，请移动点位");require(p.teleportTo(l,q.x,q.y,q.z,Set.of(),p.getYRot(),p.getXRot(),false),"传送失败");}
    private static Group group(JsonObject d){Group g=working(d.has("group")?text(d,"group"):selected());require(g!=null,"分组不存在");return g;}
    private static Point point(JsonObject d){String id=text(d,"id");return group(d).points.stream().filter(q->q.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("点位不存在"));}
    private static String name(JsonObject d){String v=text(d,"name").strip();require(!v.isBlank()&&v.length()<=32&&v.chars().noneMatch(Character::isISOControl),"名称须为1–32个字符");return v;}
    private static String mode(JsonObject d,String fallback){String v=d.has("mode")?text(d,"mode"):fallback;require("solo".equals(v)||"duo".equals(v),"模式须为solo或duo");return v;}
    private static String text(JsonObject d,String key){require(d.has(key)&&d.get(key).isJsonPrimitive(),"缺少参数："+key);return d.get(key).getAsString();}
    private static double number(JsonObject d,String key){double v=d.get(key).getAsDouble();require(Double.isFinite(v),"参数不是有限数");return v;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
    private static String message(Exception e){return e.getMessage()==null?"请求失败":e.getMessage();}
    private static ServerLevel level(MinecraftServer s,String d){require(d.length()<=128,"Dimension identifier too long");ServerLevel l=s.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(d)));require(l!=null,"点位维度不存在");return l;}
    private static void validateXZ(ServerLevel l,double x,double z){require(Double.isFinite(x)&&Double.isFinite(z)&&Math.abs(x)<29999900&&Math.abs(z)<29999900&&l.getWorldBorder().isWithinBounds(BlockPos.containing(x,0,z)),"点位超出世界边界");}
    private static void validatePosition(ServerLevel l,double x,double y,double z){validateXZ(l,x,z);require(Double.isFinite(y)&&y>=l.getMinY()+1&&y<l.getMaxY()-2,"点位超出世界高度");}
    private static void loadAround(ServerLevel l,double x,double z){int cx=((int)Math.floor(x))>>4,cz=((int)Math.floor(z))>>4;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)l.getChunk(cx+dx,cz+dz);}
    private static JsonArray pointsJson(Group group){JsonArray a=new JsonArray();for(Point q:group.points)a.add(q.json());return a;}
    private static JsonObject config(){JsonObject o=new JsonObject();o.addProperty("activeMode",activeMode);o.addProperty("selectedGroup",selected());o.add("draftPoints",pointsJson(draft));o.addProperty("height",height);o.addProperty("speed",speed);JsonArray groups=new JsonArray();for(var e:GROUPS.entrySet()){Group group=e.getValue();JsonObject g=new JsonObject();g.addProperty("id",e.getKey());g.addProperty("name",group.name);g.add("points",pointsJson(group));groups.add(g);}o.add("groups",groups);return o;}
    private static void parsePoints(Group group,JsonArray points){require(points.size()<=MAX_POINTS,"点位过多");Set<String> ids=new HashSet<>();for(JsonElement e:points){JsonObject q=e.getAsJsonObject();String id=text(q,"id"),dimension=text(q,"dimension");require(id.length()<=64&&ids.add(id),"重复点位标识");double x=number(q,"x"),y=number(q,"y"),z=number(q,"z");validatePosition(level(server,dimension),x,y,z);group.points.add(new Point(id,name(q),dimension,x,y,z));}}
    private static void applyConfig(JsonObject saved){
        JsonObject o=AirborneGroupRules.sharedConfig(saved);Map<String,Group> parsed=new LinkedHashMap<>();JsonArray groups=o.getAsJsonArray("groups");require(groups.size()<=MAX_GROUPS+2,"分组过多");double h=o.has("height")?number(o,"height"):200,v=o.has("speed")?number(o,"speed"):.8;require(AirborneRules.validConfig(h,v),"无效飞行配置");
        for(JsonElement e:groups){JsonObject g=e.getAsJsonObject();String id=text(g,"id");require(id.length()<=64&&!parsed.containsKey(id),"重复分组标识");Group group=new Group(name(g),"solo");parsePoints(group,g.getAsJsonArray("points"));if(id.equals("default")&&group.name.equals("默认分组")){if(group.points.isEmpty())continue;group.name="1";}parsed.put(id,group);}
        String m=o.has("activeMode")?modeValue(text(o,"activeMode")):"solo",id=text(o,"selectedGroup");if(id.equals("default")&&!parsed.containsKey(id))id="";require(id.isEmpty()||parsed.containsKey(id),"所选分组不存在");Group newDraft=new Group("","solo");parsePoints(newDraft,o.getAsJsonArray("draftPoints"));
        GROUPS.clear();GROUPS.putAll(parsed);selectedGroup=id;draft=newDraft;activeMode=m;height=h;speed=v;
    }
    private static String modeValue(String value){require(value.equals("solo")||value.equals("duo"),"无效模式");return value;}
    private static void load(MinecraftServer s){server=s;PLAYERS.clear();ROUTES.clear();MATCH_TEAMS.clear();LAST_ADMIN.clear();active=false;countdown=0;GROUPS.clear();selectedGroup="";draft=new Group("","solo");activeMode="solo";height=200;speed=.8;loadError="";try{applyConfig(AirborneConfigStore.load(AirborneConfigStore.file(s)));}catch(IOException|RuntimeException e){GROUPS.clear();selectedGroup="";draft=new Group("","solo");loadError="空降配置读取失败，请检查存档poisoncircle/airborne.json（未覆盖原文件）";LOG.error(loadError,e);}}
    private static void persist()throws IOException{JsonObject data=config();require(JSON.toJson(data).length()<=24000,"Configuration exceeds network limit");AirborneConfigStore.save(AirborneConfigStore.file(server),data);}
    private static void broadcast(String message){if(server!=null)for(ServerPlayer p:server.getPlayerList().getPlayers())send(p,message);}
    private static void send(ServerPlayer p,String message){
        if(server==null||!ServerPlayNetworking.canSend(p,AirborneStatePayload.TYPE))return;if(operator(p)&&!message.isBlank())p.sendSystemMessage(Component.literal(message));JsonObject o=config();o.add("draftPoints",pointsJson(draft));o.addProperty("operator",operator(p));o.addProperty("mode",activeMode);o.addProperty("status",active?(countdown>0?"countdown":"active"):"idle");o.addProperty("countdown",(countdown+19)/20);o.addProperty("participants",active?PLAYERS.size():server.getPlayerList().getPlayers().stream().filter(pl->pl.isAlive()&&!pl.isCreative()&&!pl.isSpectator()).count());o.addProperty("seq",server.getTickCount());o.addProperty("message",operator(p)?loadError:"");
        JsonArray teams=new JsonArray(),poses=new JsonArray();for(Route route:ROUTES){JsonObject t=new JsonObject();t.addProperty("name",activeMode.equals("duo")?"队伍 "+(teams.size()+1):"独立玩家 "+(teams.size()+1));JsonArray names=new JsonArray();for(UUID id:route.ids)names.add(PLAYERS.get(id).scoreName);t.add("players",names);teams.add(t);}
        for(PlayerState st:PLAYERS.values()){ServerPlayer pl=server.getPlayerList().getPlayer(st.id);if(pl!=null&&pl.level()==p.level()){JsonObject pose=new JsonObject();pose.addProperty("entityId",pl.getId());pose.addProperty("phase",presentationPhase(st));pose.addProperty("standProgress",standProgress(st));poses.add(pose);}}o.add("teams",teams);o.add("poses",poses);
        PlayerState st=PLAYERS.get(p.getUUID());JsonObject self=new JsonObject();self.addProperty("phase",st==null?"none":presentationPhase(st));self.addProperty("standProgress",st==null?1:standProgress(st));if(st!=null){self.addProperty("entityId",p.getId());self.addProperty("planeId",st.route.plane==null?-1:st.route.plane.getId());self.addProperty("x",p.getX());self.addProperty("y",p.getY());self.addProperty("z",p.getZ());self.addProperty("vx",st.velocity.x);self.addProperty("vy",st.velocity.y);self.addProperty("vz",st.velocity.z);if(st.target!=null)self.add("target",st.target.json());}o.add("self",self);String json=JSON.toJson(o);if(json.length()>32768){LOG.error("Airborne snapshot exceeded bounded protocol");return;}ServerPlayNetworking.send(p,new AirborneStatePayload(json));
    }
    private static String presentationPhase(PlayerState st){return st.phase==Phase.ABOARD&&st.route!=null&&st.route.departureAt>=0&&server.getTickCount()-st.route.departureAt>=AirborneDeparture.DOOR_TICKS?"departing":st.phase.name().toLowerCase(Locale.ROOT);}
    private static double standProgress(PlayerState st){if(st.phase!=Phase.ABOARD)return 1;if(st.route==null||st.route.departureAt<0)return 0;return Math.max(0,Math.min(1,(server.getTickCount()-st.route.departureAt-AirborneDeparture.DOOR_TICKS)/(double)AirborneDeparture.STAND_TICKS));}
    private enum Phase{COUNTDOWN,ABOARD,DIVING,LANDING,LANDED,DONE}
    private static final class Group{String name,mode;final List<Point> points=new ArrayList<>();Group(String n,String m){name=n;mode=m;}}
    private static final class Point{final String id;String name,dimension;double x,y,z;Point(String id,String name,String dimension,double x,double y,double z){this.id=id;this.name=name;this.dimension=dimension;this.x=x;this.y=y;this.z=z;}Point copy(){return new Point(id,name,dimension,x,y,z);}JsonObject json(){JsonObject o=new JsonObject();o.addProperty("id",id);o.addProperty("name",name);o.addProperty("dimension",dimension);o.addProperty("x",x);o.addProperty("y",y);o.addProperty("z",z);return o;}}
    private static final class Route{final Point point;final ServerLevel level;final Vec3 origin,velocity;final float yaw;final List<UUID> ids;AirbornePlaneEntity plane;int emptyTicks,departureAt=-1;Route(Point q,ServerLevel l,Vec3 o,Vec3 v,float yaw,List<UUID> ids){point=q;level=l;origin=o;velocity=v;this.yaw=yaw;this.ids=List.copyOf(ids);}}
    private static final class PlayerState{final UUID id;final String scoreName,oldTeam;final boolean oldNoGravity;final ResourceKey<Level> originDimension;final Vec3 origin;final float originYaw,originPitch;Phase phase=Phase.COUNTDOWN;Point target;Route route;String matchTeam;Vec3 velocity=Vec3.ZERO;AirborneInputPayload input;int lastInput=-1,fallProtectionUntil=-1,flightTicks,boardedAt,chunkX=Integer.MIN_VALUE,chunkZ=Integer.MIN_VALUE;PlayerState(ServerPlayer p){id=p.getUUID();scoreName=p.getScoreboardName();PlayerTeam team=p.getTeam();oldTeam=team==null?null:team.getName();oldNoGravity=p.isNoGravity();originDimension=p.level().dimension();origin=p.position();originYaw=p.getYRot();originPitch=p.getXRot();}}
    private static final class SetterItem extends Item{SetterItem(Properties p){super(p);}
        @Override public InteractionResult use(Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){if(player.isShiftKeyDown()){if(player instanceof ServerPlayer sp)admin(sp.level().getServer(),sp,new AirborneAdminPayload("pack_group","{}"));return InteractionResult.SUCCESS;}return InteractionResult.PASS;}
        @Override public InteractionResult useOn(UseOnContext c){
        if(c.getLevel().isClientSide())return InteractionResult.SUCCESS;if(!(c.getPlayer() instanceof ServerPlayer p)||!operator(p))return InteractionResult.FAIL;if(p.isShiftKeyDown()){admin(p.level().getServer(),p,new AirborneAdminPayload("pack_group","{}"));return InteractionResult.SUCCESS;}try{
            require(!active,"比赛期间不能添加点位");require(loadError.isEmpty(),loadError);Group group=working();require(group!=null&&group.points.size()<MAX_POINTS,"每组最多8个点位");BlockPos block=c.getClickedPos();double x=block.getX()+.5,z=block.getZ()+.5,y=p.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,block.getX(),block.getZ());validatePosition(p.level(),x,y,z);require(p.level().getFluidState(BlockPos.containing(x,y-1,z)).isEmpty(),"Point must be on dry ground");AABB standing=new AABB(x-.3,y,z-.3,x+.3,y+1.8,z+.3);require(p.level().noCollision(p,standing)&&!p.level().noCollision(p,standing.move(0,-.1,0)),"Point needs solid ground and headroom");if(group.points.stream().anyMatch(q->q.dimension.equals(p.level().dimension().identifier().toString())&&q.x==x&&q.y==y&&q.z==z)){send(p,"此处已有当前组点位");return InteractionResult.SUCCESS;}Point q=new Point(UUID.randomUUID().toString(),"点位 "+(group.points.size()+1),p.level().dimension().identifier().toString(),x,y,z);group.points.add(q);try{persist();}catch(IOException|RuntimeException e){group.points.remove(q);throw new IllegalArgumentException("保存点位失败",e);}broadcast("");send(p,"已添加 "+q.name);
        }catch(RuntimeException e){send(p,message(e));}return InteractionResult.SUCCESS;
    }}
}

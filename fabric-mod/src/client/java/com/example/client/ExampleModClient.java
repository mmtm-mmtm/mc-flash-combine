package com.example.client;
import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.net.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;
import com.example.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;import net.minecraft.core.*;import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;import net.minecraft.server.level.*;
import net.minecraft.world.*;import net.minecraft.world.entity.*;import net.minecraft.world.item.*;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.phys.*;
public class ExampleModClient implements ClientModInitializer {
 public static final String WORLD="MC Dad n Me Bridge";public static final double UNIT=32,OX=2600,OZ=8;
 public static volatile int facing=1;public static volatile float renderYaw=90;public static volatile double cameraX,cameraY;
 private static ExampleModClient active;private volatile JsonObject flash;private volatile long lastFlash;
 private volatile String blockedScene="",resetId="";private volatile JsonObject lastReset=new JsonObject();private boolean arenaCleared;private long nextBossSpawn;
 private volatile String output="{\"ready\":false,\"reason\":\"等待 MC 专用世界\"}";
 private final ConcurrentLinkedQueue<JsonObject> actions=new ConcurrentLinkedQueue<>();private final Set<String> held=new HashSet<>();
 private final Map<String,BridgeActor> enemies=new HashMap<>();private final Map<String,Float> lastHealth=new HashMap<>();private final Map<String,Double> damage=new HashMap<>();
 private final Set<BlockPos> obstacles=new HashSet<>(),floor=new HashSet<>();
 private volatile Map<String,Integer> enemyIds=Map.of();private String scene="";private int travelAck,hitAck,ticks,explosions,summons;private long nextSummon;private BlockPos mining;
 
 public static boolean remoteMining(){return active!=null&&active.mining!=null;}public static void pump(Minecraft mc){if(active!=null)active.input(mc,false);}
 static double x(double fx){return (fx+OX)/UNIT;}static double z(double fy){return fy/UNIT+OZ;}static double fx(double x){return x*UNIT-OX;}static double fy(double z){return (z-OZ)*UNIT;}
 static double num(JsonObject o,String k,double d){var v=o.get(k);if(v==null||v.isJsonNull()||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())return d;double n=v.getAsDouble();return Double.isFinite(n)?n:d;}
 public void onInitializeClient(){active=this;EntityRendererRegistry.register(BridgeEntities.PARTNER,BridgeActorRenderer::new);
  try{var http=HttpServer.create(new InetSocketAddress("127.0.0.1",18126),0);http.setExecutor(Executors.newFixedThreadPool(6,r->{var t=new Thread(r,"dadnme-http");t.setDaemon(true);return t;}));
   http.createContext("/state",e->{if(cors(e))return;try{if(e.getRequestMethod().equals("POST")){var s=JsonParser.parseString(new String(e.getRequestBody().readNBytes(300000),StandardCharsets.UTF_8)).getAsJsonObject();if(s.has("game")&&s.get("game").getAsString().equals("dadnme")){if(!s.get("scene").getAsString().equals(blockedScene)){flash=s;lastFlash=System.nanoTime();}}}var s=JsonParser.parseString(output).getAsJsonObject();s.addProperty("spriteFrame",BridgeImages.spriteFrame);var sceneImage=SceneOverlay.latest;s.addProperty("sceneFrame",sceneImage.frame());s.addProperty("sceneCameraX",sceneImage.cameraX());s.addProperty("sceneCameraY",sceneImage.cameraY());reply(e,"application/json",s.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ex){e.sendResponseHeaders(400,-1);e.close();}});
   http.createContext("/action",e->{if(cors(e))return;try{var action=JsonParser.parseString(new String(e.getRequestBody().readNBytes(10000),StandardCharsets.UTF_8)).getAsJsonObject();var current=flash;
    if(!(current==null&&!blockedScene.isEmpty())&&(!action.has("scene")||(current!=null&&action.get("scene").getAsString().equals(current.get("scene").getAsString()))))actions.add(action);e.sendResponseHeaders(204,-1);}catch(Exception ex){e.sendResponseHeaders(400,-1);}finally{e.close();}});
   http.createContext("/reset",e->{if(cors(e))return;try{
    if(!e.getRequestMethod().equals("POST")){e.sendResponseHeaders(405,-1);e.close();return;}
    var mc=Minecraft.getInstance();if(!valid(mc)){e.sendResponseHeaders(409,-1);e.close();return;}
    String request=UUID.randomUUID().toString();var server=mc.getSingleplayerServer();
    var receipt=mc.submit(()->{clearClient(mc);return true;}).thenCompose(unused->server.submit(()->{
     var previous=flash;blockedScene=previous!=null&&previous.has("scene")?previous.get("scene").getAsString():scene;
     flash=null;lastFlash=0;scene="";resetId=request;actions.clear();
     StreetBounds.current=null;lastReset=ReplayReset.clear(server,floor,obstacles);arenaCleared=true;
     enemies.clear();enemyIds=Map.of();lastHealth.clear();damage.clear();hitAck=0;
     NativeAudio.scene("reset:"+request);SceneOverlay.blocks=List.of();SceneOverlay.history.clear();
     var response=lastReset.deepCopy();response.addProperty("resetId",request);response.addProperty("ready",false);response.addProperty("reason","等待新的 Flash 场景");output=response.toString();return response;
    })).thenCompose(result->mc.submit(()->{clearClient(mc);return result;})).get(30,TimeUnit.SECONDS);
    reply(e,"application/json",receipt.toString().getBytes(StandardCharsets.UTF_8));
   }catch(Exception ex){System.err.println("Replay reset failed: "+ex);e.sendResponseHeaders(500,-1);e.close();}});
   for(String route:new String[]{"/sprite","/scene","/frame","/hud"})http.createContext(route,e->{if(cors(e))return;reply(e,"image/png",route.equals("/sprite")?BridgeImages.sprite:route.equals("/scene")?sceneBytes(e):route.equals("/hud")?BridgeImages.hud:BridgeImages.frame);});AudioHttp.register(http);http.start();
  }catch(Exception ex){throw new RuntimeException(ex);}
  ServerTickEvents.END_SERVER_TICK.register(this::server);ClientTickEvents.START_CLIENT_TICK.register(mc->input(mc,false));ClientTickEvents.END_CLIENT_TICK.register(mc->{input(mc,true);NativeAudio.tick(mc,valid(mc)&&System.nanoTime()-lastFlash<2000000000L,false);});
 }
 static byte[] sceneBytes(HttpExchange e){
  try{String q=e.getRequestURI().getRawQuery();if(q!=null&&q.startsWith("t=")){long n=Long.parseLong(q.substring(2));var snap=SceneOverlay.history.get(n);if(snap!=null)return snap.pixels();}}catch(Exception ignored){}
  return SceneOverlay.latest.pixels();
 }
 static boolean cors(HttpExchange e)throws java.io.IOException{e.getResponseHeaders().set("Access-Control-Allow-Origin","http://127.0.0.1:18125");e.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");if(e.getRequestMethod().equals("OPTIONS")){e.sendResponseHeaders(204,-1);e.close();return true;}return false;}
 static void reply(HttpExchange e,String type,byte[] bytes)throws java.io.IOException{e.getResponseHeaders().set("Content-Type",type);e.getResponseHeaders().set("Cache-Control","no-store");if(bytes.length==0)e.sendResponseHeaders(204,-1);else{e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);}e.close();}
 static boolean valid(Minecraft mc){return mc.player!=null&&mc.getSingleplayerServer()!=null&&mc.getSingleplayerServer().getWorldData().getLevelName().equals(WORLD);}
 private void server(MinecraftServer server){
  if(!server.getWorldData().getLevelName().equals(WORLD)||server.getPlayerList().getPlayers().isEmpty())return;var world=server.overworld();var p=server.getPlayerList().getPlayers().get(0);var s=flash;
  BridgeAllies.tick(server);
  if(s==null||!s.has("ready")||!s.get("ready").getAsBoolean()||System.nanoTime()-lastFlash>2000000000L){p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);return;}
  String id=s.get("scene").getAsString();if(!scene.equals(id)){
   if(!arenaCleared)lastReset=ReplayReset.clear(server,floor,obstacles);arenaCleared=false;
   p=server.getPlayerList().respawn(p,false);p.connection.player=p;
   for(var e:enemies.values())e.discard();enemies.clear();lastHealth.clear();damage.clear();

   obstacles.clear();travelAck=0;hitAck=0;explosions=0;summons=0;nextSummon=0;
   p.teleportTo(world,x(num(s,"x",-2400)),80.1,z(num(s,"y",250)),180,0);p.removeAllEffects();p.setAbsorptionAmount(0);p.clearFire();p.setTicksFrozen(0);p.setAirSupply(p.getMaxAirSupply());p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);p.stopUsingItem();p.setSprinting(false);p.stopRiding();
   p.setHealth(p.getMaxHealth());p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(5);p.getFoodData().setExhaustion(0);p.setExperienceLevels(0);p.setExperiencePoints(0);p.totalExperience=0;
   p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.STONE,64));p.getInventory().setItem(1,new ItemStack(Items.DIAMOND_SWORD));p.getInventory().setItem(2,new ItemStack(Items.DIAMOND_PICKAXE));p.getInventory().setItem(3,new ItemStack(Items.OAK_PLANKS,64));p.getInventory().setItem(4,new ItemStack(Items.TNT,32));p.getInventory().setItem(5,new ItemStack(Items.FLINT_AND_STEEL));p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));p.getInventory().selected=0;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));p.inventoryMenu.broadcastFullState();scene=id;NativeAudio.scene(id);
  }
  int travel=(int)num(s,"travel",0);if(travel>travelAck){p.stopRiding();p.stopUsingItem();p.teleportTo(world,x(num(s,"travelX",num(s,"x",0))),80.1,z(num(s,"travelY",num(s,"y",0))),p.getYRot(),0);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;travelAck=travel;}
  p.setGameMode(RemoteInventory.creativeRequested?GameType.CREATIVE:GameType.SURVIVAL);p.setNoGravity(false);ticks++;cameraX=num(s,"cameraX",2600);cameraY=num(s,"cameraY",150);int px=(int)p.getX();
  double arenaMinX=x(num(s,"minX",-2600)),arenaMaxX=x(num(s,"maxX",0)),arenaMinZ=z(num(s,"minY",0)),arenaMaxZ=z(num(s,"maxY",400));
  var barriers=new ArrayList<AABB>();for(var el:s.getAsJsonArray("colliders")){var c=el.getAsJsonObject();double x0=x(num(c,"x0",0)),x1=x(num(c,"x1",0)),z0=z(num(c,"y0",0)),z1=z(num(c,"y1",0));if(x1>x0&&z1>z0)barriers.add(new AABB(x0,0,z0,x1,1,z1));}
  StreetBounds.current=new StreetBounds.Arena(arenaMinX,arenaMaxX,arenaMinZ,arenaMaxZ,List.copyOf(barriers));
  int confined=0;for(var entity:world.getAllEntities())if(entity instanceof Mob mob&&StreetBounds.contain(mob))confined++;
  for(int xx=Math.max(px-18,(int)Math.floor(arenaMinX));xx<=Math.min(px+34,(int)Math.floor(arenaMaxX));xx++)for(int zz=Math.max(0,(int)Math.floor(arenaMinZ));zz<=Math.min(35,(int)Math.floor(arenaMaxZ));zz++){var pos=new BlockPos(xx,79,zz);if(floor.add(pos))world.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),2);}
  Set<BlockPos> wanted=new HashSet<>();for(var el:s.getAsJsonArray("colliders")){var c=el.getAsJsonObject();int x0=(int)Math.ceil(x(num(c,"x0",0))),x1=(int)Math.floor(x(num(c,"x1",0))),z0=(int)Math.ceil(z(num(c,"y0",0))),z1=(int)Math.floor(z(num(c,"y1",0)));int height=Math.min(3,Math.max(1,(int)(num(c,"height",32)/UNIT)));for(int xx=Math.max(px-24,x0);xx<=Math.min(px+36,x1);xx++)for(int zz=Math.max(0,z0);zz<=Math.min(35,z1);zz++)for(int yy=80;yy<80+height;yy++)wanted.add(new BlockPos(xx,yy,zz));}
  for(var pos:obstacles)if(!wanted.contains(pos))world.setBlock(pos,Blocks.AIR.defaultBlockState(),2);for(var pos:wanted)if(!obstacles.contains(pos))world.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),2);obstacles.clear();obstacles.addAll(wanted);
  double minX=x(num(s,"minX",-2600))+.4,maxX=x(num(s,"maxX",0))-.4,minZ=z(num(s,"minY",0))+.3,maxZ=z(num(s,"maxY",400))-.3;double cx=Math.max(minX,Math.min(maxX,p.getX())),cz=Math.max(minZ,Math.min(maxZ,p.getZ()));if(Math.abs(cx-p.getX())+Math.abs(cz-p.getZ())>.01)p.teleportTo(world,cx,p.getY(),cz,p.getYRot(),p.getXRot());p.fallDistance=Math.min(p.fallDistance,3);
  var board=server.getScoreboard();var enemyTeam=board.getPlayerTeam("flash_enemies");if(enemyTeam==null)enemyTeam=board.addPlayerTeam("flash_enemies");
  var living=new HashSet<String>();var ids=new HashMap<String,Integer>();for(var el:s.getAsJsonArray("enemies")){var e=el.getAsJsonObject();String key=e.get("id").getAsString();living.add(key);BridgeActor actor=enemies.get(key);float hp=(float)num(e,"hp",60)/3;
   if(actor==null){actor=BridgeEntities.PARTNER.create(world);if(actor==null)continue;actor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(Math.max(20,num(e,"maxHP",60)/3));actor.setHealth(hp);world.addFreshEntity(actor);enemies.put(key,actor);lastHealth.put(key,actor.getHealth());}
   actor.combatTarget=!(e.has("object")&&e.get("object").getAsBoolean());if(actor.combatTarget&&actor.getTeam()!=enemyTeam)board.addPlayerToTeam(actor.getScoreboardName(),enemyTeam);
   float actual=actor.getHealth(),old=lastHealth.getOrDefault(key,actual);if(actual<old)damage.merge(key,(double)(old-actual),Double::sum);if(hp<actual)actor.setHealth(hp);lastHealth.put(key,actor.getHealth());double bodyY=num(e,"bodyY",0);actor.setPos(x(num(e,"x",0)),80-bodyY/UNIT,z(num(e,"y",0)));
   var bounds=e.has("bounds")?e.getAsJsonObject("bounds"):new JsonObject();double halfWidth=Math.max(.3,Math.min(2,(num(bounds,"x1",32)-num(bounds,"x0",0))/UNIT/2)),height=Math.max(.6,Math.min(4,(num(bounds,"y1",60)-num(bounds,"y0",0))/UNIT));
   actor.setBoundingBox(new AABB(actor.getX()-halfWidth,actor.getY(),actor.getZ()-.4,actor.getX()+halfWidth,actor.getY()+height,actor.getZ()+.4));
   actor.setInvulnerable(e.has("nohit")&&e.get("nohit").getAsBoolean()&&bodyY>=0);actor.setDeltaMovement(Vec3.ZERO);ids.put(key,actor.getId());
  }
  for(var it=enemies.entrySet().iterator();it.hasNext();){var en=it.next();if(!living.contains(en.getKey())){en.getValue().discard();lastHealth.remove(en.getKey());it.remove();}}enemyIds=Map.copyOf(ids);
  for(var el:s.getAsJsonArray("hits")){var h=el.getAsJsonObject();int seq=h.get("id").getAsInt();if(seq<=hitAck)continue;var attacker=BridgeEntities.PARTNER.create(world);if(attacker!=null){attacker.setPos(x(num(h,"x",num(s,"x",0)+64)),80,z(num(h,"y",num(s,"y",0))));p.hurt(world.damageSources().mobAttack(attacker),(float)num(h,"damage",1));}hitAck=seq;}
  var state=new JsonObject();state.addProperty("ready",true);state.addProperty("resetId",resetId);state.add("lastReset",lastReset);state.addProperty("food",p.getFoodData().getFoodLevel());state.addProperty("experience",p.totalExperience);state.addProperty("fireTicks",p.getRemainingFireTicks());state.addProperty("effects",p.getActiveEffects().size());state.addProperty("scene",id);state.addProperty("x",fx(p.getX()));state.addProperty("y",fy(p.getZ()));state.addProperty("height",(p.getY()-80)*UNIT);state.addProperty("hp",p.getHealth());state.addProperty("facing",facing);state.addProperty("hitAck",hitAck);state.addProperty("sprinting",p.isSprinting());state.addProperty("blocking",p.isBlocking());state.addProperty("usingItem",p.isUsingItem());state.addProperty("useTicks",p.getTicksUsingItem());state.addProperty("useDispatch",RemoteUse.last);
  var entityTypes=new JsonObject();int entityCount=0;
  for(var level:server.getAllLevels())for(var entity:level.getAllEntities())if(!(entity instanceof net.minecraft.world.entity.player.Player)&&!(entity instanceof BridgeActor)){
   String type=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();entityTypes.addProperty(type,entityTypes.has(type)?entityTypes.get(type).getAsInt()+1:1);entityCount++;
  }
  state.addProperty("nativeEntityCount",entityCount);state.add("nativeEntityTypes",entityTypes);state.addProperty("alliedMobs",BridgeAllies.enabled(server));state.addProperty("travelAck",travelAck);state.addProperty("flashPaused",s.has("paused")&&s.get("paused").getAsBoolean());
  var bounds=new JsonObject();bounds.addProperty("minX",arenaMinX);bounds.addProperty("maxX",arenaMaxX);bounds.addProperty("minZ",arenaMinZ);bounds.addProperty("maxZ",arenaMaxZ);bounds.addProperty("barriers",barriers.size());bounds.addProperty("corrected",confined);state.add("arenaBounds",bounds);
  var allies=new JsonArray();for(var level:server.getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof Mob mob){var info=new JsonObject();info.addProperty("id",entity.getId());info.addProperty("type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());info.addProperty("allied",entity.isAlliedTo(p));info.addProperty("targetPlayer",mob.getTarget() instanceof net.minecraft.world.entity.player.Player);info.addProperty("x",entity.getX());info.addProperty("z",entity.getZ());info.addProperty("width",entity.getBbWidth());allies.add(info);}state.add("mobAlliances",allies);
  for(var infoEl:allies){var info=infoEl.getAsJsonObject();var entity=world.getEntity(info.get("id").getAsInt());if(entity instanceof Mob mob){info.addProperty("targetFlash",mob.getTarget() instanceof BridgeActor);info.addProperty("targetNativeId",mob.getTarget()==null?-1:mob.getTarget().getId());}}
  var ds=new JsonArray();for(var en:damage.entrySet()){var d=new JsonObject();d.addProperty("id",en.getKey());d.addProperty("amount",en.getValue());d.addProperty("dead",enemies.containsKey(en.getKey())&&!enemies.get(en.getKey()).isAlive());ds.add(d);}state.add("damage",ds);
  var list=new JsonArray();var views=new ArrayList<SceneOverlay.BlockView>();for(int xx=px-24;xx<=px+30;xx++)for(int zz=(int)p.getZ()-12;zz<=(int)p.getZ()+12;zz++)for(int yy=80;yy<88;yy++){var pos=new BlockPos(xx,yy,zz);var bs=world.getBlockState(pos);if(bs.isAir()||bs.is(Blocks.BEDROCK))continue;views.add(new SceneOverlay.BlockView(pos,bs));var b=new JsonObject();b.addProperty("key",xx+":"+yy+":"+zz);b.addProperty("x",fx(xx+.5));b.addProperty("y",fy(zz+.5));b.addProperty("height",(yy-79)*UNIT);b.addProperty("id",BuiltInRegistries.BLOCK.getKey(bs.getBlock()).toString());list.add(b);}SceneOverlay.blocks=List.copyOf(views);state.add("blocks",list);
  state.add("enemies",s.get("enemies"));if(s.has("feedback"))state.add("feedback",s.get("feedback"));state.add("playerProxy",s.get("playerProxy"));state.addProperty("checkpoint",num(s,"checkpoint",0));
  var inv=new JsonArray();for(int i=0;i<9;i++){var stack=p.getInventory().getItem(i);var slot=new JsonObject();slot.addProperty("id",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());slot.addProperty("count",stack.getCount());inv.add(slot);}state.add("hotbar",inv);state.addProperty("selected",p.getInventory().selected);state.addProperty("inventoryOpen",BridgeImages.inventoryOpen);state.addProperty("nativeEnemies",enemies.size());state.addProperty("nativeColliders",obstacles.size());state.addProperty("swinging",Minecraft.getInstance().player!=null&&Minecraft.getInstance().player.swinging);output=state.toString();
 }
 private void clearClient(Minecraft mc){
  actions.clear();RemoteUse.release(mc);held.clear();mining=null;mc.gameMode.stopDestroyBlock();RemoteInventory.cancel(mc);RemoteInventory.resetRequest();mc.setScreen(null);
  mc.options.keyUp.setDown(false);mc.options.keyDown.setDown(false);mc.options.keyLeft.setDown(false);mc.options.keyRight.setDown(false);mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);mc.options.keySprint.setDown(false);mc.options.keyUse.setDown(false);
  facing=1;renderYaw=90;com.example.BridgeUseRay.movementYaw=180;
 }
 private void input(Minecraft mc,boolean tick){if(!valid(mc))return;RemoteInventory.ensureBound(mc);JsonObject a;while((a=actions.poll())!=null){String type=a.get("type").getAsString();
  if(type.equals("key")){String key=a.get("key").getAsString();boolean down=a.get("down").getAsBoolean();if(down)held.add(key);else held.remove(key);if(down&&(key.equals("ArrowLeft")||key.equals("KeyA")))facing=-1;if(down&&(key.equals("ArrowRight")||key.equals("KeyD")))facing=1;if(down&&key.equals("KeyE")){RemoteUse.release(mc);held.clear();RemoteInventory.toggle(mc);}if(down&&key.matches("Digit[1-9]")){int n=Integer.parseInt(key.substring(5))-1;mc.player.getInventory().selected=n;mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(n));}}
  else if(type.equals("release")){RemoteUse.release(mc);held.clear();mining=null;mc.gameMode.stopDestroyBlock();if(mc.player.isUsingItem())mc.gameMode.releaseUsingItem(mc.player);RemoteInventory.cancel(mc);}
  else if(type.equals("alliedMobs"))mc.getSingleplayerServer().execute(()->BridgeAllies.enable(mc.getSingleplayerServer()));
  else if(type.equals("summonBoss")){
   String boss=a.has("boss")?a.get("boss").getAsString():"",requestScene=scene;var playerId=mc.player.getUUID();var server=mc.getSingleplayerServer();
   if(boss.equals("dragon")||boss.equals("wither"))server.execute(()->{
    var p=server.getPlayerList().getPlayer(playerId);if(p==null||!scene.equals(requestScene)||flash==null||!server.getWorldData().getLevelName().equals(WORLD))return;
    long now=server.overworld().getGameTime();if(now<nextBossSpawn)return;nextBossSpawn=now+20;
    double sx=p.getX()+(facing>0?3:-3),sz=p.getZ();var bounds=StreetBounds.current;if(bounds!=null){sx=Math.max(bounds.minX()+1,Math.min(bounds.maxX()-1,sx));sz=Math.max(bounds.minZ()+1,Math.min(bounds.maxZ()-1,sz));}
    String command=String.format(Locale.ROOT,"summon minecraft:%s %.3f %.3f %.3f",boss.equals("dragon")?"ender_dragon":"wither",sx,p.getY()+2,sz);
    server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(2).withSuppressedOutput(),command);
   });
  }
  else if(type.equals("inventoryInput"))RemoteInventory.input(mc,a);
  else if(type.equals("aim"))RemoteUse.aim(a);
  else if(type.equals("shield")){boolean down=a.get("down").getAsBoolean();if(down){held.add("shield");mc.gameMode.useItem(mc.player,InteractionHand.OFF_HAND);}else{held.remove("shield");mc.gameMode.releaseUsingItem(mc.player);}}
  else if(type.equals("click")){if(RemoteInventory.active()||RemoteInventory.creativeRequested)continue;double tx=x(num(a,"x",0)),tz=z(num(a,"y",0));if(Math.abs(tx-mc.player.getX())>.1)facing=tx>mc.player.getX()?1:-1;int button=a.get("button").getAsInt();
   if(button==0){Entity target=StreetTargets.attack(mc,flash,enemyIds,num(a,"x",0),num(a,"y",0));if(target!=null){mc.gameMode.attack(mc.player,target);mc.player.swing(InteractionHand.MAIN_HAND);}else{var pos=new BlockPos((int)Math.floor(tx),80,(int)Math.floor(tz));for(int yy=87;yy>=80;yy--){var c=new BlockPos(pos.getX(),yy,pos.getZ());if(!mc.level.getBlockState(c).isAir()&&!mc.level.getBlockState(c).is(Blocks.BEDROCK)){pos=c;break;}}mc.player.swing(InteractionHand.MAIN_HAND);if(mc.player.position().distanceTo(Vec3.atCenterOf(pos))<5&&!mc.level.getBlockState(pos).isAir()){mining=pos;mc.gameMode.startDestroyBlock(pos,Direction.UP);}}}
   else{mining=null;mc.gameMode.stopDestroyBlock();RemoteUse.press(mc,a);}}
  else if(type.equals("mouseUp")){int button=(int)num(a,"button",-1);if(button<=0){mining=null;mc.gameMode.stopDestroyBlock();}if(button<0||button==2)RemoteUse.release(mc);}else if(type.equals("reset")){clearClient(mc);scene="";}
 }
 boolean inv=RemoteInventory.active(),connected=System.nanoTime()-lastFlash<2000000000L;boolean shield=held.contains("shield");float yaw=shield?(facing>0?-90:90):180;if(RemoteUse.held)RemoteUse.orientation(mc,false);else{mc.player.setYRot(yaw);mc.player.setXRot(0);}
 com.example.BridgeUseRay.controlledPlayer=mc.player.getUUID();RemoteUse.update(mc,connected&&!inv);if(shield)mc.options.keyUse.setDown(connected&&!inv);
 double dx=(held.contains("ArrowRight")||held.contains("KeyD")?1:0)-(held.contains("ArrowLeft")||held.contains("KeyA")?1:0),dz=(held.contains("ArrowDown")||held.contains("KeyS")?1:0)-(held.contains("ArrowUp")||held.contains("KeyW")?1:0);double forward=Math.min(1,Math.hypot(dx,dz)),left=0;if(forward>0){com.example.BridgeUseRay.movementYaw=(float)Math.toDegrees(Math.atan2(-dx,dz));if(!RemoteUse.held&&!shield)mc.player.setYRot(com.example.BridgeUseRay.movementYaw);}
 if(!inv&&connected){if(RemoteUse.held||shield)renderYaw=-mc.player.getYRot();else if(dx!=0||dz!=0)renderYaw=(float)Math.toDegrees(Math.atan2(dx,-dz));else if(mc.player.swinging)renderYaw=facing>0?90:270;}
 mc.options.keySprint.setDown(connected&&!inv&&(held.contains("ControlLeft")||held.contains("ControlRight")));mc.options.keyLeft.setDown(connected&&!inv&&left>.1);mc.options.keyRight.setDown(connected&&!inv&&left<-.1);mc.options.keyUp.setDown(connected&&!inv&&forward>.1);mc.options.keyDown.setDown(connected&&!inv&&forward<-.1);mc.options.keyJump.setDown(connected&&!inv&&held.contains("Space"));mc.options.keyShift.setDown(connected&&!inv&&(held.contains("ShiftLeft")||held.contains("ShiftRight")));
 if(tick&&mining!=null){if(mc.level.getBlockState(mining).isAir())mining=null;else if(mc.gameMode.continueDestroyBlock(mining,Direction.UP))mc.player.swing(InteractionHand.MAIN_HAND);}
 }
}

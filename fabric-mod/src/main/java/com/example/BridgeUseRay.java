package com.example;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;

/** A browser-selected 2D hit accompanies vanilla use packets, on both prediction and server execution. */
public final class BridgeUseRay {
    public static final ResourceLocation CHANNEL=new ResourceLocation("mcflash","use_ray");
    private record Entry(BlockHitResult hit,long expires){}
    private static final Map<UUID,Entry> client=new ConcurrentHashMap<>(),server=new ConcurrentHashMap<>();
    private static final ThreadLocal<Entry> context=new ThreadLocal<>();
    public static volatile UUID controlledPlayer;
    public static volatile float movementYaw=180;
    public static void initialize(){
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL,(game,player,handler,buf,response)->{
            var hit=buf.readBlockHitResult();boolean miss=buf.readBoolean();
            game.execute(()->{
                if(!game.getWorldData().getLevelName().equals("MC Dad n Me Bridge"))return;
                var point=hit.getLocation();
                if(!Double.isFinite(point.x+point.y+point.z)||point.y<60||point.y>120||player.position().distanceToSqr(point)>128*128)return;
                if(!miss&&player.getEyePosition().distanceToSqr(point)>6*6)return;
                server.put(player.getUUID(),new Entry(miss?BlockHitResult.miss(point,hit.getDirection(),hit.getBlockPos()):hit,System.currentTimeMillis()+1500));
            });
        });
    }
    public static void client(Player player,BlockHitResult hit){client.put(player.getUUID(),new Entry(hit,System.currentTimeMillis()+1500));}
    public static void enter(Player player){var entry=(player.level().isClientSide?client:server).get(player.getUUID());if(entry!=null&&entry.expires>System.currentTimeMillis())context.set(entry);else context.remove();}
    public static void exit(){context.remove();}
    public static BlockHitResult current(ClipContext.Fluid fluid){var entry=context.get();return entry!=null&&entry.expires>System.currentTimeMillis()?entry.hit:null;}
}

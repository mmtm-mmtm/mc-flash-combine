package com.example.client;

import com.example.BridgeUseRay;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;

/** Native hand/block/entity/item dispatch, fed by the browser's continuous pointer gesture. */
public final class RemoteUse {
    public static volatile boolean held;
    public static volatile String last="";
    private static Vec3 point;
    private static long next;
    private record Target(BlockHitResult ray,boolean block){}
    public static void aim(JsonObject action){
        double x=ExampleModClient.x(ExampleModClient.num(action,"x",0));
        double screenY=ExampleModClient.num(action,"y",0);
        var player=Minecraft.getInstance().player;
        if(player!=null&&Double.isFinite(x+screenY))point=new Vec3(x,80+(ExampleModClient.fy(player.getZ())-screenY)/32,player.getZ());
    }
    public static void press(Minecraft mc,JsonObject action){aim(action);held=true;next=0;mc.options.keyUse.setDown(true);repeat(mc);}
    public static void orientation(Minecraft mc){orientation(mc,true);}
    public static void orientation(Minecraft mc,boolean send){
        if(point==null)return;var delta=point.subtract(mc.player.getEyePosition());
        float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.max(.001,Math.hypot(delta.x,delta.z))));
        if(mc.player.isBlocking())pitch=0;
        mc.player.setYRot(yaw);mc.player.setXRot(pitch);
        if(Math.abs(point.x-mc.player.getX())>.15)ExampleModClient.facing=point.x>mc.player.getX()?1:-1;
        if(send)mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(yaw,pitch,mc.player.onGround()));
    }
    public static void release(Minecraft mc){
        if(mc.player!=null&&mc.player.isUsingItem()){orientation(mc);mc.gameMode.releaseUsingItem(mc.player);}
        held=false;mc.options.keyUse.setDown(false);next=0;
    }
    public static void update(Minecraft mc,boolean allowed){if(!allowed&&held)release(mc);mc.options.keyUse.setDown(allowed&&held);}
    private static Target target(Minecraft mc){
        // Invert the street projection at each height, so visible blocks remain clickable.
        double groundZ=point.z-(point.y-80);var eye=mc.player.getEyePosition();
        for(int y=87;y>=79;y--){
            var at=BlockPos.containing(point.x,y,groundZ+(y-79));
            if(mc.level.getBlockState(at).isAir())continue;
            var hit=new Vec3(point.x,y+1,at.getZ()+.5);
            if(eye.distanceTo(hit)<=mc.gameMode.getPickRange()+.25)return new Target(new BlockHitResult(hit,Direction.UP,at,false),true);
        }
        var at=BlockPos.containing(point.x,79,groundZ);
        var hit=new Vec3(point.x,80,groundZ);
        if(eye.distanceTo(hit)<=mc.gameMode.getPickRange()+.25)return new Target(new BlockHitResult(hit,Direction.UP,at,false),true);
        return new Target(BlockHitResult.miss(point,Direction.UP,BlockPos.containing(point)),false);
    }
    private static Entity entity(Minecraft mc){
        Entity found=null;double distance=Double.POSITIVE_INFINITY;
        for(var e:mc.level.entitiesForRendering()){
            if(e==mc.player||!e.isPickable()||e.isRemoved()||!e.getBoundingBox().inflate(.5).contains(point))continue;
            double d=mc.player.getEyePosition().distanceToSqr(point);if(d<distance&&d<=25){distance=d;found=e;}
        }
        return found;
    }
    public static void repeat(Minecraft mc){
        if(!held||point==null||mc.player==null||mc.gameMode==null||RemoteInventory.active()||RemoteInventory.creativeRequested||mc.gameMode.hasInfiniteItems()||mc.player.isUsingItem())return;
        long now=System.nanoTime();if(now<next)return;next=now+200000000L;
        var target=target(mc);orientation(mc);
        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(mc.player.getInventory().selected));
        BridgeUseRay.client(mc.player,target.ray);var buf=PacketByteBufs.create();buf.writeBlockHitResult(target.ray);buf.writeBoolean(target.ray.getType()==HitResult.Type.MISS);ClientPlayNetworking.send(BridgeUseRay.CHANNEL,buf);
        var hitEntity=entity(mc);
        for(var hand:InteractionHand.values()){
            if(!mc.player.getItemInHand(hand).isItemEnabled(mc.level.enabledFeatures()))return;
            InteractionResult result=InteractionResult.PASS;
            boolean blockFailed=false;
            if(hitEntity!=null){
                result=mc.gameMode.interactAt(mc.player,hitEntity,new EntityHitResult(hitEntity,point),hand);
                if(!result.consumesAction())result=mc.gameMode.interact(mc.player,hitEntity,hand);
            }
            if(hitEntity==null&&!result.consumesAction()&&target.block){result=mc.gameMode.useItemOn(mc.player,hand,target.ray);blockFailed=result==InteractionResult.FAIL;}
            if(!blockFailed&&!result.consumesAction()&&!mc.player.getItemInHand(hand).isEmpty())result=mc.gameMode.useItem(mc.player,hand);
            last="hand="+hand+" result="+result+" hit="+target.ray.getType()+" block="+target.ray.getBlockPos()+" face="+target.ray.getDirection()+" item="+mc.player.getItemInHand(hand);
            if(result.shouldSwing())mc.player.swing(hand);
            if(result.consumesAction()||blockFailed)return;
        }
    }
}

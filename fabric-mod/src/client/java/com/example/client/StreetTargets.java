package com.example.client;

import com.example.BridgeActor;
import com.google.gson.*;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Pick the visible street silhouette, then enforce native world-space reach. */
final class StreetTargets {
    static Entity attack(Minecraft mc,JsonObject snapshot,Map<String,Integer> ids,double px,double py){
        Entity found=null;double best=Double.POSITIVE_INFINITY;
        if(snapshot!=null&&snapshot.has("enemies"))for(var value:snapshot.getAsJsonArray("enemies")){
            var target=value.getAsJsonObject();var eid=ids.get(target.get("id").getAsString());
            Entity actor=eid==null?null:mc.level.getEntity(eid);
            if(!reachable(mc,actor))continue;
            var bounds=target.getAsJsonObject("bounds");
            double x=ExampleModClient.num(target,"x",0),y=ExampleModClient.num(target,"y",0);
            double x0=bounds==null?x-24:ExampleModClient.num(bounds,"x0",x-24),x1=bounds==null?x+24:ExampleModClient.num(bounds,"x1",x+24);
            double y0=bounds==null?y-60:ExampleModClient.num(bounds,"y0",y-60),y1=bounds==null?y+8:ExampleModClient.num(bounds,"y1",y+8);
            double score=score(px,py,x0,y0,x1,y1);
            if(score<best){best=score;found=actor;}
        }
        for(var entity:mc.level.entitiesForRendering()){
            if(entity instanceof BridgeActor||!entity.isPickable()||!reachable(mc,entity))continue;
            var b=entity.getBoundingBox();
            double score=score(px,py,ExampleModClient.fx(b.minX),ExampleModClient.fy(b.minZ)-(b.maxY-80)*32,
                ExampleModClient.fx(b.maxX),ExampleModClient.fy(b.maxZ)-(b.minY-80)*32);
            if(score<best){best=score;found=entity;}
        }
        return found;
    }
    private static boolean reachable(Minecraft mc,Entity entity){
        if(entity==null||entity==mc.player||!entity.isAlive()||entity.isRemoved())return false;
        var b=entity.getBoundingBox();var eye=mc.player.getEyePosition();
        double dx=Math.max(b.minX-eye.x,Math.max(0,eye.x-b.maxX));
        double dy=Math.max(b.minY-eye.y,Math.max(0,eye.y-b.maxY));
        double dz=Math.max(b.minZ-eye.z,Math.max(0,eye.z-b.maxZ));
        return dx*dx+dy*dy+dz*dz<=4.5*4.5;
    }
    private static double score(double x,double y,double x0,double y0,double x1,double y1){
        if(x<x0-8||x>x1+8||y<y0-8||y>y1+8)return Double.POSITIVE_INFINITY;
        return Math.hypot(x-(x0+x1)/2,y-(y0+y1)/2);
    }
}

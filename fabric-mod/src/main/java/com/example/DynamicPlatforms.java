package com.example;

import com.google.gson.*;
import java.util.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Immutable Flash polygon snapshots shared by the integrated server and native client. */
public final class DynamicPlatforms {
    public record Platform(int id,String name,double x,double y,double angle,List<AABB> boxes,List<VoxelShape> shapes) {
        public double support(AABB player){
            double top=Double.NaN;
            for(AABB box:boxes)if(player.maxX>box.minX+1e-5&&player.minX<box.maxX-1e-5&&player.maxZ>box.minZ&&player.minZ<box.maxZ)
                top=Double.isNaN(top)?box.maxY:Math.max(top,box.maxY);
            return top;
        }
    }
    public record Snapshot(String scene,Map<Integer,Platform> platforms){}
    public static volatile Snapshot current=new Snapshot("",Map.of());
    public static final ThreadLocal<Integer> carrying=ThreadLocal.withInitial(()->-1);
    private static String previousJson="";
    public static synchronized void clear(){previousJson="";current=new Snapshot("",Map.of());}
    public static synchronized void update(String scene,JsonArray data){
        String json=data==null?"[]":data.toString();
        if(scene.equals(current.scene)&&json.equals(previousJson))return;
        Map<Integer,Platform> platforms=new HashMap<>();
        if(data!=null)for(JsonElement entry:data){
            JsonObject p=entry.getAsJsonObject();JsonArray points=p.getAsJsonArray("vertices");
            if(points==null||points.size()<3||points.size()>12)continue;
            double[] xs=new double[points.size()],ys=new double[points.size()];
            double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
            for(int i=0;i<points.size();i++){
                var v=points.get(i).getAsJsonArray();xs[i]=v.get(0).getAsDouble()+.5;ys[i]=100.5-v.get(1).getAsDouble();
                minX=Math.min(minX,xs[i]);maxX=Math.max(maxX,xs[i]);minY=Math.min(minY,ys[i]);maxY=Math.max(maxY,ys[i]);
            }
            if(!Double.isFinite(minX+maxX+minY+maxY)||minX< -2||maxX>42||minY<65||maxY>106)continue;
            List<AABB> boxes=new ArrayList<>();double area=0;
            for(int i=0;i<xs.length;i++){int j=(i+1)%xs.length;area+=xs[i]*ys[j]-xs[j]*ys[i];}
            if(Math.abs(Math.abs(area)/2-(maxX-minX)*(maxY-minY))<1e-5){
                boxes.add(new AABB(minX,minY,0,maxX,maxY,1));
            }else{
                // Convex rotated polygons become fine horizontal bands; rectangular lifts remain exact.
                int bands=Math.max(1,Math.min(128,(int)Math.ceil((maxY-minY)*16)));
                for(int b=0;b<bands;b++){
                    double lo=minY+(maxY-minY)*b/bands,hi=minY+(maxY-minY)*(b+1)/bands,mid=(lo+hi)/2;
                    double left=Double.POSITIVE_INFINITY,right=Double.NEGATIVE_INFINITY;
                    for(int i=0;i<xs.length;i++){int j=(i+1)%xs.length;
                        if((ys[i]<=mid&&ys[j]>mid)||(ys[j]<=mid&&ys[i]>mid)){
                            double x=xs[i]+(mid-ys[i])*(xs[j]-xs[i])/(ys[j]-ys[i]);left=Math.min(left,x);right=Math.max(right,x);
                        }
                    }
                    if(left<right)boxes.add(new AABB(left,lo,0,right,hi,1));
                }
            }
            var shapes=boxes.stream().map(Shapes::create).toList();int id=p.get("id").getAsInt();
            platforms.put(id,new Platform(id,p.get("name").getAsString(),p.get("x").getAsDouble()+.5,
                100.5-p.get("y").getAsDouble(),-p.get("angle").getAsDouble(),List.copyOf(boxes),shapes));
        }
        previousJson=json;current=new Snapshot(scene,Map.copyOf(platforms));
    }
    public static List<VoxelShape> colliders(AABB query){
        List<VoxelShape> result=new ArrayList<>();
        for(Platform p:current.platforms.values())if(!p.name().equals("platform")&&p.id()!=carrying.get())for(int i=0;i<p.boxes.size();i++)
            if(p.boxes.get(i).intersects(query))result.add(p.shapes.get(i));
        return result;
    }
}

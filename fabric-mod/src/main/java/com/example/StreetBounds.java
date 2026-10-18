package com.example;

import java.util.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Flash walking limits apply to native mobs, including jumps and no-physics bosses. */
public final class StreetBounds {
    public record Arena(double minX,double maxX,double minZ,double maxZ,List<AABB> obstacles){}
    public static volatile Arena current;
    public static List<VoxelShape> colliders(Entity entity,AABB query){
        var a=current;if(a==null||!(entity instanceof Mob)||entity.level().dimension()!=Level.OVERWORLD)return List.of();
        var result=new ArrayList<VoxelShape>();double lo=query.minY-1,hi=query.maxY+1;
        if(query.minX<a.minX)result.add(Shapes.create(new AABB(query.minX-1,lo,query.minZ-1,a.minX,hi,query.maxZ+1)));
        if(query.maxX>a.maxX)result.add(Shapes.create(new AABB(a.maxX,lo,query.minZ-1,query.maxX+1,hi,query.maxZ+1)));
        if(query.minZ<a.minZ)result.add(Shapes.create(new AABB(query.minX-1,lo,query.minZ-1,query.maxX+1,hi,a.minZ)));
        if(query.maxZ>a.maxZ)result.add(Shapes.create(new AABB(query.minX-1,lo,a.maxZ,query.maxX+1,hi,query.maxZ+1)));
        for(var box:a.obstacles)if(query.maxX>box.minX&&query.minX<box.maxX&&query.maxZ>box.minZ&&query.minZ<box.maxZ)
            result.add(Shapes.create(new AABB(box.minX,lo,box.minZ,box.maxX,hi,box.maxZ)));
        return result;
    }
    public static boolean contain(Mob mob){
        var a=current;if(a==null)return false;
        double r=mob.getBbWidth()/2+.02,rx=Math.min(r,(a.maxX-a.minX)*.45),rz=Math.min(r,(a.maxZ-a.minZ)*.45);
        double x=Math.max(a.minX+rx,Math.min(a.maxX-rx,mob.getX())),z=Math.max(a.minZ+rz,Math.min(a.maxZ-rz,mob.getZ()));
        for(var box:a.obstacles){
            double x0=box.minX-rx,x1=box.maxX+rx,z0=box.minZ-rz,z1=box.maxZ+rz;
            if(x>x0&&x<x1&&z>z0&&z<z1){
                double d=Math.min(Math.min(x-x0,x1-x),Math.min(z-z0,z1-z));
                if(d==x-x0)x=x0;else if(d==x1-x)x=x1;else if(d==z-z0)z=z0;else z=z1;
            }
        }
        x=Math.max(a.minX+rx,Math.min(a.maxX-rx,x));z=Math.max(a.minZ+rz,Math.min(a.maxZ-rz,z));
        boolean dx=Math.abs(x-mob.getX())>.0001,dz=Math.abs(z-mob.getZ())>.0001;
        if(dx||dz){mob.teleportTo(x,mob.getY(),z);var v=mob.getDeltaMovement();mob.setDeltaMovement(dx?0:v.x,v.y,dz?0:v.z);mob.getNavigation().stop();return true;}return false;
    }
}

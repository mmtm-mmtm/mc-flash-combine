package com.example.client;

import com.example.DynamicPlatforms;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** Carry a supported native player by the platform's real change in pose, then let MC collide. */
public final class PlatformMotion {
    private static DynamicPlatforms.Snapshot previous;
    public static volatile int standing=-1;
    public static volatile double carriedX,carriedY;
    public static void tick(Minecraft mc){
        var now=DynamicPlatforms.current;var old=previous;previous=now;standing=-1;carriedX=carriedY=0;
        if(mc.player==null||mc.getSingleplayerServer()==null||
           !mc.getSingleplayerServer().getWorldData().getLevelName().equals("MC Dad n Me Bridge")||
           old==null||!old.scene().equals(now.scene())||mc.options.keyJump.isDown()||!mc.player.isAlive())return;
        var player=mc.player;var box=player.getBoundingBox();
        for(var p:old.platforms().values()){
            if(p.name().equals("platform"))continue;
            var next=now.platforms().get(p.id());if(next==null)continue;
            double top=p.support(box);
            if(Double.isNaN(top)||Math.abs(box.minY-top)>.16||player.getDeltaMovement().y>.12)continue;
            double angle=next.angle()-p.angle(),rx=player.getX()-p.x(),ry=box.minY-p.y();
            double dx=next.x()+rx*Math.cos(angle)-ry*Math.sin(angle)-player.getX();
            double dy=next.y()+rx*Math.sin(angle)+ry*Math.cos(angle)-box.minY;
            if(Math.abs(dx)>1||Math.abs(dy)>1)continue;
            standing=p.id();
            if(Math.abs(dx)+Math.abs(dy)>1e-7){
                double x=player.getX(),y=player.getY();
                DynamicPlatforms.carrying.set(p.id());
                try{player.move(MoverType.SELF,new Vec3(dx,dy,0));}
                finally{DynamicPlatforms.carrying.remove();}
                carriedX=player.getX()-x;carriedY=player.getY()-y;
                player.fallDistance=0;player.setOnGround(true);
                if(player.getDeltaMovement().y<0)player.setDeltaMovement(player.getDeltaMovement().multiply(1,0,1));
            }
            break;
        }
    }
}

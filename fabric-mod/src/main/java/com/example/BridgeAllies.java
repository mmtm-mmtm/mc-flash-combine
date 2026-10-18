package com.example;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;

/** A persistent opt-in alliance in the dedicated crossover save. */
public final class BridgeAllies extends SavedData {
    private boolean enabled;
    private static boolean valid(ServerLevel level){return level.getServer().getWorldData().getLevelName().equals("MC Dad n Me Bridge");}
    private static BridgeAllies data(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(BridgeAllies::load,BridgeAllies::new,"mc_dadnme_allies");}
    private static BridgeAllies load(CompoundTag tag){var d=new BridgeAllies();d.enabled=tag.getBoolean("enabled");return d;}
    @Override public CompoundTag save(CompoundTag tag){tag.putBoolean("enabled",enabled);return tag;}
    public static boolean enabled(MinecraftServer server){return data(server).enabled;}
    public static void enable(MinecraftServer server){var d=data(server);d.enabled=true;d.setDirty();tick(server);}
    public static boolean friendly(Entity a,Entity b){return a!=null&&b!=null&&a.level() instanceof ServerLevel level&&valid(level)&&enabled(level.getServer())&&member(a)&&member(b);}
    private static boolean member(Entity e){return e instanceof Player||(e instanceof Mob&&!(e instanceof BridgeActor));}
    public static void tick(MinecraftServer server){
        if(!server.getWorldData().getLevelName().equals("MC Dad n Me Bridge")||!enabled(server))return;
        var board=server.getScoreboard();PlayerTeam team=board.getPlayerTeam("mc_bridge_allies");if(team==null)team=board.addPlayerTeam("mc_bridge_allies");team.setAllowFriendlyFire(false);
        for(var level:server.getAllLevels())for(var entity:level.getAllEntities())if(member(entity)){
            if(entity.getTeam()!=team)board.addPlayerToTeam(entity.getScoreboardName(),team);
            if(entity instanceof Mob mob){
                if(friendly(mob,mob.getTarget())){mob.setTarget(null);mob.getNavigation().stop();}
                if(friendly(mob,mob.getLastHurtByMob()))mob.setLastHurtByMob(null);
                if(mob instanceof NeutralMob neutral&&neutral.getPersistentAngerTarget()!=null){var target=level.getEntity(neutral.getPersistentAngerTarget());if(target!=null&&friendly(mob,target))neutral.stopBeingAngry();}
                if(mob instanceof WitherBoss wither)for(int i=0;i<3;i++){var target=level.getEntity(wither.getAlternativeTarget(i));if(friendly(mob,target))wither.setAlternativeTarget(i,0);}
            }
        }
    }
}

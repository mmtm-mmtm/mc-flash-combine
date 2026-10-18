package com.example;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

/** Persist replay membership so unloaded entities from an older run cannot return. */
public final class ReplayEpoch extends SavedData {
    private static final String PREFIX="mc_dadnme_run_";
    private String epoch=UUID.randomUUID().toString();
    private static boolean valid(ServerLevel level){return level.getServer().getWorldData().getLevelName().equals("MC Dad n Me Bridge");}
    private static ReplayEpoch data(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(ReplayEpoch::load,ReplayEpoch::new,"mc_dadnme_replay");}
    private static ReplayEpoch load(CompoundTag tag){var data=new ReplayEpoch();if(tag.contains("epoch"))data.epoch=tag.getString("epoch");return data;}
    @Override public CompoundTag save(CompoundTag tag){tag.putString("epoch",epoch);return tag;}
    public static String rotate(MinecraftServer server){var data=data(server);data.epoch=UUID.randomUUID().toString();data.setDirty();return data.epoch;}
    public static void fresh(ServerLevel level,Entity entity){
        if(!valid(level)||entity instanceof Player)return;
        entity.getTags().removeIf(tag->tag.startsWith(PREFIX));entity.addTag(PREFIX+data(level.getServer()).epoch);
    }
    public static void loaded(Entity entity,ServerLevel level){
        if(!valid(level)||entity instanceof Player)return;
        if(!entity.getTags().contains(PREFIX+data(level.getServer()).epoch))entity.discard();
    }
}

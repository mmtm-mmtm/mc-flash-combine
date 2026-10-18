package com.example.client;

import com.example.ReplayEpoch;
import com.example.mixin.ReplayChunksAccess;
import com.google.gson.JsonObject;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;

/** Reset only this dedicated replay save; no files or other saves are deleted. */
final class ReplayReset {
    static JsonObject clear(MinecraftServer server,Set<BlockPos> floor,Set<BlockPos> obstacles){
        if(!server.getWorldData().getLevelName().equals(ExampleModClient.WORLD))throw new IllegalStateException("Not the replay save");
        String epoch=ReplayEpoch.rotate(server);int removed=0,cleared=0;
        for(var level:server.getAllLevels()){
            var entities=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!(entity instanceof Player))entities.add(entity);
            for(var entity:entities){entity.discard();removed++;}
        }
        var level=server.overworld();var chunks=new HashSet<Long>();
        for(var holder:((ReplayChunksAccess)level.getChunkSource().chunkMap).mcflashChunks()){
            var chunk=holder.getFullChunk();if(chunk!=null)chunks.add(chunk.getPos().toLong());
        }
        for(var pos:floor)chunks.add(new ChunkPos(pos).toLong());for(var pos:obstacles)chunks.add(new ChunkPos(pos).toLong());
        // Previously visited chunks can already be unloaded and saved on disk.
        var region=server.getWorldPath(LevelResource.ROOT).resolve("region");
        var storedBlocks=ConcurrentHashMap.<Long>newKeySet();var reads=new ArrayList<CompletableFuture<?>>();
        if(Files.isDirectory(region))try(var files=Files.list(region)){
            for(var path:files.filter(p->p.getFileName().toString().matches("r\\.-?\\d+\\.-?\\d+\\.mca")).toList()){
                var name=path.getFileName().toString().split("\\.");int rx=Integer.parseInt(name[1]),rz=Integer.parseInt(name[2]);
                try(var file=new RandomAccessFile(path.toFile(),"r")){
                    if(file.length()<4096)continue;
                    for(int i=0;i<1024;i++)if(file.readInt()!=0){
                        long key=ChunkPos.asLong(rx*32+(i&31),rz*32+(i>>5));if(chunks.contains(key))continue;
                        reads.add(level.getChunkSource().chunkMap.read(new ChunkPos(key)).thenAccept(tag->{if(tag.isPresent()&&hasBlocks(tag.get()))storedBlocks.add(key);}));
                    }
                }
            }
        }catch(java.io.IOException ex){throw new IllegalStateException("Cannot enumerate saved replay chunks",ex);}
        CompletableFuture.allOf(reads.toArray(CompletableFuture[]::new)).join();chunks.addAll(storedBlocks);
        var pos=new BlockPos.MutableBlockPos();
        for(long key:chunks){
            var chunkPos=new ChunkPos(key);var chunk=level.getChunk(chunkPos.x,chunkPos.z);var sections=chunk.getSections();
            for(int index=0;index<sections.length;index++){
                var section=sections[index];if(section.hasOnlyAir())continue;int bottom=level.getMinBuildHeight()+index*16;
                for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    if(section.getBlockState(x,y,z).isAir())continue;
                    pos.set(chunkPos.getMinBlockX()+x,bottom+y,chunkPos.getMinBlockZ()+z);
                    level.setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);cleared++;
                }
            }
            var area=new BoundingBox(chunkPos.getMinBlockX(),level.getMinBuildHeight(),chunkPos.getMinBlockZ(),chunkPos.getMaxBlockX(),level.getMaxBuildHeight()-1,chunkPos.getMaxBlockZ());
            level.getBlockTicks().clearArea(area);level.getFluidTicks().clearArea(area);
        }
        floor.clear();obstacles.clear();var result=new JsonObject();result.addProperty("epoch",epoch);result.addProperty("removedEntities",removed);result.addProperty("clearedBlocks",cleared);result.addProperty("chunks",chunks.size());return result;
    }
    private static boolean hasBlocks(CompoundTag chunk){
        var sections=chunk.getList("sections",10);
        for(int i=0;i<sections.size();i++){
            var palette=sections.getCompound(i).getCompound("block_states").getList("palette",10);
            for(int j=0;j<palette.size();j++){
                String name=palette.getCompound(j).getString("Name");
                if(!name.equals("minecraft:air")&&!name.equals("minecraft:cave_air")&&!name.equals("minecraft:void_air"))return true;
            }
        }
        return false;
    }
}

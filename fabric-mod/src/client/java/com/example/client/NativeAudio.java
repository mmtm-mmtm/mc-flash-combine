package com.example.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;

/** Forward resolved vanilla sound instances, retaining vanilla timing, variants and lifecycle. */
public final class NativeAudio {
    private static final class Voice {
        final long id,started; final SoundInstance instance;
        int age; boolean active;
        Voice(long id,SoundInstance instance){this.id=id;this.instance=instance;started=System.currentTimeMillis();}
    }
    private static final IdentityHashMap<SoundInstance,Voice> voices=new IdentityHashMap<>();
    private static final ArrayDeque<JsonObject> events=new ArrayDeque<>();
    private static final String boot=UUID.randomUUID().toString();
    private static String scene="",owner="";
    private static long lease,seq,serial,forwarded;
    private static boolean registered,resetPending;
    private static volatile boolean enabled,paused;
    private static volatile long mutedWrites;
    public static boolean routing(){return enabled;}
    public static float nativeVolume(float value){if(!enabled)return value;mutedWrites++;return 0;}

    public static synchronized void scene(String value){
        if(scene.equals(value))return;
        scene=value;owner="";lease=0;resetPending=true;clear();
    }
    private static void emit(JsonObject event){event.addProperty("seq",++seq);events.add(event);while(events.size()>1024)events.removeFirst();}
    public static synchronized void clear(){voices.clear();events.clear();var e=new JsonObject();e.addProperty("op","reset");emit(e);}
    public static synchronized void stopped(SoundInstance sound){var v=voices.remove(sound);if(v!=null){var e=new JsonObject();e.addProperty("op","stop");e.addProperty("id",v.id);emit(e);}}
    public static synchronized void stopped(ResourceLocation name,SoundSource category){
        for(var v:new ArrayList<>(voices.values()))if((name==null||v.instance.getLocation().equals(name))&&(category==null||v.instance.getSource()==category))stopped(v.instance);
    }
    private static JsonObject describe(Voice voice){
        var mc=Minecraft.getInstance();var sound=voice.instance;var clip=sound.getSound();var e=new JsonObject();
        e.addProperty("id",voice.id);e.addProperty("event",sound.getLocation().toString());e.addProperty("clip",clip.getPath().toString());
        e.addProperty("category",sound.getSource().getName());e.addProperty("started",voice.started);
        e.addProperty("loop",sound.isLooping()&&sound.getDelay()==0);e.addProperty("pitch",Mth.clamp(sound.getPitch(),.5f,2f));
        double gain=Mth.clamp(sound.getVolume()*mc.options.getSoundSourceVolume(sound.getSource()),0,1)*mc.options.getSoundSourceVolume(SoundSource.MASTER),pan=0;
        if(!sound.isRelative()&&sound.getAttenuation()!=SoundInstance.Attenuation.NONE&&mc.player!=null){
            double range=Math.max(1,sound.getVolume())*clip.getAttenuationDistance();
            double distance=mc.player.getEyePosition().distanceTo(new net.minecraft.world.phys.Vec3(sound.getX(),sound.getY(),sound.getZ()));
            gain*=Math.max(0,1-distance/Math.max(1,range));pan=Mth.clamp((sound.getX()-mc.player.getX())/Math.max(1,range),-.85,.85);
        }
        e.addProperty("gain",gain);e.addProperty("pan",pan);return e;
    }
    private static synchronized void played(SoundInstance sound){
        if(!enabled||sound.getSound()==null||sound.getSound()==SoundManager.EMPTY_SOUND||sound.getSound()==SoundManager.INTENTIONALLY_EMPTY_SOUND)return;
        stopped(sound);
        if(voices.size()>=256)stopped(voices.values().stream().min(Comparator.comparingLong(v->v.id)).orElseThrow().instance);
        var voice=new Voice(++serial,sound);voices.put(sound,voice);forwarded++;
        var e=describe(voice);e.addProperty("op","play");emit(e);
    }
    public static synchronized void tick(Minecraft mc,boolean available,boolean scenePaused){
        if(!registered){mc.getSoundManager().addListener((sound,event)->played(sound));registered=true;}
        boolean desired=available&&System.currentTimeMillis()-lease<2500&&!owner.isEmpty();
        if(desired!=enabled||resetPending){enabled=desired;resetPending=false;mc.getSoundManager().stop();clear();}
        paused=scenePaused||mc.isPaused();
        if(!enabled||paused)return;
        for(var voice:new ArrayList<>(voices.values())){
            voice.age++;
            boolean active=mc.getSoundManager().isActive(voice.instance);
            if(active)voice.active=true;
            if(!active&&(voice.active||voice.age>8))stopped(voice.instance);
        }
    }
    public static synchronized JsonObject poll(String client,String requestedScene,long after,boolean observe){
        var result=new JsonObject();result.addProperty("boot",boot);result.addProperty("scene",scene);
        boolean matches=!scene.isEmpty()&&scene.equals(requestedScene)&&(observe||client.matches("[A-Za-z0-9-]{1,80}"));
        result.addProperty("ready",matches);
        if(!matches)return result;
        // A scene belongs to its current page. An expired lease can be reclaimed after a reload.
        long now=System.currentTimeMillis();
        if(!observe){if(owner.isEmpty()||owner.equals(client)||now-lease>2500){owner=client;lease=now;}else{result.addProperty("ready",false);return result;}}
        result.addProperty("enabled",enabled);result.addProperty("paused",paused);result.addProperty("seq",seq);result.addProperty("forwarded",forwarded);
        result.addProperty("nativeMutedWrites",mutedWrites);
        result.addProperty("reset",after<0||after>seq||(!events.isEmpty()&&after<events.getFirst().get("seq").getAsLong()-1));
        var list=new JsonArray();for(var e:events)if(e.get("seq").getAsLong()>after)list.add(e.deepCopy());result.add("events",list);
        var live=new JsonArray();for(var v:voices.values())live.add(describe(v));result.add("active",live);return result;
    }
}

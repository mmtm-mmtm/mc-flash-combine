package com.example.client;

import com.sun.net.httpserver.HttpServer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class AudioHttp {
    public static void register(HttpServer http){
        http.createContext("/audio/",exchange->{
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin","http://127.0.0.1:18125");
            exchange.getResponseHeaders().set("Cache-Control","no-store");
            try {
                if(!exchange.getRequestMethod().equals("GET")){exchange.sendResponseHeaders(405,-1);return;}
                var params=new HashMap<String,String>();var query=exchange.getRequestURI().getRawQuery();
                if(query!=null)for(String part:query.split("&")){var bits=part.split("=",2);if(bits.length==2)params.put(URLDecoder.decode(bits[0],StandardCharsets.UTF_8),URLDecoder.decode(bits[1],StandardCharsets.UTF_8));}
                byte[] data;
                if(exchange.getRequestURI().getPath().equals("/audio/events")){
                    data=NativeAudio.poll(params.getOrDefault("client",""),params.getOrDefault("scene",""),Long.parseLong(params.getOrDefault("after","-1")),params.getOrDefault("observe","").equals("1")).toString().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type","application/json");
                }else if(exchange.getRequestURI().getPath().equals("/audio/clip")){
                    var path=new ResourceLocation(params.getOrDefault("path",""));
                    if(!path.getPath().startsWith("sounds/")||!path.getPath().endsWith(".ogg")||path.getPath().contains("..")){exchange.sendResponseHeaders(400,-1);return;}
                    var resource=Minecraft.getInstance().getResourceManager().getResource(path);
                    if(resource.isEmpty()){exchange.sendResponseHeaders(404,-1);return;}
                    try(var input=resource.get().open()){data=input.readNBytes(32*1024*1024);}
                    exchange.getResponseHeaders().set("Content-Type","audio/ogg");
                    exchange.getResponseHeaders().set("Cache-Control","private,max-age=3600");
                }else{exchange.sendResponseHeaders(404,-1);return;}
                exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);
            }catch(Exception ex){exchange.sendResponseHeaders(400,-1);}finally{exchange.close();}
        });
    }
}

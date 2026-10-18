package com.example.client;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

/** Browser input retains vanilla inventory press/drag/release semantics. */
public final class RemoteInventory {
    public static double x=.5,y=.5;
    public static boolean shift,control,alt;
    private static int button=-1;
    private static double lastX,lastY;
    private static net.minecraft.client.player.LocalPlayer owner;
    public static volatile boolean creativeRequested;
    private static volatile boolean pendingOpen;
    public static boolean active(){return Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;}
    public static void open(Minecraft mc){
        creativeRequested=true;pendingOpen=true;
    }
    public static void resetRequest(){creativeRequested=false;pendingOpen=false;}
    public static void ensureBound(Minecraft mc){
        if(pendingOpen&&mc.gameMode!=null&&mc.gameMode.hasInfiniteItems()){
            owner=mc.player;mc.setScreen(new CreativeModeInventoryScreen(mc.player,mc.level.enabledFeatures(),false));pendingOpen=false;
        }
        if(mc.screen instanceof CreativeModeInventoryScreen&&owner!=mc.player){cancel(mc);mc.setScreen(null);resetRequest();}
        if(creativeRequested&&!pendingOpen&&!(mc.screen instanceof CreativeModeInventoryScreen))resetRequest();
    }
    public static void toggle(Minecraft mc){
        cancel(mc);
        if(active()){mc.screen.onClose();resetRequest();}
        else if(pendingOpen)resetRequest();else open(mc);
    }
    public static void modifiers(JsonObject a){
        shift=a.has("shift")&&a.get("shift").getAsBoolean();
        control=a.has("ctrl")&&a.get("ctrl").getAsBoolean();
        alt=a.has("alt")&&a.get("alt").getAsBoolean();
    }
    public static void cancel(Minecraft mc){
        if(button>=0&&active())mc.screen.mouseReleased(lastX,lastY,button);
        button=-1;shift=control=alt=false;
    }
    public static void input(Minecraft mc,JsonObject a){
        if(!active())return;
        modifiers(a);
        String phase=a.get("phase").getAsString();
        if(phase.equals("cancel")){cancel(mc);return;}
        if(phase.equals("key")){
            int key=a.get("keyCode").getAsInt(),mods=(shift?1:0)|(control?2:0)|(alt?4:0);
            if(a.get("down").getAsBoolean()){
                mc.screen.keyPressed(key,0,mods);
                if(mc.screen!=null&&a.has("text")&&!control&&!alt&&!a.get("text").getAsString().isEmpty())mc.screen.charTyped(a.get("text").getAsString().charAt(0),mods);
            }else mc.screen.keyReleased(key,0,mods);
            return;
        }
        double px=a.get("x").getAsDouble(),py=a.get("y").getAsDouble();
        if(!Double.isFinite(px+py))return;
        x=px;y=py;
        double gx=x*mc.getWindow().getGuiScaledWidth(),gy=y*mc.getWindow().getGuiScaledHeight();
        if(phase.equals("down")){
            button=a.get("button").getAsInt();mc.screen.mouseClicked(gx,gy,button);
        }else if(phase.equals("move")){
            mc.screen.mouseMoved(gx,gy);
            if(button>=0){
                int steps=Math.min(128,Math.max(1,(int)Math.ceil(Math.max(Math.abs(gx-lastX),Math.abs(gy-lastY))/8)));
                for(int i=1;i<=steps;i++)mc.screen.mouseDragged(lastX+(gx-lastX)*i/steps,lastY+(gy-lastY)*i/steps,button,(gx-lastX)/steps,(gy-lastY)/steps);
            }
        }else if(phase.equals("up")){
            mc.screen.mouseReleased(gx,gy,a.get("button").getAsInt());button=-1;
        }else if(phase.equals("wheel"))mc.screen.mouseScrolled(gx,gy,a.get("delta").getAsDouble());
        lastX=gx;lastY=gy;
    }
}

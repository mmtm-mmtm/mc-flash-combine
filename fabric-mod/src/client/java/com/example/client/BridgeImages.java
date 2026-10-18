package com.example.client;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.joml.Matrix4f;

public final class BridgeImages {
 // The player is rendered at 50 pixels per native unit, then enlarged by the page.
 public static final int PLAYER_MODEL_SCALE=50;
 public static final float ENTITY_DISPLAY_SCALE=PLAYER_MODEL_SCALE*184.8F/128F*.9F;
 public static volatile byte[] sprite=new byte[0], frame=new byte[0], hud=new byte[0];
 public static volatile boolean inventoryOpen=false;
 public static volatile double playerX,playerY;
 public static volatile long spriteFrame;
 private static TextureTarget target,hudTarget;
 private static long nextHud;
 private static long next;
 public static void capture() {
  Minecraft mc=Minecraft.getInstance();
  mc.options.pauseOnLostFocus=false;
  ExampleModClient.pump(mc);
  if(mc.player==null||mc.getSingleplayerServer()==null||!mc.getSingleplayerServer().getWorldData().getLevelName().equals("MC Dad n Me Bridge"))return;
  mc.options.pauseOnLostFocus=false;
  playerX=mc.player.getX()-0.5;playerY=99.7-mc.player.getY();
  long now=System.nanoTime();if(now<next)return;next=now+33333333L;
  inventoryOpen=RemoteInventory.active();
  Matrix4f oldProjection=new Matrix4f(RenderSystem.getProjectionMatrix());
  var oldSorting=RenderSystem.getVertexSorting();var mv=RenderSystem.getModelViewStack();boolean pushed=false;
  try {
   if(inventoryOpen){try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){frame=image.asByteArray();}}
   if(target==null){target=new TextureTarget(128,128,true,Minecraft.ON_OSX);target.setClearColor(0,0,0,0);}
   mv.pushPose();pushed=true;mv.setIdentity();mv.translate(0,0,-11000);RenderSystem.applyModelViewMatrix();
   target.clear(Minecraft.ON_OSX);target.bindWrite(true);
   RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,128,128,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
   GuiGraphics gui=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
   renderSideways(gui,mc);gui.flush();
   RenderSystem.bindTexture(target.getColorTextureId());
   try(var image=new com.mojang.blaze3d.platform.NativeImage(128,128,false)) {
      image.downloadTexture(0,false);image.flipY();sprite=image.asByteArray();spriteFrame++;
   }
   if(now>=nextHud){nextHud=now+100000000L;captureHud(mc);}
   SceneOverlay.capture(mc,now);
  } catch(Exception e) {System.err.println("Bridge sprite capture: "+e.getMessage());next=now+1000000000L;}
  finally {if(pushed)mv.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(oldProjection,oldSorting);mc.getMainRenderTarget().bindWrite(true);}
 }
 private static void captureHud(Minecraft mc) throws java.io.IOException {
  int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
  if(hudTarget==null||hudTarget.width!=w||hudTarget.height!=h){
   if(hudTarget!=null)hudTarget.destroyBuffers();
   hudTarget=new TextureTarget(w,h,true,Minecraft.ON_OSX);hudTarget.setClearColor(0,0,0,0);
  }
  hudTarget.clear(Minecraft.ON_OSX);hudTarget.bindWrite(true);
  RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,w,h,0,1000,21000),VertexSorting.ORTHOGRAPHIC_Z);
  var graphics=new GuiGraphics(mc,mc.renderBuffers().bufferSource());
  var access=(com.example.client.mixin.BridgeGuiAccess)mc.gui;
  access.mcflashHotbar(mc.getFrameTime(),graphics);access.mcflashHealth(graphics);
  mc.gui.renderExperienceBar(graphics,w/2-91);mc.gui.renderSelectedItemName(graphics);graphics.flush();
  RenderSystem.bindTexture(hudTarget.getColorTextureId());
  try(var all=new com.mojang.blaze3d.platform.NativeImage(w,h,false);
      var cropped=new com.mojang.blaze3d.platform.NativeImage(184,64,false)) {
   all.downloadTexture(0,false);all.flipY();
   for(int y=0;y<64;y++)for(int x=0;x<184;x++)cropped.setPixelRGBA(x,y,all.getPixelRGBA(w/2-92+x,h-64+y));
   hud=cropped.asByteArray();
  }
 }
 private static void renderSideways(GuiGraphics gui,Minecraft mc) {
  var p=mc.player;
  float body=p.yBodyRot,oldBody=p.yBodyRotO,yaw=p.getYRot(),oldYaw=p.yRotO;
  float head=p.yHeadRot,oldHead=p.yHeadRotO,pitch=p.getXRot(),oldPitch=p.xRotO;
  try {
   float side=ExampleModClient.renderYaw;
   p.yBodyRot=p.yBodyRotO=side;p.setYRot(side);p.yRotO=side;
   p.yHeadRot=p.yHeadRotO=side;p.setXRot(0);p.xRotO=0;
   InventoryScreen.renderEntityInInventory(gui,64,115,PLAYER_MODEL_SCALE,
     new org.joml.Quaternionf().rotateZ((float)Math.PI),new org.joml.Quaternionf(),p);
  } finally {
   p.yBodyRot=body;p.yBodyRotO=oldBody;p.setYRot(yaw);p.yRotO=oldYaw;
   p.yHeadRot=head;p.yHeadRotO=oldHead;p.setXRot(pitch);p.xRotO=oldPitch;
  }
 }
}

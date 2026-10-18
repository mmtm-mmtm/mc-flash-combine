package com.example.client;
import java.util.*;
import com.mojang.blaze3d.pipeline.TextureTarget;import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;import net.minecraft.world.level.block.RenderShape;import net.minecraft.world.level.block.state.BlockState;import org.joml.Matrix4f;
/** Native models projected onto the original street's x/depth plane. */
public final class SceneOverlay {
 public static final int WIDTH=960,HEIGHT=540;
 public record BlockView(BlockPos pos,BlockState state){}
 public static volatile List<BlockView> blocks=List.of();public static volatile byte[] image=new byte[0];public static volatile long frame;public static String error="";
 public record Snapshot(long frame,byte[] pixels,double cameraX,double cameraY){}
 public static volatile Snapshot latest=new Snapshot(0,new byte[0],0,0);
 public static final java.util.concurrent.ConcurrentHashMap<Long,Snapshot> history=new java.util.concurrent.ConcurrentHashMap<>();
 public static volatile int renderedFluids;
 private static TextureTarget target;private static long next;private static double renderCameraX,renderCameraY;
 private static Matrix4f street(){
  // world X -> screen X; world Z -> street depth; world Y -> screen height.
  return new Matrix4f(32,0,0,0, 0,-32,-16,0, 0,32,-32,0,
   (float)(renderCameraX-2600),(float)(renderCameraY-8*32+80*32),0,1);
 }
 public static void capture(Minecraft mc,long now)throws java.io.IOException {
  if(now<next||mc.level==null)return;next=now+50000000L;
  renderCameraX=ExampleModClient.cameraX;renderCameraY=ExampleModClient.cameraY;
  if(target==null){target=new TextureTarget(WIDTH,HEIGHT,true,Minecraft.ON_OSX);target.setClearColor(0,0,0,0);}
  var projection=new Matrix4f(RenderSystem.getProjectionMatrix());var sorting=RenderSystem.getVertexSorting();var model=RenderSystem.getModelViewStack();model.pushPose();model.setIdentity();RenderSystem.applyModelViewMatrix();var buffer=mc.renderBuffers().bufferSource();var dispatcher=mc.getEntityRenderDispatcher();
  try{target.clear(Minecraft.ON_OSX);target.bindWrite(true);RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0,WIDTH,HEIGHT,0,-8192,8192),VertexSorting.ORTHOGRAPHIC_Z);Lighting.setupForFlatItems();RenderSystem.enableDepthTest();
   int fluids=0;
   for(var b:blocks){var pose=new PoseStack();pose.mulPoseMatrix(street());pose.translate(b.pos.getX(),b.pos.getY(),b.pos.getZ());try{if(NativeFluids.render(mc,b.pos,b.state,street(),buffer))fluids++;mc.getBlockRenderer().renderSingleBlock(b.state,pose,buffer,15728880,OverlayTexture.NO_OVERLAY);var tile=mc.level.getBlockEntity(b.pos);if(tile!=null)mc.getBlockEntityRenderDispatcher().renderItem(tile,pose,buffer,15728880,OverlayTexture.NO_OVERLAY);}catch(Exception ex){error=ex.toString();}}
   renderedFluids=fluids;
   dispatcher.setRenderShadow(false);
   for(var e:mc.level.entitiesForRendering()){
    if(e==mc.player||e instanceof com.example.BridgeActor||e.isInvisible()||e.isRemoved()||e.distanceTo(mc.player)>40)continue;
    // Project only the world anchor onto the street. Model-local Z is depth,
    // not screen height: applying street() to vertices shears turning mobs.
    var pose=new PoseStack();
    pose.translate(e.getX()*32+renderCameraX-2600,(e.getZ()-8)*32-(e.getY()-80)*32+renderCameraY,-e.getY()*16-e.getZ()*32);
    float size=BridgeImages.ENTITY_DISPLAY_SCALE;
    pose.scale(size,-size,-size);
    try{dispatcher.render(e,0,0,0,e.getYRot(),mc.getFrameTime(),pose,buffer,15728880);}catch(Exception ex){error=ex.toString();}
   }buffer.endBatch();RenderSystem.bindTexture(target.getColorTextureId());try(var pixels=new com.mojang.blaze3d.platform.NativeImage(WIDTH,HEIGHT,false)){pixels.downloadTexture(0,false);pixels.flipY();image=pixels.asByteArray();frame++;var snap=new Snapshot(frame,image,renderCameraX,renderCameraY);history.put(frame,snap);latest=snap;history.keySet().removeIf(n->n<frame-8);}
  }finally{dispatcher.setRenderShadow(true);model.popPose();RenderSystem.applyModelViewMatrix();RenderSystem.setProjectionMatrix(projection,sorting);mc.getMainRenderTarget().bindWrite(true);}
 }
}

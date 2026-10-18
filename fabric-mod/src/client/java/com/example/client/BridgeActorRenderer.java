package com.example.client;
import com.example.BridgeActor;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
/** Collision/damage proxy only. The original Flash character owns its appearance. */
public final class BridgeActorRenderer extends EntityRenderer<BridgeActor> {
 public BridgeActorRenderer(EntityRendererProvider.Context c){super(c);}
 public ResourceLocation getTextureLocation(BridgeActor e){return new ResourceLocation("minecraft","textures/entity/player/wide/steve.png");}
}

package com.example;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;

public final class BridgeEntities implements ModInitializer {
    public static final EntityType<BridgeActor> PARTNER=Registry.register(BuiltInRegistries.ENTITY_TYPE,
        new ResourceLocation("mcflash","partner"),EntityType.Builder.<BridgeActor>of(BridgeActor::new,MobCategory.MISC)
            .sized(.6F,1.6F).clientTrackingRange(8).noSave().build("mcflash:partner"));
    @Override public void onInitialize(){BridgeContainment.initialize();BridgeUseRay.initialize();net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register(ReplayEpoch::loaded);FabricDefaultAttributeRegistry.register(PARTNER,LivingEntity.createLivingAttributes());}
}

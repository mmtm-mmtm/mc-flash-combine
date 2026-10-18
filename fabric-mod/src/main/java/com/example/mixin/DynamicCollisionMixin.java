package com.example.mixin;

import com.example.DynamicPlatforms;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Iterator;
import java.util.NoSuchElementException;

@Mixin(CollisionGetter.class)
public interface DynamicCollisionMixin {
    @Inject(method="getBlockCollisions",at=@At("RETURN"),cancellable=true)
    private void mcflashDynamicShapes(Entity entity,AABB query,CallbackInfoReturnable<Iterable<VoxelShape>> cir){
        var extra=new java.util.ArrayList<>(DynamicPlatforms.colliders(query));extra.addAll(com.example.StreetBounds.colliders(entity,query));
        if(extra.isEmpty())return;
        Iterable<VoxelShape> original=cir.getReturnValue();
        cir.setReturnValue(()->new Iterator<>(){
            final Iterator<VoxelShape> a=original.iterator(),b=extra.iterator();
            public boolean hasNext(){return a.hasNext()||b.hasNext();}
            public VoxelShape next(){if(a.hasNext())return a.next();if(b.hasNext())return b.next();throw new NoSuchElementException();}
        });
    }
}

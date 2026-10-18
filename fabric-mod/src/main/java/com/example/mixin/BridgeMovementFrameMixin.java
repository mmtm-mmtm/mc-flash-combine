package com.example.mixin;
import com.example.BridgeUseRay;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Entity.class)
public class BridgeMovementFrameMixin {
    // Street input uses native forward movement while shields/bows keep independent aim.
    @ModifyArg(method="moveRelative",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"),index=2)
    private float mcflashWorldMovement(float yaw){return ((Entity)(Object)this).getUUID().equals(BridgeUseRay.controlledPlayer)?BridgeUseRay.movementYaw:yaw;}
}

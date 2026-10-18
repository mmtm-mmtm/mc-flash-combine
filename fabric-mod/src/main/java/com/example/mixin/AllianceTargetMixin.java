package com.example.mixin;
import com.example.BridgeAllies;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public class AllianceTargetMixin {
    @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
    private void allies(LivingEntity target,CallbackInfo ci){if(BridgeAllies.friendly((Mob)(Object)this,target))ci.cancel();}
}

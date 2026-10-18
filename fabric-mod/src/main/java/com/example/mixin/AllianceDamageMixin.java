package com.example.mixin;
import com.example.BridgeAllies;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public class AllianceDamageMixin {
    @Inject(method="hurt",at=@At("HEAD"),cancellable=true)
    private void allies(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){if(BridgeAllies.friendly((LivingEntity)(Object)this,source.getEntity()))ci.setReturnValue(false);}
}

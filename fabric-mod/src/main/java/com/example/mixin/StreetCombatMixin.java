package com.example.mixin;
import com.example.StreetCombat;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public class StreetCombatMixin {
    @Inject(method="serverAiStep",at=@At("HEAD"))
    private void flashEnemies(CallbackInfo ci){StreetCombat.target((Mob)(Object)this);}
}

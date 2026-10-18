package com.example.mixin;
import com.example.ReplayEpoch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerLevel.class)
public class ReplaySpawnMixin {
    @Inject(method="addEntity",at=@At("HEAD"))
    private void mcflashFresh(Entity entity,CallbackInfoReturnable<Boolean> ci){ReplayEpoch.fresh((ServerLevel)(Object)this,entity);}
}

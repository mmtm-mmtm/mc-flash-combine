package com.example.mixin;
import java.util.Optional;
import com.example.BridgeContainment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin({ExplosionDamageCalculator.class,EntityBasedExplosionDamageCalculator.class})
public class ContainmentExplosionMixin {
    @Inject(method="getBlockExplosionResistance",at=@At("HEAD"),cancellable=true)
    private void mcflashTransparent(Explosion blast,BlockGetter world,BlockPos pos,BlockState state,FluidState fluid,CallbackInfoReturnable<Optional<Float>> ci){if(state.is(BridgeContainment.PLANE))ci.setReturnValue(Optional.of(0f));}
    @Inject(method="shouldBlockExplode",at=@At("HEAD"),cancellable=true)
    private void mcflashKeepPlane(Explosion blast,BlockGetter world,BlockPos pos,BlockState state,float power,CallbackInfoReturnable<Boolean> ci){if(state.is(BridgeContainment.PLANE))ci.setReturnValue(false);}
}

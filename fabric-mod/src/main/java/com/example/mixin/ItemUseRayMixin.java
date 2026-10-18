package com.example.mixin;
import com.example.BridgeUseRay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Item.class)
public class ItemUseRayMixin {
    @Inject(method="getPlayerPOVHitResult",at=@At("HEAD"),cancellable=true)
    private static void mcflashRay(Level level,Player player,ClipContext.Fluid fluid,CallbackInfoReturnable<BlockHitResult> ci){var hit=BridgeUseRay.current(fluid);if(hit!=null)ci.setReturnValue(hit);}
}

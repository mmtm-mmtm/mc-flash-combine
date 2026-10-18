package com.example.mixin;
import com.example.BridgeUseRay;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayerGameMode.class)
public class ServerUseRayMixin {
    @Inject(method="useItem",at=@At("HEAD"))
    private void mcflashBegin(ServerPlayer player,Level level,ItemStack item,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){BridgeUseRay.enter(player);}
    @Inject(method="useItem",at=@At("RETURN"))
    private void mcflashEnd(ServerPlayer player,Level level,ItemStack item,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){BridgeUseRay.exit();}
}

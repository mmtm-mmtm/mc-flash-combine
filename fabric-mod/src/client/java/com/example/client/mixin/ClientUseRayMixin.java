package com.example.client.mixin;
import com.example.BridgeUseRay;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MultiPlayerGameMode.class)
public class ClientUseRayMixin {
    @Inject(method="useItem",at=@At("HEAD"))
    private void mcflashBegin(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){BridgeUseRay.enter(player);}
    @Inject(method="useItem",at=@At("RETURN"))
    private void mcflashEnd(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){BridgeUseRay.exit();}
}

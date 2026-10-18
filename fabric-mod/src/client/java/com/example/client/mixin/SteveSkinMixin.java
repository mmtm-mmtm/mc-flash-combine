package com.example.client.mixin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractClientPlayer.class)
public class SteveSkinMixin {
 @Inject(method="getSkinTextureLocation",at=@At("HEAD"),cancellable=true)
 private void mcflashSteve(CallbackInfoReturnable<ResourceLocation> ci){ci.setReturnValue(DefaultPlayerSkin.getDefaultSkin());}
 @Inject(method="getModelName",at=@At("HEAD"),cancellable=true)
 private void mcflashWide(CallbackInfoReturnable<String> ci){ci.setReturnValue("default");}
}

package com.example.client.mixin;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.example.client.BridgeImages;
@Mixin(GameRenderer.class)
public class BridgeRenderMixin {
 @Inject(method="render",at=@At("TAIL"))
 private void mcflashCapture(float tickDelta,long startTime,boolean tick,CallbackInfo ci){BridgeImages.capture();}
}

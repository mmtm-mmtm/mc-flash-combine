package com.example.client.mixin;
import com.example.client.ExampleModClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public class BridgeMiningInputMixin {
    // The desktop mouse is released while the browser owns a mining gesture. Vanilla must not abort it each tick.
    @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
    private void mcflashRemoteMining(boolean pressed,CallbackInfo ci){if(ExampleModClient.remoteMining())ci.cancel();}
}

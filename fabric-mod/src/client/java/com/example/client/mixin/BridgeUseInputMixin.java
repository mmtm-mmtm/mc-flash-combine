package com.example.client.mixin;
import com.example.client.RemoteUse;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public class BridgeUseInputMixin {
    @Shadow private int rightClickDelay;
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void mcflashUse(CallbackInfo ci){if(RemoteUse.held){rightClickDelay=4;RemoteUse.repeat(Minecraft.getInstance());ci.cancel();}}
}

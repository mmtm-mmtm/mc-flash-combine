package com.example.client.mixin;
import com.example.client.RemoteInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MouseHandler.class)
public class InventoryPointerMixin {
    @Inject(method="xpos",at=@At("HEAD"),cancellable=true)
    private void x(CallbackInfoReturnable<Double> c){if(RemoteInventory.active())c.setReturnValue(RemoteInventory.x*Minecraft.getInstance().getWindow().getScreenWidth());}
    @Inject(method="ypos",at=@At("HEAD"),cancellable=true)
    private void y(CallbackInfoReturnable<Double> c){if(RemoteInventory.active())c.setReturnValue(RemoteInventory.y*Minecraft.getInstance().getWindow().getScreenHeight());}
}

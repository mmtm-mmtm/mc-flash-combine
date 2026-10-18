package com.example.client.mixin;
import com.example.client.RemoteInventory;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(InputConstants.class)
public class InventoryKeysMixin {
    @Inject(method="isKeyDown",at=@At("HEAD"),cancellable=true)
    private static void keys(long window,int key,CallbackInfoReturnable<Boolean> c){
        if(!RemoteInventory.active())return;
        if(key==340||key==344)c.setReturnValue(RemoteInventory.shift);
        else if(key==341||key==345)c.setReturnValue(RemoteInventory.control);
        else if(key==342||key==346)c.setReturnValue(RemoteInventory.alt);
    }
}

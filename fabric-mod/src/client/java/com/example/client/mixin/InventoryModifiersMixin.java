package com.example.client.mixin;
import com.example.client.RemoteInventory;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Screen.class)
public class InventoryModifiersMixin {
    @Inject(method="hasShiftDown",at=@At("HEAD"),cancellable=true)
    private static void shift(CallbackInfoReturnable<Boolean> c){if(RemoteInventory.active())c.setReturnValue(RemoteInventory.shift);}
    @Inject(method="hasControlDown",at=@At("HEAD"),cancellable=true)
    private static void control(CallbackInfoReturnable<Boolean> c){if(RemoteInventory.active())c.setReturnValue(RemoteInventory.control);}
    @Inject(method="hasAltDown",at=@At("HEAD"),cancellable=true)
    private static void alt(CallbackInfoReturnable<Boolean> c){if(RemoteInventory.active())c.setReturnValue(RemoteInventory.alt);}
}

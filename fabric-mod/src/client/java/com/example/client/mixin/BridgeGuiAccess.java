package com.example.client.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface BridgeGuiAccess {
    @Invoker("renderHotbar") void mcflashHotbar(float partialTick,GuiGraphics graphics);
    @Invoker("renderPlayerHealth") void mcflashHealth(GuiGraphics graphics);
}

package com.example.client.mixin;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MultiPlayerGameMode.class)
public interface BridgeMiningAccess {
    @Accessor("destroyProgress") float mcflashProgress();
    @Accessor("destroyBlockPos") BlockPos mcflashTarget();
    @Accessor("isDestroying") boolean mcflashDestroying();
}

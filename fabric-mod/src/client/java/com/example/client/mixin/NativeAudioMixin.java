package com.example.client.mixin;

import com.example.client.NativeAudio;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class NativeAudioMixin {
    @Inject(method="stopAll",at=@At("HEAD"))
    private void mcflashStopAll(CallbackInfo ci){NativeAudio.clear();}
    @Inject(method="stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",at=@At("HEAD"))
    private void mcflashStop(SoundInstance sound,CallbackInfo ci){NativeAudio.stopped(sound);}
    @Inject(method="stop(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/sounds/SoundSource;)V",at=@At("HEAD"))
    private void mcflashStopCategory(ResourceLocation name,SoundSource source,CallbackInfo ci){NativeAudio.stopped(name,source);}
}

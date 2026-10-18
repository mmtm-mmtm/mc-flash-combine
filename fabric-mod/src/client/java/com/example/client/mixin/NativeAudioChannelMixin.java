package com.example.client.mixin;

import com.example.client.NativeAudio;
import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keep native sound simulation alive, but send output to the browser once it owns audio. */
@Mixin(Channel.class)
public class NativeAudioChannelMixin {
    @ModifyVariable(method="setVolume",at=@At("HEAD"),argsOnly=true)
    private float mcflashBrowserOutput(float value){return NativeAudio.nativeVolume(value);}
}

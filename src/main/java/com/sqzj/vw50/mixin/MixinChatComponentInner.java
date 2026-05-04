package com.sqzj.vw50.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.sqzj.vw50.utils.MixinHandler;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1")
public class MixinChatComponentInner {

    @Shadow @Final ChatComponent this$0;

    @Inject(method = "accept", at = @At(value = "TAIL"))
    private void accept(GuiMessage.Line line, int lineIndex, float alpha, CallbackInfo ci, @Local(name = "textTop") int textTop) {
        MixinHandler.extractRenderState$accept_Hook(this.this$0, line, textTop);
    }

}
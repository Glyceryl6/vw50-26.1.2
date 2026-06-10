package com.sqzj.vw50.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.sqzj.vw50.misc.hook.HookChatComponent;
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
    @Shadow @Final float val$textOpacity;
    @Shadow @Final int val$chatBottom;

    @Inject(method = "accept", at = @At(value = "HEAD"), cancellable = true)
    private void accept(GuiMessage.Line line, int lineIndex, float alpha, CallbackInfo ci) {
        HookChatComponent.extractRenderState$accept_InjectHead(this.this$0, line, lineIndex, alpha, this.val$chatBottom, this.val$textOpacity, ci);
    }

    @Inject(method = "accept", at = @At(value = "TAIL"))
    private void accept(GuiMessage.Line line, int lineIndex, float alpha, CallbackInfo ci, @Local(name = "textTop") int textTop) {
        HookChatComponent.extractRenderState$accept_InjectTail(this.this$0, line, textTop);
    }

}
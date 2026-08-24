package com.sqzj.vw50.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.misc.hook.HookChatComponent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @Shadow
    @Final
    public List<GuiMessage> allMessages;

    @Inject(method = "lambda$extractRenderState$1", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private static void extractRenderState(
        int chatBottom, int entryHeight, ChatComponent.ChatGraphicsAccess graphics, int maxWidth,
        float backgroundOpacity, GuiMessage.Line var5x, int lineIndex, float alphax, CallbackInfo ci) {
        GuiMessageExtraData extraData = GuiMessageAttachment.get(var5x.parent());
        if (extraData != null && extraData.isRedEnvelope) {
            // Red-envelope messages are rendered completely by VW50.  Cancel the vanilla
            // per-line background, otherwise the reserved blank lines produce black bars
            // and the card appears to block adjacent normal chat messages.
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;forEachLine(Lnet/minecraft/client/gui/components/ChatComponent$AlphaCalculator;Lnet/minecraft/client/gui/components/ChatComponent$LineConsumer;)I", shift = At.Shift.AFTER))
    private void extractRenderState(ChatComponent.ChatGraphicsAccess graphics, int screenHeight, int ticks, ChatComponent.DisplayMode displayMode, CallbackInfo ci, @Local(name = "chatBottom") int chatBottom, @Local(name = "textOpacity") float textOpacity) {
        // Red-envelope cards are drawn from the actual GuiMessage.Line callback so they
        // scroll, fade and clip exactly like the vanilla chat entry that owns them.
        if (graphics instanceof ChatComponent.DrawingFocusedGraphicsAccess access) {
            HookChatComponent.renderClaimListPanel(access, chatBottom, textOpacity);
        }
    }

    @Inject(method = "clearMessages", at = @At(value = "HEAD"))
    public void clearMessages(boolean history, CallbackInfo ci) {
        GuiMessageAttachment.clear();
    }

    @Inject(method = "addMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;logChatMessage(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V", shift = At.Shift.AFTER))
    private void addMessage(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci, @Local(name = "message") GuiMessage message) {
        if (source == GuiMessageSource.PLAYER) {
            HookChatComponent.addMessage_Inject(this.allMessages, message);
        }
    }

}
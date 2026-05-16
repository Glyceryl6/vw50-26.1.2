package com.sqzj.vw50.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.sqzj.vw50.api.IChatComponentExtensions;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.hook.HookChatComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent implements IChatComponentExtensions {

    @Shadow @Final public Minecraft minecraft;
    @Shadow @Final public List<GuiMessage> allMessages;
    @Unique private Component VW50$markedMessage = Component.empty();
    @Unique private boolean VW50$mouseOverRepeatButton;

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At(value = "HEAD"))
    private void extractRenderState(ChatComponent.ChatGraphicsAccess graphics, int screenHeight, int ticks, ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
        HookChatComponent.chatGraphicsAccess = graphics;
    }

    @Inject(method = "lambda$extractRenderState$1", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private static void extractRenderState(
            int chatBottom, int entryHeight, ChatComponent.ChatGraphicsAccess graphics, int maxWidth,
            float backgroundOpacity, GuiMessage.Line var5x, int lineIndex, float alphax, CallbackInfo ci) {
        HookChatComponent.extractRenderState$lambda$1_Inject(chatBottom, graphics, maxWidth, backgroundOpacity, var5x, lineIndex, alphax, ci);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;forEachLine(Lnet/minecraft/client/gui/components/ChatComponent$AlphaCalculator;Lnet/minecraft/client/gui/components/ChatComponent$LineConsumer;)I", shift = At.Shift.AFTER))
    private void extractRenderState(ChatComponent.ChatGraphicsAccess graphics, int screenHeight, int ticks, ChatComponent.DisplayMode displayMode, CallbackInfo ci,
                                    @Local(name = "chatBottom") int chatBottom, @Local(name = "textOpacity") float textOpacity, @Local(name = "chatLineSpacing") double chatLineSpacing) {
        HookChatComponent.extractRenderState$forEachLine_InjectAfter(this.allMessages, ticks, textOpacity, chatBottom, chatLineSpacing);
    }

    @Inject(method = "clearMessages", at = @At(value = "HEAD"))
    public void clearMessages(boolean history, CallbackInfo ci) {
        GuiMessageAttachment.clear();
    }

    @Inject(method = "addMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;logChatMessage(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V", shift = At.Shift.AFTER))
    private void addMessage(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci, @Local(name = "message") GuiMessage message) {
        if (source == GuiMessageSource.PLAYER) {
            if (HookChatComponent.addMessage_Inject(this.allMessages, message)) {
                this.VW50$markedMessage = message.content();
            } else {
                this.VW50$markedMessage = Component.empty();
            }
        }
    }

    @Override
    public boolean VW50$isMouseOverRepeatButton() {
        return this.VW50$mouseOverRepeatButton;
    }

    @Override
    public void VW50$setMouseOverRepeatButton(boolean value) {
        this.VW50$mouseOverRepeatButton = value;
    }

    @Override
    public Component VW50$getMarkedMessage() {
        return this.VW50$markedMessage;
    }

    @Override
    public void VW50$setMarkedMessage(Component component) {
        this.VW50$markedMessage = component;
    }

}
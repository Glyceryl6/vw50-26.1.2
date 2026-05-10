package com.sqzj.vw50.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.sqzj.vw50.api.IChatComponentExtensions;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.hook.HookChatComponent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChatComponent.class)
public class MixinChatComponent implements IChatComponentExtensions {

    @Shadow @Final public List<GuiMessage> allMessages;
    @Unique private boolean VW50$mouseOverRepeatButton;
    @Unique private Component VW50$markedMessage = Component.empty();

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At(value = "HEAD"))
    private void extractRenderState(ChatComponent.ChatGraphicsAccess graphics, int screenHeight, int ticks, ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
        HookChatComponent.chatGraphicsAccess = graphics;
    }

    @Inject(method = "clearMessages", at = @At(value = "HEAD"))
    public void clearMessages(boolean history, CallbackInfo ci) {
        GuiMessageAttachment.clear();
    }

    @Inject(method = "addMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;logChatMessage(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V", shift = At.Shift.AFTER))
    private void addMessage(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci, @Local(name = "message") GuiMessage message) {
        if (source == GuiMessageSource.PLAYER) {
            if (HookChatComponent.addMessage_Hook(this.allMessages, message)) {
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
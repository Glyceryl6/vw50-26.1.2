package com.sqzj.vw50.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.sqzj.vw50.api.IChatComponentExtensions;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.misc.hook.HookChatComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.*;
import org.jspecify.annotations.Nullable;
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
    @Unique private boolean VW50$mouseOverRedEnvelope;
    @Unique @Nullable
    private UUID VW50$mouseOverRedEnvelopeId;

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At(value = "HEAD"))
    private void extractRenderState(ChatComponent.ChatGraphicsAccess graphics, int screenHeight, int ticks, ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
        HookChatComponent.chatGraphicsAccess = graphics;
        this.VW50$mouseOverRepeatButton = false;
        this.VW50$mouseOverRedEnvelope = false;
        this.VW50$mouseOverRedEnvelopeId = null;
    }

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

    @Inject(method = "clearMessages", at = @At(value = "HEAD"))
    public void clearMessages(boolean history, CallbackInfo ci) {
        GuiMessageAttachment.clear();
    }

    @Inject(method = "addMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;logChatMessage(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V", shift = At.Shift.AFTER))
    private void addMessage(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci, @Local(name = "message") GuiMessage message) {
        if (source == GuiMessageSource.PLAYER) {
            if (HookChatComponent.addMessage_Inject(this.allMessages, message)) {
                GuiMessageExtraData extraData = GuiMessageAttachment.get(message);
                String repeatText = extraData == null ? HookChatComponent.extractRepeatBody(message.content()) : extraData.repeatText;
                this.VW50$markedMessage = Component.literal(repeatText);
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

    @Override
    public boolean VW50$isMouseOverRedEnvelope() {
        return this.VW50$mouseOverRedEnvelope;
    }

    @Override
    public void VW50$setMouseOverRedEnvelope(boolean value) {
        this.VW50$mouseOverRedEnvelope = value;
    }

    @Nullable
    @Override
    public UUID VW50$getMouseOverRedEnvelopeId() {
        return this.VW50$mouseOverRedEnvelopeId;
    }

    @Override
    public void VW50$setMouseOverRedEnvelopeId(UUID id) {
        this.VW50$mouseOverRedEnvelopeId = id;
    }

}
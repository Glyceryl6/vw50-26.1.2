package com.sqzj.vw50.mixin;

import com.google.common.collect.Lists;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Predicate;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent implements IChatComponentExtensions {

    @Shadow @Final public Minecraft minecraft;
    @Shadow @Final public List<GuiMessage> allMessages;
    @Shadow @Final public List<GuiMessage.Line> trimmedMessages;
    @Shadow public abstract void scrollChat(int dir);
    @Shadow public abstract void refreshTrimmedMessages();
    @Shadow public abstract boolean isChatFocused();
    @Shadow public abstract double getScale();
    @Shadow public abstract int getWidth();
    @Shadow public abstract int getHeight();
    @Shadow public abstract int getLineHeight();
    @Shadow public abstract int forEachLine(
            ChatComponent.AlphaCalculator alphaCalculator,
            ChatComponent.LineConsumer lineConsumer);
    @Shadow public int chatScrollbarPos;
    @Shadow public boolean newMessageSinceScroll;
    @Shadow public Predicate<GuiMessage> visibleMessageFilter;
    @Unique private Component VW50$markedMessage = Component.empty();
    @Unique private boolean VW50$mouseOverRepeatButton;
    @Unique private final Map<GuiMessage, Integer> VW50$messageCustomWidth = new WeakHashMap<>();
    @Unique private final Map<GuiMessage, Integer> VW50$messageLineCount = new WeakHashMap<>();
    @Unique private final Map<GuiMessage, Integer> VW50$messageBottomMargin = new WeakHashMap<>();
    @Unique private final Map<GuiMessage, Integer> VW50$messageTopMargin = new WeakHashMap<>();
    @Unique private final List<Integer> VW50$lineAccumulatedOffsets = new ArrayList<>();

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At(value = "HEAD"))
    private void extractRenderState(ChatComponent.ChatGraphicsAccess graphics, int screenHeight, int ticks, ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
        HookChatComponent.chatGraphicsAccess = graphics;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;forEachLine(Lnet/minecraft/client/gui/components/ChatComponent$AlphaCalculator;Lnet/minecraft/client/gui/components/ChatComponent$LineConsumer;)I", shift = At.Shift.AFTER))
    private void extractRenderState(ChatComponent.ChatGraphicsAccess graphics, int screenHeight, int ticks, ChatComponent.DisplayMode displayMode, CallbackInfo ci,
                                    @Local(name = "chatBottom") int chatBottom, @Local(name = "textOpacity") float textOpacity, @Local(name = "chatLineSpacing") double chatLineSpacing) {
        HookChatComponent.extractRenderState$forEachLine_InjectAfter(this.allMessages, ticks, textOpacity, chatBottom, chatLineSpacing);
    }

    @ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V", at = @At(value = "STORE"), name = "count")
    private int extractRenderState(int count, @Local(name = "graphics", argsOnly = true) ChatComponent.ChatGraphicsAccess graphics, @Local(name = "ticks", argsOnly = true) int ticks,
                                   @Local(name = "isForeground") boolean isForeground, @Local(name = "chatBottom") int chatBottom,
                                   @Local(name = "backgroundOpacity") float backgroundOpacity, @Local(name = "entryHeight") int entryHeight) {
        ChatComponent.AlphaCalculator alphaCalculator = isForeground
                ? ChatComponent.AlphaCalculator.FULLY_VISIBLE
                : ChatComponent.AlphaCalculator.timeBased(ticks);
        float scale = (float) this.getScale();
        int maxWidth = Mth.ceil(this.getWidth() / scale);
        graphics.updatePose(pose -> {
            pose.scale(scale, scale);
            pose.translate(4.0F, 0.0F);
        });

        return this.forEachLine(alphaCalculator, (_, lineIndex, alpha) -> {
            int offset = this.VW50$getLineOffset(lineIndex);
            int defaultBottom = chatBottom - lineIndex * entryHeight;
            int entryBottom = defaultBottom - offset;
            int entryTop = entryBottom - entryHeight;
            int color = ARGB.black(alpha * backgroundOpacity);
            graphics.fill(-4, entryTop, maxWidth + 4 + 4, entryBottom, color);
        });
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

    @Redirect(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;floor(D)I"))
    public int addMessageToDisplayQueue(double v, @Local(name = "message", argsOnly = true) GuiMessage message) {
        return this.VW50$getEffectiveWidthForMessage(message);
    }

    @Inject(method = "refreshTrimmedMessages", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", shift = At.Shift.AFTER), cancellable = true)
    private void refreshTrimmedMessages(CallbackInfo ci) {
        this.VW50$messageLineCount.clear();
        this.VW50$lineAccumulatedOffsets.clear();
        for (GuiMessage message : Lists.reverse(this.allMessages)) {
            if (this.visibleMessageFilter.test(message)) {
                int maxWidth = this.VW50$getEffectiveWidthForMessage(message);
                List<FormattedCharSequence> lines = message.splitLines(this.minecraft.font, maxWidth);
                this.VW50$messageLineCount.put(message, lines.size());
                boolean chatting = this.isChatFocused();
                for (int i = 0; i < lines.size(); i++) {
                    FormattedCharSequence line = lines.get(i);
                    if (chatting && this.chatScrollbarPos > 0) {
                        this.newMessageSinceScroll = true;
                        this.scrollChat(1);
                    }

                    boolean endOfEntry = i == lines.size() - 1;
                    this.trimmedMessages.addFirst(new GuiMessage.Line(message, line, endOfEntry));
                }
            }
        }

        this.VW50$recalculateLineOffsets();
        ci.cancel();
    }

    @Inject(method = "getLinesPerPage", at = @At(value = "RETURN"), cancellable = true)
    public void getLinesPerPage(CallbackInfoReturnable<Integer> cir) {
        if (this.VW50$lineAccumulatedOffsets.isEmpty()) {
            cir.setReturnValue(this.getHeight() / this.getLineHeight());
        }

        int lastOffset = this.VW50$lineAccumulatedOffsets.isEmpty() ? 0 : this.VW50$lineAccumulatedOffsets.getLast();
        int totalHeight = this.trimmedMessages.size() * this.getLineHeight() + lastOffset;
        int linesPerPage = Math.max(1, this.getHeight() * this.trimmedMessages.size() / Math.max(1, totalHeight));
        cir.setReturnValue(Math.min(linesPerPage, this.trimmedMessages.size()));
    }

    @Unique
    private int VW50$getEffectiveWidthForMessage(GuiMessage message) {
        Integer customWidth = this.VW50$messageCustomWidth.get(message);
        if (customWidth != null && customWidth > 0) return customWidth;
        return Mth.floor(this.getWidth() / this.getScale());
    }

    @Unique
    private void VW50$recalculateLineOffsets() {
        this.VW50$lineAccumulatedOffsets.clear();
        int accumulatedOffset = 0;
        int lineIndex = 0;
        GuiMessage currentMessage = null;
        GuiMessage prevMessage = null;
        int currentMessageLineStartIndex = 0;
        for (GuiMessage.Line line : this.trimmedMessages) {
            GuiMessage message = line.parent();
            if (message != currentMessage) {
                if (prevMessage != null) {
                    int bottomMargin = VW50$getMessageBottomMargin(prevMessage);
                    int topMargin = VW50$getMessageTopMargin(currentMessage);
                    int margin = Math.max(bottomMargin, topMargin);
                    accumulatedOffset += margin;
                }

                prevMessage = currentMessage;
                currentMessage = message;
                currentMessageLineStartIndex = lineIndex;
            }

            int internalLineOffset = (lineIndex - currentMessageLineStartIndex) * this.getLineHeight();
            this.VW50$lineAccumulatedOffsets.add(accumulatedOffset + internalLineOffset);
            lineIndex++;
        }
    }

    @Unique
    public int VW50$getMessageLineCount(GuiMessage message) {
        return this.VW50$messageLineCount.getOrDefault(message, 1);
    }

    @Unique
    public int VW50$getMessageBottomMargin(GuiMessage message) {
        return this.VW50$messageBottomMargin.getOrDefault(message, 0);
    }

    @Unique
    public int VW50$getMessageTopMargin(GuiMessage message) {
        return this.VW50$messageTopMargin.getOrDefault(message, 0);
    }

    @Unique
    public void VW50$setMessageMargins(GuiMessage message, int topMargin, int bottomMargin) {
        if (topMargin <= 0) {
            this.VW50$messageTopMargin.remove(message);
        } else {
            this.VW50$messageTopMargin.put(message, topMargin);
        }

        if (bottomMargin <= 0) {
            this.VW50$messageBottomMargin.remove(message);
        } else {
            this.VW50$messageBottomMargin.put(message, bottomMargin);
        }

        this.refreshTrimmedMessages();
    }

    @Override
    public void VW50$setMessageHeight(GuiMessage message, int totalHeight) {
        if (totalHeight <= 0) {
            this.VW50$messageBottomMargin.remove(message);
            this.VW50$messageTopMargin.remove(message);
        } else {
            int lineCount = this.VW50$getMessageLineCount(message);
            int defaultHeight = lineCount * 9;
            int extraHeight = totalHeight - defaultHeight;
            int halfExtra = extraHeight / 2;
            int remainder = extraHeight % 2;
            this.VW50$messageTopMargin.put(message, halfExtra);
            this.VW50$messageBottomMargin.put(message, halfExtra + remainder);
        }

        this.refreshTrimmedMessages();
    }

    @Override
    public void VW50$setMessageWidth(GuiMessage message, int width) {
        if (width <= 0) {
            this.VW50$messageCustomWidth.remove(message);
        } else {
            this.VW50$messageCustomWidth.put(message, width);
        }

        this.refreshTrimmedMessages();
    }

    @Override
    public int VW50$getLineOffset(int lineIndex) {
        if (lineIndex >= 0 && lineIndex < this.VW50$lineAccumulatedOffsets.size()) {
            return this.VW50$lineAccumulatedOffsets.get(lineIndex);
        }

        return 0;
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
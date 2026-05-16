package com.sqzj.vw50.misc.hook;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.sqzj.vw50.VW50;
import com.sqzj.vw50.api.IChatComponentExtensions;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

public class HookChatComponent {

    public static final WidgetSprites PLUS_ONE_SPRITES = new WidgetSprites(VW50.prefix("plus_one_default"), VW50.prefix("plus_one"));
    public static final Identifier RED_ENV_COMPONENT_LOCATION = VW50.prefix("textures/gui/red_env_chat_component/default.png");
    public static ChatComponent.ChatGraphicsAccess chatGraphicsAccess = null;

    public static void extractRenderState$lambda$1_Inject(
            int chatBottom, ChatComponent.ChatGraphicsAccess graphics, int maxWidth, float backgroundOpacity,
            GuiMessage.Line var5x, int lineIndex, float alphax, CallbackInfo ci) {
        GuiMessageExtraData extraData = GuiMessageAttachment.get(var5x.parent());
        boolean flag = extraData != null && extraData.isRedEnvelope;
        if (graphics instanceof ChatComponent.DrawingFocusedGraphicsAccess && flag) {
            double chatLineSpacing = Minecraft.getInstance().options.chatLineSpacing().get();
            int entryHeight = Mth.floor(9 * (chatLineSpacing + 1.0F));
            int entryBottom = chatBottom - lineIndex * entryHeight;
            int entryTop = entryBottom - entryHeight;
            int color = ARGB.black(alphax * backgroundOpacity);
            graphics.fill(-4, entryTop, maxWidth + 4 + 4, entryBottom, color);
            ci.cancel();
        }
    }

    public static void extractRenderState$forEachLine_InjectAfter(List<GuiMessage> allMessages, int ticks, float textOpacity, int chatBottom, double chatLineSpacing) {
        if (chatGraphicsAccess instanceof ChatComponent.DrawingBackgroundGraphicsAccess access) {
            final int entryBottomToMessageY = (int)Math.round(8.0 * (chatLineSpacing + 1.0) - 4.0 * chatLineSpacing);
            final int entryHeight = (int)((9 + 15) * (chatLineSpacing + 1.0F));
            final int width = 192 / 2, height = 72 / 2;
            for (int i = 0; i < allMessages.size(); i++) {
                int entryBottom = chatBottom - i * entryHeight;
                int textTop = entryBottom - entryBottomToMessageY;
                GuiMessage message = allMessages.get(i);
                GuiMessageExtraData extraData = GuiMessageAttachment.get(message);
                if (extraData != null && extraData.isRedEnvelope) {
                    float alpha = AlphaCalculator.timeBased(ticks).calculate(message);
                    ActiveTextCollector.Parameters parameters = access.parameters.withOpacity(alpha * textOpacity);
                    access.graphics.blit(RenderPipelines.GUI_TEXTURED, RED_ENV_COMPONENT_LOCATION, 4, textTop - 15,
                            0.0F, 0.0F, width, height, width, height, ARGB.white(parameters.opacity()));
                    access.textRenderer.accept(TextAlignment.LEFT, 0, textTop, parameters, message.content());
                }
            }
        }
    }

    public static void extractRenderState$accept_InjectHead(
            ChatComponent chat, GuiMessage.Line line, int lineIndex,
            float alpha, int chatBottom, float textOpacity, CallbackInfo ci) {
        if (chatGraphicsAccess instanceof ChatComponent.DrawingFocusedGraphicsAccess access) {
            GuiMessageExtraData extraData = GuiMessageAttachment.get(line.parent());
            if (extraData != null && extraData.isRedEnvelope && chat instanceof IChatComponentExtensions) {
                final int width = 192 / 2, height = 72 / 2, messageHeight = 9;
                double chatLineSpacing = Minecraft.getInstance().options.chatLineSpacing().get();
                if (lineIndex + 1 < chat.trimmedMessages.size()) {
                    GuiMessage.Line nextLine = chat.trimmedMessages.get(lineIndex + 1);
                    GuiMessageExtraData nextExtraData = GuiMessageAttachment.get(nextLine.parent());
                    if (nextExtraData == null || !nextExtraData.isRedEnvelope) chatLineSpacing = 15.0D;
                }

                int entryHeight = (int) (messageHeight * (chatLineSpacing + 1.0F));
                int entryBottomToMessageY = (int) Math.round(8.0F * (chatLineSpacing + 1.0F) - 4.0F * chatLineSpacing);
                int textTop = chatBottom - lineIndex * entryHeight - entryBottomToMessageY;
                ActiveTextCollector.Parameters parameters = access.parameters.withOpacity(alpha * textOpacity);
                access.graphics.blit(RenderPipelines.GUI_TEXTURED, RED_ENV_COMPONENT_LOCATION,
                        4, textTop - 15, 0.0F, 0.0F, width, height, width, height);
                access.textRenderer.accept(TextAlignment.LEFT, 0, textTop, parameters, line.content());
                ci.cancel();
            }
        }
    }

    public static void extractRenderState$accept_InjectTail(ChatComponent chat, GuiMessage.Line line, int textTop) {
        if (chatGraphicsAccess instanceof ChatComponent.DrawingFocusedGraphicsAccess access) {
            GuiMessageExtraData extraData = GuiMessageAttachment.get(line.parent());
            if (extraData != null && chat instanceof IChatComponentExtensions extensions) {
                Minecraft minecraft = Minecraft.getInstance();
                GuiGraphicsExtractor graphics = access.graphics;
                int iconLeft = line.getTagIconLeft(minecraft.font);
                if (extraData.canPlusOne && line.endOfEntry()) {
                    final int messageHeight = 9, iconSize = 9;
                    int right = iconLeft + iconSize;
                    int textBottom = textTop + messageHeight;
                    boolean isMouseOver = access.isMouseOver(iconLeft, textTop, right, textBottom);
                    Identifier location = PLUS_ONE_SPRITES.get(true, isMouseOver);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, location, iconLeft, textTop, iconSize, iconSize);
                    graphics.requestCursor(isMouseOver ? CursorTypes.POINTING_HAND : CursorType.DEFAULT);
                    extensions.VW50$setMouseOverRepeatButton(isMouseOver);
                }
            }
        }
    }

    public static boolean addMessage_Inject(List<GuiMessage> allMessages, GuiMessage message) {
        Predicate<GuiMessage> predicate = g -> g.source() == GuiMessageSource.PLAYER;
        List<GuiMessage> messageList = allMessages.stream().filter(predicate).toList();
        if (!messageList.isEmpty()) {
            GuiMessage messageFirst = messageList.getFirst();
            String firstContent = messageFirst.content().getString();
            if (message.content().getString().equals(firstContent)) {
                GuiMessageExtraData extraData = new GuiMessageExtraData(Boolean.TRUE, Boolean.FALSE);
                GuiMessageAttachment.put(message, extraData);
                GuiMessageAttachment.remove(messageFirst);
                return true;
            } else {
                GuiMessageAttachment.clear();
                return false;
            }
        }

        return false;
    }

    @FunctionalInterface
    public interface AlphaCalculator {

        static AlphaCalculator timeBased(int currentTickTime) {
            return message -> {
                int tickDelta = currentTickTime - message.addedTime();
                double t = tickDelta / 200.0;
                t = 1.0 - t;
                t *= 10.0;
                t = Mth.clamp(t, 0.0, 1.0);
                t *= t;
                return (float)t;
            };
        }

        float calculate(GuiMessage message);

    }

}
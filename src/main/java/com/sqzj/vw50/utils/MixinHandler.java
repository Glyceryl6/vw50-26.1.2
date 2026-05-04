package com.sqzj.vw50.utils;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.sqzj.vw50.VW50;
import com.sqzj.vw50.api.IChatComponentExtensions;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.MessageSignature;

import java.util.List;

public class MixinHandler {

    public static final WidgetSprites REPEAT_SPRITES = new WidgetSprites(
            VW50.prefix("repeat"), VW50.prefix("repeat_highlighted"));
    public static ChatComponent.ChatGraphicsAccess chatGraphicsAccess = null;
    public static MessageSignature signature = null;

    public static void extractRenderState$accept_Hook(ChatComponent chat, GuiMessage.Line line, int textTop) {
        if (chatGraphicsAccess instanceof ChatComponent.DrawingFocusedGraphicsAccess access) {
            Minecraft minecraft = Minecraft.getInstance();
            GuiGraphicsExtractor graphics = access.graphics;
            final int messageHeight = 9, iconSize = 10;
            int iconLeft = line.getTagIconLeft(minecraft.font);
            int top = textTop + messageHeight - iconSize;
            int right = iconLeft + iconSize;
            int textBottom = textTop + messageHeight;
            boolean isMouseOver = access.isMouseOver(iconLeft, top, right, textBottom);
            if (GuiMessageAttachment.get(line.parent()) != null) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                        VW50.prefix("plus_one"),
                        iconLeft, top, iconSize, iconSize);
                if (chat instanceof IChatComponentExtensions extensions) {
                    graphics.requestCursor(isMouseOver ? CursorTypes.POINTING_HAND : CursorType.DEFAULT);
                    extensions.VW50$setMouseOverRepeatButton(isMouseOver);
                }
            }
        }
    }

    public static boolean addMessage_Hook(List<GuiMessage> allMessages, GuiMessage message) {
        List<GuiMessage> messageList = allMessages.stream().filter(
                g -> g.source() == GuiMessageSource.PLAYER).toList();
        if (!messageList.isEmpty()) {
            GuiMessage messageFirst = messageList.getFirst();
            String firstContent = messageFirst.content().getString();
            if (message.content().getString().equals(firstContent)) {
                GuiMessageAttachment.put(message, Boolean.TRUE);
                GuiMessageAttachment.remove(messageFirst);
                return true;
            } else {
                GuiMessageAttachment.clear();
                return false;
            }
        }

        return false;
    }

}
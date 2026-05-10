package com.sqzj.vw50.misc.hook;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.sqzj.vw50.VW50;
import com.sqzj.vw50.api.IChatComponentExtensions;
import com.sqzj.vw50.client.gui.components.RedEnvelopeGraphicsAccess;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Predicate;

public class HookChatComponent {

    public static final WidgetSprites PLUS_ONE_SPRITES = new WidgetSprites(
            VW50.prefix("plus_one_default"), VW50.prefix("plus_one"));
    public static ChatComponent.ChatGraphicsAccess chatGraphicsAccess = null;

    public static void extractRenderState$accept_Hook(ChatComponent chat, GuiMessage.Line line, int textTop) {
        if (chatGraphicsAccess instanceof ChatComponent.DrawingFocusedGraphicsAccess access) {
            Minecraft minecraft = Minecraft.getInstance();
            GuiGraphicsExtractor graphics = access.graphics;
            final int messageHeight = 9, iconSize = 9;
            int iconLeft = line.getTagIconLeft(minecraft.font);
            int top = textTop + messageHeight - iconSize;
            int right = iconLeft + iconSize;
            int textBottom = textTop + messageHeight;
            if (GuiMessageAttachment.get(line.parent()) != null && line.endOfEntry()) {
                boolean isMouseOver = access.isMouseOver(iconLeft, top, right, textBottom);
                Identifier location = PLUS_ONE_SPRITES.get(true, isMouseOver);
                if (chat instanceof IChatComponentExtensions extensions) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, location, iconLeft, top, iconSize, iconSize);
                    graphics.requestCursor(isMouseOver ? CursorTypes.POINTING_HAND : CursorType.DEFAULT);
                    extensions.VW50$setMouseOverRepeatButton(isMouseOver);
                }
            }
        }

        if (chatGraphicsAccess instanceof RedEnvelopeGraphicsAccess access) {
            Minecraft minecraft = Minecraft.getInstance();
            GuiGraphicsExtractor graphics = access.graphics;

        }
    }

    public static boolean addMessage_Hook(List<GuiMessage> allMessages, GuiMessage message) {
        Predicate<GuiMessage> predicate = g -> g.source() == GuiMessageSource.PLAYER;
        List<GuiMessage> messageList = allMessages.stream().filter(predicate).toList();
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
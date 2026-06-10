package com.sqzj.vw50.misc.hook;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.sqzj.vw50.VW50;
import com.sqzj.vw50.api.IChatComponentExtensions;
import com.sqzj.vw50.common.envelope.RedEnvelopeStatus;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.server.network.RedEnvelopeSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HookChatComponent {

    public static final WidgetSprites PLUS_ONE_SPRITES = new WidgetSprites(VW50.prefix("plus_one_default"), VW50.prefix("plus_one"));
    public static final Identifier RED_ENV_COMPONENT_LOCATION = VW50.prefix("textures/item/empty_red_envelope.png");
    public static ChatComponent.ChatGraphicsAccess chatGraphicsAccess = null;

    private static final int RED_ENV_LEFT = 4;
    private static final int RED_ENV_MIN_WIDTH = 118;
    private static final int RED_ENV_DEFAULT_WIDTH = 158;
    private static final int RED_ENV_MAX_WIDTH = 220;
    private static final int RED_ENV_MIN_HEIGHT = 32;
    private static final int RED_ENV_INLINE_GAP = 4;
    private static final int VANILLA_MESSAGE_HEIGHT = 9;
    private static final int RED_ENV_HEADER_GAP = 2;
    private static final int CARD_TEXT_LINE_HEIGHT = 10;
    private static final int CARD_VERTICAL_PADDING = 6;
    private static final int CARD_DETAIL_GAP = 2;
    private static final int CARD_TEXT_LEFT_OFFSET = 30;
    private static final int CARD_RIGHT_PADDING = 8;
    private static final int EXCLUSIVE_FACE_SIZE = 8;
    private static final int EXCLUSIVE_FACE_GAP = 12;
    private static final Pattern VANILLA_PLAYER_MESSAGE = Pattern.compile("^<[^>]+>\\s*(.*)$");

    public static void extractRenderState$accept_InjectHead(
            ChatComponent chat, GuiMessage.Line line, int lineIndex,
            float alpha, int chatBottom, float textOpacity, CallbackInfo ci) {
        GuiMessageExtraData extraData = GuiMessageAttachment.get(line.parent());
        if (extraData == null || !extraData.isRedEnvelope) return;
        ci.cancel();
        if (!line.endOfEntry()) return;
        double chatLineSpacing = Minecraft.getInstance().options.chatLineSpacing().get();
        int entryHeight = getEntryHeight(chatLineSpacing);
        int textTop = getTextTop(chatBottom, lineIndex, entryHeight, chatLineSpacing);
        RedEnvelopeLayout layout = layoutFromExtra(extraData);
        int messageBottom = textTop + VANILLA_MESSAGE_HEIGHT;
        int renderTop = messageBottom - layout.totalHeight();
        int cardTop = layout.wrapped() ? renderTop + VANILLA_MESSAGE_HEIGHT : renderTop;
        int cardLeft = layout.wrapped() ? RED_ENV_LEFT : getInlineCardLeft(extraData);
        int senderTop = layout.wrapped() ? renderTop : cardTop + Math.max(0, (layout.cardHeight() - VANILLA_MESSAGE_HEIGHT) / 2);
        if (chatGraphicsAccess instanceof ChatComponent.DrawingFocusedGraphicsAccess access) {
            boolean isMouseOver = access.isMouseOver(cardLeft, cardTop, cardLeft + layout.cardWidth(), cardTop + layout.cardHeight());
            ActiveTextCollector.Parameters parameters = access.parameters.withOpacity(alpha * textOpacity);
            renderSenderPrefix(access.textRenderer, parameters, extraData, 0, senderTop);
            renderRedEnvelope(access.graphics, access.textRenderer, parameters, extraData, cardLeft, cardTop, layout.cardWidth(), layout.cardHeight(), isMouseOver);
            if (isMouseOver && extraData.redEnvelopeId != null && chat instanceof IChatComponentExtensions extensions) {
                access.graphics.requestCursor(CursorTypes.POINTING_HAND);
                extensions.VW50$setMouseOverRedEnvelope(true);
                extensions.VW50$setMouseOverRedEnvelopeId(extraData.redEnvelopeId);
            }
        } else if (chatGraphicsAccess instanceof ChatComponent.DrawingBackgroundGraphicsAccess access) {
            ActiveTextCollector.Parameters parameters = access.parameters.withOpacity(alpha * textOpacity);
            renderSenderPrefix(access.textRenderer, parameters, extraData, 0, senderTop);
            renderRedEnvelope(access.graphics, access.textRenderer, parameters, extraData, cardLeft, cardTop, layout.cardWidth(), layout.cardHeight(), false);
        }
    }

    public static RedEnvelopeLayout computeLayout(Minecraft minecraft, RedEnvelopeSnapshot snapshot) {
        int chatWidth = Math.max(RED_ENV_MIN_WIDTH, minecraft.gui.getChat().getWidth());
        int senderWidth = minecraft.font.width(makeSenderText(snapshot));
        int desiredWidth = getDesiredCardWidth(minecraft, snapshot);
        int inlineAvailable = chatWidth - RED_ENV_LEFT - senderWidth - RED_ENV_INLINE_GAP;
        boolean wrapped = inlineAvailable < RED_ENV_MIN_WIDTH || senderWidth + RED_ENV_INLINE_GAP + desiredWidth > chatWidth - RED_ENV_LEFT;
        int maxAvailable = wrapped ? chatWidth - RED_ENV_LEFT : inlineAvailable;
        int cardWidth = clamp(desiredWidth, Math.clamp(maxAvailable, 60, RED_ENV_MIN_WIDTH), Math.clamp(maxAvailable, 60, RED_ENV_MAX_WIDTH));
        int cardHeight = getCardHeight(minecraft, snapshot, cardWidth);
        int totalHeight = wrapped ? VANILLA_MESSAGE_HEIGHT + RED_ENV_HEADER_GAP + cardHeight : cardHeight;
        int entryHeight = getEntryHeight(minecraft.options.chatLineSpacing().get());
        int placeholderLines = Math.max(1, (int)Math.ceil(Math.max(0, totalHeight - VANILLA_MESSAGE_HEIGHT) / (double)entryHeight) + 1);
        return new RedEnvelopeLayout(wrapped, cardWidth, cardHeight, totalHeight, placeholderLines);
    }

    public record RedEnvelopeLayout(boolean wrapped, int cardWidth, int cardHeight, int totalHeight, int placeholderLines) { }

    private static RedEnvelopeLayout layoutFromExtra(GuiMessageExtraData extraData) {
        return new RedEnvelopeLayout(
                extraData.redEnvelopeWrapped,
                Math.max(60, extraData.redEnvelopeCardWidth),
                Math.max(RED_ENV_MIN_HEIGHT, extraData.redEnvelopeCardHeight),
                Math.max(RED_ENV_MIN_HEIGHT, extraData.redEnvelopeTotalHeight),
                Math.max(1, extraData.redEnvelopePlaceholderLines));
    }

    private static int getEntryHeight(double chatLineSpacing) {
        return (int)Math.floor(VANILLA_MESSAGE_HEIGHT * (chatLineSpacing + 1.0F));
    }

    private static int getTextTop(int chatBottom, int lineIndex, int entryHeight, double chatLineSpacing) {
        int entryBottomToMessageY = (int)Math.round(8.0 * (chatLineSpacing + 1.0) - 4.0 * chatLineSpacing);
        return chatBottom - lineIndex * entryHeight - entryBottomToMessageY;
    }

    private static int getInlineCardLeft(GuiMessageExtraData extraData) {
        return Minecraft.getInstance().font.width(makeSenderText(extraData.redEnvelopeSnapshot));
    }

    private static void renderSenderPrefix(ActiveTextCollector textRenderer, ActiveTextCollector.Parameters parameters, GuiMessageExtraData extraData, int left, int top) {
        textRenderer.accept(TextAlignment.LEFT, left, top, parameters, Component.literal(makeSenderText(extraData.redEnvelopeSnapshot)));
    }

    private static void renderRedEnvelope(
            GuiGraphicsExtractor graphics,
            ActiveTextCollector textRenderer,
            ActiveTextCollector.Parameters parameters,
            GuiMessageExtraData extraData,
            int left, int top, int width, int height, boolean hovered) {
        RedEnvelopeSnapshot snapshot = extraData.redEnvelopeSnapshot;
        boolean inactive = snapshot != null && snapshot.status() != RedEnvelopeStatus.ACTIVE;
        int alphaWhite = ARGB.white(parameters.opacity());
        int border = applyOpacity(hovered ? 0xFFFFD27A : 0xFFFFB24A, parameters.opacity());
        int body = applyOpacity(inactive ? 0xFF8A5C4A : 0xFFC83F2D, parameters.opacity());
        int body2 = applyOpacity(inactive ? 0xFF5D443C : 0xFF9D231C, parameters.opacity());
        graphics.fill(left, top, left + width, top + height, body);
        graphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, body2);
        graphics.fill(left, top, left + width, top + 1, border);
        graphics.fill(left, top + height - 1, left + width, top + height, border);
        int iconY = top + Math.max(8, (height - 16) / 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, RED_ENV_COMPONENT_LOCATION, left + 8, iconY,
                0.0F, 0.0F, 16, 16, 16, 16, alphaWhite);
        int textLeft = left + CARD_TEXT_LEFT_OFFSET;
        int textWidth = Math.max(20, width - CARD_TEXT_LEFT_OFFSET - CARD_RIGHT_PADDING);
        int y = top + CARD_VERTICAL_PADDING;
        for (String line : wrapText(makeTitleText(snapshot), textWidth)) {
            textRenderer.accept(TextAlignment.LEFT, textLeft, y, parameters, Component.literal(line).withStyle(ChatFormatting.GOLD));
            y += CARD_TEXT_LINE_HEIGHT;
        }

        y += CARD_DETAIL_GAP;
        renderCardDetail(graphics, textRenderer, parameters, snapshot, textLeft, y, textWidth, alphaWhite);
    }

    private static void renderCardDetail(
            GuiGraphicsExtractor graphics,
            ActiveTextCollector textRenderer,
            ActiveTextCollector.Parameters parameters,
            RedEnvelopeSnapshot snapshot,
            int textLeft,
            int y,
            int textWidth,
            int alphaWhite) {
        if (snapshot == null) {
            drawWrapped(textRenderer, parameters, textLeft, y, textWidth, Component.translatable("red_envelope.chat.click").getString(), ChatFormatting.YELLOW);
            return;
        }
        if (snapshot.status() != RedEnvelopeStatus.ACTIVE) {
            drawWrapped(textRenderer, parameters, textLeft, y, textWidth, Component.translatable("red_envelope.chat.finished").getString(), ChatFormatting.DARK_GRAY);
            return;
        }
        if (snapshot.viewerClaimed()) {
            drawWrapped(textRenderer, parameters, textLeft, y, textWidth, Component.translatable("red_envelope.chat.claimed", snapshot.claimedCount(), snapshot.playerCount()).getString(), ChatFormatting.GRAY);
            return;
        }
        if (snapshot.usePassword()) {
            String password = snapshot.password().isBlank() ? "?" : snapshot.password();
            drawWrapped(textRenderer, parameters, textLeft, y, textWidth, Component.translatable("red_envelope.chat.copy_password", password).getString(), ChatFormatting.YELLOW);
            return;
        }
        if (!snapshot.exclusiveUser().isBlank()) {
            graphics.fill(textLeft - 1, y - 1, textLeft + EXCLUSIVE_FACE_SIZE + 1, y + EXCLUSIVE_FACE_SIZE + 1, applyOpacity(0xFFFFD27A, parameters.opacity()));
            renderPlayerHead(graphics, snapshot.exclusiveUser(), textLeft, y, alphaWhite);
            drawWrapped(textRenderer, parameters, textLeft + EXCLUSIVE_FACE_GAP, y, Math.max(20, textWidth - EXCLUSIVE_FACE_GAP), Component.translatable("red_envelope.chat.exclusive_value", snapshot.exclusiveUser()).getString(), ChatFormatting.YELLOW);
            return;
        }

        drawWrapped(textRenderer, parameters, textLeft, y, textWidth, Component.translatable("red_envelope.chat.click", snapshot.claimedCount(), snapshot.playerCount()).getString(), ChatFormatting.YELLOW);
    }

    private static void drawWrapped(ActiveTextCollector textRenderer, ActiveTextCollector.Parameters parameters, int x, int y, int width, String text, ChatFormatting formatting) {
        int currentY = y;
        for (String line : wrapText(text, width)) {
            textRenderer.accept(TextAlignment.LEFT, x, currentY, parameters, Component.literal(line).withStyle(formatting));
            currentY += CARD_TEXT_LINE_HEIGHT;
        }
    }

    private static int getDesiredCardWidth(Minecraft minecraft, RedEnvelopeSnapshot snapshot) {
        int titleWidth = minecraft.font.width(makeTitleText(snapshot));
        int detailWidth = minecraft.font.width(makeDetailText(snapshot));
        if (snapshot != null && !snapshot.exclusiveUser().isBlank()) {
            detailWidth += EXCLUSIVE_FACE_GAP;
        }

        int desired = CARD_TEXT_LEFT_OFFSET + Math.max(titleWidth, detailWidth) + CARD_RIGHT_PADDING;
        return Mth.clamp(desired, RED_ENV_DEFAULT_WIDTH, RED_ENV_MAX_WIDTH);
    }

    private static int getCardHeight(Minecraft minecraft, RedEnvelopeSnapshot snapshot, int cardWidth) {
        int textWidth = Math.max(20, cardWidth - CARD_TEXT_LEFT_OFFSET - CARD_RIGHT_PADDING);
        int titleLines = wrapText(makeTitleText(snapshot), textWidth).size();
        int detailTextWidth = textWidth;
        if (snapshot != null && !snapshot.exclusiveUser().isBlank()) {
            detailTextWidth = Math.max(20, textWidth - EXCLUSIVE_FACE_GAP);
        }

        int detailLines = wrapText(makeDetailText(snapshot), detailTextWidth).size();
        int detailHeight = Math.max(CARD_TEXT_LINE_HEIGHT, detailLines * CARD_TEXT_LINE_HEIGHT);
        return Math.max(RED_ENV_MIN_HEIGHT, CARD_VERTICAL_PADDING * 2 + titleLines * CARD_TEXT_LINE_HEIGHT + CARD_DETAIL_GAP + detailHeight);
    }

    private static List<String> wrapText(String text, int maxWidth) {
        Minecraft minecraft = Minecraft.getInstance();
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            result.add("");
            return result;
        }

        int safeWidth = Math.max(1, maxWidth);
        StringBuilder current = new StringBuilder();
        text.codePoints().forEach(codePoint -> {
            String s = new String(Character.toChars(codePoint));
            if ("\n".equals(s)) {
                result.add(current.toString());
                current.setLength(0);
                return;
            }

            if (!current.isEmpty() && minecraft.font.width(current + s) > safeWidth) {
                result.add(current.toString());
                current.setLength(0);
            }

            current.append(s);
        });
        if (!current.isEmpty() || result.isEmpty()) result.add(current.toString());
        return result;
    }

    private static int clamp(int value, int min, int max) {
        if (max < min) return max;
        return Mth.clamp(value, min, max);
    }

    private static String makeTitleText(RedEnvelopeSnapshot snapshot) {
        if (snapshot == null || snapshot.title().isBlank()) return Component.translatable("red_envelope.chat.card_title").getString();
        return snapshot.title();
    }

    private static String makeDetailText(RedEnvelopeSnapshot snapshot) {
        if (snapshot == null) return Component.translatable("red_envelope.chat.click").getString();
        if (snapshot.status() != RedEnvelopeStatus.ACTIVE) return Component.translatable("red_envelope.chat.finished").getString();
        if (snapshot.viewerClaimed()) return Component.translatable("red_envelope.chat.claimed", snapshot.claimedCount(), snapshot.playerCount()).getString();
        if (snapshot.usePassword()) {
            String password = snapshot.password().isBlank() ? "?" : snapshot.password();
            return Component.translatable("red_envelope.chat.copy_password", password).getString();
        }
        if (!snapshot.exclusiveUser().isBlank()) return Component.translatable("red_envelope.chat.exclusive_value", snapshot.exclusiveUser()).getString();
        return Component.translatable("red_envelope.chat.click", snapshot.claimedCount(), snapshot.playerCount()).getString();
    }

    private static void renderPlayerHead(GuiGraphicsExtractor graphics, String playerName, int x, int y, int alphaWhite) {
        Identifier skin = getPlayerSkin(playerName).body().texturePath();
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8.0F, 8.0F, 8, 8, 64, 64, alphaWhite);
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40.0F, 8.0F, 8, 8, 64, 64, alphaWhite);
    }

    private static PlayerSkin getPlayerSkin(String playerName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(playerName);
            if (info != null) return info.getSkin();
        }
        UUID fallback = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
        return DefaultPlayerSkin.get(fallback);
    }

    private static int applyOpacity(int argb, float opacity) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * opacity);
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    private static String makeSenderText(RedEnvelopeSnapshot snapshot) {
        if (snapshot == null) return "<Server> ";
        String sender = snapshot.senderName().isBlank() ? "Server" : snapshot.senderName();
        return "<" + sender + "> ";
    }

    public static void extractRenderState$accept_InjectTail(ChatComponent chat, GuiMessage.Line line, int textTop) {
        if (chatGraphicsAccess instanceof ChatComponent.DrawingFocusedGraphicsAccess access) {
            GuiMessageExtraData extraData = GuiMessageAttachment.get(line.parent());
            if (extraData != null && !extraData.isRedEnvelope && chat instanceof IChatComponentExtensions extensions) {
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
            String firstContent = extractRepeatBody(messageFirst.content());
            String currentContent = extractRepeatBody(message.content());
            if (!currentContent.isBlank() && Objects.equals(currentContent, firstContent)) {
                GuiMessageExtraData extraData = new GuiMessageExtraData(Boolean.TRUE, Boolean.FALSE);
                extraData.repeatText = currentContent;
                GuiMessageAttachment.put(message, extraData);
                GuiMessageAttachment.remove(messageFirst);
                return true;
            } else {
                GuiMessageAttachment.clearRepeatMarks();
                return false;
            }
        }
        return false;
    }

    public static String extractRepeatBody(Component component) {
        if (component.getContents() instanceof TranslatableContents contents && "chat.type.text".equals(contents.getKey())) {
            Object[] args = contents.getArgs();
            if (args.length >= 2) {
                Object messageArg = args[1];
                if (messageArg instanceof Component messageComponent) return messageComponent.getString().trim();
                return String.valueOf(messageArg).trim();
            }
        }

        String raw = component.getString().trim();
        Matcher matcher = VANILLA_PLAYER_MESSAGE.matcher(raw);
        if (matcher.matches()) return matcher.group(1).trim();
        return raw;
    }

}
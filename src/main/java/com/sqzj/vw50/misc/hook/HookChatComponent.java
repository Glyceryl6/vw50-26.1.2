package com.sqzj.vw50.misc.hook;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.sqzj.vw50.VW50;
import com.sqzj.vw50.client.ClientRedEnvelopeManager;
import com.sqzj.vw50.common.envelope.RedEnvelopeStatus;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.server.network.ClaimRedEnvelopePayload;
import com.sqzj.vw50.server.network.ClaimSnapshot;
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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
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
    private static final int CLAIM_PANEL_WIDTH = 232;
    private static final int CLAIM_PANEL_ROW_HEIGHT = 14;
    private static final int EXCLUSIVE_FACE_SIZE = 8;
    private static final int EXCLUSIVE_FACE_GAP = 12;
    private static final Pattern VANILLA_PLAYER_MESSAGE = Pattern.compile("^<[^>]+>\\s*(.*)$");

    public static void extractRenderState$accept_InjectHead(
        ChatComponent.ChatGraphicsAccess graphics, GuiMessage.Line line, int lineIndex,
        float alpha, int chatBottom, float textOpacity, CallbackInfo ci) {
        GuiMessageExtraData extraData = GuiMessageAttachment.get(line.parent());
        if (extraData == null || !extraData.isRedEnvelope) return;
        ci.cancel();
        if (!line.endOfEntry()) return;
        double chatLineSpacing = Minecraft.getInstance().options.chatLineSpacing().get();
        RedEnvelopeRenderLayout renderLayout = getRedEnvelopeRenderLayout(extraData, lineIndex, chatBottom, chatLineSpacing);
        Bounds cardBounds = renderLayout.cardBounds();
        if (graphics instanceof ChatComponent.DrawingFocusedGraphicsAccess access) {
            boolean isMouseOver = access.isMouseOver(cardBounds.left(), cardBounds.top(), cardBounds.right(), cardBounds.bottom());
            ActiveTextCollector.Parameters parameters = access.parameters.withOpacity(alpha * textOpacity);
            renderSenderPrefix(access.textRenderer, parameters, extraData, 0, renderLayout.senderTop());
            renderRedEnvelope(access.graphics, access.textRenderer, parameters, extraData,
                cardBounds.left(), cardBounds.top(), cardBounds.width(), cardBounds.height(), isMouseOver);
            if (isMouseOver && extraData.redEnvelopeId != null) {
                access.graphics.requestCursor(CursorTypes.POINTING_HAND);
            }
        } else if (graphics instanceof ChatComponent.DrawingBackgroundGraphicsAccess access) {
            ActiveTextCollector.Parameters parameters = access.parameters.withOpacity(alpha * textOpacity);
            renderSenderPrefix(access.textRenderer, parameters, extraData, 0, renderLayout.senderTop());
            renderRedEnvelope(access.graphics, access.textRenderer, parameters, extraData,
                cardBounds.left(), cardBounds.top(), cardBounds.width(), cardBounds.height(), false);
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
        int placeholderLines = Math.max(1, (int) Math.ceil(Math.max(0, totalHeight - VANILLA_MESSAGE_HEIGHT) / (double) entryHeight) + 1);
        return new RedEnvelopeLayout(wrapped, cardWidth, cardHeight, totalHeight, placeholderLines);
    }

    public record RedEnvelopeLayout(boolean wrapped, int cardWidth, int cardHeight, int totalHeight,
                                    int placeholderLines) {
    }

    private record Bounds(int left, int top, int right, int bottom) {

        private int width() {
            return this.right - this.left;
        }

        private int height() {
            return this.bottom - this.top;
        }

        private boolean contains(float x, float y) {
            return x >= this.left && x < this.right && y >= this.top && y < this.bottom;
        }

    }

    private record RedEnvelopeRenderLayout(Bounds cardBounds, int senderTop) {
    }

    private record ClaimPanelLayout(int left, int top, int width, int height, Bounds closeBounds) {
    }

    private sealed interface InteractionTarget permits CloseClaimPanelTarget, RedEnvelopeTarget, RepeatTarget {
    }

    private enum CloseClaimPanelTarget implements InteractionTarget {
        INSTANCE
    }

    private record RedEnvelopeTarget(UUID id) implements InteractionTarget {
    }

    private record RepeatTarget(String text) implements InteractionTarget {
    }

    private static RedEnvelopeLayout layoutFromExtra(GuiMessageExtraData extraData) {
        return new RedEnvelopeLayout(extraData.redEnvelopeWrapped,
            Math.max(60, extraData.redEnvelopeCardWidth),
            Math.max(RED_ENV_MIN_HEIGHT, extraData.redEnvelopeCardHeight),
            Math.max(RED_ENV_MIN_HEIGHT, extraData.redEnvelopeTotalHeight),
            Math.max(1, extraData.redEnvelopePlaceholderLines));
    }

    private static RedEnvelopeRenderLayout getRedEnvelopeRenderLayout(
        GuiMessageExtraData extraData, int lineIndex, int chatBottom, double chatLineSpacing) {
        int entryHeight = getEntryHeight(chatLineSpacing);
        int textTop = getTextTop(chatBottom, lineIndex, entryHeight, chatLineSpacing);
        RedEnvelopeLayout layout = layoutFromExtra(extraData);
        int messageBottom = textTop + VANILLA_MESSAGE_HEIGHT;
        int renderTop = messageBottom - layout.totalHeight();
        int cardTop = layout.wrapped() ? renderTop + VANILLA_MESSAGE_HEIGHT : renderTop;
        int cardLeft = layout.wrapped() ? RED_ENV_LEFT : getInlineCardLeft(extraData);
        int senderTop = layout.wrapped()
            ? renderTop
            : cardTop + Math.max(0, (layout.cardHeight() - VANILLA_MESSAGE_HEIGHT) / 2);
        return new RedEnvelopeRenderLayout(
            new Bounds(cardLeft, cardTop, cardLeft + layout.cardWidth(), cardTop + layout.cardHeight()),
            senderTop);
    }

    private static Bounds getFinishNoticeBounds(ChatComponent chat, GuiMessage.Line line, int textTop) {
        int width = Math.min(Minecraft.getInstance().font.width(line.content()), chat.getWidth());
        return new Bounds(RED_ENV_LEFT, textTop, RED_ENV_LEFT + Math.max(40, width), textTop + VANILLA_MESSAGE_HEIGHT);
    }

    private static Bounds getRepeatButtonBounds(GuiMessage.Line line, int textTop) {
        int iconLeft = line.getTagIconLeft(Minecraft.getInstance().font);
        return new Bounds(iconLeft, textTop, iconLeft + 9, textTop + VANILLA_MESSAGE_HEIGHT);
    }

    private static ClaimPanelLayout getClaimPanelLayout(ChatComponent chat, RedEnvelopeSnapshot snapshot, int chatBottom) {
        int rowCount = Math.max(1, snapshot.claims().size());
        int width = Math.clamp(chat.getWidth(), 140, CLAIM_PANEL_WIDTH);
        int height = 25 + rowCount * CLAIM_PANEL_ROW_HEIGHT + 8;
        int left = RED_ENV_LEFT;
        int top = Math.max(8, chatBottom - height - 92);
        int closeX = left + width - 16;
        int closeY = top + 6;
        return new ClaimPanelLayout(left, top, width, height,
            new Bounds(closeX - 2, closeY - 2, closeX + 10, closeY + 10));
    }

    private static Vector2f getChatLocalMouse(ChatComponent chat, int mouseX, int mouseY) {
        float scale = (float) chat.getScale();
        Matrix3x2f pose = new Matrix3x2f();
        // Mirror ChatComponent's focused render transform so input and rendering use the same coordinate space.
        pose.scale(scale, scale);
        pose.translate(4.0F, 0.0F);
        return pose.invert(new Matrix3x2f()).transformPosition(mouseX, mouseY, new Vector2f());
    }

    public static boolean handleMouseClick(
        ChatComponent chat, ChatComponent.DisplayMode displayMode,
        int screenHeight, int mouseX, int mouseY) {
        InteractionTarget target = findInteractionTarget(chat, displayMode, screenHeight, mouseX, mouseY);
        if (target == null) return false;

        Minecraft minecraft = Minecraft.getInstance();
        if (target == CloseClaimPanelTarget.INSTANCE) {
            ClientRedEnvelopeManager.closeClaimList();
            return true;
        }

        if (target instanceof RedEnvelopeTarget redEnvelope) {
            UUID id = redEnvelope.id();
            RedEnvelopeSnapshot snapshot = ClientRedEnvelopeManager.getSnapshot(id);
            if (snapshot != null && (snapshot.status() != RedEnvelopeStatus.ACTIVE || snapshot.viewerClaimed())) {
                ClientRedEnvelopeManager.toggleClaimList(id);
            } else if (snapshot != null && snapshot.usePassword()) {
                String password = snapshot.password();
                if (!password.isBlank()) {
                    minecraft.keyboardHandler.setClipboard(password);
                    if (minecraft.player != null) {
                        minecraft.player.sendOverlayMessage(Component.translatable("red_envelope.chat.password_copied").withStyle(ChatFormatting.GOLD));
                    }
                }
            } else {
                ClientPacketDistributor.sendToServer(new ClaimRedEnvelopePayload(id));
            }

            return true;
        }

        if (target instanceof RepeatTarget repeat) {
            String text = repeat.text().trim();
            if (minecraft.player != null && !text.isBlank()) {
                minecraft.player.connection.sendChat(text);
            }

            return true;
        }

        return false;
    }

    private static @Nullable InteractionTarget findInteractionTarget(
        ChatComponent chat, ChatComponent.DisplayMode displayMode,
        int screenHeight, int mouseX, int mouseY) {
        Vector2f localMouse = getChatLocalMouse(chat, mouseX, mouseY);
        float scale = (float) chat.getScale();
        int chatBottom = Mth.floor((screenHeight - ChatComponent.BOTTOM_MARGIN) / scale);

        if (!chat.trimmedMessages.isEmpty() || displayMode.showRestrictedPrompt) {
            RedEnvelopeSnapshot selected = ClientRedEnvelopeManager.getSelectedClaimListSnapshot().orElse(null);
            if (selected != null) {
                ClaimPanelLayout panel = getClaimPanelLayout(chat, selected, chatBottom);
                if (panel.closeBounds().contains(localMouse.x, localMouse.y)) {
                    return CloseClaimPanelTarget.INSTANCE;
                }
            }
        }

        double chatLineSpacing = Minecraft.getInstance().options.chatLineSpacing().get();
        int entryHeight = getEntryHeight(chatLineSpacing);
        InteractionTarget[] result = new InteractionTarget[1];
        chat.forEachLine(ChatComponent.AlphaCalculator.FULLY_VISIBLE, (line, lineIndex, _) -> {
            GuiMessageExtraData extraData = GuiMessageAttachment.get(line.parent());
            if (extraData == null) return;

            int textTop = getTextTop(chatBottom, lineIndex, entryHeight, chatLineSpacing);
            if (extraData.isRedEnvelope) {
                if (line.endOfEntry() && extraData.redEnvelopeId != null) {
                    Bounds bounds = getRedEnvelopeRenderLayout(extraData, lineIndex, chatBottom, chatLineSpacing).cardBounds();
                    if (bounds.contains(localMouse.x, localMouse.y)) {
                        result[0] = new RedEnvelopeTarget(extraData.redEnvelopeId);
                    }
                }

                return;
            }

            if (extraData.isRedEnvelopeFinishNotice && line.endOfEntry() && extraData.redEnvelopeId != null) {
                Bounds bounds = getFinishNoticeBounds(chat, line, textTop);
                if (bounds.contains(localMouse.x, localMouse.y)) {
                    result[0] = new RedEnvelopeTarget(extraData.redEnvelopeId);
                }
            }

            if (extraData.canPlusOne && line.endOfEntry() && !(result[0] instanceof RedEnvelopeTarget)) {
                Bounds bounds = getRepeatButtonBounds(line, textTop);
                if (bounds.contains(localMouse.x, localMouse.y)) {
                    result[0] = new RepeatTarget(extraData.repeatText);
                }
            }
        });
        return result[0];
    }

    private static int getEntryHeight(double chatLineSpacing) {
        return (int) Math.floor(VANILLA_MESSAGE_HEIGHT * (chatLineSpacing + 1.0F));
    }

    private static int getTextTop(int chatBottom, int lineIndex, int entryHeight, double chatLineSpacing) {
        int entryBottomToMessageY = (int) Math.round(8.0 * (chatLineSpacing + 1.0) - 4.0 * chatLineSpacing);
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
        int baseColor = snapshot == null ? RedEnvelopeSnapshot.DEFAULT_CARD_COLOR : snapshot.cardColor();
        int border = applyOpacity(hovered ? 0xFFFFD27A : lighten(baseColor, 46), parameters.opacity());
        int body = applyOpacity(inactive ? desaturate(baseColor) : baseColor, parameters.opacity());
        int body2 = applyOpacity(inactive ? darken(desaturate(baseColor), 42) : darken(baseColor, 40), parameters.opacity());
        graphics.fill(left, top, left + width, top + height, body);
        graphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, body2);
        graphics.fill(left, top, left + width, top + 1, border);
        graphics.fill(left, top + height - 1, left + width, top + height, border);
        int iconY = top + Math.max(8, (height - 16) / 2);
        Identifier iconIdentifier = snapshot == null
            ? RedEnvelopeSnapshot.DEFAULT_ICON_IDENTIFIER
            : snapshot.iconIdentifier();
        graphics.blit(RenderPipelines.GUI_TEXTURED, iconIdentifier, left + 8, iconY,
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

    public static void renderClaimListPanel(ChatComponent.DrawingFocusedGraphicsAccess access, int chatBottom, float textOpacity) {
        ClientRedEnvelopeManager.getSelectedClaimListSnapshot().ifPresent(snapshot -> {
            Minecraft minecraft = Minecraft.getInstance();
            ClaimPanelLayout panel = getClaimPanelLayout(minecraft.gui.getChat(), snapshot, chatBottom);
            int left = panel.left();
            int top = panel.top();
            int width = panel.width();
            int height = panel.height();
            GuiGraphicsExtractor graphics = access.graphics;
            ActiveTextCollector textRenderer = access.textRenderer;
            ActiveTextCollector.Parameters parameters = access.parameters.withOpacity(textOpacity);
            int body = applyOpacity(0xFFD24635, textOpacity);
            int inner = applyOpacity(0xFF8E211A, textOpacity);
            int border = applyOpacity(0xFFFFD27A, textOpacity);
            graphics.fill(left, top, left + width, top + height, body);
            graphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, inner);
            graphics.outline(left, top, width, height, border);
            textRenderer.accept(TextAlignment.LEFT, left + 8, top + 7, parameters, Component.translatable("red_envelope.claim_list.title", snapshot.title()).withStyle(ChatFormatting.GOLD));
            Bounds closeBounds = panel.closeBounds();
            int closeX = closeBounds.left() + 2;
            int closeY = closeBounds.top() + 2;
            boolean closeHover = access.isMouseOver(closeBounds.left(), closeBounds.top(), closeBounds.right(), closeBounds.bottom());
            textRenderer.accept(TextAlignment.LEFT, closeX, closeY, parameters, Component.literal("×").withStyle(closeHover ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            if (closeHover) {
                access.graphics.requestCursor(CursorTypes.POINTING_HAND);
            }

            int y = top + 22;
            for (ClaimSnapshot claim : snapshot.claims()) {
                renderPlayerHead(graphics, claim.playerName(), left + 8, y + 2, ARGB.white(textOpacity));
                textRenderer.accept(TextAlignment.LEFT, left + 22, y + 2, parameters, Component.literal(claim.playerName()).withStyle(ChatFormatting.WHITE));
                String amount = Component.translatable("red_envelope.claim_list.amount", claim.amount()).getString();
                textRenderer.accept(TextAlignment.LEFT, left + width - 8 - minecraft.font.width(amount), y + 2, parameters, Component.literal(amount).withStyle(ChatFormatting.YELLOW));
                y += CLAIM_PANEL_ROW_HEIGHT;
            }

            if (snapshot.claims().isEmpty()) {
                textRenderer.accept(TextAlignment.LEFT, left + 8, y + 2, parameters, Component.translatable("red_envelope.claim_list.empty").withStyle(ChatFormatting.GRAY));
            }
        });
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
        if (snapshot == null || snapshot.title().isBlank())
            return Component.translatable("red_envelope.chat.card_title").getString();
        return snapshot.title();
    }

    private static String makeDetailText(RedEnvelopeSnapshot snapshot) {
        if (snapshot == null) return Component.translatable("red_envelope.chat.click").getString();
        if (snapshot.status() != RedEnvelopeStatus.ACTIVE)
            return Component.translatable("red_envelope.chat.finished").getString();
        if (snapshot.viewerClaimed())
            return Component.translatable("red_envelope.chat.claimed", snapshot.claimedCount(), snapshot.playerCount()).getString();
        if (snapshot.usePassword()) {
            String password = snapshot.password().isBlank() ? "?" : snapshot.password();
            return Component.translatable("red_envelope.chat.copy_password", password).getString();
        }
        if (!snapshot.exclusiveUser().isBlank())
            return Component.translatable("red_envelope.chat.exclusive_value", snapshot.exclusiveUser()).getString();
        return Component.translatable("red_envelope.chat.click", snapshot.claimedCount(), snapshot.playerCount()).getString();
    }

    private static int darken(int argb, int amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.max(0, ((argb >>> 16) & 0xFF) - amount);
        int g = Math.max(0, ((argb >>> 8) & 0xFF) - amount);
        int b = Math.max(0, (argb & 0xFF) - amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lighten(int argb, int amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + amount);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + amount);
        int b = Math.min(255, (argb & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int desaturate(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        int gray = (r + g + b) / 3;
        return (a << 24) | (((r + gray) / 2) << 16) | (((g + gray) / 2) << 8) | ((b + gray) / 2);
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

    public static void extractRenderState$accept_InjectTail(
        ChatComponent chat, ChatComponent.ChatGraphicsAccess graphicsAccess,
        GuiMessage.Line line, int textTop) {
        if (graphicsAccess instanceof ChatComponent.DrawingFocusedGraphicsAccess access) {
            GuiMessageExtraData extraData = GuiMessageAttachment.get(line.parent());
            if (extraData != null && !extraData.isRedEnvelope) {
                GuiGraphicsExtractor graphics = access.graphics;
                if (extraData.isRedEnvelopeFinishNotice && line.endOfEntry() && extraData.redEnvelopeId != null) {
                    Bounds bounds = getFinishNoticeBounds(chat, line, textTop);
                    boolean isMouseOver = access.isMouseOver(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
                    if (isMouseOver) {
                        graphics.requestCursor(CursorTypes.POINTING_HAND);
                    }
                }

                if (extraData.canPlusOne && line.endOfEntry()) {
                    Bounds bounds = getRepeatButtonBounds(line, textTop);
                    boolean isMouseOver = access.isMouseOver(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
                    Identifier location = PLUS_ONE_SPRITES.get(true, isMouseOver);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, location,
                        bounds.left(), bounds.top(), bounds.width(), bounds.height());
                    graphics.requestCursor(isMouseOver ? CursorTypes.POINTING_HAND : CursorType.DEFAULT);
                }
            }
        }
    }

    public static void addMessage_Inject(List<GuiMessage> allMessages, GuiMessage message) {
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
            } else {
                GuiMessageAttachment.clearRepeatMarks();
            }
        }
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

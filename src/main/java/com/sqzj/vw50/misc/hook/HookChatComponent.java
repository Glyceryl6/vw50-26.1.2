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
    private static final int RED_ENV_WIDTH = 158;
    private static final int RED_ENV_HEIGHT = 32;
    private static final int VANILLA_MESSAGE_HEIGHT = 9;
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
        int cardTop = textTop + VANILLA_MESSAGE_HEIGHT - RED_ENV_HEIGHT;
        boolean wrapped = extraData.redEnvelopeWrapped;
        int cardLeft = wrapped ? RED_ENV_LEFT : getInlineCardLeft(extraData);
        int senderTop = wrapped ? cardTop - VANILLA_MESSAGE_HEIGHT : cardTop + (RED_ENV_HEIGHT - VANILLA_MESSAGE_HEIGHT) / 2;
        if (chatGraphicsAccess instanceof ChatComponent.DrawingFocusedGraphicsAccess access) {
            boolean isMouseOver = access.isMouseOver(cardLeft, cardTop, cardLeft + RED_ENV_WIDTH, cardTop + RED_ENV_HEIGHT);
            ActiveTextCollector.Parameters parameters = access.parameters.withOpacity(alpha * textOpacity);
            renderSenderPrefix(access.textRenderer, parameters, extraData, 0, senderTop);
            renderRedEnvelope(access.graphics, access.textRenderer, parameters, extraData, cardLeft, cardTop, true, isMouseOver);
            if (isMouseOver && extraData.redEnvelopeId != null && chat instanceof IChatComponentExtensions extensions) {
                access.graphics.requestCursor(CursorTypes.POINTING_HAND);
                extensions.VW50$setMouseOverRedEnvelope(true);
                extensions.VW50$setMouseOverRedEnvelopeId(extraData.redEnvelopeId);
            }
        } else if (chatGraphicsAccess instanceof ChatComponent.DrawingBackgroundGraphicsAccess access) {
            ActiveTextCollector.Parameters parameters = access.parameters.withOpacity(alpha * textOpacity);
            renderSenderPrefix(access.textRenderer, parameters, extraData, 0, senderTop);
            renderRedEnvelope(access.graphics, access.textRenderer, parameters, extraData, cardLeft, cardTop, false, false);
        }
    }

    private static int getEntryHeight(double chatLineSpacing) {
        return Mth.floor(VANILLA_MESSAGE_HEIGHT * (chatLineSpacing + 1.0F));
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
            int left, int top, boolean focused, boolean hovered) {
        RedEnvelopeSnapshot snapshot = extraData.redEnvelopeSnapshot;
        boolean inactive = snapshot != null && snapshot.status() != RedEnvelopeStatus.ACTIVE;
        int alphaWhite = ARGB.white(parameters.opacity());
        int border = applyOpacity(hovered ? 0xFFFFD27A : 0xFFFFB24A, parameters.opacity());
        int body = applyOpacity(inactive ? 0xFF8A5C4A : 0xFFC83F2D, parameters.opacity());
        int body2 = applyOpacity(inactive ? 0xFF5D443C : 0xFF9D231C, parameters.opacity());
        graphics.fill(left, top, left + RED_ENV_WIDTH, top + RED_ENV_HEIGHT, body);
        graphics.fill(left + 2, top + 2, left + RED_ENV_WIDTH - 2, top + RED_ENV_HEIGHT - 2, body2);
        graphics.fill(left, top, left + RED_ENV_WIDTH, top + 1, border);
        graphics.fill(left, top + RED_ENV_HEIGHT - 1, left + RED_ENV_WIDTH, top + RED_ENV_HEIGHT, border);
        graphics.blit(RenderPipelines.GUI_TEXTURED, RED_ENV_COMPONENT_LOCATION,
                left + 8, top + 8, 0.0F, 0.0F,
                16, 16, 16, 16, alphaWhite);
        int textLeft = left + 30;
        Component title = makeTitle(snapshot);
        textRenderer.accept(TextAlignment.LEFT, textLeft, top + 5, parameters, title);
        renderCardDetail(graphics, textRenderer, parameters, snapshot, textLeft, top, alphaWhite);
        if (focused && snapshot != null && snapshot.usePassword()) {
            Component hint = Component.translatable("red_envelope.chat.password_hint").withStyle(ChatFormatting.YELLOW);
            textRenderer.accept(TextAlignment.LEFT, left + RED_ENV_WIDTH + 6, top + 10, parameters, hint);
        }
    }

    private static void renderCardDetail(
            GuiGraphicsExtractor graphics,
            ActiveTextCollector textRenderer,
            ActiveTextCollector.Parameters parameters,
            RedEnvelopeSnapshot snapshot,
            int textLeft, int top, int alphaWhite) {
        if (snapshot == null) {
            textRenderer.accept(TextAlignment.LEFT, textLeft, top + 19, parameters, Component.translatable("red_envelope.chat.click"));
            return;
        }
        if (snapshot.status() != RedEnvelopeStatus.ACTIVE) {
            textRenderer.accept(TextAlignment.LEFT, textLeft, top + 19, parameters, Component.translatable("red_envelope.chat.finished").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (snapshot.viewerClaimed()) {
            textRenderer.accept(TextAlignment.LEFT, textLeft, top + 19, parameters, Component.translatable("red_envelope.chat.claimed", snapshot.claimedCount(), snapshot.playerCount()).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (snapshot.usePassword()) {
            String password = snapshot.password().isBlank() ? "?" : snapshot.password();
            textRenderer.accept(TextAlignment.LEFT, textLeft, top + 19, parameters, Component.translatable("red_envelope.chat.password_value", password).withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (!snapshot.exclusiveUser().isBlank()) {
            int faceY = top + 18;
            graphics.fill(textLeft - 1, faceY - 1, textLeft + 9, faceY + 9, applyOpacity(0xFFFFD27A, parameters.opacity()));
            renderPlayerHead(graphics, snapshot.exclusiveUser(), textLeft, faceY, alphaWhite);
            textRenderer.accept(TextAlignment.LEFT, textLeft + 12, top + 19, parameters, Component.translatable("red_envelope.chat.exclusive_value", snapshot.exclusiveUser()).withStyle(ChatFormatting.YELLOW));
            return;
        }

        textRenderer.accept(TextAlignment.LEFT, textLeft, top + 19, parameters, Component.translatable("red_envelope.chat.click", snapshot.claimedCount(), snapshot.playerCount()).withStyle(ChatFormatting.YELLOW));
    }

    private static void renderPlayerHead(GuiGraphicsExtractor graphics, String playerName, int x, int y, int alphaWhite) {
        Identifier skin = getPlayerSkin(playerName).body().texturePath();
        // Face layer and hat layer.  If the target player is not in the local tab list,
        // DefaultPlayerSkin supplies a stable fallback skin.
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8.0F, 8.0F, 8, 8, 64, 64, alphaWhite);
        graphics.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40.0F, 8.0F, 8, 8, 64, 64, alphaWhite);
    }

    private static PlayerSkin getPlayerSkin(String playerName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(playerName);
            if (info != null) return info.getSkin();
        }

        UUID fallback = UUID.nameUUIDFromBytes(("OfflinePlayer: " + playerName).getBytes(StandardCharsets.UTF_8));
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

    private static Component makeTitle(RedEnvelopeSnapshot snapshot) {
        if (snapshot == null) return Component.translatable("red_envelope.chat.card_title").withStyle(ChatFormatting.GOLD);
        String title = snapshot.title().isBlank() ? Component.translatable("red_envelope.chat.card_title").getString() : snapshot.title();
        return Component.literal(title).withStyle(ChatFormatting.GOLD);
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
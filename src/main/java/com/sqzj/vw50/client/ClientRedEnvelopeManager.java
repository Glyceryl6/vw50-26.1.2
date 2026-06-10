package com.sqzj.vw50.client;

import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.server.network.ClaimResultPayload;
import com.sqzj.vw50.server.network.RedEnvelopeSnapshot;
import com.sqzj.vw50.server.network.RedEnvelopeSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientRedEnvelopeManager {

    private static final Map<UUID, GuiMessage> CHAT_MESSAGES = new HashMap<>();
    private static final Map<UUID, RedEnvelopeSnapshot> SNAPSHOTS = new HashMap<>();

    private static final int RED_ENVELOPE_CARD_WIDTH = 158;
    private static final int RED_ENVELOPE_INLINE_GAP = 4;
    private static final String INLINE_PLACEHOLDER = " \n \n ";
    private static final String WRAPPED_PLACEHOLDER = " \n \n \n ";

    public static void handleSync(RedEnvelopeSyncPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (payload.clearOld()) {
            CHAT_MESSAGES.clear();
            SNAPSHOTS.clear();
        }

        for (RedEnvelopeSnapshot snapshot : payload.envelopes()) {
            SNAPSHOTS.put(snapshot.id(), snapshot);
            addOrUpdateChatMessage(minecraft, snapshot);
        }
    }

    public static void handleClaimResult(ClaimResultPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        MutableComponent message = Component.translatable(payload.message(), payload.amount());
        // Claim feedback should not become a normal chat line; otherwise repeated
        // clicks push the red-envelope entry away from surrounding chat messages.
        minecraft.player.sendOverlayMessage(message.withStyle(payload.success() ? ChatFormatting.GOLD : ChatFormatting.RED));
    }

    public static RedEnvelopeSnapshot getSnapshot(UUID id) {
        return SNAPSHOTS.get(id);
    }

    private static Component makeChatContent(RedEnvelopeSnapshot snapshot, boolean wrapped) {
        String sender = snapshot.senderName().isBlank() ? "Server" : snapshot.senderName();
        String title = snapshot.title().isBlank() ? "Red Envelope" : snapshot.title();
        String placeholder = wrapped ? WRAPPED_PLACEHOLDER : INLINE_PLACEHOLDER;
        // This string is still useful for the vanilla chat log/search, but the
        // visible red-envelope entry is drawn by HookChatComponent.  The blank
        // placeholder lines reserve the exact vertical room for the custom card.
        return Component.literal("<" + sender + "> [" + title + "]\n" + placeholder);
    }

    private static boolean shouldWrapCard(Minecraft minecraft, RedEnvelopeSnapshot snapshot) {
        ChatComponent chat = minecraft.gui.getChat();
        String sender = snapshot.senderName().isBlank() ? "Server" : snapshot.senderName();
        int senderWidth = minecraft.font.width("<" + sender + "> ");
        return senderWidth + RED_ENVELOPE_INLINE_GAP + RED_ENVELOPE_CARD_WIDTH > chat.getWidth();
    }

    private static void addOrUpdateChatMessage(Minecraft minecraft, RedEnvelopeSnapshot snapshot) {
        GuiMessage existing = CHAT_MESSAGES.get(snapshot.id());
        if (existing != null) {
            GuiMessageExtraData data = GuiMessageAttachment.get(existing);
            if (data != null) {
                data.redEnvelopeSnapshot = snapshot;
                data.isRedEnvelope = true;
            }

            return;
        }

        boolean wrapped = shouldWrapCard(minecraft, snapshot);
        ChatComponent chat = minecraft.gui.getChat();
        GuiMessage message = new GuiMessage(minecraft.gui.getGuiTicks(), makeChatContent(snapshot, wrapped), null, GuiMessageSource.SYSTEM_SERVER, GuiMessageTag.systemSinglePlayer());
        GuiMessageExtraData data = GuiMessageExtraData.redEnvelope(snapshot);
        data.redEnvelopeWrapped = wrapped;
        GuiMessageAttachment.put(message, data);
        CHAT_MESSAGES.put(snapshot.id(), message);
        if (chat.visibleMessageFilter.test(message)) {
            chat.logChatMessage(message);
            chat.addMessageToDisplayQueue(message);
        }
    }

}
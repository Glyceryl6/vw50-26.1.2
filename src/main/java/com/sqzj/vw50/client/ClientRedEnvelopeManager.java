package com.sqzj.vw50.client;

import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.misc.hook.HookChatComponent;
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
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientRedEnvelopeManager {

    private static final Map<UUID, GuiMessage> CHAT_MESSAGES = new HashMap<>();
    private static final Map<UUID, RedEnvelopeSnapshot> SNAPSHOTS = new HashMap<>();

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
        minecraft.player.sendOverlayMessage(message.withStyle(payload.success() ? ChatFormatting.GOLD : ChatFormatting.RED));
    }

    @Nullable
    public static RedEnvelopeSnapshot getSnapshot(UUID id) {
        return SNAPSHOTS.get(id);
    }

    private static Component makeChatContent(RedEnvelopeSnapshot snapshot, HookChatComponent.RedEnvelopeLayout layout) {
        String sender = snapshot.senderName().isBlank() ? "Server" : snapshot.senderName();
        String label = Component.translatable("red_envelope.chat.card_title").getString();
        StringBuilder builder = new StringBuilder();
        // Keep a short, readable log entry, but reserve the visible height using
        // controlled blank lines so long titles/passwords do not make vanilla chat
        // wrap unpredictably before VW50 draws the card.
        builder.append("<").append(sender).append("> [").append(label).append("]");
        int lines = Math.max(1, layout.placeholderLines());
        for (int i = 1; i < lines; i++) {
            builder.append('\n').append(' ');
        }

        return Component.literal(builder.toString());
    }

    private static void applyLayout(GuiMessageExtraData data, HookChatComponent.RedEnvelopeLayout layout) {
        data.redEnvelopeWrapped = layout.wrapped();
        data.redEnvelopeCardWidth = layout.cardWidth();
        data.redEnvelopeCardHeight = layout.cardHeight();
        data.redEnvelopeTotalHeight = layout.totalHeight();
        data.redEnvelopePlaceholderLines = layout.placeholderLines();
    }

    private static void addOrUpdateChatMessage(Minecraft minecraft, RedEnvelopeSnapshot snapshot) {
        HookChatComponent.RedEnvelopeLayout layout = HookChatComponent.computeLayout(minecraft, snapshot);
        GuiMessage existing = CHAT_MESSAGES.get(snapshot.id());
        if (existing != null) {
            GuiMessageExtraData data = GuiMessageAttachment.get(existing);
            if (data != null) {
                data.redEnvelopeSnapshot = snapshot;
                data.isRedEnvelope = true;
                applyLayout(data, layout);
            }

            return;
        }

        ChatComponent chat = minecraft.gui.getChat();
        GuiMessage message = new GuiMessage(minecraft.gui.getGuiTicks(), makeChatContent(snapshot, layout), null, GuiMessageSource.SYSTEM_SERVER, GuiMessageTag.systemSinglePlayer());
        GuiMessageExtraData data = GuiMessageExtraData.redEnvelope(snapshot);
        applyLayout(data, layout);
        GuiMessageAttachment.put(message, data);
        CHAT_MESSAGES.put(snapshot.id(), message);
        if (chat.visibleMessageFilter.test(message)) {
            chat.logChatMessage(message);
            chat.addMessageToDisplayQueue(message);
        }
    }

}
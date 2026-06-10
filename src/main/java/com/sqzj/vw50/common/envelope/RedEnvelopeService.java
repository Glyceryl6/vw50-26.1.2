package com.sqzj.vw50.common.envelope;

import com.sqzj.vw50.client.menu.SendRedEnvelopeMenu;
import com.sqzj.vw50.common.registry.VWItems;
import com.sqzj.vw50.server.network.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RedEnvelopeService {

    public static final int DEFAULT_SEND_COOLDOWN_TICKS = 20 * 30;
    public static final int DEFAULT_REPEAT_MAX_PER_MINUTE = 6;
    public static final int DEFAULT_REPEAT_MIN_INTERVAL_MS = 1200;

    private static final Map<UUID, Long> LAST_SEND_GAME_TIME = new ConcurrentHashMap<>();
    private static final Deque<QueuedClaim> CLAIM_QUEUE = new ArrayDeque<>();
    private static final Random RANDOM = new Random();

    public static RedEnvelopeSavedData getData(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(RedEnvelopeSavedData.TYPE);
    }

    public static void createFromMenu(ServerPlayer player, SendRedEnvelopePayload payload) {
        if (!(player.containerMenu instanceof SendRedEnvelopeMenu menu)) {
            sendError(player, "red_envelope.error.no_menu");
            return;
        }
        
        List<ItemStack> stacks = menu.giftSlot.copyToList().stream().filter(stack -> !stack.isEmpty()).toList();
        if (stacks.isEmpty()) {
            sendError(player, "red_envelope.error.empty_stack");
            return;
        }
        
        ItemStack stack = stacks.getFirst().copy();
        CreateResult result = create(player, stack, payload, false, false);
        if (result.created()) {
            menu.giftSlot.set(0, ItemResource.of(stack), 0);
            consumeEmptyEnvelope(player);
            player.closeContainer();
        }
    }

    public static CreateResult create(ServerPlayer player, ItemStack stack, SendRedEnvelopePayload payload, boolean ignoreLimit, boolean systemEnvelope) {
        MinecraftServer server = player.server;
        long gameTime = server.overworld().getGameTime();
        if (!ignoreLimit && !player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            RedEnvelopeSavedData data = getData(server);
            Optional<SendLimitRecord> limit = data.getLimit(player.getUUID());
            if (limit.isPresent() && limit.get().blocked()) {
                player.sendSystemMessage(Component.translatable("red_envelope.error.blocked").withStyle(ChatFormatting.RED), true);
                return CreateResult.fail();
            }
            
            int cooldown = limit.map(SendLimitRecord::cooldownTicks).orElse(DEFAULT_SEND_COOLDOWN_TICKS);
            long last = LAST_SEND_GAME_TIME.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2L);
            long remaining = last + cooldown - gameTime;
            if (remaining > 0) {
                player.sendSystemMessage(Component.translatable("red_envelope.error.cooldown", (remaining + 19) / 20).withStyle(ChatFormatting.RED), true);
                return CreateResult.fail();
            }
        }

        Validation validation = validate(server, stack, payload);
        if (!validation.ok()) {
            player.sendSystemMessage(Component.translatable(validation.translationKey()).withStyle(ChatFormatting.RED), true);
            return CreateResult.fail();
        }

        String title = payload.title().isBlank() ? Component.translatable("red_envelope.default_title").getString() : payload.title().trim();
        String password = payload.propertyType() == SendRedEnvelopePayload.PropertyType.PASSWORD ? payload.propertyValue().trim() : "";
        String exclusive = payload.propertyType() == SendRedEnvelopePayload.PropertyType.EXCLUSIVE ? payload.propertyValue().trim() : "";
        List<String> visible = exclusive.isBlank() ? List.of() : List.of(exclusive);
        RedEnvelopeRecord record = new RedEnvelopeRecord(
                player.getUUID(),
                player.getGameProfile().name(),
                title,
                "",
                stack,
                stack.getCount(),
                payload.propertyType() == SendRedEnvelopePayload.PropertyType.EXCLUSIVE ? 1 : payload.playerCount(),
                payload.lucky(),
                payload.returnWhenExpired(),
                !password.isBlank(),
                password,
                exclusive,
                false,
                visible,
                gameTime,
                systemEnvelope);

        if (!systemEnvelope) {
            getData(server).addEnvelope(record);
        }
        
        LAST_SEND_GAME_TIME.put(player.getUUID(), gameTime);
        broadcastEnvelope(server, record);
        player.sendSystemMessage(Component.translatable("red_envelope.sent", record.title).withStyle(ChatFormatting.GOLD), true);
        return CreateResult.success(record.id);
    }

    private static Validation validate(MinecraftServer server, ItemStack stack, SendRedEnvelopePayload payload) {
        if (stack.isEmpty()) return Validation.fail("red_envelope.error.empty_stack");
        if (payload.playerCount() <= 0 || payload.playerCount() > 256) return Validation.fail("red_envelope.error.bad_number");
        int count = payload.propertyType() == SendRedEnvelopePayload.PropertyType.EXCLUSIVE ? 1 : payload.playerCount();
        if (count > stack.getCount()) return Validation.fail("red_envelope.error.more_players_than_items");
        if (payload.title().length() > 20) return Validation.fail("red_envelope.error.title_too_long");
        if (payload.propertyValue().length() > 30) return Validation.fail("red_envelope.error.property_too_long");
        if (payload.propertyType() == SendRedEnvelopePayload.PropertyType.PASSWORD && payload.propertyValue().startsWith("/")) {
            return Validation.fail("red_envelope.error.password_illegal");
        }

        if (payload.propertyType() == SendRedEnvelopePayload.PropertyType.EXCLUSIVE) {
            String name = payload.propertyValue().trim();
            if (name.isBlank() || server.getPlayerList().getPlayerByName(name) == null) {
                return Validation.fail("red_envelope.error.exclusive_offline");
            }
        }

        return Validation.OK;
    }

    public static void queueClaim(ServerPlayer player, UUID envelopeId, boolean fromPassword) {
        long gameTime = player.server.overworld().getGameTime();
        CLAIM_QUEUE.addLast(new QueuedClaim(player.getUUID(), envelopeId, fromPassword, gameTime));
        player.sendSystemMessage(Component.translatable("red_envelope.claim.queued").withStyle(ChatFormatting.YELLOW), true);
    }

    public static void tick(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        RedEnvelopeSavedData data = getData(server);
        processOneQueuedClaim(server, data, gameTime);
        expireActiveEnvelopes(server, data, gameTime);
        data.pruneOldDestroyed(gameTime);
    }

    private static void processOneQueuedClaim(MinecraftServer server, RedEnvelopeSavedData data, long gameTime) {
        if (CLAIM_QUEUE.isEmpty()) return;
        List<QueuedClaim> ready = new ArrayList<>();
        while (!CLAIM_QUEUE.isEmpty()) {
            QueuedClaim claim = CLAIM_QUEUE.removeFirst();
            if (server.getPlayerList().getPlayer(claim.playerUuid()) != null) {
                ready.add(claim);
            }
        }

        if (ready.isEmpty()) return;
        Collections.shuffle(ready);
        QueuedClaim chosen = ready.removeFirst();
        ready.forEach(CLAIM_QUEUE::addLast);
        ServerPlayer player = server.getPlayerList().getPlayer(chosen.playerUuid());
        if (player != null) {
            claimNow(server, data, player, chosen.envelopeId(), chosen.fromPassword(), gameTime);
        }
    }

    private static void claimNow(MinecraftServer server, RedEnvelopeSavedData data, ServerPlayer player, UUID envelopeId, boolean fromPassword, long gameTime) {
        Optional<RedEnvelopeRecord> optional = data.getEnvelope(envelopeId);
        if (optional.isEmpty()) {
            sendClaimResult(player, envelopeId, false, 0, "red_envelope.claim.not_found");
            return;
        }

        RedEnvelopeRecord record = optional.get();
        if (!record.isVisibleTo(player.getGameProfile().name())) {
            sendClaimResult(player, envelopeId, false, 0, "red_envelope.claim.hidden");
            return;
        }

        if (!record.isActive(gameTime)) {
            sendClaimResult(player, envelopeId, false, 0, "red_envelope.claim.finished");
            syncEnvelope(server, record);
            return;
        }

        if (record.hasClaimed(player.getUUID())) {
            sendClaimResult(player, envelopeId, false, 0, "red_envelope.claim.already");
            return;
        }

        if (record.usePassword && !fromPassword) {
            sendClaimResult(player, envelopeId, false, 0, "red_envelope.claim.need_password");
            return;
        }

        if (!record.exclusiveUser.isBlank() && !record.exclusiveUser.equalsIgnoreCase(player.getGameProfile().name())) {
            sendClaimResult(player, envelopeId, false, 0, "red_envelope.claim.exclusive");
            return;
        }

        int amount = calculateAmount(record);
        ItemStack reward = record.copyStackWithAmount(amount);
        if (!player.getInventory().add(reward.copy())) {
            player.drop(reward, false);
        }

        record.addClaim(player.getUUID(), player.getGameProfile().name(), amount, gameTime);
        data.setDirty();
        sendClaimResult(player, envelopeId, true, amount, "red_envelope.claim.success");
        syncEnvelope(server, record);
    }

    private static int calculateAmount(RedEnvelopeRecord record) {
        int remainingClaims = Math.max(1, record.remainingClaims());
        if (remainingClaims == 1) return Math.max(1, record.remainingAmount);
        if (!record.lucky) {
            return Math.max(1, record.remainingAmount / remainingClaims);
        }

        int max = Math.max(1, (record.remainingAmount / remainingClaims) * 2);
        max = Math.min(max, record.remainingAmount - (remainingClaims - 1));
        return 1 + RANDOM.nextInt(Math.max(1, max));
    }

    private static void expireActiveEnvelopes(MinecraftServer server, RedEnvelopeSavedData data, long gameTime) {
        for (RedEnvelopeRecord record : data.envelopes) {
            if (record.status == RedEnvelopeStatus.ACTIVE && gameTime >= record.expireGameTime) {
                record.status = RedEnvelopeStatus.EXPIRED;
                if (record.returnWhenExpired && record.remainingAmount > 0 && !record.systemEnvelope) {
                    ItemStack returned = record.copyStackWithAmount(record.remainingAmount);
                    ServerPlayer sender = server.getPlayerList().getPlayer(record.senderUuid);
                    if (sender != null) {
                        if (!sender.getInventory().add(returned.copy())) sender.drop(returned, false);
                        sender.sendSystemMessage(Component.translatable("red_envelope.expired.returned", record.title).withStyle(ChatFormatting.GOLD), false);
                    } else {
                        data.addPendingReturn(new PendingReturnRecord(record.senderUuid, record.senderName, returned, gameTime));
                    }
                }

                data.setDirty();
                syncEnvelope(server, record);
            }
        }
    }

    public static void deliverPendingReturns(ServerPlayer player) {
        RedEnvelopeSavedData data = getData(player.server);
        for (PendingReturnRecord record : data.removePendingReturns(player.getUUID())) {
            ItemStack stack = record.stack().copy();
            if (!player.getInventory().add(stack.copy())) player.drop(stack, false);
            player.sendSystemMessage(Component.translatable("red_envelope.pending_return").withStyle(ChatFormatting.GOLD), false);
        }
    }

    public static void syncActiveTo(ServerPlayer player) {
        MinecraftServer server = player.server;
        long gameTime = server.overworld().getGameTime();
        List<RedEnvelopeSnapshot> snapshots = getData(server).envelopes.stream()
                .filter(record -> record.isVisibleTo(player.getGameProfile().name()))
                .filter(record -> record.status == RedEnvelopeStatus.ACTIVE)
                .map(record -> RedEnvelopeSnapshot.of(record, player.getUUID(), gameTime)).toList();
        PacketDistributor.sendToPlayer(player, new RedEnvelopeSyncPayload(snapshots, true));
    }

    public static void broadcastEnvelope(MinecraftServer server, RedEnvelopeRecord record) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (record.isVisibleTo(player.getGameProfile().name())) {
                PacketDistributor.sendToPlayer(player, new RedEnvelopeSyncPayload(List.of(RedEnvelopeSnapshot.of(record, player.getUUID(), server.overworld().getGameTime())), false));
            }
        }
    }

    public static void syncEnvelope(MinecraftServer server, RedEnvelopeRecord record) {
        broadcastEnvelope(server, record);
    }

    public static boolean tryPasswordClaim(ServerPlayer player, String rawText) {
        MinecraftServer server = player.server;
        long gameTime = server.overworld().getGameTime();
        for (RedEnvelopeRecord record : getData(server).envelopes) {
            if (record.isActive(gameTime) && record.usePassword && record.password.equals(rawText) && record.isVisibleTo(player.getGameProfile().name())) {
                queueClaim(player, record.id, true);
                return true;
            }
        }
        
        return false;
    }

    private static void sendClaimResult(ServerPlayer player, UUID id, boolean success, int amount, String translationKey) {
        PacketDistributor.sendToPlayer(player, new ClaimResultPayload(id, success, amount, translationKey));
    }

    private static void sendError(ServerPlayer player, String translationKey) {
        player.sendSystemMessage(Component.translatable(translationKey).withStyle(ChatFormatting.RED), true);
    }

    private static void consumeEmptyEnvelope(ServerPlayer player) {
        if (player.getAbilities().instabuild) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main.is(VWItems.EMPTY_RED_PACKET.get())) {
            main.shrink(1);
        } else if (off.is(VWItems.EMPTY_RED_PACKET.get())) {
            off.shrink(1);
        }
    }

    public static void setPlayerLimit(ServerPlayer executor, ServerPlayer target, int cooldownSeconds, boolean blocked) {
        RedEnvelopeSavedData data = getData(executor.server);
        data.setLimit(new SendLimitRecord(target.getUUID(), target.getGameProfile().name(), Math.max(0, cooldownSeconds) * 20, blocked));
        executor.sendSystemMessage(Component.translatable("red_envelope.permission.updated", target.getGameProfile().name()));
    }

    public record CreateResult(boolean created, UUID id) {
        public static CreateResult success(UUID id) { return new CreateResult(true, id); }
        public static CreateResult fail() { return new CreateResult(false, new UUID(0L, 0L)); }
    }

    private record Validation(boolean ok, String translationKey) {
        private static final Validation OK = new Validation(true, "");
        private static Validation fail(String translationKey) { return new Validation(false, translationKey); }
    }

    private record QueuedClaim(UUID playerUuid, UUID envelopeId, boolean fromPassword, long enqueueGameTime) { }
    
}
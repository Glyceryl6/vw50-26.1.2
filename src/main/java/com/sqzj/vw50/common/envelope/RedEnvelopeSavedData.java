package com.sqzj.vw50.common.envelope;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sqzj.vw50.VW50;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RedEnvelopeSavedData extends SavedData {

    public static final SavedDataType<RedEnvelopeSavedData> TYPE = new SavedDataType<>(
            VW50.prefix("red_envelopes"), RedEnvelopeSavedData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    RedEnvelopeRecord.CODEC.listOf().fieldOf("envelopes").forGetter(data -> data.envelopes),
                    PendingReturnRecord.CODEC.listOf().fieldOf("pending_returns").forGetter(data -> data.pendingReturns),
                    SendLimitRecord.CODEC.listOf().fieldOf("send_limits").forGetter(data -> data.sendLimits)
            ).apply(instance, RedEnvelopeSavedData::new)));

    public final List<RedEnvelopeRecord> envelopes = new ArrayList<>();
    public final List<PendingReturnRecord> pendingReturns = new ArrayList<>();
    public final List<SendLimitRecord> sendLimits = new ArrayList<>();

    public RedEnvelopeSavedData() {}

    public RedEnvelopeSavedData(List<RedEnvelopeRecord> envelopes, List<PendingReturnRecord> pendingReturns, List<SendLimitRecord> sendLimits) {
        this.envelopes.addAll(envelopes);
        this.pendingReturns.addAll(pendingReturns);
        this.sendLimits.addAll(sendLimits);
    }

    public Optional<RedEnvelopeRecord> getEnvelope(UUID id) {
        return this.envelopes.stream().filter(envelope -> envelope.id.equals(id)).findFirst();
    }

    public void addEnvelope(RedEnvelopeRecord record) {
        this.envelopes.add(record);
        this.setDirty();
    }

    public void addPendingReturn(PendingReturnRecord record) {
        this.pendingReturns.add(record);
        this.setDirty();
    }

    public Optional<SendLimitRecord> getLimit(UUID playerUuid) {
        return this.sendLimits.stream().filter(limit -> limit.playerUuid().equals(playerUuid)).findFirst();
    }

    public void setLimit(SendLimitRecord newLimit) {
        this.sendLimits.removeIf(limit -> limit.playerUuid().equals(newLimit.playerUuid()));
        this.sendLimits.add(newLimit);
        this.setDirty();
    }

    public List<PendingReturnRecord> removePendingReturns(UUID playerUuid) {
        List<PendingReturnRecord> removed = new ArrayList<>();
        Iterator<PendingReturnRecord> iterator = this.pendingReturns.iterator();
        while (iterator.hasNext()) {
            PendingReturnRecord record = iterator.next();
            if (record.playerUuid().equals(playerUuid)) {
                removed.add(record);
                iterator.remove();
            }
        }

        if (!removed.isEmpty()) {
            this.setDirty();
        }

        return removed;
    }

    public void pruneOldDestroyed(long gameTime) {
        long cutoff = gameTime - 20L * 60L * 60L * 8L;
        if (this.envelopes.removeIf(envelope -> envelope.status != RedEnvelopeStatus.ACTIVE && envelope.expireGameTime < cutoff)) {
            this.setDirty();
        }
    }

}
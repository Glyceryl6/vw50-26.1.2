package com.sqzj.vw50.server.network;

import com.sqzj.vw50.common.envelope.RedEnvelopeRecord;
import com.sqzj.vw50.common.envelope.RedEnvelopeStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

public record RedEnvelopeSnapshot(
        UUID id,
        String title,
        String sign,
        String senderName,
        String exclusiveUser,
        boolean usePassword,
        String password,
        boolean hidden,
        boolean viewerClaimed,
        int remainingAmount,
        int playerCount,
        int claimedCount,
        int remainingTicks,
        RedEnvelopeStatus status) {

    public static RedEnvelopeSnapshot of(RedEnvelopeRecord record, UUID viewerUuid, long gameTime) {
        int remainingTicks = (int)Math.max(0, record.expireGameTime - gameTime);
        return new RedEnvelopeSnapshot(record.id, record.title, record.sign, record.senderName, record.exclusiveUser,
                record.usePassword, record.password, record.hidden, record.hasClaimed(viewerUuid), record.remainingAmount, record.playerCount,
                record.claims.size(), remainingTicks, record.status);
    }

    public static RedEnvelopeSnapshot decode(RegistryFriendlyByteBuf buf) {
        return new RedEnvelopeSnapshot(
                buf.readUUID(),
                buf.readUtf(64),
                buf.readUtf(64),
                buf.readUtf(32),
                buf.readUtf(32),
                buf.readBoolean(),
                buf.readUtf(64),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                RedEnvelopeStatus.STREAM_CODEC.decode(buf));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.id);
        buf.writeUtf(this.title, 64);
        buf.writeUtf(this.sign, 64);
        buf.writeUtf(this.senderName, 32);
        buf.writeUtf(this.exclusiveUser, 32);
        buf.writeBoolean(this.usePassword);
        buf.writeUtf(this.password, 64);
        buf.writeBoolean(this.hidden);
        buf.writeBoolean(this.viewerClaimed);
        buf.writeVarInt(this.remainingAmount);
        buf.writeVarInt(this.playerCount);
        buf.writeVarInt(this.claimedCount);
        buf.writeVarInt(this.remainingTicks);
        RedEnvelopeStatus.STREAM_CODEC.encode(buf, this.status);
    }

}
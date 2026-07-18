package com.sqzj.vw50.server.network;

import com.sqzj.vw50.common.envelope.ClaimRecord;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

public record ClaimSnapshot(UUID playerUuid, String playerName, int amount) {

    public static ClaimSnapshot of(ClaimRecord record) {
        return new ClaimSnapshot(record.playerUuid(), record.playerName(), record.amount());
    }

    public static ClaimSnapshot decode(RegistryFriendlyByteBuf buf) {
        return new ClaimSnapshot(buf.readUUID(), buf.readUtf(32), buf.readVarInt());
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.playerUuid);
        buf.writeUtf(this.playerName, 32);
        buf.writeVarInt(this.amount);
    }
}

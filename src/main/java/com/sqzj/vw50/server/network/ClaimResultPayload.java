package com.sqzj.vw50.server.network;

import com.sqzj.vw50.VW50;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record ClaimResultPayload(UUID envelopeId, boolean success, int amount, String message) implements CustomPacketPayload {

    public static final Type<ClaimResultPayload> TYPE = new Type<>(VW50.prefix("claim_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimResultPayload> STREAM_CODEC = StreamCodec.ofMember(ClaimResultPayload::encode, ClaimResultPayload::decode);

    public static ClaimResultPayload decode(RegistryFriendlyByteBuf buf) {
        return new ClaimResultPayload(buf.readUUID(), buf.readBoolean(), buf.readVarInt(), buf.readUtf(128));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.envelopeId);
        buf.writeBoolean(this.success);
        buf.writeVarInt(this.amount);
        buf.writeUtf(this.message, 128);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
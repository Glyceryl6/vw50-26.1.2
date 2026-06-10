package com.sqzj.vw50.server.network;

import com.sqzj.vw50.VW50;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record ClaimRedEnvelopePayload(UUID envelopeId) implements CustomPacketPayload {

    public static final Type<ClaimRedEnvelopePayload> TYPE = new Type<>(VW50.prefix("claim_red_envelope"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimRedEnvelopePayload> STREAM_CODEC = StreamCodec.ofMember(
            ClaimRedEnvelopePayload::encode, ClaimRedEnvelopePayload::decode);

    public static ClaimRedEnvelopePayload decode(RegistryFriendlyByteBuf buf) {
        return new ClaimRedEnvelopePayload(buf.readUUID());
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.envelopeId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
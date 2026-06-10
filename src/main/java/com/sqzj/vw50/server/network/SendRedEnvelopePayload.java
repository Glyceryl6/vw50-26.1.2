package com.sqzj.vw50.server.network;

import com.sqzj.vw50.VW50;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SendRedEnvelopePayload(
        String title,
        int playerCount,
        boolean lucky,
        boolean returnWhenExpired,
        PropertyType propertyType,
        String propertyValue) implements CustomPacketPayload {

    public static final Type<SendRedEnvelopePayload> TYPE = new Type<>(VW50.prefix("send_red_envelope"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SendRedEnvelopePayload> STREAM_CODEC = StreamCodec.ofMember(SendRedEnvelopePayload::encode, SendRedEnvelopePayload::decode);

    public static SendRedEnvelopePayload decode(RegistryFriendlyByteBuf buf) {
        return new SendRedEnvelopePayload(
                buf.readUtf(64),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                PropertyType.values()[buf.readVarInt()],
                buf.readUtf(64));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.title, 64);
        buf.writeVarInt(this.playerCount);
        buf.writeBoolean(this.lucky);
        buf.writeBoolean(this.returnWhenExpired);
        buf.writeVarInt(this.propertyType.ordinal());
        buf.writeUtf(this.propertyValue, 64);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum PropertyType {
        NORMAL,
        PASSWORD,
        EXCLUSIVE
    }

}

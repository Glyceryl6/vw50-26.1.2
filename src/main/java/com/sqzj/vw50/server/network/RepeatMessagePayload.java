package com.sqzj.vw50.server.network;

import com.sqzj.vw50.VW50;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RepeatMessagePayload(String message) implements CustomPacketPayload {

    public static final Type<RepeatMessagePayload> TYPE = new Type<>(VW50.prefix("repeat_message"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RepeatMessagePayload> STREAM_CODEC = StreamCodec.ofMember(RepeatMessagePayload::encode, RepeatMessagePayload::decode);

    public static RepeatMessagePayload decode(RegistryFriendlyByteBuf buf) {
        return new RepeatMessagePayload(buf.readUtf(256));
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(this.message, 256);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

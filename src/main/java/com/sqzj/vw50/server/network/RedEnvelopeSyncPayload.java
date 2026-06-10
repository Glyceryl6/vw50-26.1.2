package com.sqzj.vw50.server.network;

import com.sqzj.vw50.VW50;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record RedEnvelopeSyncPayload(List<RedEnvelopeSnapshot> envelopes, boolean clearOld) implements CustomPacketPayload {

    public static final Type<RedEnvelopeSyncPayload> TYPE = new Type<>(VW50.prefix("sync_red_envelopes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RedEnvelopeSyncPayload> STREAM_CODEC = StreamCodec.ofMember(RedEnvelopeSyncPayload::encode, RedEnvelopeSyncPayload::decode);

    public static RedEnvelopeSyncPayload decode(RegistryFriendlyByteBuf buf) {
        boolean clearOld = buf.readBoolean();
        int size = buf.readVarInt();
        List<RedEnvelopeSnapshot> envelopes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            envelopes.add(RedEnvelopeSnapshot.decode(buf));
        }
        return new RedEnvelopeSyncPayload(envelopes, clearOld);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.clearOld);
        buf.writeVarInt(this.envelopes.size());
        for (RedEnvelopeSnapshot envelope : this.envelopes) {
            envelope.encode(buf);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

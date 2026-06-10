package com.sqzj.vw50.server.network;

import com.sqzj.vw50.common.envelope.RedEnvelopeService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ServerPayloadHandler {

    public static void handleSendRedEnvelope(SendRedEnvelopePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            RedEnvelopeService.createFromMenu(player, payload);
        }
    }

    public static void handleClaimRedEnvelope(ClaimRedEnvelopePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            RedEnvelopeService.queueClaim(player, payload.envelopeId(), false);
        }
    }

}
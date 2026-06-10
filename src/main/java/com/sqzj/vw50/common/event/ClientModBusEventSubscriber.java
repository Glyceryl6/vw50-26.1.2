package com.sqzj.vw50.common.event;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.client.ClientRedEnvelopeManager;
import com.sqzj.vw50.client.gui.RedEnvelopeEditScreen;
import com.sqzj.vw50.common.registry.VWMenus;
import com.sqzj.vw50.server.network.ClaimResultPayload;
import com.sqzj.vw50.server.network.RedEnvelopeSyncPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

@EventBusSubscriber(modid = VW50.MOD_ID, value = Dist.CLIENT)
public class ClientModBusEventSubscriber {

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(VWMenus.SEND_RED_ENVELOPE_MENU.get(), RedEnvelopeEditScreen::new);
    }

    @SubscribeEvent
    public static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(RedEnvelopeSyncPayload.TYPE, ClientRedEnvelopeManager::handleSync);
        event.register(ClaimResultPayload.TYPE, ClientRedEnvelopeManager::handleClaimResult);
    }

}
package com.sqzj.vw50.common.event;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.client.gui.RedEnvelopeEditScreen;
import com.sqzj.vw50.common.registry.VWMenus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = VW50.MOD_ID)
public class ModBusEventSubscriber {

    @SubscribeEvent
    public static void registerNetworks(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
    }

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(VWMenus.SEND_RED_ENVELOPE_MENU.get(), RedEnvelopeEditScreen::new);
    }

}
package com.sqzj.vw50.common.event;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.utils.MixinHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;

@EventBusSubscriber(modid = VW50.MOD_ID, value = Dist.CLIENT)
public class ClientEventSubscriber {

    @SubscribeEvent
    public static void onClientSendMessage(ClientChatEvent event) {

    }

    @SubscribeEvent
    public static void onClientPlayerChat(ClientChatReceivedEvent.Player event) {
        MixinHandler.signature = event.getPlayerChatMessage().signature();
    }

}
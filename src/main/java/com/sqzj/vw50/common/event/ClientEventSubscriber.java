package com.sqzj.vw50.common.event;

import com.sqzj.vw50.VW50;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;

@EventBusSubscriber(modid = VW50.MOD_ID, value = Dist.CLIENT)
public class ClientEventSubscriber {

    @SubscribeEvent
    public static void onClientSendMessage(ClientChatEvent event) {

    }

}
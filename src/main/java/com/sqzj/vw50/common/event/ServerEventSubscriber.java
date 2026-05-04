package com.sqzj.vw50.common.event;

import com.sqzj.vw50.VW50;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

@EventBusSubscriber(modid = VW50.MOD_ID)
public class ServerEventSubscriber {

    @SubscribeEvent
    public static void onServerChatSubmitted(ServerChatEvent event) {

    }

}
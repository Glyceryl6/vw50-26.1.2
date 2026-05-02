package com.example.vw50.common.data;

import com.example.vw50.VW50;
import com.example.vw50.common.data.provider.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = VW50.MOD_ID)
public class VWDataGenerator {

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        event.createProvider(VWBlockModelProvider::new);
        event.createProvider(VWItemModelProvider::new);
        event.createProvider(VWLanguageENProvider::new);
        event.createProvider(VWLanguageCNProvider::new);
        event.createProvider(VWRecipeProvider.Runner::new);
    }

}
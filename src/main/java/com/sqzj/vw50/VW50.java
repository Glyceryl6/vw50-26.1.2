package com.sqzj.vw50;

import com.sqzj.vw50.common.registry.VWCreativeModeTabs;
import com.sqzj.vw50.common.registry.VWItems;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import java.util.Locale;

@Mod(VW50.MOD_ID)
public class VW50 {

    public static final String MOD_ID = "vw50";

    public VW50(IEventBus modEventBus) {
        VWCreativeModeTabs.TABS.register(modEventBus);
        VWItems.ITEMS.register(modEventBus);
    }

    public static Identifier prefix(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name.toLowerCase(Locale.ROOT));
    }

}
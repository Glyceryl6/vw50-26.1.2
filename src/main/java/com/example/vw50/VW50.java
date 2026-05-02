package com.example.vw50;

import com.example.vw50.common.registry.VWItems;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

import java.util.Locale;

@Mod(VW50.MOD_ID)
public class VW50 {

    public static final String MOD_ID = "vw50";

    public VW50(IEventBus modEventBus, ModContainer modContainer) {
        VWItems.ITEMS.register(modEventBus);
    }

    public static Identifier prefix(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name.toLowerCase(Locale.ROOT));
    }

}
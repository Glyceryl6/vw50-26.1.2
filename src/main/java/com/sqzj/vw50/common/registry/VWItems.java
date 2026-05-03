package com.sqzj.vw50.common.registry;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.common.items.EmptyRedPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class VWItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VW50.MOD_ID);
    public static final DeferredHolder<Item, ? extends Item> EMPTY_RED_PACKET = register("empty_red_envelope", EmptyRedPacket::new);

    public static DeferredHolder<Item, ? extends Item> register(
            String name, Function<Item.Properties, Item> itemFactory) {
        return register(name, itemFactory, Item.Properties::new);
    }

    public static DeferredHolder<Item, ? extends Item> register(
            String name, Function<Item.Properties, Item> itemFactory, Supplier<Item.Properties> properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, VW50.prefix(name));
        return ITEMS.registerItem(name, itemFactory, () -> properties.get().setId(key));
    }

}
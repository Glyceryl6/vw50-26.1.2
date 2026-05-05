package com.sqzj.vw50.common.registry;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.client.menu.SendRedEnvelopeMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class VWMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, VW50.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<SendRedEnvelopeMenu>> SEND_RED_ENVELOPE_MENU = register(SendRedEnvelopeMenu::new, "send_red_envelope");

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> register(IContainerFactory<T> factory, String name) {
        return MENUS.register(name + "_menu", () -> new MenuType<>(factory, FeatureFlags.DEFAULT_FLAGS));
    }

}
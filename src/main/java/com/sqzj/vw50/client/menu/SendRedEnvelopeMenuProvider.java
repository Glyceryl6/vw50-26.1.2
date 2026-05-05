package com.sqzj.vw50.client.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class SendRedEnvelopeMenuProvider implements MenuProvider {

    private final UUID senderUuid;

    public SendRedEnvelopeMenuProvider(UUID senderUuid) {
        this.senderUuid = senderUuid;
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SendRedEnvelopeMenu(containerId, inventory, this.senderUuid);
    }

}
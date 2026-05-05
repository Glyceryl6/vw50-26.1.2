package com.sqzj.vw50.client.menu;

import com.sqzj.vw50.common.registry.VWMenus;
import com.sqzj.vw50.misc.misc.VWItemStacksResourceHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

import java.util.UUID;

public class SendRedEnvelopeMenu extends AbstractContainerMenu {

    public final StacksResourceHandler<ItemStack, ItemResource> giftSlot = new VWItemStacksResourceHandler(1);
    private final UUID senderUuid;

    public SendRedEnvelopeMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, buf.readUUID());
    }

    public SendRedEnvelopeMenu(int containerId, Inventory inventory, UUID senderUuid) {
        super(VWMenus.SEND_RED_ENVELOPE_MENU.get(), containerId);
        this.addSlot(new ResourceHandlerSlot(this.giftSlot, this.giftSlot::set, 0, 81, 42));
        this.addStandardInventorySlots(inventory, 9, 107);
        this.senderUuid = senderUuid;
    }

    public UUID getSenderUuid() {
        return this.senderUuid;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            this.giftSlot.copyToList().forEach(carried -> dropOrPlaceInInventory(player, carried));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack copyStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            copyStack = originalStack.copy();
            if (slotIndex == 0) {
                if (!this.moveItemStackTo(originalStack, 1, 36, Boolean.FALSE)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(originalStack, copyStack);
            } else {
                if (!this.moveItemStackTo(originalStack , 0, 1, Boolean.FALSE)) {
                    if (slotIndex < 28) {
                        if (!this.moveItemStackTo(originalStack, 28, 36, Boolean.FALSE)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (slotIndex < 36) {
                        if (!this.moveItemStackTo(originalStack, 1, 28, Boolean.FALSE)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (originalStack.getCount() == copyStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, originalStack);
        }

        return copyStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

}
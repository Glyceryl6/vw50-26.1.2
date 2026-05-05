package com.sqzj.vw50.common.items;

import com.sqzj.vw50.client.menu.SendRedEnvelopeMenuProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.function.Consumer;

/** @noinspection deprecation*/
public class EmptyRedEnvelope extends Item {

    public EmptyRedEnvelope(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && !level.isClientSide()) {
            UUID uuid = serverPlayer.getUUID();
            SendRedEnvelopeMenuProvider provider = new SendRedEnvelopeMenuProvider(uuid);
            serverPlayer.openMenu(provider, byteBuf -> byteBuf.writeUUID(uuid));
        }
        
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("item.vw50.empty_red_envelope.tooltips").withStyle(ChatFormatting.GOLD));
    }

}
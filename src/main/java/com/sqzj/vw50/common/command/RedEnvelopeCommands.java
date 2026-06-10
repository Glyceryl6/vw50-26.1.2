package com.sqzj.vw50.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sqzj.vw50.common.envelope.RedEnvelopeRecord;
import com.sqzj.vw50.common.envelope.RedEnvelopeService;
import com.sqzj.vw50.server.network.SendRedEnvelopePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;

public class RedEnvelopeCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vw50").then(Commands.literal("hand").then(Commands.argument("playerCount", IntegerArgumentType.integer(1, 256))
                        .executes(context -> sendHand(context.getSource(), IntegerArgumentType.getInteger(context, "playerCount"), "")).then(Commands.argument("label", StringArgumentType.greedyString())
                                .executes(context -> sendHand(context.getSource(), IntegerArgumentType.getInteger(context, "playerCount"), StringArgumentType.getString(context, "label"))))))
                .then(Commands.literal("history").executes(context -> showHistory(context.getSource())))
                .then(Commands.literal("permission").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("cooldown").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("seconds", IntegerArgumentType.integer(0, 3600))
                                .executes(context -> setLimit(context.getSource(), EntityArgument.getPlayer(context, "player"), IntegerArgumentType.getInteger(context, "seconds"), false)))))
                        .then(Commands.literal("block").then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setLimit(context.getSource(), EntityArgument.getPlayer(context, "player"), 0, true))))
                        .then(Commands.literal("unblock").then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setLimit(context.getSource(), EntityArgument.getPlayer(context, "player"), 0, false))))));
    }

    private static int sendHand(CommandSourceStack source, int playerCount, String label) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("red_envelope.error.player_only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable("red_envelope.error.empty_stack"));
            return 0;
        }

        ItemStack gift = stack.copy();
        SendRedEnvelopePayload payload = new SendRedEnvelopePayload(label, playerCount, true, true, SendRedEnvelopePayload.PropertyType.NORMAL, "");
        RedEnvelopeService.CreateResult result = RedEnvelopeService.create(player, gift, payload, false, false);
        if (result.created() && !player.getAbilities().instabuild) {
            stack.shrink(gift.getCount());
        }

        return result.created() ? 1 : 0;
    }

    private static int showHistory(CommandSourceStack source) {
        var data = RedEnvelopeService.getData(source.getServer());
        source.sendSuccess(() -> Component.translatable("red_envelope.history.header", data.envelopes.size()).withStyle(ChatFormatting.GOLD), false);
        data.envelopes.stream().sorted(Comparator.comparingLong((RedEnvelopeRecord record) -> record.createdGameTime).reversed()).limit(8)
                .forEach(record -> source.sendSuccess(() -> Component.literal(
                        "#" + record.id + " " + record.title + " " + record.status.getSerializedName()
                                + " " + record.claims.size() + "/" + record.playerCount), false));
        return data.envelopes.size();
    }

    private static int setLimit(CommandSourceStack source, ServerPlayer target, int cooldownSeconds, boolean blocked) {
        ServerPlayer executor;
        try {
            executor = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("red_envelope.error.player_only"));
            return 0;
        }
        RedEnvelopeService.setPlayerLimit(executor, target, cooldownSeconds, blocked);
        return 1;
    }
}

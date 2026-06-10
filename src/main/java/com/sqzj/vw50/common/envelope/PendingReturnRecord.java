package com.sqzj.vw50.common.envelope;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public record PendingReturnRecord(UUID playerUuid, String playerName, ItemStack stack, long createdGameTime) {

    public static final Codec<PendingReturnRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_uuid").forGetter(PendingReturnRecord::playerUuid),
            Codec.STRING.fieldOf("player_name").forGetter(PendingReturnRecord::playerName),
            ItemStack.CODEC.fieldOf("stack").forGetter(PendingReturnRecord::stack),
            Codec.LONG.fieldOf("created_game_time").forGetter(PendingReturnRecord::createdGameTime)
    ).apply(instance, PendingReturnRecord::new));
}

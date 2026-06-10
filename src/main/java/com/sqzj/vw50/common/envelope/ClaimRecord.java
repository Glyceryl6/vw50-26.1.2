package com.sqzj.vw50.common.envelope;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record ClaimRecord(UUID playerUuid, String playerName, int amount, long claimedGameTime) {

    public static final Codec<ClaimRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_uuid").forGetter(ClaimRecord::playerUuid),
            Codec.STRING.fieldOf("player_name").forGetter(ClaimRecord::playerName),
            Codec.INT.fieldOf("amount").forGetter(ClaimRecord::amount),
            Codec.LONG.fieldOf("claimed_game_time").forGetter(ClaimRecord::claimedGameTime)
    ).apply(instance, ClaimRecord::new));
}

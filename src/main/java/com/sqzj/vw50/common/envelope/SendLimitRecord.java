package com.sqzj.vw50.common.envelope;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record SendLimitRecord(UUID playerUuid, String playerName, int cooldownTicks, boolean blocked) {

    public static final Codec<SendLimitRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_uuid").forGetter(SendLimitRecord::playerUuid),
            Codec.STRING.fieldOf("player_name").forGetter(SendLimitRecord::playerName),
            Codec.INT.fieldOf("cooldown_ticks").forGetter(SendLimitRecord::cooldownTicks),
            Codec.BOOL.fieldOf("blocked").forGetter(SendLimitRecord::blocked)
    ).apply(instance, SendLimitRecord::new));
}

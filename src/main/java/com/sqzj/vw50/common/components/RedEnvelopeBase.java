package com.sqzj.vw50.common.components;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public record RedEnvelopeBase(Properties properties) {

    public static final Codec<RedEnvelopeBase> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.fieldOf("properties").forGetter(RedEnvelopeBase::properties)).apply(instance, RedEnvelopeBase::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RedEnvelopeBase> STREAM_CODEC = StreamCodec.composite(
            Properties.STREAM_CODEC, RedEnvelopeBase::properties, RedEnvelopeBase::new);

    public static class Properties {

        public static final Codec<Properties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("sender_uuid").forGetter(object -> object.senderUUID),
                Codec.STRING.fieldOf("title").forGetter(object -> object.title),
                Codec.STRING.fieldOf("preset_mark").forGetter(object -> object.presetMark),
                ItemStack.CODEC.listOf().fieldOf("gifts").forGetter(object -> object.gifts),
                LifeCycle.CODEC.fieldOf("life_cycle").forGetter(object -> object.lifeCycle)).apply(instance, Properties::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Properties> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, p -> p.senderUUID, ByteBufCodecs.STRING_UTF8, p -> p.title,
                ByteBufCodecs.STRING_UTF8, p -> p.presetMark, ItemStack.OPTIONAL_LIST_STREAM_CODEC, p -> p.gifts,
                LifeCycle.STREAM_CODEC, p -> p.lifeCycle, Properties::new);

        public UUID senderUUID;
        public String title;
        public String presetMark;
        public List<ItemStack> gifts;
        public LifeCycle lifeCycle;

        public Properties() {
            this(new UUID(0, 0), "", "", Lists.newArrayList(), LifeCycle.NOT_CREATED);
        }

        public Properties(
                UUID senderUUID, String title, String presetMark,
                List<ItemStack> gifts, LifeCycle lifeCycle) {
            this.senderUUID = senderUUID;
            this.title = title;
            this.presetMark = presetMark;
            this.gifts = gifts;
            this.lifeCycle = lifeCycle;
        }

    }

    public enum LifeCycle implements StringRepresentable {

        NOT_CREATED("not_created"),
        ACTIVATING("activating"),
        DESTROYED("destroyed");

        public static final Codec<LifeCycle> CODEC = StringRepresentable.fromEnum(LifeCycle::values);
        public static final StreamCodec<ByteBuf, LifeCycle> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
        private final String name;

        LifeCycle(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

    }

}
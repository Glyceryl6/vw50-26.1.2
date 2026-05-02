package com.sqzj.vw50.common.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RedEnvelopeSettings(Properties properties) {

    public static final Codec<RedEnvelopeSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.fieldOf("properties").forGetter(RedEnvelopeSettings::properties)).apply(instance, RedEnvelopeSettings::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RedEnvelopeSettings> STREAM_CODEC = StreamCodec.composite(
            Properties.STREAM_CODEC, RedEnvelopeSettings::properties, RedEnvelopeSettings::new);

    public static class Properties {

        public static final Codec<Properties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("use_password").forGetter(object -> object.usePassword),
                Codec.STRING.fieldOf("password").forGetter(object -> object.password),
                Codec.BOOL.fieldOf("exclusive").forGetter(object -> object.exclusive),
                Codec.STRING.fieldOf("exclusive_user").forGetter(object -> object.exclusiveUser),
                Codec.BOOL.fieldOf("hide").forGetter(object -> object.hide),
                Codec.BOOL.fieldOf("try_luck").forGetter(object -> object.tryLuck)).apply(instance, Properties::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Properties> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, p -> p.usePassword, ByteBufCodecs.STRING_UTF8, p -> p.password,
                ByteBufCodecs.BOOL, p -> p.exclusive, ByteBufCodecs.STRING_UTF8, p -> p.exclusiveUser,
                ByteBufCodecs.BOOL, p -> p.hide, ByteBufCodecs.BOOL, p -> p.tryLuck, Properties::new);

        public boolean usePassword;
        public String password;
        public boolean exclusive;
        public String exclusiveUser;
        public boolean hide;
        public boolean tryLuck;

        public Properties() {
            this(false, "", false, "", false, false);
        }

        public Properties(
                boolean usePassword, String password, boolean exclusive,
                String exclusiveUser, boolean hide, boolean tryLuck) {
            this.usePassword = usePassword;
            this.password = password;
            this.exclusive = exclusive;
            this.exclusiveUser = exclusiveUser;
            this.hide = hide;
            this.tryLuck = tryLuck;
        }

    }

}
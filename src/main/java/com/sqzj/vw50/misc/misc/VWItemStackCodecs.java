package com.sqzj.vw50.misc.misc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class VWItemStackCodecs {

    public static final MapCodec<ItemStack> MAP_CODEC = MapCodec.recursive("ItemStack", _ -> RecordCodecBuilder.mapCodec(i -> i.group(
            Item.CODEC_WITH_BOUND_COMPONENTS.fieldOf("id").forGetter(ItemStack::typeHolder),
            ExtraCodecs.intRange(1, 256).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(s -> s.components.asPatch())).apply(i, ItemStack::new)));
    public static final Codec<ItemStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(Codec.lazyInitialized(MAP_CODEC::codec))
            .xmap(itemStack -> itemStack.orElse(ItemStack.EMPTY), itemStack -> itemStack.isEmpty() ? Optional.empty() : Optional.of(itemStack));

}
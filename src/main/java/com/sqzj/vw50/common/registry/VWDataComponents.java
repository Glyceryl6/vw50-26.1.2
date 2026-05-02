package com.sqzj.vw50.common.registry;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.common.components.RedEnvelopeBase;
import com.sqzj.vw50.common.components.RedEnvelopeSettings;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** @noinspection NullableProblems*/
public class VWDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPE = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, VW50.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RedEnvelopeBase>> RED_ENVELOPE_BASE = DATA_COMPONENT_TYPE.register("red_envelope_base",
            () -> DataComponentType.<RedEnvelopeBase>builder().persistent(RedEnvelopeBase.CODEC).networkSynchronized(RedEnvelopeBase.STREAM_CODEC).cacheEncoding().build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RedEnvelopeSettings>> RED_ENVELOPE_SETTINGS = DATA_COMPONENT_TYPE.register("red_envelope_settings",
            () -> DataComponentType.<RedEnvelopeSettings>builder().persistent(RedEnvelopeSettings.CODEC).networkSynchronized(RedEnvelopeSettings.STREAM_CODEC).cacheEncoding().build());

}
package com.sqzj.vw50.common.registry;

import com.sqzj.vw50.VW50;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class VWParticleTypes {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, VW50.MOD_ID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RED_ENVELOPE = register("red_envelope");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> YUANBAO = register("yuanbao");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name) {
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(false));
    }

}
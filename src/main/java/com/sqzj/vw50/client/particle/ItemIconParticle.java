package com.sqzj.vw50.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.function.Supplier;

public class ItemIconParticle extends SingleQuadParticle {
    private final Layer layer;

    private ItemIconParticle(ClientLevel level, double x, double y, double z,
                             double xSpeed, double ySpeed, double zSpeed, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.layer = Layer.bySprite(sprite);
        this.setParticleSpeed(xSpeed, ySpeed, zSpeed);
        this.quadSize = 0.25F;
        this.lifetime = 20;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.friction = 1.0F;
    }

    @Override
    protected Layer getLayer() {
        return this.layer;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final Supplier<? extends Item> item;
        private final ItemStackRenderState scratchRenderState = new ItemStackRenderState();

        public Provider(Supplier<? extends Item> item) {
            this.item = item;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            TextureAtlasSprite sprite = this.getSprite(level, random);
            return new ItemIconParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);
        }

        private TextureAtlasSprite getSprite(ClientLevel level, RandomSource random) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.getItemModelResolver().updateForTopItem(
                this.scratchRenderState,
                new ItemStackTemplate(this.item.get()).create(),
                ItemDisplayContext.GROUND,
                level,
                null,
                0
            );
            Material.Baked material = this.scratchRenderState.pickParticleMaterial(random);
            return material != null
                ? material.sprite()
                : minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
        }
    }
}
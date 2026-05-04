package com.sqzj.vw50.common.data.provider;

import com.sqzj.vw50.common.registry.VWItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class VWRecipeProvider extends RecipeProvider {

    public VWRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        this.shapeless(RecipeCategory.MISC, VWItems.EMPTY_RED_PACKET.get())
                .requires(Items.PAPER, 2).requires(Items.RED_DYE)
                .unlockedBy(getHasName(Items.PAPER), this.has(Items.PAPER))
                .save(this.output);
    }

    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput recipeOutput) {
            return new VWRecipeProvider(provider, recipeOutput);
        }

        @Override
        public String getName() {
            return "VW50 Recipes";
        }

    }

}
package com.example.vw50.common.data.provider;

import com.example.vw50.VW50;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.PackOutput;

public class VWBlockModelProvider extends ModelProvider {

    public VWBlockModelProvider(PackOutput output) {
        super(output, VW50.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

    }

}
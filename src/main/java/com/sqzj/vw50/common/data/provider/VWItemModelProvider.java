package com.sqzj.vw50.common.data.provider;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.common.registry.VWItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

public class VWItemModelProvider extends ModelProvider {

    public VWItemModelProvider(PackOutput output) {
        super(output, VW50.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(VWItems.EMPTY_RED_PACKET.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(VWItems.YUANBAO.get(), ModelTemplates.FLAT_ITEM);
    }

}
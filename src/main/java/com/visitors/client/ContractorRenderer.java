package com.visitors.client;

import com.visitors.VisitorsMod;
import com.visitors.entity.ContractorEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ContractorRenderer extends MobRenderer<ContractorEntity, PlayerModel<ContractorEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(VisitorsMod.MOD_ID,
            "textures/entity/elegant.png");

    public ContractorRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ContractorEntity entity) {
        return TEXTURE;
    }
}

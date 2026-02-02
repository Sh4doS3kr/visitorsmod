package com.visitors.client;

import com.visitors.VisitorsMod;
import com.visitors.entity.VisitorEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer para la entidad Visitor usando el modelo de Villager.
 */
public class VisitorRenderer extends MobRenderer<VisitorEntity, PlayerModel<VisitorEntity>> {

        private static final ResourceLocation TEXTURE = new ResourceLocation(VisitorsMod.MOD_ID,
                        "textures/entity/visitor_custom.png");

        private static final ResourceLocation CHICKEN_LEG_ICON = new ResourceLocation(VisitorsMod.MOD_ID,
                        "textures/gui/chicken_leg_icon.png");
        private static final ResourceLocation ALERT_ICON = new ResourceLocation(VisitorsMod.MOD_ID,
                        "textures/gui/alert_icon.png");

        public VisitorRenderer(EntityRendererProvider.Context context) {
                super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        }

        @Override
        public ResourceLocation getTextureLocation(VisitorEntity entity) {
                return TEXTURE;
        }

        @Override
        public void render(VisitorEntity entity, float entityYaw, float partialTicks,
                        com.mojang.blaze3d.vertex.PoseStack poseStack,
                        net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight) {
                super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

                if (entity.isEscaping()) {
                        renderIcon(entity, poseStack, buffer, packedLight, ALERT_ICON);
                } else if (entity.isHungry()) {
                        renderIcon(entity, poseStack, buffer, packedLight, CHICKEN_LEG_ICON);
                }
        }

        private void renderIcon(VisitorEntity entity, com.mojang.blaze3d.vertex.PoseStack poseStack,
                        net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight,
                        ResourceLocation icon) {
                poseStack.pushPose();
                poseStack.translate(0.0D, entity.getBbHeight() + 0.5F, 0.0D);
                poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
                poseStack.scale(-0.025F, -0.025F, 0.025F);

                com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer = buffer
                                .getBuffer(net.minecraft.client.renderer.RenderType.text(icon));

                float size = 16.0F; // Size of icon
                float x = -size / 2;
                float y = -size / 2;

                // Draw quad
                // matrix, x, y, z, u, v, color(r,g,b,a), light
                org.joml.Matrix4f matrix4f = poseStack.last().pose();

                vertexConsumer.vertex(matrix4f, x, y + size, 0).color(1.0F, 1.0F, 1.0F, 1.0F).uv(0, 1).uv2(packedLight)
                                .endVertex();
                vertexConsumer.vertex(matrix4f, x + size, y + size, 0).color(1.0F, 1.0F, 1.0F, 1.0F).uv(1, 1)
                                .uv2(packedLight)
                                .endVertex();
                vertexConsumer.vertex(matrix4f, x + size, y, 0).color(1.0F, 1.0F, 1.0F, 1.0F).uv(1, 0).uv2(packedLight)
                                .endVertex();
                vertexConsumer.vertex(matrix4f, x, y, 0).color(1.0F, 1.0F, 1.0F, 1.0F).uv(0, 0).uv2(packedLight)
                                .endVertex();

                poseStack.popPose();
        }
}

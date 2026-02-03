package com.visitors.client;

import com.visitors.VisitorsMod;
import com.visitors.entity.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VisitorsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
                event.registerEntityRenderer(ModEntities.VISITOR.get(), VisitorRenderer::new);
                event.registerEntityRenderer(ModEntities.TRASH.get(), TrashRenderer::new);
                event.registerEntityRenderer(ModEntities.CONTRACTOR.get(), ContractorRenderer::new);
                event.registerEntityRenderer(ModEntities.INSPECTOR.get(), InspectorRenderer::new);
                event.registerEntityRenderer(ModEntities.CLEANER.get(), CleanerRenderer::new);
        }
}

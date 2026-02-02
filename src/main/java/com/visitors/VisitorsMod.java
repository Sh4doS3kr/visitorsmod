package com.visitors;

import com.visitors.command.ModCommands;
import com.visitors.entity.ModEntities;
import com.visitors.entity.VisitorEntity;
import com.visitors.network.ModMessages;
import com.visitors.manager.VisitorSpawnManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VisitorsMod.MOD_ID)
public class VisitorsMod {
    public static final String MOD_ID = "visitors";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public VisitorsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register entities
        ModEntities.ENTITIES.register(modEventBus);
        com.visitors.item.ModItems.register(modEventBus);
        com.visitors.block.ModBlocks.register(modEventBus);

        // Register setup events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onAttributeCreate);

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(VisitorSpawnManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(ModCommands.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModMessages.register();
        LOGGER.info("Visitors Mod initialized!");
    }

    private void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.VISITOR.get(), VisitorEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Visitors Mod: Server starting, spawn manager ready!");
    }
}

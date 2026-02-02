package com.visitors.network;

import com.visitors.VisitorsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(VisitorsMod.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(S2CRatingSyncPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CRatingSyncPacket::new)
                .encoder(S2CRatingSyncPacket::toBytes)
                .consumerMainThread(S2CRatingSyncPacket::handle)
                .add();

        net.messageBuilder(S2CPerformancePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CPerformancePacket::new)
                .encoder(S2CPerformancePacket::toBytes)
                .consumerMainThread(S2CPerformancePacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAllClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}

package com.visitors.network;

import com.visitors.client.RatingHUDOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CManagementSyncPacket {
    private final long nextInspection;
    private final long nextContractor;
    private final boolean isClosed;

    public S2CManagementSyncPacket(long nextInspection, long nextContractor, boolean isClosed) {
        this.nextInspection = nextInspection;
        this.nextContractor = nextContractor;
        this.isClosed = isClosed;
    }

    public S2CManagementSyncPacket(FriendlyByteBuf buf) {
        this.nextInspection = buf.readLong();
        this.nextContractor = buf.readLong();
        this.isClosed = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeLong(nextInspection);
        buf.writeLong(nextContractor);
        buf.writeBoolean(isClosed);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client side
            RatingHUDOverlay.setManagementData(nextInspection, nextContractor, isClosed);
        });
        return true;
    }
}

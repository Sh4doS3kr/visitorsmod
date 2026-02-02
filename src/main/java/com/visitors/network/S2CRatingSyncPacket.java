package com.visitors.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CRatingSyncPacket {
    private final float rating;
    private final int count;

    public S2CRatingSyncPacket(float rating, int count) {
        this.rating = rating;
        this.count = count;
    }

    public S2CRatingSyncPacket(FriendlyByteBuf buf) {
        this.rating = buf.readFloat();
        this.count = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(rating);
        buf.writeInt(count);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client-side handling
            com.visitors.client.RatingHUDOverlay.setRating(rating, count);
        });
        return true;
    }
}

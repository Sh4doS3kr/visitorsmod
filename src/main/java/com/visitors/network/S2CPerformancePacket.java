package com.visitors.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.visitors.client.RatingHUDOverlay;

import java.util.function.Supplier;

public class S2CPerformancePacket {
    private final float tps;
    private final float mspt;

    public S2CPerformancePacket(float tps, float mspt) {
        this.tps = tps;
        this.mspt = mspt;
    }

    public S2CPerformancePacket(FriendlyByteBuf buf) {
        this.tps = buf.readFloat();
        this.mspt = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(tps);
        buf.writeFloat(mspt);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            RatingHUDOverlay.setPerformance(tps, mspt);
        });
        return true;
    }
}

package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class InfrastructurePower {
    private InfrastructurePower() {
    }

    public static boolean bypassed(Level level) {
        return !MobileCellNetworkConfig.REQUIRE_ENERGY.get()
                || level instanceof ServerLevel serverLevel
                && NetworkSavedData.get(serverLevel.getServer().overworld()).isPowerTestEnabled();
    }
}

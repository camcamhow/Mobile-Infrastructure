package com.example.mobilecellnetwork.plans;

import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PlanUsage;
import com.example.mobilecellnetwork.billing.BillingManager;
import com.example.mobilecellnetwork.network.PhoneNotificationPacket;
import com.example.mobilecellnetwork.network.CoverageEngine;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class DataUsageService {
    private DataUsageService() {
    }

    public static boolean spend(ServerPlayer player, double megabytes) {
        if (!Double.isFinite(megabytes) || megabytes < 0.0D) {
            throw new IllegalArgumentException("Data usage must be a finite, non-negative amount.");
        }
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        if (!usage.isCycleActive()) {
            notify(player, "Your plan is suspended.");
            return false;
        }
        PlanRegistry.Plan plan = PlanRegistry.getPlan(usage.getActivePlanId());
        boolean roaming = CoverageEngine.isRoamingConnection(player);
        if (roaming && !plan.allowRoaming()) {
            notify(player, "Your plan does not allow roaming.");
            return false;
        }
        long allowance = plan.dataAllowanceMb();
        double used = usage.getDataUsedMb() + usage.getPartialDataUsedMb();
        if (!plan.allowOverage() && allowance >= 0 && used + megabytes > allowance) {
            notify(player, "Data limit reached");
            return false;
        }

        long wholeMegabytes = (long) Math.floor(usage.getPartialDataUsedMb() + megabytes);
        BillingManager.ConnectionType connectionType = BillingManager.getConnectionType(player, plan);
        var agreement = BillingManager.getRoamingAgreement(player, plan);
        long charges = BillingManager.computeCharges(player.getData(ModCapabilities.PHONE_DATA), usage,
                0L, 0L, wholeMegabytes, connectionType, agreement, plan);
        long roamingCharges = BillingManager.computeRoamingCharges(0L, 0L, wholeMegabytes,
                connectionType, agreement, plan);
        String hostNetworkId = BillingManager.getRoamingHostNetworkId(player);
        if (!BillingManager.chargeUsage(player, charges, roamingCharges, "Data usage", hostNetworkId)) {
            return false;
        }
        usage.addDataUsageMb(megabytes);
        com.example.mobilecellnetwork.network.NetworkHandler.syncPlanData(player);
        if (allowance > 0 && used < allowance * 0.9D
                && usage.getDataUsedMb() + usage.getPartialDataUsedMb() >= allowance * 0.9D) {
            notify(player, "You have used at least 90% of your data allowance.");
        }
        return true;
    }

    private static void notify(ServerPlayer player, String message) {
        PacketDistributor.sendToPlayer(player,
                new PhoneNotificationPacket("PLAN_WARNING", "Plan warning", message, false));
    }
}

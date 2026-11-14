package com.example.mobilecellnetwork.plans;

import com.example.mobilecellnetwork.capabilities.PlanUsage;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.billing.BillingManager;
import com.example.mobilecellnetwork.network.NetworkHandler;
import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.util.CurrencyUtil;
import net.minecraft.server.level.ServerPlayer;

public final class PlanSubscriptionHandler {
    public enum SelectPlanResult { SUCCESS, INSUFFICIENT_FUNDS, PLAN_NOT_FOUND, PLAN_ALREADY_ACTIVE }

    public enum CancelPlanResult { SUCCESS }

    private PlanSubscriptionHandler() {
    }

    /** Formats a price using the currency symbol. */
    public static String formatCurrency(long amount) {
        return CurrencyUtil.format(amount);
    }

    public static SelectPlanResult selectPlan(ServerPlayer player, String planId) {
        PlanRegistry.Plan plan = planId == null ? null : PlanRegistry.findPlan(planId);
        if (plan == null) {
            return SelectPlanResult.PLAN_NOT_FOUND;
        }
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        PlanRegistry.Plan previous = usage.getActivePlanId().isBlank()
                ? null : PlanRegistry.findPlan(usage.getActivePlanId());
        if (usage.getActivePlanId().equals(plan.id())) {
            return SelectPlanResult.PLAN_ALREADY_ACTIVE;
        }
        if (player.getData(ModCapabilities.BANK_ACCOUNT).getBalance() < plan.price()) {
            return SelectPlanResult.INSUFFICIENT_FUNDS;
        }
        BillingManager.resetPlanRevenueCycles(player, previous, plan);
        if (!BillingManager.chargePlanSubscription(player, plan)) {
            return SelectPlanResult.INSUFFICIENT_FUNDS;
        }

        long day = Math.floorDiv(player.serverLevel().getDayTime(), 24_000L);
        usage.setActivePlanId(plan.id());
        usage.resetDailyCounters(day);
        usage.setCycleStartDay(day);
        usage.setCycleActive(true);
        player.getData(ModCapabilities.PHONE_DATA).setLastKnownPlanId(plan.id());
        player.getData(ModCapabilities.PHONE_DATA).setHomeNetworkId(plan.carrierNetworkId());
        com.example.mobilecellnetwork.network.PhoneDeviceService.saveActiveProfile(player);
        NetworkSavedData.get(player.server.overworld()).setPlanAssignment(player.getUUID(), plan.id());
        NetworkHandler.syncPlanData(player);
        NetworkHandler.syncPhoneData(player);
        return SelectPlanResult.SUCCESS;
    }

    public static CancelPlanResult cancelPlan(ServerPlayer player) {
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        PlanRegistry.Plan previous = usage.getActivePlanId().isBlank()
                ? null : PlanRegistry.findPlan(usage.getActivePlanId());
        if (previous != null) {
            BillingManager.resetPlanRevenueCycles(player, previous, null);
        }
        usage.setActivePlanId("");
        usage.setPlanCanceled(true);
        usage.setCycleActive(false);
        player.getData(ModCapabilities.PHONE_DATA).setLastKnownPlanId("");
        player.getData(ModCapabilities.PHONE_DATA).setHomeNetworkId("");
        com.example.mobilecellnetwork.network.PhoneDeviceService.saveActiveProfile(player);
        NetworkSavedData.get(player.server.overworld()).clearPlanAssignment(player.getUUID());
        com.example.mobilecellnetwork.voice.CallManager.hangUp(player.getUUID());
        NetworkHandler.syncPlanData(player);
        NetworkHandler.syncPhoneData(player);
        return CancelPlanResult.SUCCESS;
    }
}

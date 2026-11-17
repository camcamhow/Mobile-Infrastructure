package com.example.mobilecellnetwork.billing;

import com.example.mobilecellnetwork.capabilities.BankAccount;
import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PlanUsage;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.data.NetworkSettings;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.data.RoamingAgreement;
import com.example.mobilecellnetwork.network.CoverageEngine;
import com.example.mobilecellnetwork.network.NetworkHandler;
import com.example.mobilecellnetwork.network.PhoneNotificationPacket;
import com.example.mobilecellnetwork.plans.PlanRegistry;
import com.example.mobilecellnetwork.util.CurrencyUtil;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BillingManager {
    private static final int BILLING_INTERVAL_TICKS = 1_200;

    private BillingManager() {
    }

    public static void init() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % BILLING_INTERVAL_TICKS != 0) {
            return;
        }
        NetworkSavedData data = NetworkSavedData.get(event.getServer().overworld());
        java.util.Set<UUID> online = new java.util.HashSet<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            processPlayer(player, false);
        }
        if (MobileCellNetworkConfig.BILLING_ENABLED.get()) {
            data.getPlanAssignments().keySet().stream()
                    .filter(playerId -> !online.contains(playerId))
                    .forEach(data::queueBillingCheck);
        }
    }

    public static void onPlayerLogin(ServerPlayer player) {
        NetworkSavedData savedData = savedData(player);
        boolean wasQueued = savedData.takeBillingCheck(player.getUUID());
        if (!MobileCellNetworkConfig.BILLING_ENABLED.get() && !wasQueued) {
            return;
        }
        processPlayer(player, false);
    }

    public enum ConnectionType {
        HOME,
        SHARED,
        ROAMING
    }

    public static long computeCharges(PhoneData phone, PlanUsage usage,
                                      long minutes, long texts, long mb,
                                      ConnectionType connectionType,
                                      RoamingAgreement agreement,
                                      PlanRegistry.Plan plan) {
        if (minutes < 0L || texts < 0L || mb < 0L) {
            throw new IllegalArgumentException("Usage increments must be non-negative.");
        }
        if (usage == null || plan == null) {
            return 0L;
        }

        long charges = computeRoamingCharges(minutes, texts, mb, connectionType, agreement, plan);
        if (plan.allowOverage()) {
            charges = addSaturated(charges, multiplySaturated(
                    incrementalOverage(usage.getMinutesUsed(), minutes, plan.callMinutes()),
                    plan.overageRatePerMinute()));
            charges = addSaturated(charges, multiplySaturated(
                    incrementalOverage(usage.getTextsUsed(), texts, plan.textMessages()),
                    plan.overageRatePerText()));
            charges = addSaturated(charges, multiplySaturated(
                    incrementalOverage(usage.getDataUsedMb(), mb, plan.dataAllowanceMb()),
                    plan.overageRatePerMb()));
        }
        return charges;
    }

    public static long computeRoamingCharges(long minutes, long texts, long mb,
                                             ConnectionType connectionType,
                                             RoamingAgreement agreement,
                                             PlanRegistry.Plan plan) {
        if (minutes < 0L || texts < 0L || mb < 0L) {
            throw new IllegalArgumentException("Usage increments must be non-negative.");
        }
        if (connectionType != ConnectionType.ROAMING || agreement == null || plan == null
                || plan.roamingMode() != PlanRegistry.RoamingMode.CHARGED
                || !MobileCellNetworkConfig.ROAMING_FEE_ENABLED.get()) {
            return 0L;
        }
        long charges = multiplySaturated(minutes, agreement.ratePerMinute());
        charges = addSaturated(charges, multiplySaturated(texts, agreement.ratePerText()));
        return addSaturated(charges, multiplySaturated(mb, agreement.ratePerMb()));
    }

    public static String getRoamingHostNetworkId(ServerPlayer player) {
        var antenna = CoverageEngine.getStrongestAntennaFor(player);
        return antenna == null ? "" : antenna.getNetworkId();
    }

    private static long incrementalOverage(long used, long increment, long allowance) {
        if (allowance < 0L || increment == 0L) {
            return 0L;
        }
        long before = Math.max(0L, used - allowance);
        long after = Math.max(0L, addSaturated(used, increment) - allowance);
        return Math.max(0L, after - before);
    }

    private static long multiplySaturated(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
    }

    private static long addSaturated(long left, long right) {
        return right > 0L && left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
    }

    public static RoamingAgreement getRoamingAgreement(ServerPlayer player, PlanRegistry.Plan plan) {
        if (plan == null || plan.carrierNetworkId().isBlank()) {
            return null;
        }
        var antenna = CoverageEngine.getStrongestAntennaFor(player);
        if (antenna == null) {
            return null;
        }
        NetworkSettings settings = savedData(player).getNetworkSettings(antenna.getNetworkId());
        RoamingAgreement agreement = settings == null
                ? null : settings.getAgreements().get(plan.carrierNetworkId());
        if (agreement != null) {
            return agreement;
        }
        Long legacyRate = savedData(player).getRoamingAgreementFee(
                antenna.getNetworkId(), plan.carrierNetworkId());
        return legacyRate == null ? null : new RoamingAgreement(
                plan.carrierNetworkId(), legacyRate, 0L, plan.roamingFeePerMb(), 0L);
    }

    public static ConnectionType getConnectionType(ServerPlayer player, PlanRegistry.Plan plan) {
        if (CoverageEngine.isRoamingConnection(player)) {
            return ConnectionType.ROAMING;
        }
        String hostNetworkId = getRoamingHostNetworkId(player);
        return plan != null && !plan.carrierNetworkId().isBlank()
                && !plan.carrierNetworkId().equals(hostNetworkId)
                ? ConnectionType.SHARED : ConnectionType.HOME;
    }

    public static boolean chargeUsage(ServerPlayer player, long charges, long roamingCharges,
                                      String reason, String hostNetworkId) {
        if (charges < 0L || roamingCharges < 0L || roamingCharges > charges) {
            throw new IllegalArgumentException("Usage charges are invalid.");
        }
        if (charges == 0L) {
            return true;
        }
        BankAccount account = player.getData(ModCapabilities.BANK_ACCOUNT);
        if (account.getBalance() < charges) {
            notify(player, "PLAN_WARNING", "Payment failed",
                    "Insufficient funds - usage was blocked");
            return false;
        }
        long nonRoamingCharges = charges - roamingCharges;
        if (roamingCharges > 0L
                && !account.withdraw(roamingCharges, "Roaming charge: " + roamingCharges)) {
            return false;
        }
        String overageType = switch (reason) {
            case "Call usage" -> "call minutes";
            case "SMS usage" -> "text messages";
            case "Data usage" -> "data MB";
            default -> "usage";
        };
        if (nonRoamingCharges > 0L
                && !account.withdraw(nonRoamingCharges, "Overage: " + overageType)) {
            return false;
        }
        NetworkHandler.syncBankAccount(player);
        if (roamingCharges > 0L && hostNetworkId != null && !hostNetworkId.isBlank()) {
            CoverageEngine.recordRoamingFee(player, roamingCharges);
            distributeRoamingRevenue(player, roamingCharges, reason, hostNetworkId);
        }
        if (nonRoamingCharges > 0L) {
            PlanRegistry.Plan plan = PlanRegistry.getPlan(
                    player.getData(ModCapabilities.PLAN_USAGE).getActivePlanId());
            savedData(player).creditRevenue(plan.carrierNetworkId(), nonRoamingCharges,
                    player.getGameProfile().getName() + " - overage " + overageType);
        }
        return true;
    }

    public static boolean chargePlanSubscription(ServerPlayer player, PlanRegistry.Plan plan) {
        if (plan == null) {
            return false;
        }
        BankAccount account = player.getData(ModCapabilities.BANK_ACCOUNT);
        if (!account.withdraw(plan.price(), "Plan subscription: " + plan.id())) {
            return false;
        }
        recordPlanSubscriptionRevenue(player, plan);
        NetworkHandler.syncBankAccount(player);
        return true;
    }

    public static void resetPlanRevenueCycles(ServerPlayer player, PlanRegistry.Plan previous,
                                              PlanRegistry.Plan next) {
        NetworkSavedData data = savedData(player);
        long currentDay = currentMinecraftDay(player.server);
        java.util.Set<String> networkIds = new java.util.LinkedHashSet<>();
        if (previous != null && previous.carrierNetworkId() != null
                && !previous.carrierNetworkId().isBlank()) {
            networkIds.add(previous.carrierNetworkId());
        }
        if (next != null && next.carrierNetworkId() != null && !next.carrierNetworkId().isBlank()) {
            networkIds.add(next.carrierNetworkId());
        }
        networkIds.stream().filter(id -> data.getNetwork(id) != null)
                .forEach(id -> data.resetRevenueThisCycle(id, currentDay));
    }

    public static void recordPlanSubscriptionRevenue(ServerPlayer player, PlanRegistry.Plan plan) {
        if (plan == null || plan.price() <= 0L) {
            return;
        }
        NetworkSavedData data = savedData(player);
        data.creditRevenue(plan.carrierNetworkId(), plan.price(),
                "Subscription from " + player.getGameProfile().getName());
        data.addRevenueThisCycle(plan.carrierNetworkId(), plan.price());
    }

    public static void onPlayerLogout(ServerPlayer player) {
        savedData(player).queueBillingCheck(player.getUUID());
    }

    public static boolean processPlayer(ServerPlayer player, boolean force) {
        if (!MobileCellNetworkConfig.BILLING_ENABLED.get() && !force) {
            return false;
        }
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        if (!usage.isCycleActive() && !force) {
            return false;
        }
        long currentDay = currentMinecraftDay(player.server);
        long daysElapsed = currentDay - usage.getCycleStartDay();
        PlanRegistry.Plan plan = PlanRegistry.getPlan(usage.getActivePlanId());
        long daysUntilRenewal = (long) plan.billingCycleDays() - daysElapsed;
        sendRenewalWarning(player, usage, daysUntilRenewal);
        if (!force && daysElapsed < plan.billingCycleDays()) {
            return false;
        }
        renew(player, usage, plan, currentDay);
        return true;
    }

    public static boolean chargeRoamingFee(ServerPlayer player, long fee, String reason) {
        if (fee < 0L) {
            return false;
        }
        if (!MobileCellNetworkConfig.ROAMING_FEE_ENABLED.get() || fee == 0L) {
            return true;
        }
        return chargeUsage(player, fee, fee, reason, getRoamingHostNetworkId(player));
    }

    public static void distributeRoamingRevenue(ServerPlayer player, long fee, String reason,
                                                String hostNetworkId) {
        if (fee <= 0L || hostNetworkId == null || hostNetworkId.isBlank()) {
            return;
        }
        NetworkSavedData data = savedData(player);
        PlanRegistry.Plan plan = PlanRegistry.getPlan(player.getData(ModCapabilities.PLAN_USAGE)
                .getActivePlanId());
        String planNetworkId = plan.carrierNetworkId();
        double homeFraction = MobileCellNetworkConfig.ROAMING_REVENUE_SPLIT_HOME.get();
        long homeShare = Math.min(fee, (long) Math.floor(fee * homeFraction));
        long hostShare = fee - homeShare;
        boolean homeExists = !planNetworkId.isBlank() && data.getNetwork(planNetworkId) != null;
        boolean hostExists = data.getNetwork(hostNetworkId) != null;
        String playerName = player.getGameProfile().getName();

        if (planNetworkId.equals(hostNetworkId) && homeExists) {
            data.creditRevenue(planNetworkId, fee, "Roaming revenue from " + playerName);
            data.addRevenueThisCycle(planNetworkId, fee);
        } else if (homeExists && hostExists) {
            data.creditRevenue(planNetworkId, homeShare, "Roaming from " + playerName);
            data.creditRevenue(hostNetworkId, hostShare, "Roaming revenue from " + playerName);
            data.addRevenueThisCycle(hostNetworkId, hostShare);
        } else if (homeExists) {
            data.creditRevenue(planNetworkId, fee, "Roaming from " + playerName);
        } else if (hostExists) {
            data.creditRevenue(hostNetworkId, fee, "Roaming revenue from " + playerName);
            data.addRevenueThisCycle(hostNetworkId, fee);
        }
    }

    private static void sendRenewalWarning(ServerPlayer player, PlanUsage usage, long daysUntilRenewal) {
        java.util.Set<Integer> reminderDays = new java.util.LinkedHashSet<>(
                MobileCellNetworkConfig.RENEWAL_REMINDER_DAYS.get());
        int legacyWarningDays = MobileCellNetworkConfig.DAYS_BEFORE_RENEWAL_WARNING.get();
        if (reminderDays.isEmpty() && legacyWarningDays > 0) {
            reminderDays.add(legacyWarningDays);
        }
        if (daysUntilRenewal <= 0L || daysUntilRenewal > Integer.MAX_VALUE
                || !reminderDays.contains((int) daysUntilRenewal)
                || !usage.markRenewalReminderSent((int) daysUntilRenewal)) {
            return;
        }
        int days = (int) daysUntilRenewal;
        notify(player, "PLAN_WARNING", "Plan renewal",
                days == 1 ? "Your plan renews tomorrow"
                        : "Your plan renews in " + days + " days");
    }

    private static void renew(ServerPlayer player, PlanUsage usage, PlanRegistry.Plan plan, long currentDay) {
        BankAccount account = player.getData(ModCapabilities.BANK_ACCOUNT);
        if (account.getBalance() >= plan.price()) {
            resetPlanRevenueCycles(player, plan, plan);
        }
        if (chargePlanSubscription(player, plan)) {
            usage.resetDailyCounters(currentDay);
            usage.setCycleStartDay(currentDay);
            usage.setCycleActive(true);
            usage.setRenewalWarningCycleStartDay(Long.MIN_VALUE);
            notify(player, "PLAN_WARNING", "Plan renewed",
                    "Plan renewed - charged " + CurrencyUtil.format(plan.price()));
            NetworkHandler.syncPhoneData(player);
            NetworkHandler.syncPlanData(player);
            return;
        }

        notify(player, "PLAN_WARNING", "Renewal failed", "Renewal failed - insufficient funds");
        if (plan.autoRenew() && MobileCellNetworkConfig.DOWNGRADE_ON_FAILED_RENEWAL.get()) {
            PlanRegistry.Plan defaultPlan = PlanRegistry.getDefaultPlan();
            resetPlanRevenueCycles(player, plan, defaultPlan);
            usage.setActivePlanId(defaultPlan.id());
            usage.resetDailyCounters(currentDay);
            usage.setCycleStartDay(currentDay);
            usage.setCycleActive(true);
            usage.setRenewalWarningCycleStartDay(Long.MIN_VALUE);
            player.getData(ModCapabilities.PHONE_DATA).setLastKnownPlanId(defaultPlan.id());
            savedData(player).setPlanAssignment(player.getUUID(), defaultPlan.id());
            notify(player, "PLAN_WARNING", "Plan expired", "Plan expired - downgraded to default");
        } else {
            usage.setCycleActive(false);
        }
        NetworkHandler.syncPhoneData(player);
        NetworkHandler.syncPlanData(player);
    }

    public static void sendHistory(ServerPlayer target, net.minecraft.commands.CommandSourceStack source) {
        var transactions = target.getData(ModCapabilities.BANK_ACCOUNT).getTransactionHistory();
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "Billing history for " + target.getGameProfile().getName() + ":"), false);
        transactions.stream().filter(transaction -> transaction.reason().startsWith("Plan subscription:")
                        || transaction.reason().startsWith("Plan renewal:"))
                .sorted(java.util.Comparator.comparingLong(BankAccount.Transaction::timestamp).reversed())
                .limit(20).forEach(transaction -> source.sendSuccess(() ->
                        net.minecraft.network.chat.Component.literal(
                                transaction.reason() + " | " + CurrencyUtil.format(transaction.amount()) + " | "
                                        + transaction.timestamp()), false));
    }

    private static void notify(ServerPlayer player, String category, String title, String message) {
        PacketDistributor.sendToPlayer(player,
                new PhoneNotificationPacket(category, title, message, false));
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
    }

    private static long currentMinecraftDay(MinecraftServer server) {
        return Math.floorDiv(server.overworld().getDayTime(), 24_000L);
    }

    private static NetworkSavedData savedData(ServerPlayer player) {
        return NetworkSavedData.get(player.serverLevel().getServer().overworld());
    }
}

package com.example.mobilecellnetwork.event;

import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import com.example.mobilecellnetwork.capabilities.PlanUsage;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.plans.PlanRegistry;
import com.example.mobilecellnetwork.registry.ModDataComponents;
import com.example.mobilecellnetwork.registry.ModItems;
import com.example.mobilecellnetwork.voice.CallManager;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerDataEvents {
    private PlayerDataEvents() {
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        com.example.mobilecellnetwork.network.PhoneDeviceService.onPlayerLogin(player);

        var overworld = player.serverLevel().getServer().overworld();
        NetworkSavedData savedData = NetworkSavedData.get(overworld);
        PhoneData phoneData = player.getData(ModCapabilities.PHONE_DATA);
        phoneData.setLowPowerPromptShownThisSession(false);
        PlanUsage planUsage = player.getData(ModCapabilities.PLAN_USAGE);
        String registeredNumber = savedData.getNumberFor(player.getUUID());
        if (registeredNumber == null) {
            registeredNumber = issuePhoneNumber(savedData, player);
        }
        phoneData.setPhoneNumber(registeredNumber);

        if (!planUsage.getActivePlanId().isBlank()
                && PlanRegistry.findPlan(planUsage.getActivePlanId()) == null) {
            planUsage.setActivePlanId("");
        }
        if (planUsage.getActivePlanId().isBlank() && planUsage.isPlanCanceled()) {
            phoneData.setLastKnownPlanId("");
            savedData.clearPlanAssignment(player.getUUID());
        } else if (planUsage.getActivePlanId().isBlank()) {
            String savedAssignment = savedData.getPlanAssignment(player.getUUID());
            String planId = savedAssignment != null && PlanRegistry.findPlan(savedAssignment) != null
                    ? savedAssignment : PlanRegistry.getDefaultPlan().id();
            planUsage.setActivePlanId(planId);
            planUsage.setCycleStartDay(Math.floorDiv(overworld.getDayTime(), 24_000L));
            planUsage.setCycleActive(true);
            phoneData.setLastKnownPlanId(planId);
            savedData.setPlanAssignment(player.getUUID(), planId);
            if (savedAssignment == null) {
                savedData.putPlan(planId, new NetworkSavedData.PlanRecord("Free", Map.of()));
            }
        } else {
            phoneData.setLastKnownPlanId(planUsage.getActivePlanId());
            savedData.setPlanAssignment(player.getUUID(), planUsage.getActivePlanId());
            if (planUsage.getCycleStartDay() == 0L) {
                planUsage.setCycleStartDay(Math.floorDiv(overworld.getDayTime(), 24_000L));
            }
        }

        com.example.mobilecellnetwork.billing.BillingManager.onPlayerLogin(player);
        ItemStack cellphone = findCellphone(player);
        if (!cellphone.isEmpty()) {
            Integer charge = cellphone.get(ModDataComponents.PHONE_CHARGE.get());
            if (charge != null) {
                phoneData.setCharge(charge);
            }
        }
        com.example.mobilecellnetwork.network.NetworkHandler.deliverPendingSms(player);
        for (NetworkSavedData.PendingRoamingPayout payout : savedData.takePendingRoamingPayouts(player.getUUID())) {
            var account = player.getData(ModCapabilities.BANK_ACCOUNT);
            long balance = account.getBalance();
            long amount = payout.amount();
            account.setBalance(balance > Long.MAX_VALUE - amount ? Long.MAX_VALUE : balance + amount);
            account.getTransactionHistory().add(new com.example.mobilecellnetwork.capabilities.BankAccount.Transaction(
                    amount, payout.reason(), System.currentTimeMillis()));
        }
        com.example.mobilecellnetwork.network.NetworkHandler.syncBankAccount(player);
        com.example.mobilecellnetwork.network.NetworkHandler.syncPlanData(player);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new com.example.mobilecellnetwork.network.CallLogSyncPacket(
                        java.util.List.copyOf(phoneData.getCallLog())));
    }

    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        java.util.UUID openDevice = com.example.mobilecellnetwork.network.PhoneDeviceService.activePhoneId(player);
        com.example.mobilecellnetwork.network.PhoneDeviceService.onPlayerLogout(player);
        CallManager.clear(player);
        com.example.mobilecellnetwork.network.CoverageEngine.onPlayerLogout(player);
        com.example.mobilecellnetwork.billing.BillingManager.onPlayerLogout(player);
        ItemStack cellphone = findCellphone(player);
        if (openDevice == null && !cellphone.isEmpty()) {
            PhoneData phoneData = player.getData(ModCapabilities.PHONE_DATA);
            cellphone.set(ModDataComponents.PHONE_CHARGE.get(),
                    Math.clamp(phoneData.getCharge(), 0,
                            com.example.mobilecellnetwork.config.MobileCellNetworkConfig.PHONE_MAX_CHARGE.get()));
        }
    }

    private static String issuePhoneNumber(NetworkSavedData savedData, ServerPlayer player) {
        for (int attempt = 0; attempt < 10_000; attempt++) {
            String number = Integer.toString(ThreadLocalRandom.current().nextInt(100_000_000, 1_000_000_000));
            if (savedData.registerNumber(player.getUUID(), number)) {
                return number;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique phone number for player " + player.getUUID());
    }

    private static ItemStack findCellphone(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.CELLPHONE.get())) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModItems.CELLPHONE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}

package com.example.mobilecellnetwork.capabilities;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.plans.PlanRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.common.util.INBTSerializable;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PlanUsage implements INBTSerializable<CompoundTag> {
    public static final EntityCapability<PlanUsage, Void> CAPABILITY = EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "plan_usage"), PlanUsage.class);

    private String activePlanId = "";
    private long cycleStartDay;
    private int minutesUsed;
    private int textsUsed;
    private long dataUsedMb;
    private double partialDataUsedMb;
    private boolean cycleActive;
    private boolean planCanceled;
    private long lastDailyResetDay = Long.MIN_VALUE;
    private long renewalWarningCycleStartDay = Long.MIN_VALUE;
    private final Set<Integer> sentRenewalReminderDays = new LinkedHashSet<>();

    public enum UsageType { MINUTES, TEXTS, DATA }

    /** Checks this usage against the active plan's allowance. */
    public boolean isOver(UsageType type) {
        return getOverage(type) > 0L;
    }

    public long getOverage(UsageType type) {
        PlanRegistry.Plan plan = PlanRegistry.getPlan(activePlanId);
        return getOverage(type, plan.callMinutes(), plan.textMessages(), plan.dataAllowanceMb());
    }

    public boolean isOver(UsageType type, int callMinutes, int textMessages, long dataAllowanceMb) {
        return getOverage(type, callMinutes, textMessages, dataAllowanceMb) > 0L;
    }

    public long getOverage(UsageType type, int callMinutes, int textMessages, long dataAllowanceMb) {
        switch (type) {
            case MINUTES:
                return callMinutes != -1 && minutesUsed > callMinutes
                        ? minutesUsed - (long) callMinutes : 0L;
            case TEXTS:
                return textMessages != -1 && textsUsed > textMessages
                        ? textsUsed - (long) textMessages : 0L;
            case DATA:
                return dataAllowanceMb != -1 && dataUsedMb > dataAllowanceMb
                        ? dataUsedMb - dataAllowanceMb : 0L;
            default:
                return 0L;
        }
    }

    public String getActivePlanId() { return activePlanId; }
    public void setActivePlanId(String activePlanId) {
        this.activePlanId = activePlanId == null ? "" : activePlanId;
        if (!this.activePlanId.isBlank()) {
            planCanceled = false;
        }
    }
    public boolean isPlanCanceled() { return planCanceled; }
    public void setPlanCanceled(boolean planCanceled) { this.planCanceled = planCanceled; }
    public long getCycleStartDay() { return cycleStartDay; }
    public void setCycleStartDay(long cycleStartDay) { this.cycleStartDay = cycleStartDay; }
    public int getMinutesUsed() { return minutesUsed; }
    public void setMinutesUsed(int minutesUsed) { this.minutesUsed = Math.max(0, minutesUsed); }
    public int getTextsUsed() { return textsUsed; }
    public void setTextsUsed(int textsUsed) { this.textsUsed = Math.max(0, textsUsed); }
    public long getDataUsedMb() { return dataUsedMb; }
    public void setDataUsedMb(long dataUsedMb) { this.dataUsedMb = Math.max(0L, dataUsedMb); }
    public double getPartialDataUsedMb() { return partialDataUsedMb; }
    public void addDataUsageMb(double amount) {
        if (amount <= 0.0D) {
            return;
        }
        partialDataUsedMb += amount;
        long wholeMb = (long) partialDataUsedMb;
        dataUsedMb += wholeMb;
        partialDataUsedMb -= wholeMb;
    }
    public boolean isCycleActive() { return cycleActive; }
    public void setCycleActive(boolean cycleActive) { this.cycleActive = cycleActive; }
    public long getLastDailyResetDay() { return lastDailyResetDay; }
    public long getRenewalWarningCycleStartDay() { return renewalWarningCycleStartDay; }
    public void setRenewalWarningCycleStartDay(long day) { renewalWarningCycleStartDay = day; }
    public boolean markRenewalReminderSent(int daysBeforeRenewal) {
        return sentRenewalReminderDays.add(daysBeforeRenewal);
    }
    public Set<Integer> getSentRenewalReminderDays() {
        return Set.copyOf(sentRenewalReminderDays);
    }
    public void resetDailyCounters(long day) {
        minutesUsed = 0;
        textsUsed = 0;
        dataUsedMb = 0L;
        partialDataUsedMb = 0.0D;
        cycleStartDay = day;
        cycleActive = true;
        lastDailyResetDay = day;
        renewalWarningCycleStartDay = Long.MIN_VALUE;
        sentRenewalReminderDays.clear();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("activePlanId", activePlanId);
        tag.putLong("cycleStartDay", cycleStartDay);
        tag.putInt("minutesUsed", minutesUsed);
        tag.putInt("textsUsed", textsUsed);
        tag.putLong("dataUsedMb", dataUsedMb);
        tag.putDouble("partialDataUsedMb", partialDataUsedMb);
        tag.putBoolean("cycleActive", cycleActive);
        tag.putBoolean("planCanceled", planCanceled);
        tag.putLong("lastDailyResetDay", lastDailyResetDay);
        tag.putLong("renewalWarningCycleStartDay", renewalWarningCycleStartDay);
        net.minecraft.nbt.ListTag reminders = new net.minecraft.nbt.ListTag();
        sentRenewalReminderDays.forEach(days ->
                reminders.add(net.minecraft.nbt.IntTag.valueOf(days)));
        tag.put("sentRenewalReminderDays", reminders);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        activePlanId = tag.getString("activePlanId");
        cycleStartDay = tag.getLong("cycleStartDay");
        setMinutesUsed(tag.getInt("minutesUsed"));
        setTextsUsed(tag.getInt("textsUsed"));
        setDataUsedMb(tag.getLong("dataUsedMb"));
        partialDataUsedMb = Math.max(0.0D, tag.getDouble("partialDataUsedMb"));
        cycleActive = tag.getBoolean("cycleActive");
        planCanceled = tag.getBoolean("planCanceled");
        lastDailyResetDay = tag.contains("lastDailyResetDay")
                ? tag.getLong("lastDailyResetDay") : Long.MIN_VALUE;
        renewalWarningCycleStartDay = tag.contains("renewalWarningCycleStartDay")
                ? tag.getLong("renewalWarningCycleStartDay") : Long.MIN_VALUE;
        sentRenewalReminderDays.clear();
        for (net.minecraft.nbt.Tag reminder : tag.getList(
                "sentRenewalReminderDays", net.minecraft.nbt.Tag.TAG_INT)) {
            int days = ((net.minecraft.nbt.IntTag) reminder).getAsInt();
            if (days >= 1 && days <= 365) {
                sentRenewalReminderDays.add(days);
            }
        }
        if (renewalWarningCycleStartDay == cycleStartDay) {
            int legacyWarningDay = MobileCellNetworkConfig.DAYS_BEFORE_RENEWAL_WARNING.get();
            if (legacyWarningDay > 0) {
                sentRenewalReminderDays.add(legacyWarningDay);
            }
        }
    }
}

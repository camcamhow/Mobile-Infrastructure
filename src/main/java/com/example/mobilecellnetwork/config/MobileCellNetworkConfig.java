package com.example.mobilecellnetwork.config;

import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.blockentity.AntennaRoamingPolicy;
import com.example.mobilecellnetwork.gui.PhoneStyle;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MobileCellNetworkConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue COPPER_RANGE;
    public static final ModConfigSpec.IntValue IRON_RANGE;
    public static final ModConfigSpec.IntValue DIAMOND_RANGE;
    public static final ModConfigSpec.BooleanValue TOWER_PLACEMENT_PREVIEW_ENABLED;
    public static final ModConfigSpec.IntValue TOWER_PLACEMENT_PREVIEW_MAX_DISTANCE;
    public static final ModConfigSpec.BooleanValue ALLOW_MESH_SHARING;
    public static final ModConfigSpec.BooleanValue EMERGENCY_ONLY_WHEN_NO_SERVICE;
    public static final ModConfigSpec.BooleanValue REQUIRE_ENERGY;
    public static final ModConfigSpec.IntValue ANTENNA_CAPACITY_COPPER;
    public static final ModConfigSpec.IntValue ANTENNA_CAPACITY_IRON;
    public static final ModConfigSpec.IntValue ANTENNA_CAPACITY_DIAMOND;
    public static final ModConfigSpec.IntValue ANTENNA_MAX_RECEIVE_COPPER;
    public static final ModConfigSpec.IntValue ANTENNA_MAX_RECEIVE_IRON;
    public static final ModConfigSpec.IntValue ANTENNA_MAX_RECEIVE_DIAMOND;
    public static final ModConfigSpec.IntValue ANTENNA_DRAW_COPPER;
    public static final ModConfigSpec.IntValue ANTENNA_DRAW_IRON;
    public static final ModConfigSpec.IntValue ANTENNA_DRAW_DIAMOND;
    public static final ModConfigSpec.DoubleValue LOW_POWER_RANGE_MULTIPLIER;
    public static final ModConfigSpec.EnumValue<SignalTier> LOW_POWER_MAX_TIER;
    public static final ModConfigSpec.IntValue ANTENNA_CHUNK_LOAD_GRACE_TICKS;
    public static final ModConfigSpec.IntValue PHONE_MAX_CHARGE;
    public static final ModConfigSpec.IntValue PHONE_DRAIN_PER_SECOND;
    public static final ModConfigSpec.DoubleValue LOW_POWER_DRAIN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue FLASHLIGHT_DRAIN_MULTIPLIER;
    public static final ModConfigSpec.IntValue CHARGER_FE_CAPACITY;
    public static final ModConfigSpec.IntValue CHARGER_FE_MAX_RECEIVE_PER_TICK;
    public static final ModConfigSpec.IntValue CHARGER_FE_DRAW_PER_TICK;
    public static final ModConfigSpec.IntValue CHARGER_CHARGE_RATE_PER_TICK;
    public static final ModConfigSpec.EnumValue<SignalFalloffMode> SIGNAL_FALLOFF_MODE;
    public static final ModConfigSpec.DoubleValue INVERSE_FALLOFF_K;
    public static final ModConfigSpec.DoubleValue INVERSE_MAX_RANGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue LOS_WEIGHT;
    public static final ModConfigSpec.DoubleValue LOS_SAMPLE_INTERVAL;
    public static final ModConfigSpec.IntValue LOS_MAX_SAMPLES;
    public static final ModConfigSpec.DoubleValue ATTENUATION_NONE;
    public static final ModConfigSpec.DoubleValue ATTENUATION_LOW;
    public static final ModConfigSpec.DoubleValue ATTENUATION_MEDIUM;
    public static final ModConfigSpec.DoubleValue ATTENUATION_HIGH;
    public static final ModConfigSpec.DoubleValue ATTENUATION_BLOCKING;
    public static final ModConfigSpec.DoubleValue UNKNOWN_BLOCK_ATTENUATION;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SIGNAL_THRESHOLDS;
    public static final ModConfigSpec.BooleanValue DEBUG_SIGNAL_INFO;
    public static final ModConfigSpec.EnumValue<SignalTier> LOW_POWER_MAX_SIGNAL_TIER;
    public static final ModConfigSpec.DoubleValue SIGNAL_BARS_4_THRESHOLD;
    public static final ModConfigSpec.DoubleValue SIGNAL_BARS_3_THRESHOLD;
    public static final ModConfigSpec.DoubleValue SIGNAL_BARS_2_THRESHOLD;
    public static final ModConfigSpec.DoubleValue SIGNAL_BARS_1_THRESHOLD;
    public static final ModConfigSpec.BooleanValue CHAT_LOCK_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> CHAT_LOCK_BYPASS_PERMISSION;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BLOCKED_COMMANDS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> WHITELISTED_COMMANDS;
    public static final ModConfigSpec.BooleanValue SYSTEM_MESSAGES_ENABLED;
    public static final ModConfigSpec.BooleanValue LOW_POWER_ENABLED;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> LOW_POWER_ALLOWED_APPS;
    public static final ModConfigSpec.IntValue LOW_POWER_AUTO_PROMPT_THRESHOLD;
    public static final ModConfigSpec.BooleanValue LOW_POWER_DROP_ACTIVE_CALL;
    public static final ModConfigSpec.IntValue MAP_RESOLUTION_DIVISOR;
    public static final ModConfigSpec.BooleanValue DEAD_PHONE_ALLOWS_EMERGENCY;
    public static final ModConfigSpec.IntValue DEFAULT_PLAN_TEXT_LIMIT;
    public static final ModConfigSpec.IntValue DEFAULT_PLAN_MINUTE_LIMIT;
    public static final ModConfigSpec.IntValue CALL_RING_TIMEOUT_SECONDS;
    public static final ModConfigSpec.BooleanValue BLOCK_PROXIMITY_VOICE_OUTSIDE_CALLS;
    public static final ModConfigSpec.ConfigValue<String> RINGTONE_SOUND;
    public static final ModConfigSpec.ConfigValue<String> CALL_END_SOUND;
    public static final ModConfigSpec.IntValue MAX_MESSAGES_PER_THREAD;
    public static final ModConfigSpec.IntValue SMS_MAX_LENGTH;
    public static final ModConfigSpec.ConfigValue<String> SMS_NOTIFICATION_SOUND;
    public static final ModConfigSpec.BooleanValue BILLING_ENABLED;
    public static final ModConfigSpec.IntValue DAYS_BEFORE_RENEWAL_WARNING;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends Integer>> RENEWAL_REMINDER_DAYS;
    public static final ModConfigSpec.BooleanValue DOWNGRADE_ON_FAILED_RENEWAL;
    public static final ModConfigSpec.EnumValue<AntennaRoamingPolicy> DEFAULT_ANTENNA_ROAMING_POLICY;
    public static final ModConfigSpec.EnumValue<AntennaRoamingPolicy> DEFAULT_NETWORK_ROAMING_POLICY;
    public static final ModConfigSpec.BooleanValue ROAMING_FEE_ENABLED;
    public static final ModConfigSpec.IntValue ROAMING_HOST_SHARE_PERCENT;
    public static final ModConfigSpec.DoubleValue ROAMING_REVENUE_SPLIT_HOME;
    public static final ModConfigSpec.IntValue MINIMUM_NETWORK_PASSWORD_LENGTH;
    public static final ModConfigSpec.ConfigValue<Long> DEFAULT_ROAMING_RATE_PER_MINUTE;
    public static final ModConfigSpec.ConfigValue<Long> DEFAULT_ROAMING_RATE_PER_TEXT;
    public static final ModConfigSpec.ConfigValue<Long> DEFAULT_ROAMING_RATE_PER_MB;
    public static final ModConfigSpec.BooleanValue AUTO_SEND_ROAMING_REQUESTS_DEFAULT;
    public static final ModConfigSpec.BooleanValue ALLOW_NETWORK_WITHOUT_SERVER_BLOCK;
    public static final ModConfigSpec.IntValue ROAMING_NOTIFICATION_COOLDOWN_SECONDS;
    public static final ModConfigSpec.IntValue EMERGENCY_MESSAGE_MAX_LENGTH;
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_SYMBOL;
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_NAME;
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_NAME_PLURAL;
    public static final ModConfigSpec.EnumValue<PhoneStyle> DEFAULT_PHONE_STYLE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("coverage");
        builder.comment("Maximum signal radius, in blocks, for each antenna tier.");
        COPPER_RANGE = builder.defineInRange("copperAntennaRange", 256, 1, 4096);
        IRON_RANGE = builder.defineInRange("ironAntennaRange", 512, 1, 4096);
        DIAMOND_RANGE = builder.defineInRange("diamondAntennaRange", 1024, 1, 4096);
        builder.comment("Show the coverage-radius preview while holding an antenna.");
        TOWER_PLACEMENT_PREVIEW_ENABLED = builder.define("towerPlacementPreviewEnabled", true);
        builder.comment("Maximum distance in blocks for aiming the antenna placement preview.");
        TOWER_PLACEMENT_PREVIEW_MAX_DISTANCE =
                builder.defineInRange("towerPlacementPreviewMaxDistance", 10, 1, 32);
        builder.comment("Allows nearby antennas to relay coverage through mesh links.");
        ALLOW_MESH_SHARING = builder.define("allowMeshSharing", true);
        builder.comment("When enabled, show emergency calling as the primary option without service.");
        EMERGENCY_ONLY_WHEN_NO_SERVICE = builder.define("emergencyOnlyWhenNoService", true);
        builder.comment("If false, antennas work without FE. If true, they need stored energy to provide service.");
        REQUIRE_ENERGY = builder.define("requireEnergy", true);
        builder.comment("Maximum FE energy storage for each antenna tier.");
        ANTENNA_CAPACITY_COPPER = builder.defineInRange("antennaCapacity_COPPER", 10_000, 1, Integer.MAX_VALUE);
        ANTENNA_CAPACITY_IRON = builder.defineInRange("antennaCapacity_IRON", 25_000, 1, Integer.MAX_VALUE);
        ANTENNA_CAPACITY_DIAMOND = builder.defineInRange("antennaCapacity_DIAMOND", 50_000, 1, Integer.MAX_VALUE);
        builder.comment("Maximum FE that each antenna tier can receive per tick.");
        ANTENNA_MAX_RECEIVE_COPPER = builder.defineInRange("antennaMaxReceive_COPPER", 100, 1, Integer.MAX_VALUE);
        ANTENNA_MAX_RECEIVE_IRON = builder.defineInRange("antennaMaxReceive_IRON", 400, 1, Integer.MAX_VALUE);
        ANTENNA_MAX_RECEIVE_DIAMOND = builder.defineInRange("antennaMaxReceive_DIAMOND", 1_000, 1, Integer.MAX_VALUE);
        builder.comment("FE consumed per second by each antenna tier while operating.");
        ANTENNA_DRAW_COPPER = builder.defineInRange("antennaDrawFePerSecond_COPPER", 100, 0, Integer.MAX_VALUE);
        ANTENNA_DRAW_IRON = builder.defineInRange("antennaDrawFePerSecond_IRON", 400, 0, Integer.MAX_VALUE);
        ANTENNA_DRAW_DIAMOND = builder.defineInRange("antennaDrawFePerSecond_DIAMOND", 1_600, 0, Integer.MAX_VALUE);
        builder.comment("Scales an antenna's configured range while it is in low-power mode.");
        LOW_POWER_RANGE_MULTIPLIER = builder.defineInRange("lowPowerRangeMultiplier", 0.5D, 0.0D, 1.0D);
        builder.comment("Highest signal tier allowed while an antenna itself is in low-power mode.");
        LOW_POWER_MAX_TIER = builder.defineEnum("lowPowerMaxTier", SignalTier.TWO_G);
        builder.comment("Ticks to keep an antenna's chunk loaded after its last user leaves. 20 ticks = 1 second.");
        ANTENNA_CHUNK_LOAD_GRACE_TICKS =
                builder.defineInRange("antennaChunkLoadGraceTicks", 200, 0, Integer.MAX_VALUE);
        builder.comment("Maximum charge stored in a phone item, in charge units.");
        PHONE_MAX_CHARGE = builder.defineInRange("phoneMaxCharge", 10_000, 1, 10_000);
        builder.comment("Phone charge consumed per second during normal operation.");
        PHONE_DRAIN_PER_SECOND = builder.defineInRange("phoneDrainPerSecond", 1, 0, Integer.MAX_VALUE);
        builder.comment("Multiplies phone drain while low-power mode is active.");
        FLASHLIGHT_DRAIN_MULTIPLIER = builder.defineInRange("flashlightDrainMultiplier", 4.0D, 1.0D, 100.0D);
        LOW_POWER_DRAIN_MULTIPLIER = builder.defineInRange("lowPowerDrainMultiplier", 5.0D, 1.0D, 100.0D);
        builder.comment("Maximum FE stored by a phone charger block.");
        CHARGER_FE_CAPACITY = builder.defineInRange("chargerFeCapacity", 20_000, 1, Integer.MAX_VALUE);
        builder.comment("Maximum FE the charger can receive from adjacent energy sources per tick.");
        CHARGER_FE_MAX_RECEIVE_PER_TICK =
                builder.defineInRange("chargerFeMaxReceivePerTick", 200, 1, Integer.MAX_VALUE);
        builder.comment("FE consumed by the charger per tick while charging a phone.");
        CHARGER_FE_DRAW_PER_TICK = builder.defineInRange("chargerFeDrawPerTick", 40, 0, Integer.MAX_VALUE);
        builder.comment("Phone charge units added per tick while a phone is on the charger.");
        CHARGER_CHARGE_RATE_PER_TICK =
                builder.defineInRange("chargerChargeRatePerTick", 50, 1, Integer.MAX_VALUE);
        builder.comment("Formula used to reduce signal strength with distance. INVERSE is gradual; LINEAR and QUADRATIC fall off faster; STEPPED changes in bands.");
        SIGNAL_FALLOFF_MODE = builder.defineEnum("signalFalloffMode", SignalFalloffMode.INVERSE);
        builder.comment("Controls the steepness of INVERSE distance falloff. Higher values weaken signal faster.");
        INVERSE_FALLOFF_K = builder.defineInRange("inverseFalloffK", 4.0D, 0.0D, 1000.0D);
        builder.comment("Hard cutoff multiplier beyond the antenna range. For example, 2.5 allows signal calculations up to 2.5 times range.");
        INVERSE_MAX_RANGE_MULTIPLIER = builder.defineInRange("inverseMaxRangeMultiplier", 2.5D, 1.0D, 100.0D);
        builder.comment("How much sampled block obstruction subtracts from raw signal strength. LOS loss = sampled attenuation x this value.");
        LOS_WEIGHT = builder.defineInRange("losWeight", 0.01D, 0.0D, 100.0D);
        builder.comment("Spacing in blocks between line-of-sight obstruction samples. Smaller values inspect more blocks.");
        LOS_SAMPLE_INTERVAL = builder.defineInRange("losSampleInterval", 2.0D, 0.1D, 64.0D);
        builder.comment("Maximum line-of-sight samples per signal check. If the path needs more, sampled attenuation is scaled to compensate.");
        LOS_MAX_SAMPLES = builder.defineInRange("losMaxSamples", 64, 1, 4096);
        builder.comment("Signal attenuation added by blocks in the matching signal_attenuation tags. Air uses no attenuation.");
        ATTENUATION_NONE = builder.defineInRange("attenuationNone", 0.0D, 0.0D, 1000.0D);
        ATTENUATION_LOW = builder.defineInRange("attenuationLow", 0.5D, 0.0D, 1000.0D);
        ATTENUATION_MEDIUM = builder.defineInRange("attenuationMedium", 1.5D, 0.0D, 1000.0D);
        ATTENUATION_HIGH = builder.defineInRange("attenuationHigh", 3.0D, 0.0D, 1000.0D);
        ATTENUATION_BLOCKING = builder.defineInRange("attenuationBlocking", 8.0D, 0.0D, 1000.0D);
        builder.comment("Attenuation assigned to blocks not included in any signal_attenuation tag.");
        UNKNOWN_BLOCK_ATTENUATION = builder.defineInRange("unknownBlockAttenuation", 1.0D, 0.0D, 1000.0D);
        builder.comment("Minimum raw signal-strength values for each signal tier. Values are from 0.0 to 1.0; higher entries are better tiers.");
        java.util.List<String> defaultThresholds = java.util.List.of(
                "FIVE_G_PLUS=0.90", "FIVE_G=0.80", "FOUR_G_PLUS=0.70", "FOUR_G=0.60",
                "LTE=0.50", "THREE_G=0.40", "H_PLUS=0.30", "H=0.20", "TWO_G=0.10", "E_PLUS=0.05");
        SIGNAL_THRESHOLDS = builder.defineList("signalThresholds", defaultThresholds,
                MobileCellNetworkConfig::isValidSignalThreshold);
        builder.comment("Includes distance and obstruction diagnostics in server-side signal logging/debug output.");
        DEBUG_SIGNAL_INFO = builder.define("debugSignalInfo", false);
        builder.comment("Highest phone signal tier while the phone is in low-power mode.");
        LOW_POWER_MAX_SIGNAL_TIER = builder.defineEnum("lowPowerMaxSignalTier", SignalTier.E_PLUS);
        builder.comment("Fraction of the current tier's signal range required to display 4 bars.");
        SIGNAL_BARS_4_THRESHOLD = builder.defineInRange("signalBars4Threshold", 0.80D, 0.0D, 1.0D);
        builder.comment("Fraction of the current tier's signal range required to display 3 bars.");
        SIGNAL_BARS_3_THRESHOLD = builder.defineInRange("signalBars3Threshold", 0.55D, 0.0D, 1.0D);
        builder.comment("Fraction of the current tier's signal range required to display 2 bars.");
        SIGNAL_BARS_2_THRESHOLD = builder.defineInRange("signalBars2Threshold", 0.30D, 0.0D, 1.0D);
        builder.comment("Fraction of the current tier's signal range required to display 1 bar.");
        SIGNAL_BARS_1_THRESHOLD = builder.defineInRange("signalBars1Threshold", 0.0D, 0.0D, 1.0D);
        builder.pop();
        builder.push("chat");
        builder.comment("Restricts configured player-to-player chat commands when a player has no mobile service.");
        CHAT_LOCK_ENABLED = builder.define("chatLockEnabled", true);
        builder.comment("Permission node that bypasses the chat service restriction.");
        CHAT_LOCK_BYPASS_PERMISSION =
                builder.define("chatLockBypassPermission", "mobilecellnetwork.bypasschat");
        builder.comment("Chat command names disabled by the mobile service restriction.");
        BLOCKED_COMMANDS = builder.defineList("blockedCommands",
                java.util.List.of("msg", "tell", "w", "whisper", "m", "pm", "dm"),
                value -> value instanceof String);
        builder.comment("Commands exempt from chat service restrictions.");
        WHITELISTED_COMMANDS = builder.defineList("whitelistedCommands",
                java.util.List.of("help", "tp", "gamemode", "give", "me"),
                value -> value instanceof String);
        builder.comment("Enables system chat messages generated by the mod.");
        SYSTEM_MESSAGES_ENABLED = builder.define("systemMessagesEnabled", true);
        builder.pop();
        builder.push("phone");
        builder.comment("Default smartphone operating-system style for new or previously unset phone profiles.");
        DEFAULT_PHONE_STYLE = builder.defineEnum("defaultPhoneStyle", PhoneStyle.SAMSUNG_ONE_UI);
        builder.comment("Enables low-power mode, which can reduce phone drain and restrict apps.");
        LOW_POWER_ENABLED = builder.define("lowPowerEnabled", true);
        builder.comment("App IDs permitted while low-power mode is active.");
        LOW_POWER_ALLOWED_APPS = builder.defineList("lowPowerAllowedApps",
                java.util.List.of("sms", "map", "settings"), value -> value instanceof String);
        builder.comment("Charge percentage at or below which the low-power prompt appears.");
        LOW_POWER_AUTO_PROMPT_THRESHOLD =
                builder.defineInRange("lowPowerAutoPromptThreshold", 10, 1, 100);
        builder.comment("Ends an active call when low-power mode is enabled.");
        LOW_POWER_DROP_ACTIVE_CALL = builder.define("lowPowerDropActiveCall", true);
        builder.comment("Extra map data-use divisor while low-power mode is active. Larger values use less data.");
        MAP_RESOLUTION_DIVISOR = builder.defineInRange("mapResolutionDivisor", 4, 1, 64);
        builder.comment("Allows emergency calling when the phone battery is depleted.");
        DEAD_PHONE_ALLOWS_EMERGENCY = builder.define("deadPhoneAllowsEmergency", false);
        builder.comment("Default plan text allowance. A value of 0 disables texts; negative plan-specific allowances mean unlimited.");
        DEFAULT_PLAN_TEXT_LIMIT = builder.defineInRange("defaultPlanTextLimit", 100, 0, Integer.MAX_VALUE);
        builder.comment("Default plan call-minute allowance. A value of 0 disables call minutes.");
        DEFAULT_PLAN_MINUTE_LIMIT = builder.defineInRange("defaultPlanMinuteLimit", 60, 0, Integer.MAX_VALUE);
        builder.comment("How long an incoming call rings before it times out, in seconds.");
        CALL_RING_TIMEOUT_SECONDS = builder.defineInRange("callRingTimeoutSeconds", 30, 1, 3600);
        builder.comment("Prevents proximity voice chat from working unless players are on a phone call.");
        BLOCK_PROXIMITY_VOICE_OUTSIDE_CALLS = builder.define("blockProximityVoiceOutsideCalls", true);
        builder.comment("Sound resource location played for an incoming call.");
        RINGTONE_SOUND = builder.define("ringtoneSound", "minecraft:block.note_block.bell");
        builder.comment("Sound resource location played when a call ends.");
        CALL_END_SOUND = builder.define("callEndSound", "minecraft:block.note_block.bass");
        builder.comment("Maximum stored SMS messages per conversation thread.");
        MAX_MESSAGES_PER_THREAD = builder.defineInRange("maxMessagesPerThread", 100, 1, 10000);
        builder.comment("Maximum characters allowed in one SMS message.");
        SMS_MAX_LENGTH = builder.defineInRange("smsMaxLength", 160, 1, 1000);
        builder.comment("Sound resource location played when an SMS is received.");
        SMS_NOTIFICATION_SOUND = builder.define("smsNotificationSound", "minecraft:block.note_block.hat");
        builder.pop();
        builder.push("billing");
        builder.comment("Enables automatic plan renewals and related billing behavior.");
        BILLING_ENABLED = builder.define("billingEnabled", true);
        builder.comment("Days before renewal to send payment reminders. Each interval is sent once per billing cycle.");
        RENEWAL_REMINDER_DAYS = builder.defineList("renewalReminderDays",
                java.util.List.of(7, 3, 1),
                value -> value instanceof Integer days && days >= 1 && days <= 365);
        builder.comment("Legacy single reminder interval, used only if renewalReminderDays is empty.");
        DAYS_BEFORE_RENEWAL_WARNING = builder.defineInRange("daysBeforeRenewalWarning", 1, 0, 365);
        builder.comment("Downgrades a player when their automatic plan renewal payment fails.");
        DOWNGRADE_ON_FAILED_RENEWAL = builder.define("downgradeOnFailedRenewal", true);
        builder.pop();
        builder.push("roaming");
        builder.comment("Legacy default for older per-antenna roaming policies. Paid carrier roaming requires bilateral network approval.");
        DEFAULT_ANTENNA_ROAMING_POLICY =
                builder.defineEnum("defaultAntennaRoamingPolicy", AntennaRoamingPolicy.DENY_ALL);
        builder.comment("Legacy default for older network roaming policies. Paid carrier roaming requires bilateral network approval.");
        DEFAULT_NETWORK_ROAMING_POLICY =
                builder.defineEnum("defaultNetworkRoamingPolicy", AntennaRoamingPolicy.DENY_ALL);
        builder.comment("Enables charging roaming fees for paid-roaming connections.");
        ROAMING_FEE_ENABLED = builder.define("roamingFeeEnabled", true);
        builder.comment("Legacy roaming payout setting retained for compatibility; revenue now uses roamingRevenueSplitHome.");
        ROAMING_HOST_SHARE_PERCENT = builder.defineInRange("roamingHostSharePercent", 60, 0, 100);
        builder.comment("Fraction of roaming revenue allocated to the subscriber's home network; the rest goes to the host network.");
        ROAMING_REVENUE_SPLIT_HOME = builder.defineInRange("roamingRevenueSplitHome", 0.5D, 0.0D, 1.0D);
        builder.comment("Minimum password length accepted for new network passwords.");
        MINIMUM_NETWORK_PASSWORD_LENGTH = builder.defineInRange("minimumNetworkPasswordLength", 4, 1, 64);
        builder.comment("Default roaming rates used when a network is first initialized.");
        DEFAULT_ROAMING_RATE_PER_MINUTE = builder.define("defaultRoamingRatePerMinute", 5L,
                value -> value instanceof Long rate && rate >= 0L);
        DEFAULT_ROAMING_RATE_PER_TEXT = builder.define("defaultRoamingRatePerText", 1L,
                value -> value instanceof Long rate && rate >= 0L);
        DEFAULT_ROAMING_RATE_PER_MB = builder.define("defaultRoamingRatePerMb", 1L,
                value -> value instanceof Long rate && rate >= 0L);
        builder.comment("New networks automatically send roaming proposals to other networks.");
        AUTO_SEND_ROAMING_REQUESTS_DEFAULT = builder.define("autoSendRoamingRequestsDefault", false);
        builder.comment("Permit antenna networks to operate without an assigned online server block.");
        ALLOW_NETWORK_WITHOUT_SERVER_BLOCK = builder.define("allowNetworkWithoutServerBlock", false);
        builder.comment("Minimum seconds between roaming notifications sent to one player.");
        ROAMING_NOTIFICATION_COOLDOWN_SECONDS =
                builder.defineInRange("roamingNotificationCooldownSeconds", 300, 0, Integer.MAX_VALUE);
        builder.comment("Maximum characters accepted in an emergency message.");
        EMERGENCY_MESSAGE_MAX_LENGTH = builder.defineInRange("emergencyMessageMaxLength", 280, 1, 4096);
        builder.pop();
        builder.push("currency");
        builder.comment("Currency symbol used in prices and balances.");
        CURRENCY_SYMBOL = builder.define("currencySymbol", "$",
                value -> value instanceof String symbol && symbol.length() <= 8);
        builder.comment("Singular name for one unit of currency.");
        CURRENCY_NAME = builder.define("currencyName", "dollar");
        builder.comment("Plural name for currency amounts other than one.");
        CURRENCY_NAME_PLURAL = builder.define("currencyNamePlural", "dollars");
        builder.pop();
        SPEC = builder.build();
    }

    private MobileCellNetworkConfig() {
    }

    private static boolean isValidSignalThreshold(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }
        String[] parts = entry.split("=", -1);
        if (parts.length != 2) {
            return false;
        }
        try {
            SignalTier.valueOf(parts[0]);
            double threshold = Double.parseDouble(parts[1]);
            return Double.isFinite(threshold) && threshold >= 0.0D && threshold <= 1.0D;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

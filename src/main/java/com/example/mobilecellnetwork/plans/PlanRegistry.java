package com.example.mobilecellnetwork.plans;

import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public final class PlanRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Plan> PLANS = new ConcurrentHashMap<>();
    private static final Map<String, Plan> ADMIN_OVERRIDES = new ConcurrentHashMap<>();
    private static final java.util.Set<String> ADMIN_DELETIONS = ConcurrentHashMap.newKeySet();
        private static volatile Plan defaultPlan = null;

    private PlanRegistry() {
    }

    public static void init() {
            // No default plan - plans must be loaded from JSON resources or admin overrides
    }

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(
                new com.google.gson.Gson(), "plans") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager,
                                 ProfilerFiller profiler) {
                Map<String, Plan> loaded = new ConcurrentHashMap<>();
                resources.forEach((location, json) -> {
                    try {
                        Plan plan = parsePlan(location, json.getAsJsonObject());
                        loaded.put(plan.id(), plan);
                    } catch (RuntimeException exception) {
                        LOGGER.error("Failed to load plan resource '{}'.", location, exception);
                    }
                });
                installPlans(loaded);
                net.minecraft.server.MinecraftServer server =
                        net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    server.getPlayerList().getPlayers().forEach(
                            com.example.mobilecellnetwork.network.NetworkHandler::syncPlanData);
                }
            }
        });
    }

    public static void onServerStarted(ServerStartedEvent event) {
            if (PLANS.isEmpty()) {
                LOGGER.warn("No plan JSON resources were loaded; no plans available until admin creates them.");
            }
        NetworkSavedData.get(event.getServer().overworld()).getPlans().forEach((id, record) -> {
            try {
                if ("true".equals(record.attributes().get("deleted"))) {
                    ADMIN_DELETIONS.add(id);
                    ADMIN_OVERRIDES.remove(id);
                    PLANS.remove(id);
                    return;
                }
                if (!record.attributes().containsKey("maxSignalTier")) {
                    return;
                }
                Plan saved = planFromRecord(id, record);
                ADMIN_OVERRIDES.put(id, saved);
                ADMIN_DELETIONS.remove(id);
                PLANS.put(id, saved);
                if (saved.isDefault()) {
                    defaultPlan = saved;
                }
            } catch (RuntimeException exception) {
                LOGGER.error("Ignoring invalid saved admin plan '{}'.", id, exception);
            }
        });
    }

    private static Plan parsePlan(ResourceLocation location, JsonObject json) {
        String id = string(json, "id", location.getPath());
        if (!id.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid plan id: " + id);
        }
        List<String> allowedNetworks = new ArrayList<>();
        if (json.has("allowedNetworks") && json.get("allowedNetworks").isJsonArray()) {
            json.getAsJsonArray("allowedNetworks").forEach(value -> allowedNetworks.add(value.getAsString()));
        }
        return new Plan(id, string(json, "displayName", id), string(json, "description", ""),
                longValue(json, "price", 0L), intValue(json, "billingCycleDays", 30),
                longValue(json, "dataAllowanceMb", -1L),
                enumValue(json, "dataSpeed", DataSpeed.class, DataSpeed.MEDIUM),
                enumValue(json, "maxSignalTier", SignalTier.class, SignalTier.FIVE_G_PLUS),
                intValue(json, "callMinutes", -1), intValue(json, "textMessages", -1),
                allowedNetworks, boolValue(json, "autoRenew", false),
                boolValue(json, "isDefault", false), boolValue(json, "allowRoaming", false),
                longValue(json, "roamingFeePerMinute", 5L), longValue(json, "roamingFeePerMb", 1L),
                string(json, "carrierNetworkId", ""),
                enumValue(json, "roamingMode", RoamingMode.class, RoamingMode.CHARGED),
                boolValue(json, "allowOverage", false), longValue(json, "overageRatePerMinute", 0L),
                longValue(json, "overageRatePerText", 0L), longValue(json, "overageRatePerMb", 0L));
    }

    private static void installPlans(Map<String, Plan> plans) {
        if (plans.isEmpty()) {
            LOGGER.error("Plan reload contained no valid plans; keeping the previous plan registry.");
            return;
        }
        List<Plan> defaults = plans.values().stream().filter(Plan::isDefault)
                .sorted(Comparator.comparing(Plan::id)).toList();
        if (defaults.size() > 1) {
            LOGGER.warn("Multiple plans are marked default; selecting '{}'.", defaults.get(0).id());
        }
        PLANS.clear();
        PLANS.putAll(plans);
        ADMIN_DELETIONS.forEach(PLANS::remove);
        PLANS.putAll(ADMIN_OVERRIDES);
        PLANS.values().stream().filter(Plan::isDefault).findFirst().ifPresent(plan -> defaultPlan = plan);
        defaultPlan = defaults.isEmpty() ? plans.values().stream()
                .sorted(Comparator.comparing(Plan::id)).findFirst().orElseThrow() : defaults.get(0);
                LOGGER.info("Loaded {} subscription plans; default plan is '{}'.", plans.size(), defaultPlan != null ? defaultPlan.id() : "none");
    }

    public static Plan getDefaultPlan() {
        return defaultPlan;
    }

    public static Plan getPlan(String planId) {
        return PLANS.getOrDefault(planId, defaultPlan);
    }

    public static Plan findPlan(String planId) {
        return PLANS.get(planId);
    }

    public static boolean putAdminPlan(Plan plan) {
        if (plan == null || !plan.id().matches("[a-z0-9_.-]+")) {
            return false;
        }
        ADMIN_DELETIONS.remove(plan.id());
        ADMIN_OVERRIDES.put(plan.id(), plan);
        PLANS.put(plan.id(), plan);
        if (plan.isDefault()) {
            defaultPlan = plan;
        }
        return true;
    }

    public static boolean deleteAdminPlan(String planId) {
        Plan plan = PLANS.get(planId);
        if (plan == null || plan.isDefault() || PLANS.size() <= 1) {
            return false;
        }
        PLANS.remove(planId);
        ADMIN_OVERRIDES.remove(planId);
        ADMIN_DELETIONS.add(planId);
        return true;
    }

    public static boolean isDowngrade(Plan previous, Plan next) {
        return isLowerLimit(previous.dataAllowanceMb(), next.dataAllowanceMb())
                || isLowerLimit(previous.callMinutes(), next.callMinutes())
                || isLowerLimit(previous.textMessages(), next.textMessages())
                || next.maxSignalTier().ordinal() > previous.maxSignalTier().ordinal()
                || (previous.allowedNetworks().isEmpty() && !next.allowedNetworks().isEmpty())
                || (!previous.allowedNetworks().isEmpty()
                && !next.allowedNetworks().isEmpty()
                && !previous.allowedNetworks().containsAll(next.allowedNetworks()))
                || !previous.carrierNetworkId().equals(next.carrierNetworkId());
    }

    private static boolean isLowerLimit(long oldLimit, long newLimit) {
        return oldLimit < 0 ? newLimit >= 0 : newLimit >= 0 && newLimit < oldLimit;
    }

    private static boolean isLowerLimit(int oldLimit, int newLimit) {
        return oldLimit < 0 ? newLimit >= 0 : newLimit >= 0 && newLimit < oldLimit;
    }

    private static Plan planFromRecord(String id, com.example.mobilecellnetwork.data.NetworkSavedData.PlanRecord record) {
        Map<String, String> values = record.attributes();
        return new Plan(id, record.name(), values.getOrDefault("description", ""),
                Long.parseLong(values.getOrDefault("price", "0")),
                Integer.parseInt(values.getOrDefault("billingCycleDays", "30")),
                Long.parseLong(values.getOrDefault("dataAllowanceMb", "-1")),
                DataSpeed.valueOf(values.getOrDefault("dataSpeed", "MEDIUM")),
                SignalTier.valueOf(values.getOrDefault("maxSignalTier", "FIVE_G_PLUS")),
                Integer.parseInt(values.getOrDefault("callMinutes", "-1")),
                Integer.parseInt(values.getOrDefault("textMessages", "-1")),
                values.getOrDefault("allowedNetworks", "").isBlank() ? List.of()
                        : List.of(values.get("allowedNetworks").split(",")),
                Boolean.parseBoolean(values.getOrDefault("autoRenew", "false")),
                Boolean.parseBoolean(values.getOrDefault("isDefault", "false")),
                Boolean.parseBoolean(values.getOrDefault("allowRoaming", "false")),
                Long.parseLong(values.getOrDefault("roamingFeePerMinute", "5")),
                Long.parseLong(values.getOrDefault("roamingFeePerMb", "1")),
                values.getOrDefault("carrierNetworkId", ""),
                RoamingMode.parse(values.getOrDefault("roamingMode", "CHARGED")),
                Boolean.parseBoolean(values.getOrDefault("allowOverage", "false")),
                Long.parseLong(values.getOrDefault("overageRatePerMinute", "0")),
                Long.parseLong(values.getOrDefault("overageRatePerText", "0")),
                Long.parseLong(values.getOrDefault("overageRatePerMb", "0")));
    }

    public static List<Plan> getAllPlans() {
        return PLANS.values().stream().sorted(Comparator.comparing(Plan::id)).toList();
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static long longValue(JsonObject json, String key, long fallback) {
        return json.has(key) ? json.get(key).getAsLong() : fallback;
    }

    private static int intValue(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static boolean boolValue(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static <T extends Enum<T>> T enumValue(JsonObject json, String key, Class<T> type, T fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        return Enum.valueOf(type, json.get(key).getAsString().toUpperCase(java.util.Locale.ROOT));
    }

    public enum RoamingMode {
        CHARGED,
        UNCHARGED;

        public static RoamingMode parse(String value) {
            try {
                return valueOf(value.toUpperCase(java.util.Locale.ROOT));
            } catch (RuntimeException exception) {
                return CHARGED;
            }
        }
    }

    public enum DataSpeed {
        LOW,
        MEDIUM,
        HIGH,
        FULL
    }

    public record Plan(String id, String displayName, String description, long price, int billingCycleDays,
                       long dataAllowanceMb, DataSpeed dataSpeed, SignalTier maxSignalTier,
                       int callMinutes, int textMessages, List<String> allowedNetworks,
                       boolean autoRenew, boolean isDefault, boolean allowRoaming,
                       long roamingFeePerMinute, long roamingFeePerMb, String carrierNetworkId,
                       RoamingMode roamingMode, boolean allowOverage, long overageRatePerMinute,
                       long overageRatePerText, long overageRatePerMb) {
        public Plan {
            if (id == null || displayName == null || description == null || price < 0
                    || billingCycleDays <= 0 || dataAllowanceMb < -1
                    || callMinutes < -1 || textMessages < -1 || dataSpeed == null || maxSignalTier == null
                    || roamingFeePerMinute < 0 || roamingFeePerMb < 0 || carrierNetworkId == null
                    || roamingMode == null || overageRatePerMinute < 0 || overageRatePerText < 0
                    || overageRatePerMb < 0) {
                throw new IllegalArgumentException("Plan fields are invalid.");
            }
            allowedNetworks = List.copyOf(allowedNetworks);
        }

        public Plan(String id, String displayName, String description, long price, int billingCycleDays,
                    long dataAllowanceMb, DataSpeed dataSpeed, SignalTier maxSignalTier,
                    int callMinutes, int textMessages, List<String> allowedNetworks,
                    boolean autoRenew, boolean isDefault, boolean allowRoaming,
                    long roamingFeePerMinute, long roamingFeePerMb, String carrierNetworkId) {
            this(id, displayName, description, price, billingCycleDays, dataAllowanceMb, dataSpeed,
                    maxSignalTier, callMinutes, textMessages, allowedNetworks, autoRenew, isDefault,
                    allowRoaming, roamingFeePerMinute, roamingFeePerMb, carrierNetworkId,
                    RoamingMode.CHARGED, false, 0L, 0L, 0L);
        }

        public Plan(String id, String displayName, String description, long price, int billingCycleDays,
                    long dataAllowanceMb, DataSpeed dataSpeed, SignalTier maxSignalTier,
                    int callMinutes, int textMessages, List<String> allowedNetworks,
                    boolean autoRenew, boolean isDefault) {
            this(id, displayName, description, price, billingCycleDays, dataAllowanceMb, dataSpeed,
                    maxSignalTier, callMinutes, textMessages, allowedNetworks, autoRenew, isDefault,
                    false, 5L, 1L, "");
        }

        public Plan(String id, String displayName, String description, long price, int billingCycleDays,
                    long dataAllowanceMb, DataSpeed dataSpeed, SignalTier maxSignalTier,
                    int callMinutes, int textMessages, List<String> allowedNetworks,
                    boolean autoRenew, boolean isDefault, boolean allowRoaming,
                    long roamingFeePerMinute, long roamingFeePerMb) {
            this(id, displayName, description, price, billingCycleDays, dataAllowanceMb, dataSpeed,
                    maxSignalTier, callMinutes, textMessages, allowedNetworks, autoRenew, isDefault,
                    allowRoaming, roamingFeePerMinute, roamingFeePerMb, "");
        }

        public Plan(String id, SignalTier maxSignalTier, int maxTexts, int maxMinutes) {
            this(id, id, "", 0L, 30, -1L, DataSpeed.FULL, maxSignalTier,
                    maxMinutes, maxTexts, List.of(), false, false, false, 5L, 1L, "");
        }

        public Plan(String id, SignalTier maxSignalTier) {
            this(id, maxSignalTier,
                    com.example.mobilecellnetwork.config.MobileCellNetworkConfig.DEFAULT_PLAN_TEXT_LIMIT.get(),
                    com.example.mobilecellnetwork.config.MobileCellNetworkConfig.DEFAULT_PLAN_MINUTE_LIMIT.get());
        }

        public int maxTexts() {
            return textMessages;
        }

        public int maxMinutes() {
            return callMinutes;
        }
    }
}

package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.AntennaPowerState;
import com.example.mobilecellnetwork.blockentity.AntennaRoamingPolicy;
import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity;
import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import com.example.mobilecellnetwork.capabilities.PlanUsage;
import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.plans.PlanRegistry;
import com.example.mobilecellnetwork.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class CoverageEngine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int UPDATE_INTERVAL_TICKS = 20;
    private static final Map<UUID, LastSignal> LAST_SIGNALS = new HashMap<>();
    private static final Map<UUID, Double> PARTIAL_DRAIN = new HashMap<>();
    private static final Map<RoamingCacheKey, CachedRoamingDecision> ROAMING_DECISIONS = new HashMap<>();
    private static final Map<UUID, ConnectionState> CONNECTIONS = new HashMap<>();
    private static final Map<UUID, Long> ROAMING_FEES = new HashMap<>();

    private CoverageEngine() {
    }

    public static SignalTier getSignalAt(ServerLevel level, BlockPos playerPos, String planId) {
        AntennaBlockEntity strongest = strongestAntennaAt(level, playerPos, planId);
        if (strongest == null) {
            return SignalTier.NO_SERVICE;
        }
        return updateSignalAt(level, Vec3.atCenterOf(playerPos), strongest, planId, null).tier();
    }

    public static SignalResult updateSignal(ServerPlayer player, AntennaBlockEntity antenna) {
        if (antenna == null) {
            return new SignalResult(SignalTier.NO_SERVICE, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0);
        }
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        String planId = usage.isCycleActive() ? usage.getActivePlanId() : PlanRegistry.getDefaultPlan().id();
        return updateSignalAt(player.serverLevel(), player.position(), antenna, planId,
                player.getData(ModCapabilities.PHONE_DATA));
    }

    public static SignalResult updateTestSignal(ServerPlayer player, AntennaBlockEntity antenna) {
        if (antenna == null) {
            return new SignalResult(SignalTier.NO_SERVICE, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0);
        }
        return updateSignalAt(player.serverLevel(), player.position(), antenna, null,
                player.getData(ModCapabilities.PHONE_DATA));
    }

    private static SignalResult updateSignalAt(
            ServerLevel level, Vec3 playerPos, AntennaBlockEntity antenna, String planId,
            com.example.mobilecellnetwork.capabilities.PhoneData phoneData
    ) {
        SignalResult best = directSignalAt(level, playerPos, antenna, planId, phoneData);
        for (SignalBoosterBlockEntity booster : SignalBoosterBlockEntity.nearby(level, playerPos)) {
            SignalResult upstream = directSignalAt(level, booster.center(), antenna, planId, phoneData);
            if (upstream.tier() == SignalTier.NO_SERVICE) continue;
            double distance = playerPos.distanceTo(booster.center());
            LineOfSight.Result los = LineOfSight.compute(level, playerPos, booster.center());
            double local = Math.max(0.0D, 1.0D - distance / SignalBoosterBlockEntity.RANGE
                    - los.attenuation() * MobileCellNetworkConfig.LOS_WEIGHT.get());
            double strength = Math.min(upstream.finalStrength(), local);
            SignalTier tier = applyTierCap(strengthToTier(strength), upstream.tier());
            if (tier != SignalTier.NO_SERVICE && strength > best.finalStrength()) {
                best = new SignalResult(tier, strength, distance, SignalBoosterBlockEntity.RANGE,
                        local, los.attenuation(), los.sampleCount());
            }
        }
        return best;
    }

    private static SignalResult directSignalAt(
            ServerLevel level, Vec3 playerPos, AntennaBlockEntity antenna, String planId,
            com.example.mobilecellnetwork.capabilities.PhoneData phoneData
    ) {
        if (antenna.getPowerState() == AntennaPowerState.UNPOWERED) {
            return new SignalResult(SignalTier.NO_SERVICE, 0.0D, playerPos.distanceTo(antenna.center()),
                    0.0D, 0.0D, 0.0D, 0);
        }
        if (phoneData != null && phoneData.isDead()) {
            return new SignalResult(SignalTier.NO_SERVICE, 0.0D, playerPos.distanceTo(antenna.center()),
                    0.0D, 0.0D, 0.0D, 0);
        }
        double distance = playerPos.distanceTo(antenna.center());
        double effectiveRange = antenna.getRange();
        if (antenna.getPowerState() == AntennaPowerState.LOW_POWER) {
            effectiveRange *= MobileCellNetworkConfig.LOW_POWER_RANGE_MULTIPLIER.get();
        }
        if (effectiveRange <= 0.0D
                || distance > effectiveRange * MobileCellNetworkConfig.INVERSE_MAX_RANGE_MULTIPLIER.get()) {
            return new SignalResult(SignalTier.NO_SERVICE, 0.0D, distance, effectiveRange, 0.0D, 0.0D, 0);
        }

        double ratio = distance / effectiveRange;
        double baseStrength = switch (MobileCellNetworkConfig.SIGNAL_FALLOFF_MODE.get()) {
            case INVERSE -> 1.0D / (1.0D + MobileCellNetworkConfig.INVERSE_FALLOFF_K.get() * ratio);
            case LINEAR -> Math.max(0.0D, 1.0D - ratio);
            case QUADRATIC -> Math.max(0.0D, 1.0D - ratio * ratio);
            case STEPPED -> Math.floor(Math.max(0.0D, 1.0D - ratio) * 10.0D) / 10.0D;
        };
        LineOfSight.Result los = LineOfSight.compute(level, playerPos, antenna.center());
        double finalStrength = Math.max(0.0D,
                baseStrength - los.attenuation() * MobileCellNetworkConfig.LOS_WEIGHT.get());
        SignalTier tier = strengthToTier(finalStrength);
        tier = applyTierCap(tier, antenna.getTier().maxSignalTier());
        if (planId != null) {
            SignalTier planCap = PlanRegistry.getPlan(planId).maxSignalTier();
            tier = applyTierCap(tier, planCap);
        }
        if (phoneData != null && phoneData.getCharge() > 0 && phoneData.isLowPower()
                && MobileCellNetworkConfig.LOW_POWER_ENABLED.get()) {
            tier = applyTierCap(tier, MobileCellNetworkConfig.LOW_POWER_MAX_SIGNAL_TIER.get());
        }
        if (antenna.getPowerState() == AntennaPowerState.LOW_POWER) {
            tier = applyTierCap(tier, MobileCellNetworkConfig.LOW_POWER_MAX_TIER.get());
        }
        return new SignalResult(tier, finalStrength, distance, effectiveRange, baseStrength,
                los.attenuation(), los.sampleCount());
    }

    public static SignalTier strengthToTier(double strength) {
        if (strength <= 0.0D) return SignalTier.NO_SERVICE;
        Map<String, Double> thresholds = configuredSignalThresholds();
        for (SignalTier tier : SignalTier.values()) {
            if (tier == SignalTier.NO_SERVICE) continue;
            double threshold = thresholds.getOrDefault(tier.name(), 0.0D);
            if (strength >= threshold) return tier;
        }
        return SignalTier.E;
    }

    static Map<String, Double> configuredSignalThresholds() {
        Map<String, Double> thresholds = defaultSignalThresholds();
        for (String entry : MobileCellNetworkConfig.SIGNAL_THRESHOLDS.get()) {
            String[] parts = entry.split("=", -1);
            if (parts.length != 2) {
                continue;
            }
            try {
                thresholds.put(parts[0], Double.parseDouble(parts[1]));
            } catch (NumberFormatException exception) {
                LOGGER.warn("Ignoring invalid signal threshold config entry '{}'.", entry);
            }
        }
        return thresholds;
    }

    private static Map<String, Double> defaultSignalThresholds() {
        Map<String, Double> thresholds = new HashMap<>();
        thresholds.put("FIVE_G_PLUS", 0.95D);
        thresholds.put("FIVE_G", 0.85D);
        thresholds.put("FOUR_G_PLUS", 0.75D);
        thresholds.put("FOUR_G", 0.65D);
        thresholds.put("LTE", 0.55D);
        thresholds.put("THREE_G", 0.45D);
        thresholds.put("H_PLUS", 0.35D);
        thresholds.put("H", 0.25D);
        thresholds.put("TWO_G", 0.15D);
        thresholds.put("E_PLUS", 0.08D);
        return thresholds;
    }

    private static SignalTier applyTierCap(SignalTier tier, SignalTier cap) {
        return tier.ordinal() < cap.ordinal() ? cap : tier;
    }

    public record SignalResult(
            SignalTier tier, double finalStrength, double distance, double effectiveRange,
            double baseStrength, double losAttenuation, int sampleCount
    ) {
    }

    private static final Map<UUID, Long> PREFERRED_TOWER = new HashMap<>();

    private static List<AntennaSignal> candidateTowers(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double candidateRange = configuredMaximumRange()
                * MobileCellNetworkConfig.INVERSE_MAX_RANGE_MULTIPLIER.get() + SignalBoosterBlockEntity.RANGE;
        return findLoadedAntennas(level, player.blockPosition(), (int) Math.ceil(candidateRange)).stream()
                .filter(CoverageEngine::isOperational)
                .filter(antenna -> isNetworkAllowed(level, antenna))
                .filter(antenna -> isNetworkOnline(level, antenna))
                .filter(antenna -> !(antenna instanceof TestAntennaBlockEntity)
                        || hasActivePlanAndNetwork(player, antenna))
                .filter(antenna -> roamingDecision(player, antenna).allowed())
                .map(antenna -> new AntennaSignal(antenna, updateSignal(player, antenna)))
                .filter(candidate -> candidate.signal().tier() != SignalTier.NO_SERVICE)
                .toList();
    }

    private static boolean hasActivePlanAndNetwork(ServerPlayer player, AntennaBlockEntity antenna) {
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        return usage.isCycleActive() && !usage.getActivePlanId().isBlank()
                && PlanRegistry.findPlan(usage.getActivePlanId()) != null
                && NetworkSavedData.get(player.server.overworld()).getNetwork(antenna.getNetworkId()) != null;
    }

    public static AntennaBlockEntity getStrongestTestAntennaFor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int radius = (int) Math.ceil(configuredMaximumRange()
                * MobileCellNetworkConfig.INVERSE_MAX_RANGE_MULTIPLIER.get() + SignalBoosterBlockEntity.RANGE);
        return findLoadedAntennas(level, player.blockPosition(), radius).stream()
                .filter(CoverageEngine::isOperational)
                .filter(antenna -> antenna instanceof TestAntennaBlockEntity
                        || (isNetworkAllowed(level, antenna) && isNetworkOnline(level, antenna)))
                .map(antenna -> new AntennaSignal(antenna, updateTestSignal(player, antenna)))
                .filter(candidate -> candidate.signal().tier() != SignalTier.NO_SERVICE)
                .max(Comparator.comparingDouble(candidate -> candidate.signal().finalStrength()))
                .map(AntennaSignal::antenna)
                .orElse(null);
    }

    public static void setPreferredTower(ServerPlayer player, Long pos) {
        if (pos == null) {
            PREFERRED_TOWER.remove(player.getUUID());
        } else {
            PREFERRED_TOWER.put(player.getUUID(), pos);
        }
    }

    public static void sendTowerList(ServerPlayer player) {
        List<AntennaSignal> candidates = candidateTowers(player);
        ConnectionState connection = CONNECTIONS.get(player.getUUID());
        Long preferred = preferredTower(player);
        List<TowerListPacket.Entry> entries = candidates.stream()
                .sorted(Comparator.comparingDouble((AntennaSignal c) -> c.signal().finalStrength()).reversed())
                .map(c -> new TowerListPacket.Entry(c.antenna().getBlockPos().asLong(),
                        getNetworkName(player.serverLevel(), c.antenna()), c.signal().tier().name(),
                        (int) Math.round(Math.min(1.0D, c.signal().finalStrength()) * 100.0D),
                        roamingDecision(player, c.antenna()).roaming()))
                .toList();
        PacketDistributor.sendToPlayer(player, new TowerListPacket(entries,
                connection == null ? Long.MIN_VALUE : connection.antenna().getBlockPos().asLong(),
                preferred == null ? Long.MIN_VALUE : preferred));
    }

    public static void sendMapAntennaCache(ServerPlayer player, int centerX, int centerZ, int resolution) {
        int blocksPerTile = Math.clamp(resolution, 1, 4);
        int halfWidth = 24 * blocksPerTile;
        int halfHeight = 28 * blocksPerTile;
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        String planId = usage.isCycleActive() ? usage.getActivePlanId() : PlanRegistry.getDefaultPlan().id();
        double maxRangeMultiplier = MobileCellNetworkConfig.INVERSE_MAX_RANGE_MULTIPLIER.get();
        List<AntennaCachePacket.Antenna> entries = AntennaBlockEntity.loadedAntennas(player.serverLevel()).stream()
                .filter(CoverageEngine::isOperational)
                .filter(antenna -> isNetworkAllowed(player.serverLevel(), antenna))
                .filter(antenna -> isNetworkOnline(player.serverLevel(), antenna))
                .filter(antenna -> isPlanNetworkAllowed(planId, antenna))
                .filter(antenna -> roamingDecision(player, antenna).allowed())
                .filter(antenna -> {
                    long dx = Math.max(0L, Math.abs((long) antenna.getBlockPos().getX() - centerX) - halfWidth);
                    long dz = Math.max(0L, Math.abs((long) antenna.getBlockPos().getZ() - centerZ) - halfHeight);
                    double radius = effectiveRange(antenna) * maxRangeMultiplier;
                    return dx * (double) dx + dz * (double) dz <= radius * radius;
                })
                .sorted(Comparator.comparingDouble(antenna -> {
                    double dx = antenna.getBlockPos().getX() - (double) centerX;
                    double dz = antenna.getBlockPos().getZ() - (double) centerZ;
                    return dx * dx + dz * dz;
                }))
                .limit(1024)
                .map(antenna -> new AntennaCachePacket.Antenna(antenna.getBlockPos().getX(),
                        antenna.getBlockPos().getY(), antenna.getBlockPos().getZ(),
                        effectiveRange(antenna), effectiveTier(antenna).name()))
                .toList();
        PacketDistributor.sendToPlayer(player, new AntennaCachePacket(player.getUUID(),
                player.serverLevel().dimension().location().toString(), entries));
    }

    public static AntennaBlockEntity getStrongestAntennaFor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<AntennaSignal> candidates = candidateTowers(player);
        PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
        Long preferred = preferredTower(player);
        AntennaBlockEntity strongest = null;
        if (preferred != null) {
            strongest = candidates.stream()
                    .filter(candidate -> candidate.antenna().getBlockPos().asLong() == preferred)
                    .map(AntennaSignal::antenna).findFirst().orElse(null);
        }
        if (strongest == null) {
            java.util.stream.Stream<AntennaSignal> automaticCandidates = candidates.stream();
            if (phone.getTowerMode() == com.example.mobilecellnetwork.data.TowerPreferenceMode.AUTOMATIC_PRIMARY) {
                List<AntennaSignal> homeCandidates = candidates.stream()
                        .filter(candidate -> !roamingDecision(player, candidate.antenna()).roaming())
                        .toList();
                if (!homeCandidates.isEmpty()) {
                    automaticCandidates = homeCandidates.stream();
                }
            }
            strongest = automaticCandidates
                    .max(Comparator.comparingDouble(candidate -> candidate.signal().finalStrength()))
                    .map(AntennaSignal::antenna).orElse(null);
        }
        if (strongest == null) {
            CONNECTIONS.remove(player.getUUID());
        } else {
            RoamingDecision decision = roamingDecision(player, strongest);
            CONNECTIONS.put(player.getUUID(),
                    new ConnectionState(strongest, decision.roaming(), decision.paid()));
        }
        return strongest;
    }

    private static Long preferredTower(ServerPlayer player) {
        Long selected = PREFERRED_TOWER.get(player.getUUID());
        if (selected == null) {
            PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
            if (phone.getTowerMode() == com.example.mobilecellnetwork.data.TowerPreferenceMode.PINNED
                    && phone.getPinnedTowerPos() != null) {
                selected = phone.getPinnedTowerPos().asLong();
                PREFERRED_TOWER.put(player.getUUID(), selected);
            }
        }
        return selected;
    }

    public static boolean isRoaming(ServerPlayer player) {
        ConnectionState connection = CONNECTIONS.get(player.getUUID());
        return connection != null && connection.paidRoaming();
    }

    public static boolean isRoamingConnection(ServerPlayer player) {
        ConnectionState connection = CONNECTIONS.get(player.getUUID());
        return connection != null && connection.roaming();
    }

    public static long roamingFeesThisSession(ServerPlayer player) {
        return ROAMING_FEES.getOrDefault(player.getUUID(), 0L);
    }

    public static void recordRoamingFee(ServerPlayer player, long fee) {
        if (fee <= 0L) {
            return;
        }
        ROAMING_FEES.merge(player.getUUID(), fee, (current, added) ->
                current > Long.MAX_VALUE - added ? Long.MAX_VALUE : current + added);
    }

    public static boolean chargeRoamingFee(ServerPlayer player, long fee, String reason) {
        return com.example.mobilecellnetwork.billing.BillingManager.chargeRoamingFee(player, fee, reason);
    }

    private static RoamingDecision roamingDecision(ServerPlayer player, AntennaBlockEntity antenna) {
        long now = player.serverLevel().getGameTime();
        RoamingCacheKey key = new RoamingCacheKey(player.getUUID(),
                antenna.getLevel().dimension().location().toString(), antenna.getBlockPos().asLong());
        CachedRoamingDecision cached = ROAMING_DECISIONS.get(key);
        if (cached != null && cached.expiresAtTick() > now) {
            return cached.decision();
        }

        String networkId = antenna.getNetworkId();
        PlanRegistry.Plan plan = PlanRegistry.getPlan(player.getData(ModCapabilities.PLAN_USAGE).getActivePlanId());
        boolean homeNetwork = plan.carrierNetworkId().isBlank()
                ? plan.allowedNetworks().isEmpty() || plan.allowedNetworks().contains(networkId)
                : plan.carrierNetworkId().equals(networkId)
                        || antenna.getSharedWithNetworkIds().contains(plan.carrierNetworkId());
        RoamingDecision decision;
        if (homeNetwork) {
            decision = new RoamingDecision(true, false, false);
        } else {
            NetworkSavedData savedData = NetworkSavedData.get(player.serverLevel().getServer().overworld());
            var hostSettings = savedData.getNetworkSettings(networkId);
            var visitingSettings = savedData.getNetworkSettings(plan.carrierNetworkId());
            var hostAgreement = hostSettings == null ? null
                    : hostSettings.getAgreements().get(plan.carrierNetworkId());
            var visitingAgreement = visitingSettings == null ? null
                    : visitingSettings.getAgreements().get(networkId);
            boolean networkAllowed = plan.allowedNetworks().isEmpty()
                    || plan.allowedNetworks().contains(networkId);
            boolean agreementMatches;
            if (hostAgreement != null && visitingAgreement != null) {
                agreementMatches = hostAgreement.ratePerMinute() == visitingAgreement.ratePerMinute()
                        && hostAgreement.ratePerText() == visitingAgreement.ratePerText()
                        && hostAgreement.ratePerMb() == visitingAgreement.ratePerMb();
            } else {
                Long hostApproval = savedData.getRoamingAgreementFee(networkId, plan.carrierNetworkId());
                Long visitingApproval = savedData.getRoamingAgreementFee(plan.carrierNetworkId(), networkId);
                agreementMatches = hostApproval != null && hostApproval.equals(visitingApproval)
                        && hostApproval == plan.roamingFeePerMinute();
            }
            agreementMatches &= networkAllowed;
            decision = new RoamingDecision(plan.allowRoaming() && agreementMatches,
                    plan.allowRoaming() && agreementMatches, plan.allowRoaming() && agreementMatches);
        }
        ROAMING_DECISIONS.put(key, new CachedRoamingDecision(decision, now + 20L));
        if (ROAMING_DECISIONS.size() > 8192) {
            ROAMING_DECISIONS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick() <= now);
        }
        return decision;
    }

    public static List<AntennaBlockEntity> findLoadedAntennas(Level level, BlockPos center, int radius) {
        double radiusSquared = (double) radius * radius;
        List<AntennaBlockEntity> found = new ArrayList<>();
        for (AntennaBlockEntity antenna : AntennaBlockEntity.loadedAntennas(level)) {
            if (antenna.getBlockPos().distSqr(center) <= radiusSquared) {
                found.add(antenna);
            }
        }
        return found;
    }

    public static int getAntennaCountInRange(ServerLevel level, BlockPos playerPos) {
        int maxRange = configuredMaximumRange();
        return (int) findLoadedAntennas(level, playerPos, maxRange).stream()
                .filter(antenna -> isOperational(antenna)
                        && antenna.getBlockPos().distSqr(playerPos) <= effectiveRange(antenna) * effectiveRange(antenna))
                .filter(antenna -> isNetworkAllowed(level, antenna)).count();
    }

    private record BeamLight(net.minecraft.resources.ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final Map<UUID, List<BeamLight>> BEAMS = new HashMap<>();

    private static void clearBeam(net.minecraft.server.MinecraftServer server, UUID id) {
        List<BeamLight> lights = BEAMS.remove(id);
        if (lights == null) return;
        for (BeamLight light : lights) {
            ServerLevel level = server.getLevel(light.dimension());
            if (level != null && level.getBlockState(light.pos()).is(net.minecraft.world.level.block.Blocks.LIGHT)) {
                level.setBlock(light.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void updateBeam(ServerPlayer player) {
        var server = player.server;
        UUID id = player.getUUID();
        if (!player.getData(ModCapabilities.PHONE_DATA).isFlashlightOn()) {
            clearBeam(server, id);
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(7.0D));
        var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        Vec3 tip = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? end : hit.getLocation();
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            tip = tip.add(eye.subtract(tip).normalize().scale(0.6D));
        }
        List<BeamLight> wanted = new ArrayList<>();
        double length = tip.subtract(eye).length();
        for (double d = 2.0D; d <= 7.0D; d += 2.5D) {
            if (d > length) break;
            BlockPos pos = BlockPos.containing(eye.add(tip.subtract(eye).normalize().scale(d)));
            if (wanted.stream().anyMatch(light -> light.pos().equals(pos))) continue;
            wanted.add(new BeamLight(level.dimension(), pos));
        }
        List<BeamLight> previous = BEAMS.get(id);
        if (wanted.equals(previous)) return;
        clearBeam(server, id);
        List<BeamLight> placed = new ArrayList<>();
        for (BeamLight light : wanted) {
            if (level.getBlockState(light.pos()).isAir()) {
                level.setBlock(light.pos(), net.minecraft.world.level.block.Blocks.LIGHT.defaultBlockState(), 3);
                placed.add(light);
            }
        }
        BEAMS.put(id, placed);
    }

    public static void onPlayerLogout(ServerPlayer player) {
        PREFERRED_TOWER.remove(player.getUUID());
        setFlashlight(player, false);
        clearBeam(player.server, player.getUUID());
        RoamingManager.onPlayerLogout(player);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 3 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                updateBeam(player);
            }
        }
        if (event.getServer().getTickCount() % UPDATE_INTERVAL_TICKS != 0) {
            return;
        }
        AntennaBlockEntity.serverTickAll(event.getServer());
        List<AntennaBlockEntity> antennas = AntennaBlockEntity.loadedAntennas(event.getServer());
        antennas.forEach(AntennaBlockEntity::clearUsers);
        Set<UUID> onlinePlayers = new java.util.HashSet<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!hasPhone(player)) {
                setFlashlight(player, false);
                continue;
            }
            onlinePlayers.add(player.getUUID());
            PlanUsage planUsage = player.getData(ModCapabilities.PLAN_USAGE);
            AntennaBlockEntity antenna = getStrongestAntennaFor(player);
            if (antenna != null) {
                antenna.addUser(player.getUUID());
            }
            drainPhone(player);
            boolean testing = NetworkSavedData.get(player.server.overworld()).isSignalTestEnabled(player.getUUID());
            AntennaBlockEntity displayAntenna = testing ? getStrongestTestAntennaFor(player) : antenna;
            SignalResult signalResult = testing ? updateTestSignal(player, displayAntenna)
                    : updateSignal(player, antenna);
            SignalTier signal = signalResult.tier();
            String networkName = displayAntenna == null ? ""
                    : testing ? "TEST " + getNetworkName(player.serverLevel(), displayAntenna)
                    : displayNetworkName(player, antenna);
            ConnectionState connection = CONNECTIONS.get(player.getUUID());
            boolean realRoaming = connection != null && connection.roaming();
            RoamingManager.notifyRoaming(player, realRoaming,
                    antenna == null ? "" : testing ? displayNetworkName(player, antenna) : networkName);
            boolean roaming = !testing && realRoaming;
            long roamingFees = roamingFeesThisSession(player);
            String networkId = displayAntenna == null ? "" : displayAntenna.getNetworkId();
            int count = getAntennaCountInRange(player.serverLevel(), player.blockPosition());
            LastSignal current = new LastSignal(signal, networkName, networkId,
                    player.serverLevel().dimension().location().toString(), count,
                    displayAntenna == null ? "" : displayAntenna.getPowerState().name(), signalResult.finalStrength(),
                    testing || MobileCellNetworkConfig.DEBUG_SIGNAL_INFO.get(), roaming, roamingFees);
            if (!current.equals(LAST_SIGNALS.get(player.getUUID()))) {
                boolean debug = testing || MobileCellNetworkConfig.DEBUG_SIGNAL_INFO.get();
                int signalBars = testing || displayAntenna instanceof TestAntennaBlockEntity
                        ? SignalBarCalculator.computeCoverageBars(signalResult.finalStrength(), signal)
                        : SignalBarCalculator.computeBars(signalResult.finalStrength(), signal);
                PacketDistributor.sendToPlayer(player, new SignalUpdatePacket(signal, networkName, networkId, count,
                        signalBars, signalResult.finalStrength(), debug,
                        debug ? signalResult.distance() : 0.0D,
                        debug ? signalResult.effectiveRange() : 0.0D,
                        debug ? signalResult.baseStrength() : 0.0D,
                        debug ? signalResult.losAttenuation() : 0.0D,
                        debug ? signalResult.sampleCount() : 0, roaming, roamingFees));
                LAST_SIGNALS.put(player.getUUID(), current);
            }
        }
        LAST_SIGNALS.keySet().removeIf(playerId -> !onlinePlayers.contains(playerId));
        CONNECTIONS.keySet().removeIf(playerId -> !onlinePlayers.contains(playerId));
        ROAMING_FEES.keySet().removeIf(playerId -> !onlinePlayers.contains(playerId));
        ROAMING_DECISIONS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick()
                <= event.getServer().overworld().getGameTime());
        for (AntennaBlockEntity antenna : antennas) {
            if (antenna.getLevel() instanceof ServerLevel level) {
                antenna.updateChunkTicket(level);
            }
        }
    }

    private static AntennaBlockEntity strongestAntennaAt(ServerLevel level, BlockPos position, String planId) {
        return findLoadedAntennas(level, position,
                (int) Math.ceil(configuredMaximumRange() * MobileCellNetworkConfig.INVERSE_MAX_RANGE_MULTIPLIER.get()
                        + SignalBoosterBlockEntity.RANGE)).stream()
                .filter(CoverageEngine::isOperational)
                .filter(antenna -> isNetworkAllowed(level, antenna))
                .filter(antenna -> isNetworkOnline(level, antenna))
                .filter(antenna -> !(antenna instanceof TestAntennaBlockEntity)
                        || (!planId.isBlank() && PlanRegistry.findPlan(planId) != null
                        && NetworkSavedData.get(level.getServer().overworld()).getNetwork(antenna.getNetworkId()) != null))
                .filter(antenna -> isPlanNetworkAllowed(planId, antenna))
                .map(antenna -> new AntennaSignal(antenna,
                        updateSignalAt(level, Vec3.atCenterOf(position), antenna, "default", null)))
                .filter(candidate -> candidate.signal().tier() != SignalTier.NO_SERVICE)
                .max(Comparator.comparingDouble(candidate -> candidate.signal().finalStrength()))
                .map(AntennaSignal::antenna)
                .orElse(null);
    }

    private static boolean isOperational(AntennaBlockEntity antenna) {
        return antenna.isEnabled() && antenna.getPowerState() != AntennaPowerState.UNPOWERED;
    }

    private static double effectiveRange(AntennaBlockEntity antenna) {
        double range = antenna.getTier().range();
        return antenna.getPowerState() == AntennaPowerState.LOW_POWER
                ? range * MobileCellNetworkConfig.LOW_POWER_RANGE_MULTIPLIER.get() : range;
    }

    private static SignalTier effectiveTier(AntennaBlockEntity antenna) {
        SignalTier tier = antenna.getTier().maxSignalTier();
        if (antenna.getPowerState() == AntennaPowerState.LOW_POWER
                && tier.ordinal() < MobileCellNetworkConfig.LOW_POWER_MAX_TIER.get().ordinal()) {
            return MobileCellNetworkConfig.LOW_POWER_MAX_TIER.get();
        }
        return tier;
    }

    private static boolean isNetworkAllowed(ServerLevel level, AntennaBlockEntity antenna) {
        NetworkSavedData.NetworkRecord network = NetworkSavedData
                .get(level.getServer().overworld()).getNetwork(antenna.getNetworkId());
        return network == null || network.allowed();
    }

    private static boolean isNetworkOnline(ServerLevel level, AntennaBlockEntity antenna) {
        NetworkSavedData data = NetworkSavedData.get(level.getServer().overworld());
        var network = data.getNetwork(antenna.getNetworkId());
        if (network == null) {
            return MobileCellNetworkConfig.ALLOW_NETWORK_WITHOUT_SERVER_BLOCK.get();
        }
        var settings = data.getNetworkSettings(antenna.getNetworkId());
        if (settings == null) {
            return true;
        }
        if (!settings.hasServerBlock()) {
            return MobileCellNetworkConfig.ALLOW_NETWORK_WITHOUT_SERVER_BLOCK.get();
        }
        return isServerOnline(level.getServer(), settings);
    }

    public static boolean isServerOnline(net.minecraft.server.MinecraftServer server,
                                         com.example.mobilecellnetwork.data.NetworkSettings settings) {
        if (!settings.hasServerBlock() || !settings.isServerOnline()) return false;
        var dimension = net.minecraft.resources.ResourceLocation.tryParse(settings.getServerBlockDimension());
        if (dimension == null) return false;
        ServerLevel level = server.getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, dimension));
        BlockPos pos = BlockPos.of(settings.getServerBlockPos());
        return level != null && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof com.example.mobilecellnetwork.blockentity.ServerBlockEntity rack
                && rack.isPowered();
    }

    private static boolean isPlanNetworkAllowed(String planId, AntennaBlockEntity antenna) {
        var allowedNetworks = PlanRegistry.getPlan(planId).allowedNetworks();
        return allowedNetworks.isEmpty() || allowedNetworks.contains(antenna.getNetworkId());
    }

    private static String displayNetworkName(ServerPlayer player, AntennaBlockEntity antenna) {
        ServerLevel level = player.serverLevel();
        String towerName = getNetworkName(level, antenna);
        var phoneData = player.getData(ModCapabilities.PHONE_DATA);
        PlanRegistry.Plan plan = PlanRegistry.getPlan(player.getData(ModCapabilities.PLAN_USAGE).getActivePlanId());
        String homeId = plan.carrierNetworkId();
        if (homeId == null || homeId.isBlank()) {
            if (phoneData.getHomeNetworkId().isBlank()) {
                phoneData.setHomeNetworkId(antenna.getNetworkId());
            }
            homeId = phoneData.getHomeNetworkId();
        }
        if (homeId.equals(antenna.getNetworkId())) {
            return towerName;
        }
        var home = NetworkSavedData.get(level.getServer().overworld()).getNetwork(homeId);
        return (home == null ? homeId : home.name()) + " (Roaming)";
    }

    private static String getNetworkName(ServerLevel level, AntennaBlockEntity antenna) {
        NetworkSavedData.NetworkRecord network = NetworkSavedData
                .get(level.getServer().overworld()).getNetwork(antenna.getNetworkId());
        return network == null ? antenna.getNetworkId() : network.name();
    }

    private static int configuredMaximumRange() {
        return Math.max(MobileCellNetworkConfig.COPPER_RANGE.get(),
                Math.max(MobileCellNetworkConfig.IRON_RANGE.get(), MobileCellNetworkConfig.DIAMOND_RANGE.get()));
    }

    public static void setFlashlight(ServerPlayer player, boolean on) {
        var data = player.getData(ModCapabilities.PHONE_DATA);
        if (on && (data.isDead() || data.isLowPower() || findPhone(player).isEmpty())) {
            on = false;
        }
        boolean changed = data.isFlashlightOn() != on;
        data.setFlashlightOn(on);
        if (changed) {
            PacketDistributor.sendToPlayer(player, new FlashlightPacket(on));
        }
    }

    private static void drainPhone(ServerPlayer player) {
        ItemStack phone = findPhone(player);
        if (phone.isEmpty()) {
            PARTIAL_DRAIN.remove(player.getUUID());
            return;
        }
        var phoneData = player.getData(ModCapabilities.PHONE_DATA);

        Integer itemCharge = phone.get(com.example.mobilecellnetwork.registry.ModDataComponents.PHONE_CHARGE.get());
        if (itemCharge != null && itemCharge != phoneData.getCharge()) {
            phoneData.setCharge(itemCharge);
        }
        double drain = MobileCellNetworkConfig.PHONE_DRAIN_PER_SECOND.get();
        if (phoneData.isLowPower()) {
            drain /= MobileCellNetworkConfig.LOW_POWER_DRAIN_MULTIPLIER.get();
        }
        if (phoneData.isFlashlightOn()) {
            drain *= MobileCellNetworkConfig.FLASHLIGHT_DRAIN_MULTIPLIER.get();
        }
        double accumulated = PARTIAL_DRAIN.getOrDefault(player.getUUID(), 0.0D) + drain;
        int wholeDrain = (int) accumulated;
        PARTIAL_DRAIN.put(player.getUUID(), accumulated - wholeDrain);
        int previousCharge = phoneData.getCharge();
        if (wholeDrain > 0) {
            phoneData.setCharge(Math.max(0, phoneData.getCharge() - wholeDrain));
        }
        phone.set(com.example.mobilecellnetwork.registry.ModDataComponents.PHONE_CHARGE.get(),
                phoneData.getCharge());
        PacketDistributor.sendToPlayer(player, new PhoneChargePacket(phoneData.getCharge()));
        if (phoneData.isDead() && phoneData.isFlashlightOn()) {
            setFlashlight(player, false);
        }
        if (previousCharge > 0 && phoneData.isDead()) {
            PacketDistributor.sendToPlayer(player, new PhoneDiedPacket());
        }
        double thresholdCharge = MobileCellNetworkConfig.PHONE_MAX_CHARGE.get()
                * MobileCellNetworkConfig.LOW_POWER_AUTO_PROMPT_THRESHOLD.get() / 100.0D;
        if (MobileCellNetworkConfig.LOW_POWER_ENABLED.get()
                && previousCharge >= thresholdCharge
                && phoneData.getCharge() < thresholdCharge
                && phoneData.getCharge() > 0
                && !phoneData.isLowPower()
                && !phoneData.isLowPowerPromptShownThisSession()) {
            phoneData.setLowPowerPromptShownThisSession(true);
            PacketDistributor.sendToPlayer(player, new PhoneNotificationPacket(
                    "Low Power Mode", "Your phone battery is low. Enable Low Power Mode?", true));
        }
    }

    private static ItemStack findPhone(ServerPlayer player) {
        java.util.UUID activePhoneId = PhoneDeviceService.activePhoneId(player);
        if (activePhoneId != null) {
            for (ItemStack stack : player.getInventory().items) {
                if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack)
                        && activePhoneId.equals(stack.get(com.example.mobilecellnetwork.registry.ModDataComponents
                        .PHONE_ID.get()))) {
                    return stack;
                }
            }
            for (ItemStack stack : player.getInventory().offhand) {
                if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack)
                        && activePhoneId.equals(stack.get(com.example.mobilecellnetwork.registry.ModDataComponents
                        .PHONE_ID.get()))) {
                    return stack;
                }
            }
            return ItemStack.EMPTY;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack)) return stack;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasPhone(ServerPlayer player) {
        return !findPhone(player).isEmpty();
    }

    private record AntennaSignal(AntennaBlockEntity antenna, SignalResult signal) { }
    private record LastSignal(SignalTier signal, String networkName, String networkId, String dimension,
                              int antennaCount, String powerState,
                              double finalStrength, boolean debugEnabled, boolean roaming, long roamingFees) { }
    private record RoamingCacheKey(UUID playerId, String dimension, long position) { }
    private record CachedRoamingDecision(RoamingDecision decision, long expiresAtTick) { }
    private record RoamingDecision(boolean allowed, boolean roaming, boolean paid) { }
    private record ConnectionState(AntennaBlockEntity antenna, boolean roaming, boolean paidRoaming) { }
}

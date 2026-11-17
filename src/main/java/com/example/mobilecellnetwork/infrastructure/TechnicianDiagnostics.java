package com.example.mobilecellnetwork.infrastructure;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.InfrastructurePower;
import com.example.mobilecellnetwork.blockentity.PowerTapBlockEntity;
import com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity;
import com.example.mobilecellnetwork.network.CoverageEngine;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.block.TestTowerSectionBlock;
import com.example.mobilecellnetwork.block.TowerCabinets;
import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class TechnicianDiagnostics {
    private TechnicianDiagnostics() {}

    public static void updateMeter(ServerPlayer player, ItemStack stack, BlockHitResult hit) {
        MeterReading reading = measure(player, hit);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(MeterReading.DATA_KEY, reading.save()));
    }

    private static MeterReading measure(ServerPlayer player, BlockHitResult hit) {
        var level = player.serverLevel();
        long now = level.getGameTime();
        if (hit.getType() != HitResult.Type.MISS) {
            var clicked = hit.getBlockPos();
            var state = level.getBlockState(clicked);
            var pos = state.getBlock() instanceof TestTowerSectionBlock
                    ? TestTowerSectionBlock.basePosition(clicked, state) : clicked;
            BlockEntity block = level.getBlockEntity(pos);
            if (block instanceof Serviceable service) {
                var owner = block instanceof AntennaBlockEntity antenna
                        ? antenna.getOwnerUUID() : service.installation().owner();
                if (!player.mayBuild() || !level.mayInteract(player, clicked) || !level.mayInteract(player, pos)
                        || owner != null && !owner.equals(player.getUUID()) && !player.hasPermissions(2)) {
                    return unavailable("DENIED", now);
                }
                if (block instanceof TestAntennaBlockEntity tower && tower.isWideTower()) {
                    int cabinet = TowerCabinets.at(tower.variant(), tower.variant().toModel(
                            hit.getLocation().subtract(Vec3.atLowerCornerOf(pos))));
                    if (cabinet < 0) return unavailable("AIM_CABINET", now);
                    if (!tower.isCabinetOpen(cabinet)) return unavailable("OPEN_CABINET", now);
                }
                if (block instanceof IEnergyStorage storage) {
                    boolean booster = block instanceof SignalBoosterBlockEntity;
                    Vec3 point = block instanceof SignalBoosterBlockEntity relay ? relay.center() : player.position();
                    AntennaBlockEntity antenna = block instanceof AntennaBlockEntity tower ? tower
                            : block instanceof SignalBoosterBlockEntity relay ? relay.upstream() : null;
                    var signal = radio(level, point, antenna, !booster);
                    String status = service.installation().status().name();
                    if (service.installation().ready()) {
                        if (block instanceof AntennaBlockEntity tower && !tower.isEnabled()) status = "DISABLED";
                        else if (InfrastructurePower.bypassed(level)) status = "BYPASS";
                        else if (storage.getEnergyStored() == 0) status = "NO_POWER";
                    }
                    return new MeterReading(booster ? MeterReading.Mode.BOOSTER : MeterReading.Mode.TOWER,
                            storage.getEnergyStored(), storage.getMaxEnergyStored(), signal.percent(), signal.tier(),
                            status, availableOutput(block, service.installation()), now);
                }
            }
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, clicked, hit.getDirection());
            if (storage != null) {
                boolean tap = block instanceof PowerTapBlockEntity;
                return new MeterReading(tap ? MeterReading.Mode.TAP : MeterReading.Mode.POWER,
                        storage.getEnergyStored(), storage.getMaxEnergyStored(), -1, "",
                        storage.getEnergyStored() > 0 ? "POWERED" : "NO_POWER",
                        tap ? Math.min(PowerTapStorage.RATE, storage.getEnergyStored()) : -1, now);
            }
        }
        var signal = radio(level, player.position(), strongest(level, player.position(), true), true);
        return new MeterReading(MeterReading.Mode.SURVEY, 0, 0, signal.percent(), signal.tier(),
                signal.percent() > 0 ? "RADIO_ONLY" : "NO_SERVICE", -1, now);
    }

    private static MeterReading unavailable(String status, long now) {
        return new MeterReading(MeterReading.Mode.UNAVAILABLE, 0, 0, -1, "", status, -1, now);
    }

    private static int availableOutput(BlockEntity target, Installation installation) {
        var source = installation.source();
        var level = target.getLevel();
        if (source == null || level == null || !level.hasChunkAt(source)
                || !PowerWire.canConnect(installation, target.getBlockPos())) return 0;
        var supply = level.getCapability(Capabilities.EnergyStorage.BLOCK, source, installation.sourceFace());
        return supply == null || !supply.canExtract() ? 0
                : Math.clamp(supply.extractEnergy(PowerWire.TRANSFER_PER_TICK, true), 0, PowerWire.TRANSFER_PER_TICK);
    }

    private record RadioReading(int percent, String tier) {}

    private static RadioReading radio(ServerLevel level, Vec3 point, AntennaBlockEntity antenna, boolean boosters) {
        if (antenna == null) return new RadioReading(0, SignalTier.NO_SERVICE.name());
        var result = CoverageEngine.measureRadio(level, point, antenna, boosters);
        return new RadioReading(percent(result.finalStrength()), result.tier().name());
    }

    public static void tap(Player player, PowerTapBlockEntity tap) {
        report(player, "tap", tap.energy(), PowerTapStorage.CAPACITY, PowerTapStorage.RATE);
        report(player, "tap_ports");
    }

    public static void power(Player player, Component name, net.minecraft.core.BlockPos pos, IEnergyStorage storage) {
        report(player, "header", name, pos.toShortString());
        report(player, "energy", storage.getEnergyStored(), storage.getMaxEnergyStored());
    }

    public static void equipment(ServerPlayer player, BlockEntity block, Installation installation) {
        report(player, "header", block.getBlockState().getBlock().getName(), block.getBlockPos().toShortString());
        report(player, "module", state(!installation.required() || installation.hasModule()));
        report(player, "installation", Component.translatable("diagnostic.mobilecellnetwork.status."
                + installation.status().name().toLowerCase(java.util.Locale.ROOT)));
        if (block instanceof IEnergyStorage storage) {
            report(player, "energy", storage.getEnergyStored(), storage.getMaxEnergyStored());
        }
        report(player, "bypass", state(InfrastructurePower.bypassed(block.getLevel())));
        if (installation.source() == null) {
            report(player, installation.required() ? "wire_missing" : "wire_optional");
        } else {
            var source = installation.source();
            var level = player.serverLevel();
            if (!level.hasChunkAt(source)) {
                report(player, "source_unloaded", source.toShortString());
            } else if (!PowerWire.canConnect(installation, block.getBlockPos())) {
                report(player, "wire_invalid");
            } else {
                IEnergyStorage supply = level.getCapability(Capabilities.EnergyStorage.BLOCK, source,
                        installation.sourceFace());
                if (supply == null || !supply.canExtract()) {
                    report(player, "source_missing", source.toShortString());
                } else {
                    report(player, "source", source.toShortString(), supply.getEnergyStored(), supply.getMaxEnergyStored(),
                            Math.min(PowerWire.TRANSFER_PER_TICK, supply.extractEnergy(PowerWire.TRANSFER_PER_TICK, true)));
                }
            }
        }
        if (block instanceof AntennaBlockEntity antenna) {
            report(player, "transmitter", state(antenna.isEnabled()), antenna.getNetworkId(),
                    antenna.getPowerState().name(), antenna.getAntennaDrawPerSecond());
            signal(player, player.serverLevel(), player.position(), antenna, true, "tower_signal");
        } else if (block instanceof SignalBoosterBlockEntity booster) {
            report(player, "booster", state(booster.isPowered()), SignalBoosterBlockEntity.RANGE);
            AntennaBlockEntity upstream = booster.upstream();
            signal(player, player.serverLevel(), booster.center(), upstream, false, "upstream_signal");
        }
    }

    public static void survey(ServerPlayer player) {
        AntennaBlockEntity best = strongest(player.serverLevel(), player.position(), true);
        signal(player, player.serverLevel(), player.position(), best, true, "survey");
        report(player, "radio_only");
    }

    private static AntennaBlockEntity strongest(ServerLevel level, Vec3 point, boolean includeBoosters) {
        AntennaBlockEntity best = null;
        double strength = 0;
        for (AntennaBlockEntity antenna : AntennaBlockEntity.loadedAntennas(level)) {
            if (!antenna.isEnabled()) continue;
            var reading = CoverageEngine.measureRadio(level, point, antenna, includeBoosters);
            if (reading.tier() != SignalTier.NO_SERVICE && reading.finalStrength() > strength) {
                strength = reading.finalStrength();
                best = antenna;
            }
        }
        return best;
    }

    private static void signal(Player player, ServerLevel level, Vec3 point, AntennaBlockEntity antenna,
                               boolean includeBoosters, String key) {
        var result = radio(level, point, antenna, includeBoosters);
        report(player, key, result.percent(), result.tier(), antenna == null ? "-" : antenna.getNetworkId(),
                antenna == null ? "-" : antenna.getBlockPos().toShortString());
    }

    public static int percent(double strength) {
        return (int) Math.round(Math.clamp(strength, 0, 1) * 100);
    }

    private static Component state(boolean on) {
        return Component.translatable("diagnostic.mobilecellnetwork." + (on ? "yes" : "no"));
    }

    public static void report(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("diagnostic.mobilecellnetwork." + key, args), false);
    }
}

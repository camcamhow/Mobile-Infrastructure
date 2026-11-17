package com.example.mobilecellnetwork.event;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class AntennaLifecycleHandler {
    private static final int CLEANUP_INTERVAL_TICKS = 6_000;

    private AntennaLifecycleHandler() {
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos position = event.getPos();
        var state = event.getState();
        if (state.getBlock() instanceof com.example.mobilecellnetwork.block.TestTowerSectionBlock) {
            BlockPos base = com.example.mobilecellnetwork.block.TestTowerSectionBlock.basePosition(position, state);
            var baseState = level.getBlockState(base);
            if (baseState.getBlock() instanceof com.example.mobilecellnetwork.block.TestTowerBlock) {
                var baseEvent = new BlockEvent.BreakEvent(level, base, baseState, event.getPlayer());
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(baseEvent);
                if (baseEvent.isCanceled()) event.setCanceled(true);
            }
        }
    }

    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
                || !(level.getBlockEntity(event.getPos()) instanceof AntennaBlockEntity antenna)) {
            return;
        }
        antenna.setOwner(player.getUUID(), player.getGameProfile().getName());
        antenna.setNetworkId("default");
        antenna.setRoamingPolicy(com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .DEFAULT_ANTENNA_ROAMING_POLICY.get());
        AntennaBlockEntity.registerLoaded(antenna);
        savedData(level).registerAntenna(event.getPos(), antenna);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % CLEANUP_INTERVAL_TICKS != 0) {
            return;
        }
        ServerLevel overworld = event.getServer().overworld();
        NetworkSavedData data = NetworkSavedData.get(overworld);
        List<AntennaKey> stale = new ArrayList<>();
        java.util.Set<String> antennaKeys = new java.util.HashSet<>(data.getAntennaNetworks().keySet());
        data.getNetworks().values().forEach(network -> antennaKeys.addAll(network.towers()));
        for (String encoded : antennaKeys) {
            int separator = encoded.lastIndexOf('@');
            if (separator <= 0) {
                stale.add(new AntennaKey(null, BlockPos.ZERO, encoded));
                continue;
            }
            ResourceLocation dimension = ResourceLocation.tryParse(encoded.substring(0, separator));
            long packedPosition;
            try {
                packedPosition = Long.parseLong(encoded.substring(separator + 1));
            } catch (NumberFormatException exception) {
                stale.add(new AntennaKey(dimension, BlockPos.ZERO, encoded));
                continue;
            }
            if (dimension == null) {
                stale.add(new AntennaKey(null, BlockPos.ZERO, encoded));
                continue;
            }
            ResourceKey<Level> dimensionKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    dimension);
            ServerLevel level = event.getServer().getLevel(dimensionKey);
            BlockPos position = BlockPos.of(packedPosition);
            if (level != null && level.hasChunkAt(position)
                    && !(level.getBlockEntity(position) instanceof AntennaBlockEntity)) {
                stale.add(new AntennaKey(dimension, position, encoded));
            }
        }
        stale.forEach(key -> data.unregisterAntenna(key.encoded()));
    }

    private static NetworkSavedData savedData(ServerLevel level) {
        return NetworkSavedData.get(level.getServer().overworld());
    }

    private record AntennaKey(ResourceLocation dimension, BlockPos position, String encoded) {
    }
}

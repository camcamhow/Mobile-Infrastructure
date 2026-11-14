package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.plans.PlanRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ConnectionDelayService {
    private static final List<PendingRequest> PENDING = new ArrayList<>();
    private static final Map<UUID, Integer> ACTIVE_COUNTS = new HashMap<>();

    private ConnectionDelayService() {
    }

    public static void submit(Player sender, Runnable request) {
        submit(sender, request, defaultNoService(sender), true);
    }

    public static void submitForPlanSelection(Player sender, Runnable request) {
        submit(sender, request, defaultNoService(sender), false);
    }

    private static Runnable defaultNoService(Player sender) {
        return () -> {
            if (sender instanceof ServerPlayer player) {
                player.displayClientMessage(Component.literal("No Service."), true);
            }
        };
    }

    public static void submit(Player sender, Runnable request, Runnable noService) {
        submit(sender, request, noService, true);
    }

    private static void submit(Player sender, Runnable request, Runnable noService, boolean requiresPlan) {
        if (!(sender instanceof ServerPlayer player) || player.isRemoved()) {
            return;
        }
        if (requiresPlan && !hasActivePlan(player)) {
            noService.run();
            return;
        }
        if (player.getData(ModCapabilities.PHONE_DATA).isDead()) {
            player.displayClientMessage(Component.literal("Phone is dead."), true);
            noService.run();
            return;
        }
        var antenna = CoverageEngine.getStrongestAntennaFor(player);
        if (antenna == null) {
            noService.run();
            return;
        }
        CoverageEngine.SignalResult signal = CoverageEngine.updateSignal(player, antenna);
        if (signal.tier() == SignalTier.NO_SERVICE) {
            noService.run();
            return;
        }

        int delayTicks = estimateDelayTicks(signal);
        long dueTick = player.serverLevel().getGameTime() + delayTicks;
        PENDING.add(new PendingRequest(player.server, player, dueTick, request, noService, requiresPlan));
        int activeCount = ACTIVE_COUNTS.merge(player.getUUID(), 1, Integer::sum);
        if (activeCount == 1) {
            PacketDistributor.sendToPlayer(player, new ConnectionStatePacket(true, delayTicks));
        }
    }

    private static int estimateDelayTicks(CoverageEngine.SignalResult signal) {
        int tierTicks = switch (signal.tier()) {
            case FIVE_G_PLUS, FIVE_G -> 1;
            case FOUR_G_PLUS, FOUR_G -> 2;
            case LTE -> 3;
            case THREE_G -> 6;
            case H_PLUS -> 8;
            case H -> 11;
            case TWO_G -> 16;
            case E_PLUS -> 22;
            case E, NO_SERVICE -> 30;
        };
        double strength = Math.clamp(signal.finalStrength(), 0.0D, 1.0D);
        return Math.clamp((int) Math.ceil(tierTicks * (1.0D + (1.0D - strength) * 3.0D)), 1, 90);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long gameTime = server.overworld().getGameTime();
        List<PendingRequest> due = PENDING.stream()
                .filter(request -> request.server() == server && request.dueTick() <= gameTime)
                .sorted(Comparator.comparingLong(PendingRequest::dueTick))
                .toList();
        for (PendingRequest request : due) {
            PENDING.remove(request);
            ServerPlayer player = request.player();
            try {
                if (!player.isRemoved() && (!request.requiresPlan() || hasActivePlan(player))
                        && CoverageEngine.getStrongestAntennaFor(player) != null) {
                    request.action().run();
                } else if (!player.isRemoved()) {
                    request.noService().run();
                }
            } finally {
                ACTIVE_COUNTS.computeIfPresent(player.getUUID(), (id, count) -> {
                    if (count <= 1) {
                        if (!player.isRemoved()) {
                            PacketDistributor.sendToPlayer(player, new ConnectionStatePacket(false, 0));
                        }
                        return null;
                    }
                    return count - 1;
                });
            }
        }
    }

    private static boolean hasActivePlan(ServerPlayer player) {
        var usage = player.getData(ModCapabilities.PLAN_USAGE);
        return usage.isCycleActive() && !usage.getActivePlanId().isBlank()
                && PlanRegistry.findPlan(usage.getActivePlanId()) != null;
    }

    private record PendingRequest(MinecraftServer server, ServerPlayer player, long dueTick,
                                  Runnable action, Runnable noService, boolean requiresPlan) {
    }
}

package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.infrastructure.TechnicianDiagnostics;
import com.example.mobilecellnetwork.network.CoverageEngine;
import com.example.mobilecellnetwork.registry.ModMenus;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class SignalBoosterMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 4;
    private static final int DATA_COUNT = 6;
    private final SignalBoosterBlockEntity booster;
    private final BlockPos position;
    private final List<NetworkOption> options;
    private final ContainerData data;
    private int page;
    private int upstreamPercent;

    public record NetworkOption(String id, String name, int percent) {
        public void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(id);
            buffer.writeUtf(name);
            buffer.writeVarInt(percent);
        }

        public static NetworkOption read(FriendlyByteBuf buffer) {
            return new NetworkOption(buffer.readUtf(), buffer.readUtf(), buffer.readVarInt());
        }

        public Component label() {
            return id.isEmpty() ? Component.translatable("gui.mobilecellnetwork.booster.automatic")
                    : Component.literal(name + " - " + percent + "%");
        }
    }

    public SignalBoosterMenu(int id, SignalBoosterBlockEntity booster, List<NetworkOption> options) {
        this(id, booster, booster.getBlockPos(), options);
    }

    private SignalBoosterMenu(int id, SignalBoosterBlockEntity booster, BlockPos position,
                              List<NetworkOption> options) {
        super(ModMenus.SIGNAL_BOOSTER_MENU.get(), id);
        this.booster = booster;
        this.position = position.immutable();
        this.options = List.copyOf(options);
        data = booster == null ? new SimpleContainerData(DATA_COUNT) : new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> booster.getEnergyStored();
                    case 1 -> !booster.installation().ready() ? 2 : booster.isPowered() ? 1 : 0;
                    case 2 -> selectedIndex();
                    case 3 -> upstreamPercent;
                    case 4 -> com.example.mobilecellnetwork.blockentity.InfrastructurePower.bypassed(
                            booster.getLevel()) ? 1 : 0;
                    case 5 -> page;
                    default -> throw new IndexOutOfBoundsException(index);
                };
            }

            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
        addDataSlots(data);
    }

    public static SignalBoosterMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        return new SignalBoosterMenu(id, null, buffer.readBlockPos(), buffer.readList(NetworkOption::read));
    }

    public static List<NetworkOption> options(ServerLevel level, SignalBoosterBlockEntity booster) {
        var networks = NetworkSavedData.get(level.getServer().overworld());
        var found = new LinkedHashMap<String, NetworkOption>();
        found.put("", new NetworkOption("", "", 0));
        CoverageEngine.boosterSignals(level, booster.center()).stream()
                .sorted(Comparator.comparingDouble(
                        (CoverageEngine.BoosterSignal signal) -> signal.signal().finalStrength()).reversed())
                .forEach(signal -> {
                    String id = signal.antenna().getNetworkId();
                    var network = networks.getNetwork(id);
                    found.putIfAbsent(id, new NetworkOption(id, network == null ? id : network.name(),
                            TechnicianDiagnostics.percent(signal.signal().finalStrength())));
                });
        String selected = booster.tuning().networkId();
        if (!found.containsKey(selected)) {
            var network = networks.getNetwork(selected);
            found.put(selected, new NetworkOption(selected, network == null ? selected : network.name(), 0));
        }
        return List.copyOf(found.values());
    }

    private int selectedIndex() {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id().equals(booster.tuning().networkId())) return i;
        }
        return -1;
    }

    public int value(int index) { return data.get(index); }
    public List<NetworkOption> options() { return options; }
    public int pageCount() { return Math.max(1, (options.size() + PAGE_SIZE - 1) / PAGE_SIZE); }

    @Override
    public void broadcastChanges() {
        if (booster != null && booster.getLevel() instanceof ServerLevel level
                && level.getGameTime() % 20 == 0) {
            var upstream = booster.upstream();
            upstreamPercent = upstream == null ? 0 : TechnicianDiagnostics.percent(
                    CoverageEngine.measureRadio(level, booster.center(), upstream, false).finalStrength());
        }
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (booster == null || !stillValid(player) || !(player instanceof ServerPlayer serverPlayer)) {
            player.displayClientMessage(Component.translatable("gui.mobilecellnetwork.booster.denied"), true);
            return false;
        }
        if (button == 0 && page > 0) page--;
        else if (button == 1 && page + 1 < pageCount()) page++;
        else if (button == 2) {
            booster.openMenu(serverPlayer);
            return true;
        } else if (button >= 3 && button < 3 + PAGE_SIZE) {
            int index = page * PAGE_SIZE + button - 3;
            if (index >= options.size()) return reject(player);
            String id = options.get(index).id();
            if (!id.isEmpty() && CoverageEngine.boosterSignals(serverPlayer.serverLevel(), booster.center())
                    .stream().noneMatch(signal -> signal.antenna().getNetworkId().equals(id))) {
                player.displayClientMessage(Component.translatable("gui.mobilecellnetwork.booster.unavailable"), true);
                return false;
            }
            booster.setNetwork(id);
        } else return reject(player);
        broadcastChanges();
        return true;
    }

    private boolean reject(Player player) {
        player.displayClientMessage(Component.translatable("gui.mobilecellnetwork.booster.invalid"), true);
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return booster == null ? player.level().getBlockEntity(position) instanceof SignalBoosterBlockEntity
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(position)) <= 64
                : booster.canConfigure(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}

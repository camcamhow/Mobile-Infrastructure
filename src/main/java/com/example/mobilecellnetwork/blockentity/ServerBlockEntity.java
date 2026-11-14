package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import com.example.mobilecellnetwork.gui.ServerMenu;

public class ServerBlockEntity extends BlockEntity {
    private final List<String> managedNetworks = new ArrayList<>();
    private final List<BlockPos> pendingRegistrations = new ArrayList<>();

    public ServerBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.SERVER.get(), position, state);
    }

    public List<String> getManagedNetworks() {
        return List.copyOf(managedNetworks);
    }

    public List<BlockPos> getPendingRegistrations() {
        return List.copyOf(pendingRegistrations);
    }

    public void addManagedNetwork(String networkId) {
        if (networkId != null && !managedNetworks.contains(networkId)) {
            managedNetworks.add(networkId);
            setChanged();
        }
    }

    public void removeManagedNetwork(String networkId) {
        if (managedNetworks.remove(networkId)) {
            setChanged();
        }
    }

    public String createNetwork(String name) {
        if (level == null || level.isClientSide || name == null || name.isBlank()) {
            return null;
        }
        String id = UUID.randomUUID().toString();
        NetworkSavedData.get(((net.minecraft.server.level.ServerLevel) level).getServer().overworld())
                .putNetwork(id, new NetworkSavedData.NetworkRecord(name, true, List.of(), null, "Server",
                        com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                                .DEFAULT_NETWORK_ROAMING_POLICY.get()));
        managedNetworks.add(id);
        setChanged();
        return id;
    }

    public boolean deleteNetwork(String networkId, boolean confirm) {
        if (!confirm || level == null || level.isClientSide || !managedNetworks.contains(networkId)) {
            return false;
        }
        boolean deleted = NetworkSavedData.get(((net.minecraft.server.level.ServerLevel) level)
                .getServer().overworld()).deleteNetwork(networkId);
        if (deleted) {
            managedNetworks.remove(networkId);
            setChanged();
        }
        return deleted;
    }

    public boolean registerAntenna(BlockPos antennaPosition) {
        if (level == null || level.isClientSide) {
            return false;
        }
        BlockEntity entity = level.getBlockEntity(antennaPosition);
        if (!(entity instanceof AntennaBlockEntity antenna)) {
            if (!pendingRegistrations.contains(antennaPosition)) {
                pendingRegistrations.add(antennaPosition.immutable());
                setChanged();
            }
            return false;
        }
        if (managedNetworks.isEmpty()) {
            return false;
        }
        antenna.setNetworkId(managedNetworks.getFirst());
        String key = level.dimension().location() + "@" + antennaPosition.asLong();
        NetworkSavedData.get(((net.minecraft.server.level.ServerLevel) level).getServer().overworld())
                .registerAntenna(key, antenna.getNetworkId());
        pendingRegistrations.remove(antennaPosition);
        setChanged();
        return true;
    }

    public boolean setNetworkAllowed(String networkId, boolean allowed) {
        if (level == null || level.isClientSide || !managedNetworks.contains(networkId)) {
            return false;
        }
        return NetworkSavedData.get(((net.minecraft.server.level.ServerLevel) level)
                .getServer().overworld()).setNetworkAllowed(networkId, allowed);
    }

    public void openServerMenu(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !canOpenServerMenu(serverPlayer)) {
            player.displayClientMessage(Component.literal("You need administrator permission to use this server."),
                    true);
            return;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inventory, user) -> new ServerMenu(id, inventory, worldPosition),
                Component.literal("Mobile Cell Network Server")),
                buffer -> buffer.writeBlockPos(worldPosition));
    }

    public static boolean canOpenServerMenu(ServerPlayer player) {
        if (player.hasPermissions(2) || player.server == null) {
            return player.hasPermissions(2);
        }
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        return data.getNetworks().values().stream()
                .anyMatch(network -> player.getUUID().equals(network.ownerUUID()));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        managedNetworks.clear();
        for (Tag value : tag.getList("managedNetworks", Tag.TAG_STRING)) {
            managedNetworks.add(value.getAsString());
        }
        pendingRegistrations.clear();
        ListTag registrations = tag.getList("pendingRegistrations", Tag.TAG_LONG);
        for (int index = 0; index < registrations.size(); index++) {
            pendingRegistrations.add(BlockPos.of(
                    ((net.minecraft.nbt.LongTag) registrations.get(index)).getAsLong()));
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag networkList = new ListTag();
        managedNetworks.forEach(id -> networkList.add(net.minecraft.nbt.StringTag.valueOf(id)));
        tag.put("managedNetworks", networkList);
        ListTag registrationList = new ListTag();
        pendingRegistrations.forEach(position ->
                registrationList.add(net.minecraft.nbt.LongTag.valueOf(position.asLong())));
        tag.put("pendingRegistrations", registrationList);
    }
}

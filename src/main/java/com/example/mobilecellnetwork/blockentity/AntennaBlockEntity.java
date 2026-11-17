package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.network.CoverageEngine;
import com.example.mobilecellnetwork.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.minecraft.world.phys.Vec3;

public abstract class AntennaBlockEntity extends BlockEntity implements IEnergyStorage,
        com.example.mobilecellnetwork.infrastructure.Serviceable {
    private final com.example.mobilecellnetwork.infrastructure.Installation installation =
            new com.example.mobilecellnetwork.infrastructure.Installation();

    @Override
    public com.example.mobilecellnetwork.infrastructure.Installation installation() { return installation; }
    private static final Set<AntennaBlockEntity> LOADED_ANTENNAS = new HashSet<>();

    private String networkId = "default";
    private final AntennaTier tier;
    private boolean enabled = true;
    private UUID ownerUUID;
    private String ownerName = "Server";
    private AntennaRoamingPolicy roamingPolicy = AntennaRoamingPolicy.INHERIT;
    private final Set<UUID> roamingWhitelist = new LinkedHashSet<>();
    private final Set<String> roamingNetworkWhitelist = new LinkedHashSet<>();
    private final Set<BlockPos> meshLinks = new LinkedHashSet<>();
    private final Set<String> sharedWithNetworkIds = new LinkedHashSet<>();
    private AntennaPowerState powerState = AntennaPowerState.UNPOWERED;
    private final Set<UUID> userSet = new LinkedHashSet<>();
    private int energy;
    private boolean chunkForcedByAntenna;
    private long unloadChunkAtTick = -1;

    protected AntennaBlockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType<?> type,
            BlockPos position,
            BlockState state,
            AntennaTier tier
    ) {
        super(type, position, state);
        this.tier = tier;
    }

    public String getNetworkId() { return networkId; }

    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }

    public void setOwner(UUID ownerUUID, String ownerName) {
        String normalizedName = ownerUUID == null ? "Server" : ownerName == null ? "" : ownerName;
        if (!java.util.Objects.equals(this.ownerUUID, ownerUUID) || !this.ownerName.equals(normalizedName)) {
            this.ownerUUID = ownerUUID;
            this.ownerName = normalizedName;
            setChanged();
        }
    }

    public AntennaRoamingPolicy getRoamingPolicy() { return roamingPolicy; }

    public void setRoamingPolicy(AntennaRoamingPolicy roamingPolicy) {
        AntennaRoamingPolicy normalizedPolicy = roamingPolicy == null
                ? AntennaRoamingPolicy.INHERIT : roamingPolicy;
        if (this.roamingPolicy != normalizedPolicy) {
            this.roamingPolicy = normalizedPolicy;
            setChanged();
        }
    }

    public Set<UUID> getRoamingWhitelist() { return Set.copyOf(roamingWhitelist); }

    public void setRoamingWhitelist(Set<UUID> whitelist) {
        Set<UUID> normalizedWhitelist = new LinkedHashSet<>();
        if (whitelist != null) {
            whitelist.stream().filter(java.util.Objects::nonNull).forEach(normalizedWhitelist::add);
        }
        if (!roamingWhitelist.equals(normalizedWhitelist)) {
            roamingWhitelist.clear();
            roamingWhitelist.addAll(normalizedWhitelist);
            setChanged();
        }
    }

    public void addToRoamingWhitelist(UUID playerUUID) {
        if (playerUUID != null && roamingWhitelist.add(playerUUID)) {
            setChanged();
        }
    }

    public void removeFromRoamingWhitelist(UUID playerUUID) {
        if (roamingWhitelist.remove(playerUUID)) {
            setChanged();
        }
    }

    public Set<String> getRoamingNetworkWhitelist() { return Set.copyOf(roamingNetworkWhitelist); }

    public void setRoamingNetworkWhitelist(Set<String> whitelist) {
        Set<String> normalizedWhitelist = new LinkedHashSet<>();
        if (whitelist != null) {
            whitelist.stream().filter(java.util.Objects::nonNull).forEach(normalizedWhitelist::add);
        }
        if (!roamingNetworkWhitelist.equals(normalizedWhitelist)) {
            roamingNetworkWhitelist.clear();
            roamingNetworkWhitelist.addAll(normalizedWhitelist);
            setChanged();
        }
    }

    public Set<String> getSharedWithNetworkIds() { return Set.copyOf(sharedWithNetworkIds); }

    public void addShare(String networkId) {
        if (networkId != null && !networkId.isBlank() && sharedWithNetworkIds.add(networkId)) {
            setChanged();
        }
    }

    public void removeShare(String networkId) {
        if (sharedWithNetworkIds.remove(networkId)) {
            setChanged();
        }
    }

    public void clearShares() {
        if (!sharedWithNetworkIds.isEmpty()) {
            sharedWithNetworkIds.clear();
            setChanged();
        }
    }

    public void setNetworkId(String networkId) {
        setNetworkIdAndSync(networkId);
    }

    public void setNetworkIdAndSync(String newNetworkId) {
        if (this.networkId.equals(newNetworkId)) {
            return;
        }
        this.networkId = newNetworkId;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public AntennaTier getTier() { return tier; }
    public com.example.mobilecellnetwork.network.SignalTier getMaxSignalTier() { return tier.maxSignalTier(); }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            setChanged();
            refreshPowerState();
        }
    }

    public Set<BlockPos> getMeshLinks() { return Set.copyOf(meshLinks); }
    public AntennaPowerState getPowerState() {
        return installation.ready() ? powerState : AntennaPowerState.UNPOWERED;
    }
    public boolean isPowered() { return getPowerState() != AntennaPowerState.UNPOWERED; }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("powerState", powerState.name());
        tag.putString("networkId", networkId);
        tag.putBoolean("enabled", enabled);
        tag.put("installation", installation.save());
        return tag;
    }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            powerState = AntennaPowerState.valueOf(tag.getString("powerState"));
        } catch (IllegalArgumentException exception) {
            com.mojang.logging.LogUtils.getLogger().warn("Invalid antenna power state in update", exception);
            powerState = AntennaPowerState.UNPOWERED;
        }
        networkId = tag.getString("networkId");
        enabled = tag.getBoolean("enabled");
        installation.load(tag.getCompound("installation"));
    }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet,
                                        HolderLookup.Provider registries) {
        handleUpdateTag(packet.getTag(), registries);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncPowerDisplay() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    public Set<UUID> getUserSet() { return Set.copyOf(userSet); }
    public int getAntennaDrawPerSecond() { return tier.drawPerSecond(); }
    public boolean isChunkLoadedByAntenna() { return chunkForcedByAntenna; }
    public int getRange() { return tier.range(); }
    public Vec3 center() {
        return new Vec3(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);
    }

    public void addUser(UUID playerId) {
        userSet.add(playerId);
    }

    public void removeUser(UUID playerId) {
        userSet.remove(playerId);
    }

    public void clearUsers() {
        userSet.clear();
    }

    public void onBlockTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        LOADED_ANTENNAS.add(this);
        com.example.mobilecellnetwork.infrastructure.PowerWire.tick(this, installation, this);
        if (level.getGameTime() % 40 == 0) {
            rescanMeshLinks(level);
            if (level instanceof ServerLevel serverLevel) {
                NetworkSavedData savedData = NetworkSavedData.get(serverLevel.getServer().overworld());
                if (!"default".equals(networkId) && savedData.getNetwork(networkId) == null) {
                    setNetworkId("default");
                }
                String positionKey = level.dimension().location() + "@" + worldPosition.asLong();
                savedData.registerAntenna(positionKey, networkId);
            }
        }
    }

    public static void registerLoaded(AntennaBlockEntity antenna) {
        LOADED_ANTENNAS.add(antenna);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        LOADED_ANTENNAS.add(this);
        refreshPowerState();
    }

    @Override
    public void setRemoved() {
        LOADED_ANTENNAS.remove(this);
        super.setRemoved();
    }

    private void rescanMeshLinks(Level level) {
        Set<BlockPos> links = new LinkedHashSet<>();
        if (MobileCellNetworkConfig.ALLOW_MESH_SHARING.get()) {
            double meshRangeSquared = (double) tier.meshRange() * tier.meshRange();
            for (AntennaBlockEntity antenna : CoverageEngine.findLoadedAntennas(level, worldPosition, tier.meshRange())) {
                if (antenna != this && antenna.isEnabled()
                        && antenna.getPowerState() != AntennaPowerState.UNPOWERED
                        && networkId.equals(antenna.networkId)
                        && antenna.worldPosition.distSqr(worldPosition) <= meshRangeSquared) {
                    links.add(antenna.worldPosition.immutable());
                }
            }
        }
        if (!meshLinks.equals(links)) {
            meshLinks.clear();
            meshLinks.addAll(links);
            setChanged();
        }
    }

    public static void serverTickAll(MinecraftServer server) {
        List<AntennaBlockEntity> stale = new ArrayList<>();
        for (AntennaBlockEntity antenna : LOADED_ANTENNAS) {
            if (antenna.isRemoved() || !(antenna.getLevel() instanceof ServerLevel level)
                    || level.getServer() != server) {
                stale.add(antenna);
                continue;
            }
            antenna.updatePowerState(server.getTickCount());
        }
        LOADED_ANTENNAS.removeAll(stale);
    }

    public static List<AntennaBlockEntity> loadedAntennas(MinecraftServer server) {
        return LOADED_ANTENNAS.stream()
                .filter(antenna -> !antenna.isRemoved()
                        && antenna.getLevel() instanceof ServerLevel level
                        && level.getServer() == server)
                .toList();
    }

    public static List<AntennaBlockEntity> loadedAntennas(Level level) {
        return LOADED_ANTENNAS.stream()
                .filter(antenna -> !antenna.isRemoved() && antenna.getLevel() == level)
                .toList();
    }

    private void updatePowerState(long serverTick) {
        if (serverTick % 20 != 0) {
            return;
        }
        int previousEnergy = energy;
        AntennaPowerState newState;
        if (!enabled || !installation.ready()) {
            newState = AntennaPowerState.UNPOWERED;
        } else if (!requiresEnergy()) {
            newState = AntennaPowerState.FE_DISABLED;
        } else {
            int draw = tier.drawPerSecond();
            if (energy >= draw) {
                energy -= draw;
                newState = AntennaPowerState.ACTIVE;
            } else if (energy > 0) {
                energy = 0;
                newState = AntennaPowerState.LOW_POWER;
            } else {
                newState = AntennaPowerState.UNPOWERED;
            }
        }
        if (newState != powerState) {
            powerState = newState;
            setChanged();
            syncPowerDisplay();
        }
        if (energy != previousEnergy) {
            setChanged();
        }
    }

    private void refreshPowerState() {
        if (level == null || level.isClientSide) {
            return;
        }
        AntennaPowerState refreshedState;
        if (!enabled || !installation.ready()) {
            refreshedState = AntennaPowerState.UNPOWERED;
        } else if (!requiresEnergy()) {
            refreshedState = AntennaPowerState.FE_DISABLED;
        } else if (energy >= tier.drawPerSecond()) {
            refreshedState = AntennaPowerState.ACTIVE;
        } else if (energy > 0) {
            refreshedState = AntennaPowerState.LOW_POWER;
        } else {
            refreshedState = AntennaPowerState.UNPOWERED;
        }

        if (powerState != refreshedState) {
            powerState = refreshedState;
            setChanged();
            syncPowerDisplay();
        }
    }

    protected boolean requiresEnergy() {
        return !InfrastructurePower.bypassed(level);
    }

    public void updateChunkTicket(ServerLevel level) {
        long worldTick = level.getGameTime();
        if (!userSet.isEmpty()) {
            if (unloadChunkAtTick != -1) {
                unloadChunkAtTick = -1;
                setChanged();
            }
            if (!chunkForcedByAntenna) {
                long chunkKey = net.minecraft.world.level.ChunkPos.asLong(worldPosition);
                if (!level.getForcedChunks().contains(chunkKey)) {
                    level.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, true);
                    chunkForcedByAntenna = true;
                }
            }
            return;
        }
        if (!chunkForcedByAntenna) {
            return;
        }
        if (unloadChunkAtTick < 0) {
            unloadChunkAtTick = worldTick + MobileCellNetworkConfig.ANTENNA_CHUNK_LOAD_GRACE_TICKS.get();
            setChanged();
        }
        if (worldTick >= unloadChunkAtTick) {
            level.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, false);
            chunkForcedByAntenna = false;
            unloadChunkAtTick = -1;
            setChanged();
        }
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0) return 0;
        int stored = Math.min(energy, getMaxEnergyStored());
        int accepted = Math.max(0,
                Math.min(Math.min(maxReceive, tier.maxReceivePerTick()), getMaxEnergyStored() - stored));
        if (!simulate && accepted > 0) {
            energy = stored + accepted;
            setChanged();
            refreshPowerState();
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0) return 0;
        int extracted = Math.min(maxExtract, energy);
        if (!simulate && extracted > 0) {
            energy -= extracted;
            setChanged();
            refreshPowerState();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() { return Math.min(energy, getMaxEnergyStored()); }
    @Override
    public int getMaxEnergyStored() { return tier.capacity(); }
    @Override
    public boolean canExtract() { return true; }
    @Override
    public boolean canReceive() { return energy < getMaxEnergyStored(); }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        installation.load(tag.getCompound("installation"));
        networkId = tag.contains("networkId", Tag.TAG_STRING) ? tag.getString("networkId") : "default";
        ownerUUID = tag.hasUUID("ownerUUID") ? tag.getUUID("ownerUUID") : null;
        ownerName = ownerUUID == null ? "Server"
                : tag.contains("ownerName", Tag.TAG_STRING) ? tag.getString("ownerName") : "";
        try {
            roamingPolicy = tag.contains("roamingPolicy", Tag.TAG_STRING)
                    ? AntennaRoamingPolicy.valueOf(tag.getString("roamingPolicy"))
                    : AntennaRoamingPolicy.INHERIT;
        } catch (IllegalArgumentException ignored) {
            roamingPolicy = AntennaRoamingPolicy.INHERIT;
        }
        roamingWhitelist.clear();
        for (Tag value : tag.getList("roamingWhitelist", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("player")) {
                roamingWhitelist.add(entry.getUUID("player"));
            }
        }
        roamingNetworkWhitelist.clear();
        ListTag networkWhitelist = tag.getList("roamingNetworkWhitelist", Tag.TAG_STRING);
        for (int index = 0; index < networkWhitelist.size(); index++) {
            roamingNetworkWhitelist.add(networkWhitelist.getString(index));
        }
        enabled = !tag.contains("enabled", Tag.TAG_BYTE) || tag.getBoolean("enabled");
        energy = Math.clamp(tag.getInt("energy"), 0, getMaxEnergyStored());
        try {
            powerState = tag.contains("powerState", Tag.TAG_STRING)
                    ? AntennaPowerState.valueOf(tag.getString("powerState")) : AntennaPowerState.UNPOWERED;
        } catch (IllegalArgumentException ignored) {
            powerState = AntennaPowerState.UNPOWERED;
        }
        chunkForcedByAntenna = tag.getBoolean("chunkForcedByAntenna");
        unloadChunkAtTick = tag.getLong("unloadChunkAtTick");
        meshLinks.clear();
        ListTag links = tag.getList("meshLinks", Tag.TAG_LONG);
        for (int index = 0; index < links.size(); index++) {
            meshLinks.add(BlockPos.of(((net.minecraft.nbt.LongTag) links.get(index)).getAsLong()));
        }
        sharedWithNetworkIds.clear();
        ListTag shares = tag.getList("sharedWithNetworkIds", Tag.TAG_STRING);
        for (int index = 0; index < shares.size(); index++) {
            sharedWithNetworkIds.add(shares.getString(index));
        }
        userSet.clear();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("installation", installation.save());
        tag.putString("networkId", networkId);
        if (ownerUUID != null) {
            tag.putUUID("ownerUUID", ownerUUID);
        }
        tag.putString("ownerName", ownerUUID == null ? "Server" : ownerName);
        tag.putString("roamingPolicy", roamingPolicy.name());
        ListTag whitelist = new ListTag();
        roamingWhitelist.forEach(playerUUID -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", playerUUID);
            whitelist.add(entry);
        });
        tag.put("roamingWhitelist", whitelist);
        ListTag networkWhitelist = new ListTag();
        roamingNetworkWhitelist.forEach(network -> networkWhitelist.add(net.minecraft.nbt.StringTag.valueOf(network)));
        tag.put("roamingNetworkWhitelist", networkWhitelist);
        tag.putBoolean("enabled", enabled);
        tag.putInt("energy", energy);
        tag.putString("powerState", powerState.name());
        tag.putBoolean("chunkForcedByAntenna", chunkForcedByAntenna);
        tag.putLong("unloadChunkAtTick", unloadChunkAtTick);
        ListTag links = new ListTag();
        meshLinks.forEach(position -> links.add(net.minecraft.nbt.LongTag.valueOf(position.asLong())));
        tag.put("meshLinks", links);
        ListTag shares = new ListTag();
        sharedWithNetworkIds.forEach(id -> shares.add(net.minecraft.nbt.StringTag.valueOf(id)));
        tag.put("sharedWithNetworkIds", shares);
    }
}

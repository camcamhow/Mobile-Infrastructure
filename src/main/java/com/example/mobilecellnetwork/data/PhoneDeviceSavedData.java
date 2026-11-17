package com.example.mobilecellnetwork.data;

import com.example.mobilecellnetwork.MobileCellNetwork;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class PhoneDeviceSavedData extends SavedData {
    private static final int MAX_DEVICE_PHOTOS = 24;
    private static final int MAX_PHOTO_BYTES = 1_000_000;
    private static final String DATA_NAME = MobileCellNetwork.MOD_ID + "_phone_devices";
    public static final Factory<PhoneDeviceSavedData> FACTORY =
            new Factory<>(PhoneDeviceSavedData::new, PhoneDeviceSavedData::load, DataFixTypes.LEVEL);

    private final Map<UUID, DeviceRecord> devices = new LinkedHashMap<>();
    private final Map<UUID, CompoundTag> activePersonalBackups = new LinkedHashMap<>();

    public static PhoneDeviceSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public DeviceRecord get(UUID phoneId) {
        return devices.get(phoneId);
    }

    public void saveActiveBackup(UUID playerId, CompoundTag profile) {
        activePersonalBackups.put(playerId, profile.copy());
        setDirty();
    }

    public CompoundTag takeActiveBackup(UUID playerId) {
        CompoundTag backup = activePersonalBackups.remove(playerId);
        if (backup != null) {
            setDirty();
            return backup.copy();
        }
        return null;
    }

    public DeviceRecord register(UUID phoneId, UUID ownerId, CompoundTag profile,
                                 long bankBalance, List<String> locks, String pinHash) {
        DeviceRecord existing = devices.get(phoneId);
        if (existing != null) {
            return existing;
        }
        DeviceRecord record = new DeviceRecord(phoneId, ownerId, profile, System.currentTimeMillis(),
                bankBalance, locks, pinHash);
        devices.put(phoneId, record);
        setDirty();
        return record;
    }

    public void saveProfile(UUID phoneId, CompoundTag profile) {
        DeviceRecord device = devices.get(phoneId);
        if (device != null) {
            device.profile = profile.copy();
            device.lastSeen = System.currentTimeMillis();
            setDirty();
        }
    }

    public void markSeen(UUID phoneId) {
        DeviceRecord device = devices.get(phoneId);
        if (device != null) {
            device.lastSeen = System.currentTimeMillis();
            setDirty();
        }
    }

    public void updateBankBalance(UUID ownerId, long balance) {
        boolean changed = false;
        for (DeviceRecord device : devices.values()) {
            if (ownerId.equals(device.ownerId)) {
                device.bankBalance = balance;
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public List<DeviceRecord> ownedBy(UUID ownerId) {
        return devices.values().stream()
                .filter(device -> ownerId.equals(device.ownerId))
                .sorted(java.util.Comparator.comparingLong(DeviceRecord::lastSeen).reversed())
                .toList();
    }

    public void syncKnownPhoneIds(UUID ownerId, List<UUID> knownIds) {
        for (DeviceRecord device : devices.values()) {
            if (ownerId.equals(device.ownerId)) {
                device.profile.put("knownPhoneIds", knownPhonesTag(knownIds));
            }
        }
        CompoundTag personalProfile = activePersonalBackups.get(ownerId);
        if (personalProfile != null) {
            personalProfile.put("knownPhoneIds", knownPhonesTag(knownIds));
        }
        setDirty();
    }

    public void setLocks(UUID phoneId, List<String> locks) {
        DeviceRecord device = devices.get(phoneId);
        if (device != null) {
            device.locks = normalizeLocks(locks);
            setDirty();
        }
    }

    public void setPinHash(UUID phoneId, String pinHash) {
        DeviceRecord device = devices.get(phoneId);
        if (device != null) {
            device.pinHash = pinHash == null ? "" : pinHash;
            setDirty();
        }
    }

    public boolean savePhoto(UUID phoneId, String name, byte[] image) {
        DeviceRecord device = devices.get(phoneId);
        if (device == null || name == null || !name.matches("[A-Za-z0-9_.-]{1,128}")
                || image == null || image.length == 0 || image.length > MAX_PHOTO_BYTES
                || (!device.photos.containsKey(name) && device.photos.size() >= MAX_DEVICE_PHOTOS)) {
            return false;
        }
        device.photos.put(name, image.clone());
        setDirty();
        return true;
    }

    public Map<String, byte[]> getPhotos(UUID phoneId) {
        DeviceRecord device = devices.get(phoneId);
        if (device == null) {
            return Map.of();
        }
        Map<String, byte[]> result = new LinkedHashMap<>();
        device.photos.forEach((name, bytes) -> result.put(name, bytes.clone()));
        return Map.copyOf(result);
    }

    public boolean deletePhoto(UUID phoneId, String name) {
        DeviceRecord device = devices.get(phoneId);
        if (device == null || name == null || !name.matches("[A-Za-z0-9_.-]{1,128}")
                || device.photos.remove(name) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public void clearPhotos(UUID phoneId) {
        DeviceRecord device = devices.get(phoneId);
        if (device != null) {
            device.photos.clear();
            setDirty();
        }
    }

    public CompoundTag wipeProfile(UUID phoneId, HolderLookup.Provider registries) {
        DeviceRecord device = devices.get(phoneId);
        if (device == null) {
            return null;
        }
        CompoundTag previousProfile = device.profile.copy();
        PhoneData wiped = new PhoneData();
        wiped.setPhoneNumber(previousProfile.getString("phoneNumber"));
        for (Tag value : previousProfile.getList("knownPhoneIds", Tag.TAG_STRING)) {
            try {
                wiped.addKnownPhoneId(UUID.fromString(value.getAsString()));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed device IDs while clearing the profile.
            }
        }
        CompoundTag profile = wiped.serializeNBT(registries);
        device.profile = profile;
        device.photos.clear();
        device.locks = List.of("all");
        device.pinHash = "";
        setDirty();
        return profile.copy();
    }

    private static List<String> normalizeLocks(List<String> locks) {
        if (locks == null) {
            return List.of();
        }
        return locks.stream().filter(value -> "bank".equals(value) || "all".equals(value))
                .distinct().sorted().toList();
    }

    private static ListTag knownPhonesTag(List<UUID> ids) {
        ListTag list = new ListTag();
        ids.forEach(id -> list.add(net.minecraft.nbt.StringTag.valueOf(id.toString())));
        return list;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        devices.values().forEach(device -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", device.phoneId);
            entry.putUUID("owner", device.ownerId);
            entry.put("profile", device.profile.copy());
            entry.putLong("lastSeen", device.lastSeen);
            entry.putLong("bankBalance", device.bankBalance);
            entry.putString("pinHash", device.pinHash);
            ListTag locks = new ListTag();
            device.locks.forEach(lock -> locks.add(net.minecraft.nbt.StringTag.valueOf(lock)));
            entry.put("locks", locks);
            ListTag photos = new ListTag();
            device.photos.forEach((name, image) -> {
                CompoundTag photo = new CompoundTag();
                photo.putString("name", name);
                photo.putByteArray("image", image);
                photos.add(photo);
            });
            entry.put("photos", photos);
            list.add(entry);
        });
        tag.put("devices", list);
        ListTag backups = new ListTag();
        activePersonalBackups.forEach((playerId, profile) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", playerId);
            entry.put("profile", profile.copy());
            backups.add(entry);
        });
        tag.put("activePersonalBackups", backups);
        return tag;
    }

    private static PhoneDeviceSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PhoneDeviceSavedData data = new PhoneDeviceSavedData();
        for (Tag value : tag.getList("devices", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (!entry.hasUUID("id") || !entry.hasUUID("owner")) {
                continue;
            }
            List<String> locks = new ArrayList<>();
            for (Tag lock : entry.getList("locks", Tag.TAG_STRING)) {
                locks.add(lock.getAsString());
            }
            UUID id = entry.getUUID("id");
            DeviceRecord device = new DeviceRecord(id, entry.getUUID("owner"),
                    entry.getCompound("profile"), entry.getLong("lastSeen"), entry.getLong("bankBalance"),
                    locks, entry.getString("pinHash"));
            for (Tag photoValue : entry.getList("photos", Tag.TAG_COMPOUND)) {
                CompoundTag photo = (CompoundTag) photoValue;
                byte[] image = photo.getByteArray("image");
                if (image.length > 0 && image.length <= MAX_PHOTO_BYTES
                        && device.photos.size() < MAX_DEVICE_PHOTOS) {
                    device.photos.put(photo.getString("name"), image);
                }
            }
            data.devices.put(id, device);
        }
        for (Tag value : tag.getList("activePersonalBackups", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("player")) {
                data.activePersonalBackups.put(entry.getUUID("player"), entry.getCompound("profile").copy());
            }
        }
        return data;
    }

    public static final class DeviceRecord {
        private final UUID phoneId;
        private final UUID ownerId;
        private CompoundTag profile;
        private long lastSeen;
        private long bankBalance;
        private List<String> locks;
        private String pinHash;
        private final Map<String, byte[]> photos = new LinkedHashMap<>();

        private DeviceRecord(UUID phoneId, UUID ownerId, CompoundTag profile, long lastSeen,
                             long bankBalance, List<String> locks, String pinHash) {
            this.phoneId = phoneId;
            this.ownerId = ownerId;
            this.profile = profile.copy();
            this.lastSeen = lastSeen;
            this.bankBalance = bankBalance;
            this.locks = normalizeLocks(locks);
            this.pinHash = pinHash == null ? "" : pinHash;
        }

        public UUID phoneId() { return phoneId; }
        public UUID ownerId() { return ownerId; }
        public CompoundTag profile() { return profile.copy(); }
        public long lastSeen() { return lastSeen; }
        public long bankBalance() { return bankBalance; }
        public List<String> locks() { return locks; }
        public String pinHash() { return pinHash; }
        public boolean isLocked(String feature) { return locks.contains(feature) || locks.contains("all"); }
    }
}

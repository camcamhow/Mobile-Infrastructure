package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import com.example.mobilecellnetwork.data.PhoneDeviceSavedData;
import com.example.mobilecellnetwork.registry.ModDataComponents;
import com.example.mobilecellnetwork.registry.ModItems;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class PhoneDeviceService {
    public static final String BANK_LOCK = "bank";
    public static final String ALL_LOCK = "all";
    private static final Map<UUID, Session> ACTIVE_SESSIONS = new HashMap<>();

    private PhoneDeviceService() {
    }

    public static boolean isSessionUnlocked(ServerPlayer player) {
        Session session = ACTIVE_SESSIONS.get(player.getUUID());
        return session == null || session.unlocked();
    }

    public static boolean tryUnlock(ServerPlayer player, String pin) {
        Session session = ACTIVE_SESSIONS.get(player.getUUID());
        PhoneDeviceSavedData.DeviceRecord device = activeDevice(player);
        if (session == null || device == null || device.isLocked(ALL_LOCK)) {
            return false;
        }
        boolean requiresPin = device.profile().getBoolean("lockScreenEnabled") || !device.pinHash().isBlank();
        if (requiresPin) {
            if (device.pinHash().isBlank()) {
                if (pin != null && !pin.isEmpty()) {
                    return false;
                }
            } else if (!verifyActivePin(player, pin)) {
                return false;
            }
        }
        ACTIVE_SESSIONS.put(player.getUUID(),
                new Session(session.phoneId(), session.personalProfile(), session.ownerId(),
                        session.menuId(), true));
        return true;
    }

    public static UUID prepareAndOpen(ServerPlayer player, ItemStack stack) {
        if (!stack.is(ModItems.CELLPHONE.get())) {
            throw new IllegalArgumentException("A cellphone stack is required.");
        }
        UUID phoneId = stack.get(ModDataComponents.PHONE_ID.get());
        if (phoneId == null) {
            phoneId = UUID.randomUUID();
            stack.set(ModDataComponents.PHONE_ID.get(), phoneId);
        }
        List<String> locks = normalizeLocks(stack.get(ModDataComponents.PHONE_LOCKS.get()));
        stack.set(ModDataComponents.PHONE_LOCKS.get(), locks);
        String pinHash = stack.getOrDefault(ModDataComponents.PHONE_PIN.get(), "");

        closeActiveSession(player);
        PhoneData playerData = player.getData(ModCapabilities.PHONE_DATA);
        playerData.addKnownPhoneId(phoneId);
        PhoneDeviceSavedData devices = savedData(player);
        PhoneDeviceSavedData.DeviceRecord device = devices.get(phoneId);
        if (device == null) {
            device = devices.register(phoneId, player.getUUID(), playerData.serializeNBT(player.registryAccess()),
                    player.getData(ModCapabilities.BANK_ACCOUNT).getBalance(), locks, pinHash);
        } else if (device.ownerId().equals(player.getUUID())) {
            devices.ownedBy(player.getUUID()).stream().map(PhoneDeviceSavedData.DeviceRecord::phoneId)
                    .forEach(playerData::addKnownPhoneId);
        }
        device = devices.get(phoneId);
        if (device.ownerId().equals(player.getUUID())) {
            devices.syncKnownPhoneIds(player.getUUID(), playerData.getKnownPhoneIds());
            devices.updateBankBalance(player.getUUID(),
                    player.getData(ModCapabilities.BANK_ACCOUNT).getBalance());
        }
        devices.markSeen(phoneId);
        stack.set(ModDataComponents.PHONE_LOCKS.get(), device.locks());
        stack.set(ModDataComponents.PHONE_PIN.get(), device.pinHash());

        CompoundTag personalProfile = playerData.serializeNBT(player.registryAccess());
        devices.saveActiveBackup(player.getUUID(), personalProfile);
        boolean requiresUnlock = device.profile().getBoolean("lockScreenEnabled") || !device.pinHash().isBlank();
        ACTIVE_SESSIONS.put(player.getUUID(),
                new Session(phoneId, personalProfile, device.ownerId(), -1, !requiresUnlock));
        playerData.deserializeNBT(player.registryAccess(), device.profile());
        Integer itemCharge = stack.get(ModDataComponents.PHONE_CHARGE.get());
        if (itemCharge != null) {
            playerData.setCharge(itemCharge);
        }
        playerData.setActivePhoneId(phoneId);
        return phoneId;
    }

    public static void closeActiveSession(ServerPlayer player) {
        Session session = ACTIVE_SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        PhoneData phoneData = player.getData(ModCapabilities.PHONE_DATA);
        PhoneDeviceSavedData devices = savedData(player);
        devices.saveProfile(session.phoneId(), phoneData.serializeNBT(player.registryAccess()));
        syncItem(player, session.phoneId());
        syncCharge(player, session.phoneId(), phoneData.getCharge());
        devices.takeActiveBackup(player.getUUID());
        phoneData.deserializeNBT(player.registryAccess(), session.personalProfile());
        phoneData.setActivePhoneId(null);
        NetworkHandler.syncPhoneData(player);
    }

    public static void bindMenu(ServerPlayer player, UUID phoneId, int menuId) {
        Session session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session != null && session.phoneId().equals(phoneId)) {
            ACTIVE_SESSIONS.put(player.getUUID(),
                    new Session(session.phoneId(), session.personalProfile(), session.ownerId(), menuId,
                            session.unlocked()));
        }
    }

    public static void closeSession(ServerPlayer player, UUID phoneId, int menuId) {
        Session session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session != null && session.phoneId().equals(phoneId) && session.menuId() == menuId) {
            closeActiveSession(player);
        }
    }

    public static UUID activePhoneId(ServerPlayer player) {
        Session session = ACTIVE_SESSIONS.get(player.getUUID());
        return session == null ? null : session.phoneId();
    }

    public static boolean isPhoneInInventory(ServerPlayer player, UUID phoneId) {
        return phoneId != null && !findPhone(player, phoneId).isEmpty();
    }

    public static boolean activeDeviceOwnedBy(ServerPlayer player) {
        Session session = ACTIVE_SESSIONS.get(player.getUUID());
        return session != null && player.getUUID().equals(session.ownerId());
    }

    public static PhoneDeviceSavedData.DeviceRecord activeDevice(ServerPlayer player) {
        UUID phoneId = activePhoneId(player);
        return phoneId == null ? null : savedData(player).get(phoneId);
    }

    public static boolean isAllLocked(ServerPlayer player) {
        PhoneDeviceSavedData.DeviceRecord device = activeDevice(player);
        return device != null && device.isLocked(ALL_LOCK);
    }

    public static boolean isBankLocked(ServerPlayer player) {
        PhoneDeviceSavedData.DeviceRecord device = activeDevice(player);
        return device != null && device.isLocked(BANK_LOCK);
    }

    public static UUID activeOwner(ServerPlayer player) {
        PhoneDeviceSavedData.DeviceRecord device = activeDevice(player);
        return device == null ? player.getUUID() : device.ownerId();
    }

    public static boolean verifyActivePin(ServerPlayer player, String pin) {
        PhoneDeviceSavedData.DeviceRecord device = activeDevice(player);
        if (device == null || device.pinHash().isBlank()) {
            return true;
        }
        return constantTimeEquals(device.pinHash(), hashPin(device.phoneId(), pin));
    }

    public static boolean setActivePin(ServerPlayer player, String pin) {
        PhoneDeviceSavedData.DeviceRecord device = activeDevice(player);
        if (device == null || !activeDeviceOwnedBy(player) || pin == null
                || !pin.matches("[0-9]{4,12}")) {
            return false;
        }
        String hash = hashPin(device.phoneId(), pin);
        savedData(player).setPinHash(device.phoneId(), hash);
        syncItem(player, device.phoneId());
        sendState(player);
        return true;
    }

    public static boolean removeActivePin(ServerPlayer player, String currentPin) {
        PhoneDeviceSavedData.DeviceRecord device = activeDevice(player);
        if (device == null || !activeDeviceOwnedBy(player) || !verifyActivePin(player, currentPin)) {
            return false;
        }
        savedData(player).setPinHash(device.phoneId(), "");
        syncItem(player, device.phoneId());
        sendState(player);
        return true;
    }

    public static boolean setLockScreen(ServerPlayer player, boolean enabled) {
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        if (activeDevice(player) == null || !activeDeviceOwnedBy(player)) {
            return false;
        }
        data.setLockScreenEnabled(enabled);
        savedData(player).saveProfile(activePhoneId(player), data.serializeNBT(player.registryAccess()));
        sendState(player);
        return true;
    }

    public static List<DeviceView> knownDevices(ServerPlayer player) {
        if (!activeDeviceOwnedBy(player)) {
            return List.of();
        }
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        return savedData(player).ownedBy(player.getUUID()).stream()
                .filter(device -> data.getKnownPhoneIds().contains(device.phoneId()))
                .map(device -> new DeviceView(device.phoneId(), device.lastSeen(),
                        device.phoneId().equals(activePhoneId(player))))
                .toList();
    }

    public static KnownPhoneDevicesPacket knownDevicesPacket(ServerPlayer player) {
        List<KnownPhoneDevicesPacket.Device> devices = knownDevices(player).stream()
                .map(view -> {
                    PhoneDeviceSavedData.DeviceRecord device = savedData(player).get(view.id());
                    return new KnownPhoneDevicesPacket.Device(view.id(), view.lastSeen(), view.current(),
                            view.id().toString(), device != null && device.isLocked(BANK_LOCK),
                            device != null && device.isLocked(ALL_LOCK));
                }).toList();
        return new KnownPhoneDevicesPacket(devices);
    }

    public static boolean removeKnownDevice(ServerPlayer player, UUID phoneId) {
        if (!activeDeviceOwnedBy(player) || phoneId == null) {
            return false;
        }
        player.getData(ModCapabilities.PHONE_DATA).removeKnownPhoneId(phoneId);
        PhoneDeviceSavedData devices = savedData(player);
        devices.syncKnownPhoneIds(player.getUUID(),
                player.getData(ModCapabilities.PHONE_DATA).getKnownPhoneIds());
        devices.saveProfile(activePhoneId(player),
                player.getData(ModCapabilities.PHONE_DATA).serializeNBT(player.registryAccess()));
        return true;
    }

    public static boolean setRemoteLock(ServerPlayer player, UUID targetId, String feature, boolean lock) {
        if (!activeDeviceOwnedBy(player) || targetId == null
                || !player.getData(ModCapabilities.PHONE_DATA).getKnownPhoneIds().contains(targetId)
                || !(BANK_LOCK.equals(feature) || ALL_LOCK.equals(feature) || "unlock".equals(feature))) {
            return false;
        }
        PhoneDeviceSavedData devices = savedData(player);
        PhoneDeviceSavedData.DeviceRecord target = devices.get(targetId);
        if (target == null || !player.getUUID().equals(target.ownerId())) {
            return false;
        }
        List<String> locks = new ArrayList<>(target.locks());
        if ("unlock".equals(feature)) {
            locks.clear();
        } else if (lock && !locks.contains(feature)) {
            locks.add(feature);
        } else if (!lock) {
            locks.remove(feature);
            if (ALL_LOCK.equals(feature)) {
                locks.clear();
            }
        }
        devices.setLocks(targetId, locks);
        syncItem(player, targetId);
        syncActiveTarget(player, targetId);
        if (lock && ALL_LOCK.equals(feature)) {
            for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
                if (targetId.equals(activePhoneId(online))) {
                    com.example.mobilecellnetwork.voice.CallManager.hangUp(online.getUUID());
                }
            }
        }
        return true;
    }

    public static boolean wipe(ServerPlayer player, UUID targetId) {
        if (!activeDeviceOwnedBy(player) || targetId == null
                || !player.getData(ModCapabilities.PHONE_DATA).getKnownPhoneIds().contains(targetId)) {
            return false;
        }
        PhoneDeviceSavedData.DeviceRecord target = savedData(player).get(targetId);
        if (target == null || !player.getUUID().equals(target.ownerId())) {
            return false;
        }
        CompoundTag wipedProfile = savedData(player).wipeProfile(targetId, player.registryAccess());
        syncItem(player, targetId);
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            if (targetId.equals(activePhoneId(online))) {
                online.getData(ModCapabilities.PHONE_DATA)
                        .deserializeNBT(online.registryAccess(), wipedProfile);
                online.getData(ModCapabilities.PHONE_DATA).setActivePhoneId(targetId);
            }
        }
        syncActiveTarget(player, targetId);
        PacketDistributor.sendToPlayer(player, new PhonePhotoListPacket(targetId, List.of()));
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            if (!online.getUUID().equals(player.getUUID()) && targetId.equals(activePhoneId(online))) {
                PacketDistributor.sendToPlayer(online, new PhonePhotoListPacket(targetId, List.of()));
            }
        }
        return true;
    }

    public static boolean savePhoto(ServerPlayer player, UUID phoneId, String name, byte[] image) {
        if (!hasActiveDevice(player, phoneId) || name == null || !name.matches("[A-Za-z0-9_.-]{1,128}")
                || image == null || image.length == 0 || image.length > PhonePhotoPayloadPacket.MAX_IMAGE_BYTES) {
            return false;
        }
        return savedData(player).savePhoto(phoneId, name, image);
    }

    public static void sendPhotos(ServerPlayer player, UUID phoneId) {
        if (!hasActiveDevice(player, phoneId)) {
            return;
        }
        Map<String, byte[]> photos = savedData(player).getPhotos(phoneId);
        PacketDistributor.sendToPlayer(player, new PhonePhotoListPacket(phoneId,
                photos.keySet().stream().sorted().toList()));
        photos.forEach((name, image) ->
                PacketDistributor.sendToPlayer(player, new PhonePhotoPayloadPacket(phoneId, name, image)));
    }

    private static boolean hasActiveDevice(ServerPlayer player, UUID phoneId) {
        return hasPhoneMenu(player) && phoneId != null && phoneId.equals(activePhoneId(player))
                && !isAllLocked(player);
    }

    public static void sendState(ServerPlayer player) {
        sendState(player, false);
    }

    public static void sendState(ServerPlayer player, boolean clearSensitiveData) {
        PhoneDeviceSavedData.DeviceRecord device = activeDevice(player);
        if (device == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new PhoneDeviceStatePacket(device.phoneId(),
                player.getData(ModCapabilities.PHONE_DATA).isLockScreenEnabled(),
                !device.pinHash().isBlank(), device.isLocked(ALL_LOCK), device.isLocked(BANK_LOCK),
                !player.getUUID().equals(device.ownerId()),
                player.getData(ModCapabilities.PHONE_DATA).getNotifications().stream()
                        .anyMatch(notification -> !notification.read()), clearSensitiveData));
    }

    public static void syncBankView(ServerPlayer player) {
        PhoneDeviceSavedData.DeviceRecord device = activeDevice(player);
        if (!isSessionUnlocked(player)) {
            PacketDistributor.sendToPlayer(player, new BankAccountSyncPacket(0L, List.of()));
            return;
        }
        if (device == null || device.ownerId().equals(player.getUUID())) {
            if (device != null && (device.isLocked(ALL_LOCK) || device.isLocked(BANK_LOCK))) {
                PacketDistributor.sendToPlayer(player, new BankAccountSyncPacket(0L, List.of()));
            } else {
                NetworkHandler.syncBankAccount(player);
            }
            return;
        }
        if (device.isLocked(ALL_LOCK) || device.isLocked(BANK_LOCK)) {
            PacketDistributor.sendToPlayer(player, new BankAccountSyncPacket(0L, List.of()));
            return;
        }
        ServerPlayer owner = player.server.getPlayerList().getPlayer(device.ownerId());
        long balance = owner == null ? device.bankBalance()
                : owner.getData(ModCapabilities.BANK_ACCOUNT).getBalance();
        if (owner != null) {
            savedData(player).updateBankBalance(owner.getUUID(), balance);
        }
        PacketDistributor.sendToPlayer(player, new BankAccountSyncPacket(balance, List.of()));
    }

    public static void onPlayerLogout(ServerPlayer player) {
        closeActiveSession(player);
    }

    public static void onPlayerLogin(ServerPlayer player) {
        CompoundTag backup = savedData(player).takeActiveBackup(player.getUUID());
        if (backup != null) {
            player.getData(ModCapabilities.PHONE_DATA).deserializeNBT(player.registryAccess(), backup);
        }
        savedData(player).updateBankBalance(player.getUUID(),
                player.getData(ModCapabilities.BANK_ACCOUNT).getBalance());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Session session = ACTIVE_SESSIONS.get(player.getUUID());
            if (session != null) {
                savedData(player).saveProfile(session.phoneId(),
                        player.getData(ModCapabilities.PHONE_DATA).serializeNBT(player.registryAccess()));
            }
        }
    }

    public static void refreshOwnedBankBalances(ServerPlayer player, long balance) {
        savedData(player).updateBankBalance(player.getUUID(), balance);
    }

    public static void syncItem(ServerPlayer player, UUID phoneId) {
        PhoneDeviceSavedData.DeviceRecord device = savedData(player).get(phoneId);
        if (device == null) {
            return;
        }
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            ItemStack stack = findPhone(online, phoneId);
            syncStack(stack, device);
        }
        for (net.minecraft.server.level.ServerLevel level : player.server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity) {
                    syncStack(itemEntity.getItem(), device);
                }
            }
        }
    }

    private static void syncStack(ItemStack stack, PhoneDeviceSavedData.DeviceRecord device) {
        if (stack.isEmpty() || !stack.is(ModItems.CELLPHONE.get())
                || !device.phoneId().equals(stack.get(ModDataComponents.PHONE_ID.get()))) {
            return;
        }
        stack.set(ModDataComponents.PHONE_LOCKS.get(), device.locks());
        stack.set(ModDataComponents.PHONE_PIN.get(), device.pinHash());
    }

    private static void syncCharge(ServerPlayer player, UUID phoneId, int charge) {
        int value = Math.clamp(charge, 0,
                com.example.mobilecellnetwork.config.MobileCellNetworkConfig.PHONE_MAX_CHARGE.get());
        for (ServerPlayer online : player.server.getPlayerList().getPlayers()) {
            ItemStack stack = findPhone(online, phoneId);
            if (!stack.isEmpty()) {
                stack.set(ModDataComponents.PHONE_CHARGE.get(), value);
            }
        }
        for (net.minecraft.server.level.ServerLevel level : player.server.getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity
                        && matches(itemEntity.getItem(), phoneId)) {
                    itemEntity.getItem().set(ModDataComponents.PHONE_CHARGE.get(), value);
                }
            }
        }
    }

    private static ItemStack findPhone(ServerPlayer player, UUID phoneId) {
        if (matches(player.getMainHandItem(), phoneId)) {
            return player.getMainHandItem();
        }
        if (matches(player.getOffhandItem(), phoneId)) {
            return player.getOffhandItem();
        }
        return player.getInventory().items.stream().filter(stack -> matches(stack, phoneId))
                .findFirst().orElse(ItemStack.EMPTY);
    }

    private static boolean matches(ItemStack stack, UUID phoneId) {
        return !stack.isEmpty() && stack.is(ModItems.CELLPHONE.get())
                && phoneId.equals(stack.get(ModDataComponents.PHONE_ID.get()));
    }

    private static void syncActiveTarget(ServerPlayer actor, UUID targetId) {
        for (ServerPlayer online : actor.server.getPlayerList().getPlayers()) {
            if (targetId.equals(activePhoneId(online))) {
                sendState(online);
                syncBankView(online);
                sendPhotos(online, targetId);
                if (targetId.equals(activePhoneId(actor))) {
                    NetworkHandler.syncPhoneData(online);
                }
            }
        }
    }

    private static String hashPin(UUID phoneId, String pin) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((phoneId + ":" + pin).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }

    private static List<String> normalizeLocks(List<String> locks) {
        if (locks == null) {
            return List.of();
        }
        return locks.stream().filter(value -> BANK_LOCK.equals(value) || ALL_LOCK.equals(value))
                .distinct().sorted().toList();
    }

    private static PhoneDeviceSavedData savedData(ServerPlayer player) {
        return PhoneDeviceSavedData.get(player.serverLevel());
    }

    private static boolean hasPhoneMenu(ServerPlayer player) {
        return player.containerMenu instanceof com.example.mobilecellnetwork.gui.PhoneMenu menu
                && menu.stillValid(player) && isSessionUnlocked(player) && !isAllLocked(player);
    }

    public record DeviceView(UUID id, long lastSeen, boolean current) {
    }

    private record Session(UUID phoneId, CompoundTag personalProfile, UUID ownerId, int menuId,
                           boolean unlocked) {
        private Session {
            personalProfile = personalProfile.copy();
        }
        @Override
        public CompoundTag personalProfile() {
            return personalProfile.copy();
        }
    }
}

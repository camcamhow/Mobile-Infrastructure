package com.example.mobilecellnetwork.capabilities;

import com.example.mobilecellnetwork.data.MapPointOfInterest;
import com.example.mobilecellnetwork.data.CallDirection;
import com.example.mobilecellnetwork.data.CallLogEntry;
import com.example.mobilecellnetwork.data.Chat;
import com.example.mobilecellnetwork.data.ChatType;
import com.example.mobilecellnetwork.gui.PhoneStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.minecraft.resources.ResourceLocation;

public final class PhoneData implements INBTSerializable<CompoundTag> {
    public static final EntityCapability<PhoneData, Void> CAPABILITY = EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_data"), PhoneData.class);

    public static final int MAX_CALL_LOG = 200;

    private String phoneNumber = "";
    private final Map<String, String> contacts = new LinkedHashMap<>();
    private final Set<String> installedApps = new LinkedHashSet<>();
    private final Set<String> purchasedApps = new LinkedHashSet<>();
    private final List<String> homeAppOrder = new ArrayList<>();
    private final Set<java.util.UUID> followedPlayers = new LinkedHashSet<>();
    private String socialUsername = "";
    private int socialProfileColor = 0xFF4F7CAC;
    private byte[] socialAvatar = new byte[0];
    private final Map<String, Boolean> appNotifications = new LinkedHashMap<>();
    private final List<Notification> notifications = new ArrayList<>();
    private final List<Message> inbox = new ArrayList<>();
    private final List<Message> sent = new ArrayList<>();
    private final List<Chat> chats = new ArrayList<>();
    private final List<Voicemail> voicemail = new ArrayList<>();
    private final List<MissedCall> missedCalls = new ArrayList<>();
    private final List<CallLogEntry> callLog = new ArrayList<>();
    private final List<MapPointOfInterest> mapPoints = new ArrayList<>();
    private final List<UUID> knownPhoneIds = new ArrayList<>();
    private boolean lowPowerMode;
    private boolean lockScreenEnabled = true;
    private boolean ringtoneEnabled = true;
    private boolean notificationSoundsEnabled = true;
    private boolean doNotDisturb;
    private String ringtoneSound = "minecraft:block.note_block.bell";
    private PhoneStyle phoneStyle = PhoneStyle.configuredDefault();
    private String profilePhoto = "";
    private String appearance = "default";
    private boolean setupComplete = true;
    private String deviceName = "";
    private String theme = phoneStyle == PhoneStyle.IOS ? "iphone" : "samsung";
    private String lastKnownPlanId = "";
    private String homeNetworkId = "";
    private int charge = 10_000;
    private com.example.mobilecellnetwork.data.TowerPreferenceMode towerMode =
            com.example.mobilecellnetwork.data.TowerPreferenceMode.AUTOMATIC_FASTEST;
    private long pinnedTowerPos = Long.MIN_VALUE;
    private transient boolean lowPowerPromptShownThisSession;
    private transient boolean flashlightOn;
    private transient UUID activePhoneId;

    public PhoneData() {
        installedApps.addAll(List.of("phone", "sms", "contacts", "settings"));
        installedApps.addAll(List.of("camera", "gallery"));
    }

    public String getPhoneNumber() { return phoneNumber; }
    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String photo) { profilePhoto = photo; }
    public String getAppearance() { return appearance; }
    public boolean isSetupComplete() { return setupComplete; }
    public void setSetupComplete(boolean value) { setupComplete = value; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String value) { deviceName = value; }
    public void setAppearance(String value) {
        if (!"default".equals(value) && !"light".equals(value) && !"dark".equals(value)) {
            throw new IllegalArgumentException("Invalid phone appearance.");
        }
        appearance = value;
    }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Map<String, String> getContacts() { return contacts; }
    public Set<String> getInstalledApps() { return installedApps; }
    public Set<String> getPurchasedApps() { return purchasedApps; }
    public List<String> getHomeAppOrder() { return homeAppOrder; }
    public boolean canUseApp(String appId) {
        if ("settings".equals(appId) || "store".equals(appId) || "plan".equals(appId)) {
            return true;
        }
        if ("camera".equals(appId) || "gallery".equals(appId) || "flashlight".equals(appId)) {
            return true;
        }
        if (!installedApps.contains(appId)) {
            return false;
        }
        return !lowPowerMode || com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .LOW_POWER_ALLOWED_APPS.get().stream().anyMatch(appId::equalsIgnoreCase);
    }
    public Set<java.util.UUID> getFollowedPlayers() { return followedPlayers; }
    public String getSocialUsername() { return socialUsername; }
    public void setSocialUsername(String username) { socialUsername = username; }
    public int getSocialProfileColor() { return socialProfileColor; }
    public void setSocialProfileColor(int color) { socialProfileColor = color | 0xFF000000; }
    public byte[] getSocialAvatar() { return socialAvatar.clone(); }
    public void setSocialAvatar(byte[] image) { socialAvatar = image == null ? new byte[0] : image.clone(); }
    public Map<String, Boolean> getAppNotifications() { return appNotifications; }
    public List<Notification> getNotifications() { return notifications; }
    public List<Message> getInbox() { return inbox; }
    public List<Message> getSent() { return sent; }
    public List<Chat> getChats() { return chats; }
    public List<Voicemail> getVoicemail() { return voicemail; }
    public List<MissedCall> getMissedCalls() { return missedCalls; }
    public List<CallLogEntry> getCallLog() { return callLog; }

    public CallLogEntry addCallLogEntry(CallDirection direction, String otherNumber, String otherName,
                                        int durationSeconds) {
        CallLogEntry entry = CallLogEntry.create(direction, otherNumber, otherName, durationSeconds);
        callLog.add(entry);
        while (callLog.size() > MAX_CALL_LOG) {
            callLog.remove(0);
        }
        return entry;
    }
    public List<MapPointOfInterest> getMapPoints() { return mapPoints; }
    public List<UUID> getKnownPhoneIds() { return knownPhoneIds; }
    public void addKnownPhoneId(UUID phoneId) {
        if (phoneId != null && !knownPhoneIds.contains(phoneId)) {
            knownPhoneIds.add(phoneId);
        }
    }
    public void removeKnownPhoneId(UUID phoneId) { knownPhoneIds.remove(phoneId); }
    public UUID getActivePhoneId() { return activePhoneId; }
    public void setActivePhoneId(UUID phoneId) { activePhoneId = phoneId; }
    public boolean isLockScreenEnabled() { return lockScreenEnabled; }
    public void setLockScreenEnabled(boolean enabled) { lockScreenEnabled = enabled; }
    public void addMessage(List<Message> folder, Message message, int maxPerThread) {
        folder.add(message);
        String threadNumber = folder == inbox ? message.senderNumber() : message.recipientNumber();
        long threadCount = folder.stream().filter(entry -> (folder == inbox
                ? entry.senderNumber() : entry.recipientNumber()).equals(threadNumber)).count();
        while (threadCount > maxPerThread) {
            for (int index = 0; index < folder.size(); index++) {
                Message entry = folder.get(index);
                if ((folder == inbox ? entry.senderNumber() : entry.recipientNumber()).equals(threadNumber)) {
                    folder.remove(index);
                    threadCount--;
                    break;
                }
            }
        }
    }
    public boolean isLowPowerMode() { return lowPowerMode; }
    public void setLowPowerMode(boolean lowPowerMode) { this.lowPowerMode = lowPowerMode; }
    public boolean isRingtoneEnabled() { return ringtoneEnabled; }
    public void setRingtoneEnabled(boolean ringtoneEnabled) { this.ringtoneEnabled = ringtoneEnabled; }
    public boolean areNotificationSoundsEnabled() { return notificationSoundsEnabled && !doNotDisturb; }
    public boolean isNotificationSoundsEnabled() { return notificationSoundsEnabled; }
    public void setNotificationSoundsEnabled(boolean enabled) { notificationSoundsEnabled = enabled; }
    public boolean isDoNotDisturb() { return doNotDisturb; }
    public void setDoNotDisturb(boolean enabled) { doNotDisturb = enabled; }
    public String getRingtoneSound() { return ringtoneSound; }
    public void setRingtoneSound(String sound) { ringtoneSound = sound; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) {
        this.theme = "iphone".equalsIgnoreCase(theme) ? "iphone" : "samsung";
        phoneStyle = "iphone".equalsIgnoreCase(theme) ? PhoneStyle.IOS : PhoneStyle.SAMSUNG_ONE_UI;
    }
    public PhoneStyle getPhoneStyle() { return phoneStyle; }
    public void setPhoneStyle(PhoneStyle phoneStyle) {
        this.phoneStyle = phoneStyle == null ? PhoneStyle.DEFAULT : phoneStyle;
        theme = this.phoneStyle == PhoneStyle.IOS ? "iphone" : "samsung";
    }
    public boolean hasActivePlan() { return !lastKnownPlanId.isBlank(); }
    public com.example.mobilecellnetwork.data.TowerPreferenceMode getTowerMode() { return towerMode; }
    public void setTowerMode(com.example.mobilecellnetwork.data.TowerPreferenceMode mode) {
        towerMode = mode == null ? com.example.mobilecellnetwork.data.TowerPreferenceMode.AUTOMATIC_FASTEST : mode;
    }
    /** Pinned tower block position, or null when none is pinned. */
    public net.minecraft.core.BlockPos getPinnedTowerPos() {
        return pinnedTowerPos == Long.MIN_VALUE ? null : net.minecraft.core.BlockPos.of(pinnedTowerPos);
    }
    public void setPinnedTowerPos(net.minecraft.core.BlockPos pos) {
        pinnedTowerPos = pos == null ? Long.MIN_VALUE : pos.asLong();
    }
    public String getHomeNetworkId() { return homeNetworkId; }
    public void setHomeNetworkId(String id) { homeNetworkId = id == null ? "" : id; }
    public String getLastKnownPlanId() { return lastKnownPlanId; }
    public void setLastKnownPlanId(String lastKnownPlanId) { this.lastKnownPlanId = lastKnownPlanId; }
    public int getCharge() { return charge; }
    public void setCharge(int charge) {
        this.charge = Math.clamp(charge, 0,
                com.example.mobilecellnetwork.config.MobileCellNetworkConfig.PHONE_MAX_CHARGE.get());
    }
    public boolean isLowPower() { return lowPowerMode; }
    public boolean isDead() { return charge <= 0; }
    public boolean isFlashlightOn() { return flashlightOn; }
    public void setFlashlightOn(boolean on) { flashlightOn = on; }
    public boolean isLowPowerPromptShownThisSession() { return lowPowerPromptShownThisSession; }
    public void setLowPowerPromptShownThisSession(boolean shown) { lowPowerPromptShownThisSession = shown; }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("phoneNumber", phoneNumber);
        tag.putBoolean("lowPowerMode", lowPowerMode);
        tag.putBoolean("lockScreenEnabled", lockScreenEnabled);
        tag.putBoolean("ringtoneEnabled", ringtoneEnabled);
        tag.putBoolean("notificationSoundsEnabled", notificationSoundsEnabled);
        tag.putBoolean("doNotDisturb", doNotDisturb);
        tag.putString("ringtoneSound", ringtoneSound);
        tag.putString("theme", theme);
        tag.putString("phoneStyle", phoneStyle.name());
        tag.putString("profilePhoto", profilePhoto);
        tag.putString("appearance", appearance);
        tag.putBoolean("setupComplete", setupComplete);
        tag.putString("deviceName", deviceName);
        tag.putString("lastKnownPlanId", lastKnownPlanId);
        tag.putString("homeNetworkId", homeNetworkId);
        tag.putString("towerMode", towerMode.name());
        tag.putLong("pinnedTowerPos", pinnedTowerPos);
        tag.putInt("charge", charge);
        tag.putString("socialUsername", socialUsername);
        tag.putInt("socialProfileColor", socialProfileColor);
        tag.putByteArray("socialAvatar", socialAvatar);

        CompoundTag contactTag = new CompoundTag();
        contacts.forEach(contactTag::putString);
        tag.put("contacts", contactTag);

        ListTag apps = new ListTag();
        installedApps.forEach(app -> apps.add(net.minecraft.nbt.StringTag.valueOf(app)));
        tag.put("installedApps", apps);
        ListTag purchased = new ListTag();
        purchasedApps.forEach(app -> purchased.add(net.minecraft.nbt.StringTag.valueOf(app)));
        tag.put("purchasedApps", purchased);
        ListTag order = new ListTag();
        homeAppOrder.forEach(app -> order.add(net.minecraft.nbt.StringTag.valueOf(app)));
        tag.put("homeAppOrder", order);
        ListTag follows = new ListTag();
        followedPlayers.forEach(id -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", id);
            follows.add(entry);
        });
        tag.put("followedPlayers", follows);
        CompoundTag appNotificationsTag = new CompoundTag();
        appNotifications.forEach(appNotificationsTag::putBoolean);
        tag.put("appNotifications", appNotificationsTag);
        tag.put("notifications", encodeNotifications());
        tag.put("inbox", encodeMessages(inbox));
        tag.put("sent", encodeMessages(sent));
        tag.put("chats", encodeChats());
        tag.put("voicemail", encodeVoicemail());
        tag.put("missedCalls", encodeMissedCalls());
        ListTag callLogTag = new ListTag();
        callLog.forEach(entry -> callLogTag.add(entry.toTag()));
        tag.put("callLog", callLogTag);
        tag.put("mapPoints", encodeMapPoints());
        ListTag knownDevices = new ListTag();
        knownPhoneIds.forEach(id -> knownDevices.add(net.minecraft.nbt.StringTag.valueOf(id.toString())));
        tag.put("knownPhoneIds", knownDevices);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        phoneNumber = tag.getString("phoneNumber");
        profilePhoto = tag.getString("profilePhoto");
        setupComplete = !tag.contains("setupComplete") || tag.getBoolean("setupComplete");
        deviceName = tag.getString("deviceName");
        String savedAppearance = tag.getString("appearance");
        appearance = "light".equals(savedAppearance) || "dark".equals(savedAppearance)
                ? savedAppearance : "default";
        lowPowerMode = tag.getBoolean("lowPowerMode");
        lockScreenEnabled = !tag.contains("lockScreenEnabled") || tag.getBoolean("lockScreenEnabled");
        ringtoneEnabled = !tag.contains("ringtoneEnabled") || tag.getBoolean("ringtoneEnabled");
        notificationSoundsEnabled = !tag.contains("notificationSoundsEnabled")
                || tag.getBoolean("notificationSoundsEnabled");
        doNotDisturb = tag.getBoolean("doNotDisturb");
        ringtoneSound = tag.contains("ringtoneSound") ? tag.getString("ringtoneSound")
                : "minecraft:block.note_block.bell";
        String savedTheme = tag.contains("theme") ? tag.getString("theme") : "dark";
        setTheme("light".equalsIgnoreCase(savedTheme) ? "iphone" : savedTheme);
        phoneStyle = tag.contains("phoneStyle")
                ? PhoneStyle.fromName(tag.getString("phoneStyle")) : PhoneStyle.configuredDefault();
        theme = phoneStyle == PhoneStyle.IOS ? "iphone" : "samsung";
        lastKnownPlanId = tag.getString("lastKnownPlanId");
        homeNetworkId = tag.getString("homeNetworkId");
        try {
            towerMode = tag.contains("towerMode", Tag.TAG_STRING)
                    ? com.example.mobilecellnetwork.data.TowerPreferenceMode.valueOf(tag.getString("towerMode"))
                    : com.example.mobilecellnetwork.data.TowerPreferenceMode.AUTOMATIC_FASTEST;
        } catch (IllegalArgumentException exception) {
            towerMode = com.example.mobilecellnetwork.data.TowerPreferenceMode.AUTOMATIC_FASTEST;
        }
        pinnedTowerPos = tag.contains("pinnedTowerPos", Tag.TAG_LONG) ? tag.getLong("pinnedTowerPos") : Long.MIN_VALUE;
        setCharge(tag.contains("charge", Tag.TAG_INT) ? tag.getInt("charge") : 10_000);
        socialUsername = tag.getString("socialUsername");
        socialProfileColor = tag.contains("socialProfileColor", Tag.TAG_INT)
                ? tag.getInt("socialProfileColor") | 0xFF000000 : 0xFF4F7CAC;
        socialAvatar = tag.getByteArray("socialAvatar");

        contacts.clear();
        CompoundTag contactTag = tag.getCompound("contacts");
        for (String name : contactTag.getAllKeys()) {
            contacts.put(name, contactTag.getString(name));
        }

        installedApps.clear();
        if (!tag.contains("installedApps")) {
            installedApps.addAll(List.of("phone", "sms", "contacts", "settings"));
        }
        for (Tag app : tag.getList("installedApps", Tag.TAG_STRING)) {
            installedApps.add(app.getAsString());
        }
        installedApps.addAll(List.of("camera", "gallery"));
        purchasedApps.clear();
        if (tag.contains("purchasedApps", Tag.TAG_LIST)) {
            for (Tag app : tag.getList("purchasedApps", Tag.TAG_STRING)) purchasedApps.add(app.getAsString());
        } else {
            for (String app : List.of("socials", "ringtones")) {
                if (installedApps.contains(app)) purchasedApps.add(app);
            }
        }
        homeAppOrder.clear();
        for (Tag app : tag.getList("homeAppOrder", Tag.TAG_STRING)) homeAppOrder.add(app.getAsString());
        followedPlayers.clear();
        for (Tag value : tag.getList("followedPlayers", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("player")) {
                followedPlayers.add(entry.getUUID("player"));
            }
        }
        appNotifications.clear();
        CompoundTag appNotificationTag = tag.getCompound("appNotifications");
        appNotificationTag.getAllKeys().forEach(key -> appNotifications.put(key, appNotificationTag.getBoolean(key)));
        decodeNotifications(tag.getList("notifications", Tag.TAG_COMPOUND));
        decodeMessages(tag.getList("inbox", Tag.TAG_COMPOUND), inbox);
        decodeMessages(tag.getList("sent", Tag.TAG_COMPOUND), sent);
        decodeChats(tag.getList("chats", Tag.TAG_COMPOUND));
        decodeVoicemail(tag.getList("voicemail", Tag.TAG_COMPOUND));
        decodeMissedCalls(tag.getList("missedCalls", Tag.TAG_COMPOUND));
        callLog.clear();
        for (Tag value : tag.getList("callLog", Tag.TAG_COMPOUND)) {
            callLog.add(CallLogEntry.fromTag((CompoundTag) value));
        }
        while (callLog.size() > MAX_CALL_LOG) {
            callLog.remove(0);
        }
        decodeMapPoints(tag.getList("mapPoints", Tag.TAG_COMPOUND));
        knownPhoneIds.clear();
        for (Tag value : tag.getList("knownPhoneIds", Tag.TAG_STRING)) {
            try {
                UUID id = UUID.fromString(value.getAsString());
                if (!knownPhoneIds.contains(id)) {
                    knownPhoneIds.add(id);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy device IDs.
            }
        }
    }

    private ListTag encodeNotifications() {
        ListTag list = new ListTag();
        for (Notification notification : notifications) {
            CompoundTag entry = new CompoundTag();
            entry.putString("title", notification.title());
            entry.putString("type", notification.type());
            entry.putString("body", notification.body());
            entry.putLong("timestamp", notification.timestamp());
            entry.putBoolean("read", notification.read());
            list.add(entry);
        }
        return list;
    }

    private void decodeNotifications(ListTag list) {
        notifications.clear();
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            notifications.add(new Notification(
                    entry.contains("type") ? entry.getString("type") : "SMS",
                    entry.getString("title"),
                    entry.contains("body") ? entry.getString("body") : entry.getString("message"),
                    entry.getLong("timestamp"), entry.getBoolean("read")));
        }
    }

    private static ListTag encodeMessages(List<Message> messages) {
        ListTag list = new ListTag();
        for (Message message : messages) {
            CompoundTag entry = new CompoundTag();
            entry.putString("senderNumber", message.senderNumber());
            entry.putString("recipientNumber", message.recipientNumber());
            entry.putString("body", message.body());
            entry.putLong("timestamp", message.timestamp());
            entry.putBoolean("read", message.read());
            list.add(entry);
        }
        return list;
    }

    private static void decodeMessages(ListTag list, List<Message> messages) {
        messages.clear();
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            messages.add(new Message(entry.getString("senderNumber"), entry.getString("recipientNumber"),
                    entry.getString("body"), entry.getLong("timestamp"), entry.getBoolean("read")));
        }
    }

    private ListTag encodeChats() {
        ListTag list = new ListTag();
        for (Chat chat : chats) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", chat.id());
            entry.putString("type", chat.type().name());
            entry.putString("displayName", chat.displayName());
            entry.putLong("lastReadTimestamp", chat.lastReadTimestamp());
            entry.putBoolean("pinnedToTop", chat.pinnedToTop());
            ListTag members = new ListTag();
            chat.memberNumbers().forEach(number -> members.add(net.minecraft.nbt.StringTag.valueOf(number)));
            entry.put("memberNumbers", members);
            ListTag chatMessages = new ListTag();
            for (com.example.mobilecellnetwork.data.Message message : chat.messages()) {
                CompoundTag messageTag = new CompoundTag();
                messageTag.putUUID("id", message.id());
                messageTag.putString("senderNumber", message.senderNumber());
                messageTag.putString("body", message.body());
                messageTag.putLong("timestamp", message.timestamp());
                messageTag.putBoolean("read", message.read());
                chatMessages.add(messageTag);
            }
            entry.put("messages", chatMessages);
            list.add(entry);
        }
        return list;
    }

    private void decodeChats(ListTag list) {
        chats.clear();
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            ChatType type;
            try {
                type = ChatType.valueOf(entry.getString("type"));
            } catch (IllegalArgumentException exception) {
                type = ChatType.DIRECT;
            }
            List<String> members = new ArrayList<>();
            for (Tag member : entry.getList("memberNumbers", Tag.TAG_STRING)) {
                members.add(member.getAsString());
            }
            List<com.example.mobilecellnetwork.data.Message> messages = new ArrayList<>();
            for (Tag messageValue : entry.getList("messages", Tag.TAG_COMPOUND)) {
                CompoundTag messageTag = (CompoundTag) messageValue;
                messages.add(new com.example.mobilecellnetwork.data.Message(
                        messageTag.hasUUID("id") ? messageTag.getUUID("id") : java.util.UUID.randomUUID(),
                        messageTag.getString("senderNumber"), messageTag.getString("body"),
                        messageTag.getLong("timestamp"), messageTag.getBoolean("read")));
            }
            chats.add(new Chat(entry.hasUUID("id") ? entry.getUUID("id") : java.util.UUID.randomUUID(),
                    type, entry.getString("displayName"), members, messages,
                    entry.getLong("lastReadTimestamp"), entry.getBoolean("pinnedToTop")));
        }
    }

    private ListTag encodeVoicemail() {
        ListTag list = new ListTag();
        for (Voicemail message : voicemail) {
            CompoundTag entry = new CompoundTag();
            entry.putString("caller", message.caller());
            entry.putString("message", message.message());
            entry.putLong("timestamp", message.timestamp());
            entry.putString("note", message.note());
            list.add(entry);
        }
        return list;
    }

    private void decodeVoicemail(ListTag list) {
        voicemail.clear();
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            voicemail.add(new Voicemail(
                    entry.getString("caller"), entry.getLong("timestamp"),
                    entry.contains("note") ? entry.getString("note") : entry.getString("message")));
        }
    }

    private ListTag encodeMissedCalls() {
        ListTag list = new ListTag();
        for (MissedCall call : missedCalls) {
            CompoundTag entry = new CompoundTag();
            entry.putString("number", call.number());
            entry.putLong("timestamp", call.timestamp());
            list.add(entry);
        }
        return list;
    }

    private void decodeMissedCalls(ListTag list) {
        missedCalls.clear();
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            missedCalls.add(new MissedCall(entry.getString("number"), entry.getLong("timestamp")));
        }
    }

    private ListTag encodeMapPoints() {
        ListTag list = new ListTag();
        for (MapPointOfInterest point : mapPoints) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", point.id());
            entry.putString("name", point.name());
            entry.putString("dimension", point.dimension());
            entry.putInt("x", point.x());
            entry.putInt("y", point.y());
            entry.putInt("z", point.z());
            if (point.ownerUUID() != null) {
                entry.putUUID("owner", point.ownerUUID());
            }
            entry.putString("ownerName", point.ownerName());
            entry.putString("visibility", point.visibility().name());
            list.add(entry);
        }
        return list;
    }

    private void decodeMapPoints(ListTag list) {
        mapPoints.clear();
        for (Tag value : list) {
            CompoundTag entry = (CompoundTag) value;
            try {
                MapPointOfInterest.Visibility visibility = MapPointOfInterest.Visibility.valueOf(
                        entry.getString("visibility"));
                if (visibility != MapPointOfInterest.Visibility.PUBLIC) {
                    mapPoints.add(new MapPointOfInterest(
                            entry.hasUUID("id") ? entry.getUUID("id") : java.util.UUID.randomUUID(),
                            entry.getString("name"), entry.getString("dimension"), entry.getInt("x"),
                            entry.getInt("y"), entry.getInt("z"),
                            entry.hasUUID("owner") ? entry.getUUID("owner") : null,
                            entry.getString("ownerName"), visibility));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public record Message(String senderNumber, String recipientNumber, String body, long timestamp, boolean read) {
        public Message markRead() {
            return new Message(senderNumber, recipientNumber, body, timestamp, true);
        }
    }

    public record Notification(String type, String title, String body, long timestamp, boolean read) {
        public Notification(String title, String message, long timestamp) {
            this("SMS", title, message, timestamp, false);
        }

        public String message() {
            return body;
        }
    }

    public record Voicemail(String caller, long timestamp, String note) {
        public Voicemail(String caller, String message, long timestamp) {
            this(caller, timestamp, message);
        }

        public String message() {
            return note;
        }
    }

    public record MissedCall(String number, long timestamp) {
    }
}

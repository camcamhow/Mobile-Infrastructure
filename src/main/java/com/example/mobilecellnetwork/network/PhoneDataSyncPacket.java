package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PhoneDataSyncPacket(
        String phoneNumber,
        String planId,
        boolean lowPowerMode,
        boolean phoneDead,
        boolean ringtoneEnabled,
        boolean notificationSoundsEnabled,
        boolean doNotDisturb,
        String ringtoneSound,
        String theme,
        List<ContactEntry> contacts,
        List<String> notifications,
        List<OnlinePlayer> onlinePlayers,
        List<String> installedApps,
        List<FollowedPlayer> followedPlayers,
        List<AppNotification> appNotifications
) implements CustomPacketPayload {
    public static final Type<PhoneDataSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "phone_data_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PhoneDataSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.phoneNumber());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.planId());
                ByteBufCodecs.BOOL.encode(buffer, packet.lowPowerMode());
                ByteBufCodecs.BOOL.encode(buffer, packet.phoneDead());
                ByteBufCodecs.BOOL.encode(buffer, packet.ringtoneEnabled());
                ByteBufCodecs.BOOL.encode(buffer, packet.notificationSoundsEnabled());
                ByteBufCodecs.BOOL.encode(buffer, packet.doNotDisturb());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.ringtoneSound());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.theme());
                buffer.writeVarInt(packet.contacts().size());
                for (ContactEntry contact : packet.contacts()) {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, contact.name());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, contact.number());
                }
                buffer.writeVarInt(packet.notifications().size());
                for (String notification : packet.notifications()) {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, notification);
                }
                buffer.writeVarInt(packet.onlinePlayers().size());
                for (OnlinePlayer player : packet.onlinePlayers()) {
                    buffer.writeUUID(player.id());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, player.name());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, player.number());
                }
                writeStrings(buffer, packet.installedApps());
                buffer.writeVarInt(packet.followedPlayers().size());
                packet.followedPlayers().forEach(follow -> {
                    buffer.writeUUID(follow.id());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, follow.name());
                });
                buffer.writeVarInt(packet.appNotifications().size());
                packet.appNotifications().forEach(setting -> {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, setting.appId());
                    buffer.writeBoolean(setting.enabled());
                });
            },
            buffer -> {
                String phoneNumber = ByteBufCodecs.STRING_UTF8.decode(buffer);
                String planId = ByteBufCodecs.STRING_UTF8.decode(buffer);
                boolean lowPowerMode = ByteBufCodecs.BOOL.decode(buffer);
                boolean phoneDead = ByteBufCodecs.BOOL.decode(buffer);
                boolean ringtoneEnabled = ByteBufCodecs.BOOL.decode(buffer);
                boolean notificationSoundsEnabled = ByteBufCodecs.BOOL.decode(buffer);
                boolean doNotDisturb = ByteBufCodecs.BOOL.decode(buffer);
                String ringtoneSound = ByteBufCodecs.STRING_UTF8.decode(buffer);
                String theme = ByteBufCodecs.STRING_UTF8.decode(buffer);
                List<ContactEntry> contacts = new ArrayList<>();
                int contactCount = buffer.readVarInt();
                for (int index = 0; index < contactCount; index++) {
                    contacts.add(new ContactEntry(
                            ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer)));
                }
                List<String> notifications = new ArrayList<>();
                int notificationCount = buffer.readVarInt();
                for (int index = 0; index < notificationCount; index++) {
                    notifications.add(ByteBufCodecs.STRING_UTF8.decode(buffer));
                }
                List<OnlinePlayer> onlinePlayers = new ArrayList<>();
                int playerCount = buffer.readVarInt();
                for (int index = 0; index < playerCount; index++) {
                    onlinePlayers.add(new OnlinePlayer(
                            buffer.readUUID(), ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer)));
                }
                List<String> installedApps = readStrings(buffer);
                int followCount = Math.clamp(buffer.readVarInt(), 0, 1000);
                List<FollowedPlayer> followedPlayers = new ArrayList<>(followCount);
                for (int i = 0; i < followCount; i++) {
                    followedPlayers.add(new FollowedPlayer(buffer.readUUID(), ByteBufCodecs.STRING_UTF8.decode(buffer)));
                }
                int appNotificationCount = Math.clamp(buffer.readVarInt(), 0, 100);
                List<AppNotification> appNotifications = new ArrayList<>(appNotificationCount);
                for (int i = 0; i < appNotificationCount; i++) {
                    appNotifications.add(new AppNotification(ByteBufCodecs.STRING_UTF8.decode(buffer),
                            buffer.readBoolean()));
                }
                return new PhoneDataSyncPacket(
                        phoneNumber, planId, lowPowerMode, phoneDead, ringtoneEnabled,
                        notificationSoundsEnabled, doNotDisturb, ringtoneSound, theme,
                        contacts, notifications, onlinePlayers, installedApps, followedPlayers, appNotifications);
            });

    private static void writeStrings(net.minecraft.network.RegistryFriendlyByteBuf buffer, List<String> strings) {
        buffer.writeVarInt(Math.min(strings.size(), 256));
        strings.stream().limit(256).forEach(value -> ByteBufCodecs.STRING_UTF8.encode(buffer, value));
    }

    private static List<String> readStrings(net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        int count = Math.clamp(buffer.readVarInt(), 0, 256);
        List<String> strings = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            strings.add(ByteBufCodecs.STRING_UTF8.decode(buffer));
        }
        return strings;
    }

    public record ContactEntry(String name, String number) {
    }

    public record OnlinePlayer(java.util.UUID id, String name, String number) {
    }

    public record FollowedPlayer(java.util.UUID id, String name) {
    }

    public record AppNotification(String appId, boolean enabled) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

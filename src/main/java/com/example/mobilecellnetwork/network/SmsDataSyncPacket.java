package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.capabilities.PhoneData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SmsDataSyncPacket(List<MessageEntry> inbox, List<MessageEntry> sent,
                                List<VoicemailEntry> voicemail, int missedCallCount)
        implements CustomPacketPayload {
    public static final Type<SmsDataSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "sms_data_sync"));

    public static final net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, SmsDataSyncPacket> STREAM_CODEC =
            net.minecraft.network.codec.StreamCodec.of((buffer, packet) -> {
                writeMessages(buffer, packet.inbox());
                writeMessages(buffer, packet.sent());
                buffer.writeVarInt(Math.min(1000, packet.voicemail().size()));
                packet.voicemail().stream().limit(1000).forEach(entry -> {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, entry.caller());
                    buffer.writeLong(entry.timestamp());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, entry.note());
                });
                buffer.writeVarInt(packet.missedCallCount());
            }, buffer -> new SmsDataSyncPacket(readMessages(buffer), readMessages(buffer), readVoicemail(buffer),
                    Math.clamp(buffer.readVarInt(), 0, 10000)));

    private static void writeMessages(RegistryFriendlyByteBuf buffer, List<MessageEntry> entries) {
        buffer.writeVarInt(Math.min(1000, entries.size()));
        entries.stream().limit(1000).forEach(entry -> {
            ByteBufCodecs.STRING_UTF8.encode(buffer, entry.senderNumber());
            ByteBufCodecs.STRING_UTF8.encode(buffer, entry.recipientNumber());
            ByteBufCodecs.STRING_UTF8.encode(buffer, entry.body());
            buffer.writeLong(entry.timestamp());
            buffer.writeBoolean(entry.read());
        });
    }

    private static List<MessageEntry> readMessages(RegistryFriendlyByteBuf buffer) {
        int count = Math.clamp(buffer.readVarInt(), 0, 1000);
        List<MessageEntry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new MessageEntry(ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                    buffer.readLong(), buffer.readBoolean()));
        }
        return entries;
    }

    private static List<VoicemailEntry> readVoicemail(RegistryFriendlyByteBuf buffer) {
        int count = Math.clamp(buffer.readVarInt(), 0, 1000);
        List<VoicemailEntry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new VoicemailEntry(ByteBufCodecs.STRING_UTF8.decode(buffer), buffer.readLong(),
                    ByteBufCodecs.STRING_UTF8.decode(buffer)));
        }
        return entries;
    }

    public static SmsDataSyncPacket from(PhoneData data) {
        return new SmsDataSyncPacket(data.getInbox().stream().map(MessageEntry::from).toList(),
                data.getSent().stream().map(MessageEntry::from).toList(),
                data.getVoicemail().stream().map(entry -> new VoicemailEntry(
                        entry.caller(), entry.timestamp(), entry.note())).toList(), data.getMissedCalls().size());
    }

    public record MessageEntry(String senderNumber, String recipientNumber, String body, long timestamp, boolean read) {
        public static MessageEntry from(PhoneData.Message message) {
            return new MessageEntry(message.senderNumber(), message.recipientNumber(),
                    message.body(), message.timestamp(), message.read());
        }

        public PhoneData.Message toMessage() {
            return new PhoneData.Message(senderNumber, recipientNumber, body, timestamp, read);
        }
    }

    public record VoicemailEntry(String caller, long timestamp, String note) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

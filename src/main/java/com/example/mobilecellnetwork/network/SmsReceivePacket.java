package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.data.Message;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class SmsReceivePacket implements CustomPacketPayload {
    public static final Type<SmsReceivePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "sms_receive"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SmsReceivePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeBoolean(packet.legacyMessage != null);
                if (packet.legacyMessage != null) {
                    var message = packet.legacyMessage;
                    ByteBufCodecs.STRING_UTF8.encode(buffer, message.senderNumber());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, message.recipientNumber());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, message.body());
                    buffer.writeLong(message.timestamp());
                    buffer.writeBoolean(message.read());
                } else {
                    buffer.writeUUID(packet.chatId);
                    buffer.writeUUID(packet.message.id());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, packet.message.senderNumber());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, packet.message.body());
                    buffer.writeLong(packet.message.timestamp());
                    buffer.writeBoolean(packet.message.read());
                }
            }, buffer -> {
                if (buffer.readBoolean()) {
                    return new SmsReceivePacket(new SmsDataSyncPacket.MessageEntry(
                            ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer), buffer.readLong(), buffer.readBoolean()));
                }
                UUID chatId = buffer.readUUID();
                return new SmsReceivePacket(chatId, new Message(buffer.readUUID(),
                        ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                        buffer.readLong(), buffer.readBoolean()));
            });

    private final UUID chatId;
    private final Message message;
    private final SmsDataSyncPacket.MessageEntry legacyMessage;

    public SmsReceivePacket(UUID chatId, Message message) {
        this.chatId = chatId;
        this.message = message;
        this.legacyMessage = null;
    }

    public SmsReceivePacket(SmsDataSyncPacket.MessageEntry legacyMessage) {
        this.chatId = null;
        this.message = null;
        this.legacyMessage = legacyMessage;
    }

    public UUID chatId() { return chatId; }
    public Message message() { return message; }
    public SmsDataSyncPacket.MessageEntry legacyMessage() { return legacyMessage; }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

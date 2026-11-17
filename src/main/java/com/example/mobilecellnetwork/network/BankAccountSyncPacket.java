package com.example.mobilecellnetwork.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BankAccountSyncPacket(long balance, List<Transaction> transactions) implements CustomPacketPayload {
    public static final Type<BankAccountSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "bank_account_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BankAccountSyncPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeLong(packet.balance());
                buffer.writeVarInt(Math.min(100, packet.transactions().size()));
                packet.transactions().stream().limit(100).forEach(transaction -> {
                    buffer.writeLong(transaction.amount());
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.encode(buffer, transaction.reason());
                    buffer.writeLong(transaction.timestamp());
                    net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.encode(buffer, transaction.details());
                });
            }, buffer -> {
                long balance = buffer.readLong();
                int count = Math.clamp(buffer.readVarInt(), 0, 100);
                List<Transaction> transactions = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    transactions.add(new Transaction(buffer.readLong(),
                            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.decode(buffer),
                            buffer.readLong(),
                            net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.decode(buffer)));
                }
                return new BankAccountSyncPacket(balance, transactions);
            });

    public record Transaction(long amount, String reason, long timestamp, String details) {
        public Transaction(long amount, String reason, long timestamp) {
            this(amount, reason, timestamp, "");
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

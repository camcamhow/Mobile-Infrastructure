package com.example.mobilecellnetwork.capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class BankAccount implements INBTSerializable<CompoundTag> {
    public static final EntityCapability<BankAccount, Void> CAPABILITY = EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "bank_account"), BankAccount.class);

    private long balance;
    private final List<Transaction> transactionHistory = new ArrayList<>();
    private UUID linkedCardId;

    public long getBalance() { return balance; }
    public void setBalance(long balance) { this.balance = balance; }
    public List<Transaction> getTransactionHistory() { return transactionHistory; }
    public UUID getLinkedCardId() { return linkedCardId; }
    public void setLinkedCardId(UUID linkedCardId) { this.linkedCardId = linkedCardId; }

    public void deposit(long amount, String reason) {
        if (amount < 0L) {
            throw new IllegalArgumentException("Deposit amount must be non-negative.");
        }
        if (amount > Long.MAX_VALUE - balance) {
            throw new ArithmeticException("Bank account balance overflow.");
        }
        balance += amount;
        transactionHistory.add(new Transaction(amount, reason, System.currentTimeMillis()));
    }

    public boolean withdraw(long amount, String reason) {
        if (amount < 0L) {
            throw new IllegalArgumentException("Withdrawal amount must be non-negative.");
        }
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        transactionHistory.add(new Transaction(-amount, reason, System.currentTimeMillis()));
        return true;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("balance", balance);
        if (linkedCardId != null) {
            tag.putUUID("linkedCardId", linkedCardId);
        }
        ListTag history = new ListTag();
        for (Transaction transaction : transactionHistory) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("amount", transaction.amount());
            entry.putString("reason", transaction.reason());
            entry.putLong("timestamp", transaction.timestamp());
            entry.putString("details", transaction.details());
            history.add(entry);
        }
        tag.put("transactionHistory", history);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        balance = tag.getLong("balance");
        linkedCardId = tag.hasUUID("linkedCardId") ? tag.getUUID("linkedCardId") : null;
        transactionHistory.clear();
        for (Tag value : tag.getList("transactionHistory", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            transactionHistory.add(new Transaction(entry.getLong("amount"),
                    entry.contains("reason") ? entry.getString("reason") : entry.getString("description"),
                    entry.getLong("timestamp"), entry.getString("details")));
        }
    }

    public record Transaction(long amount, String reason, long timestamp, String details) {
        public Transaction(long amount, String reason, long timestamp) {
            this(amount, reason, timestamp, "");
        }

        public String description() {
            return reason;
        }
    }
}

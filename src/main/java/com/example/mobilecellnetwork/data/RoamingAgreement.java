package com.example.mobilecellnetwork.data;

import net.minecraft.nbt.CompoundTag;

public record RoamingAgreement(String otherNetworkId, long ratePerMinute, long ratePerText, long ratePerMb,
                               long agreedAtEpochMillis) {
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("otherNetworkId", otherNetworkId);
        tag.putLong("ratePerMinute", ratePerMinute);
        tag.putLong("ratePerText", ratePerText);
        tag.putLong("ratePerMb", ratePerMb);
        tag.putLong("agreedAt", agreedAtEpochMillis);
        return tag;
    }

    public static RoamingAgreement fromTag(CompoundTag tag) {
        return new RoamingAgreement(tag.getString("otherNetworkId"), Math.max(0L, tag.getLong("ratePerMinute")),
                Math.max(0L, tag.getLong("ratePerText")), Math.max(0L, tag.getLong("ratePerMb")),
                tag.getLong("agreedAt"));
    }
}

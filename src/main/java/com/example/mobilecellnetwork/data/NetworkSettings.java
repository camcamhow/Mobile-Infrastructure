package com.example.mobilecellnetwork.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Per-network settings stored next to the NetworkRecord, keyed by the same network id. */
public final class NetworkSettings {
    public static final long NO_SERVER_BLOCK = Long.MIN_VALUE;

    private String passwordHash = "";
    private String passwordSalt = UUID.randomUUID().toString();
    private long serverBlockPos = NO_SERVER_BLOCK;
    private String serverBlockDimension = "";
    private boolean serverOnline;
    private boolean allowRoaming = true;
    private long defaultRatePerMinute;
    private long defaultRatePerText;
    private long defaultRatePerMb;
    private boolean autoSendRoamingRequests;
    private final Map<String, RoamingAgreement> agreements = new LinkedHashMap<>();
    private final Map<String, RoamingProposal> outgoingProposals = new LinkedHashMap<>();
    private final Map<String, RoamingProposal> incomingProposals = new LinkedHashMap<>();

    public NetworkSettings() {
        defaultRatePerMinute = com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .DEFAULT_ROAMING_RATE_PER_MINUTE.get();
        defaultRatePerText = com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .DEFAULT_ROAMING_RATE_PER_TEXT.get();
        defaultRatePerMb = com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .DEFAULT_ROAMING_RATE_PER_MB.get();
        autoSendRoamingRequests = com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .AUTO_SEND_ROAMING_REQUESTS_DEFAULT.get();
    }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash == null ? "" : passwordHash; }
    public void clearPassword() {
        passwordHash = "";
        passwordSalt = "";
    }
    public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String salt) { if (salt != null && !salt.isBlank()) passwordSalt = salt; }
    public long getServerBlockPos() { return serverBlockPos; }
    public String getServerBlockDimension() { return serverBlockDimension; }
    public boolean hasServerBlock() { return serverBlockPos != NO_SERVER_BLOCK; }
    public void setServerBlock(String dimension, long pos) {
        serverBlockDimension = dimension == null ? "" : dimension;
        serverBlockPos = pos;
    }
    public void clearServerBlock() {
        serverBlockPos = NO_SERVER_BLOCK;
        serverBlockDimension = "";
        serverOnline = false;
    }
    public boolean isServerOnline() { return serverOnline; }
    public void setServerOnline(boolean serverOnline) { this.serverOnline = serverOnline; }
    public boolean isAllowRoaming() { return allowRoaming; }
    public void setAllowRoaming(boolean allowRoaming) { this.allowRoaming = allowRoaming; }
    public long getDefaultRatePerMinute() { return defaultRatePerMinute; }
    public long getDefaultRatePerText() { return defaultRatePerText; }
    public long getDefaultRatePerMb() { return defaultRatePerMb; }
    public void setDefaultRates(long perMinute, long perText, long perMb) {
        defaultRatePerMinute = Math.max(0L, perMinute);
        defaultRatePerText = Math.max(0L, perText);
        defaultRatePerMb = Math.max(0L, perMb);
    }
    public boolean isAutoSendRoamingRequests() { return autoSendRoamingRequests; }
    public void setAutoSendRoamingRequests(boolean value) { autoSendRoamingRequests = value; }
    public Map<String, RoamingAgreement> getAgreements() { return agreements; }
    public Map<String, RoamingProposal> getOutgoingProposals() { return outgoingProposals; }
    public Map<String, RoamingProposal> getIncomingProposals() { return incomingProposals; }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("passwordHash", passwordHash);
        tag.putString("passwordSalt", passwordSalt);
        tag.putLong("serverBlockPos", serverBlockPos);
        tag.putString("serverBlockDimension", serverBlockDimension);
        tag.putBoolean("serverOnline", serverOnline);
        tag.putBoolean("allowRoaming", allowRoaming);
        tag.putLong("defaultRatePerMinute", defaultRatePerMinute);
        tag.putLong("defaultRatePerText", defaultRatePerText);
        tag.putLong("defaultRatePerMb", defaultRatePerMb);
        tag.putBoolean("autoSendRoamingRequests", autoSendRoamingRequests);
        tag.put("agreements", listOf(agreements.values().stream().map(RoamingAgreement::toTag).toList()));
        tag.put("outgoing", listOf(outgoingProposals.values().stream().map(RoamingProposal::toTag).toList()));
        tag.put("incoming", listOf(incomingProposals.values().stream().map(RoamingProposal::toTag).toList()));
        return tag;
    }

    public static NetworkSettings fromTag(CompoundTag tag) {
        NetworkSettings settings = new NetworkSettings();
        settings.passwordHash = tag.getString("passwordHash");
        if (tag.contains("passwordSalt", Tag.TAG_STRING)) {
            settings.passwordSalt = tag.getString("passwordSalt");
        }
        settings.serverBlockPos = tag.contains("serverBlockPos", Tag.TAG_LONG)
                ? tag.getLong("serverBlockPos") : NO_SERVER_BLOCK;
        settings.serverBlockDimension = tag.getString("serverBlockDimension");
        settings.serverOnline = tag.getBoolean("serverOnline");
        settings.allowRoaming = !tag.contains("allowRoaming") || tag.getBoolean("allowRoaming");
        settings.defaultRatePerMinute = tag.contains("defaultRatePerMinute")
                ? Math.max(0L, tag.getLong("defaultRatePerMinute")) : settings.defaultRatePerMinute;
        settings.defaultRatePerText = tag.contains("defaultRatePerText")
                ? Math.max(0L, tag.getLong("defaultRatePerText")) : settings.defaultRatePerText;
        settings.defaultRatePerMb = tag.contains("defaultRatePerMb")
                ? Math.max(0L, tag.getLong("defaultRatePerMb")) : settings.defaultRatePerMb;
        settings.autoSendRoamingRequests = tag.contains("autoSendRoamingRequests")
                ? tag.getBoolean("autoSendRoamingRequests") : settings.autoSendRoamingRequests;
        for (Tag value : tag.getList("agreements", Tag.TAG_COMPOUND)) {
            RoamingAgreement agreement = RoamingAgreement.fromTag((CompoundTag) value);
            settings.agreements.put(agreement.otherNetworkId(), agreement);
        }
        for (Tag value : tag.getList("outgoing", Tag.TAG_COMPOUND)) {
            RoamingProposal proposal = RoamingProposal.fromTag((CompoundTag) value);
            settings.outgoingProposals.put(proposal.toNetworkId(), proposal);
        }
        for (Tag value : tag.getList("incoming", Tag.TAG_COMPOUND)) {
            RoamingProposal proposal = RoamingProposal.fromTag((CompoundTag) value);
            settings.incomingProposals.put(proposal.fromNetworkId(), proposal);
        }
        return settings;
    }

    private static ListTag listOf(java.util.List<CompoundTag> tags) {
        ListTag list = new ListTag();
        list.addAll(tags);
        return list;
    }
}

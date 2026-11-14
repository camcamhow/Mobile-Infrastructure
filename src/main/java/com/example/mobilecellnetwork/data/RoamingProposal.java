package com.example.mobilecellnetwork.data;

import net.minecraft.nbt.CompoundTag;

public record RoamingProposal(String proposalId, String fromNetworkId, String toNetworkId, long ratePerMinute,
                              long ratePerText, long ratePerMb, ProposalState state, String lastMessage,
                              long lastUpdated) {
    public RoamingProposal withState(ProposalState newState, String message) {
        return new RoamingProposal(proposalId, fromNetworkId, toNetworkId, ratePerMinute, ratePerText, ratePerMb,
                newState, message == null ? "" : message, System.currentTimeMillis());
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("proposalId", proposalId);
        tag.putString("from", fromNetworkId);
        tag.putString("to", toNetworkId);
        tag.putLong("ratePerMinute", ratePerMinute);
        tag.putLong("ratePerText", ratePerText);
        tag.putLong("ratePerMb", ratePerMb);
        tag.putString("state", state.name());
        tag.putString("lastMessage", lastMessage == null ? "" : lastMessage);
        tag.putLong("lastUpdated", lastUpdated);
        return tag;
    }

    public static RoamingProposal fromTag(CompoundTag tag) {
        ProposalState state;
        try {
            state = ProposalState.valueOf(tag.getString("state"));
        } catch (IllegalArgumentException exception) {
            state = ProposalState.PENDING;
        }
        return new RoamingProposal(tag.getString("proposalId"), tag.getString("from"), tag.getString("to"),
                Math.max(0L, tag.getLong("ratePerMinute")), Math.max(0L, tag.getLong("ratePerText")),
                Math.max(0L, tag.getLong("ratePerMb")), state, tag.getString("lastMessage"),
                tag.getLong("lastUpdated"));
    }
}

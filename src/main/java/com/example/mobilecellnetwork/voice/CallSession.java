package com.example.mobilecellnetwork.voice;

import java.util.UUID;

public final class CallSession {
    private final UUID id = UUID.randomUUID();
    private final UUID callerUUID;
    private final UUID calleeUUID;
    private long startTime;
    private State state = State.RINGING;
    private boolean callerMuted;
    private boolean calleeMuted;
    private boolean callerVideo;
    private boolean calleeVideo;
    private long callerVideoFrameAt;
    private long calleeVideoFrameAt;
    private int minutesUsedThisCall;
    private int ticksElapsed;

    public CallSession(UUID callerUUID, UUID calleeUUID, long startTime) {
        this.callerUUID = callerUUID;
        this.calleeUUID = calleeUUID;
        this.startTime = startTime;
    }

    public UUID id() { return id; }
    public UUID callerUUID() { return callerUUID; }
    public UUID calleeUUID() { return calleeUUID; }
    public long startTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public State state() { return state; }
    public void setState(State state) {
        if (state == State.ACTIVE && this.state == State.RINGING) {
            ticksElapsed = 0;
        }
        this.state = state;
    }
    public boolean callerMuted() { return callerMuted; }
    public boolean calleeMuted() { return calleeMuted; }
    public int minutesUsedThisCall() { return minutesUsedThisCall; }
    public void incrementMinutesUsed() { minutesUsedThisCall++; }
    public int ticksElapsed() { return ticksElapsed; }
    public void incrementTicksElapsed() { ticksElapsed++; }

    public UUID partnerOf(UUID player) {
        if (callerUUID.equals(player)) {
            return calleeUUID;
        }
        if (calleeUUID.equals(player)) {
            return callerUUID;
        }
        throw new IllegalArgumentException("Player is not part of this call.");
    }

    public void setMuted(UUID player, boolean muted) {
        if (callerUUID.equals(player)) {
            callerMuted = muted;
        } else if (calleeUUID.equals(player)) {
            calleeMuted = muted;
        } else {
            throw new IllegalArgumentException("Player is not part of this call.");
        }
    }

    public boolean isMuted(UUID player) {
        if (callerUUID.equals(player)) {
            return callerMuted;
        }
        if (calleeUUID.equals(player)) {
            return calleeMuted;
        }
        throw new IllegalArgumentException("Player is not part of this call.");
    }

    public boolean videoEnabled(UUID player) {
        if (callerUUID.equals(player)) return callerVideo;
        if (calleeUUID.equals(player)) return calleeVideo;
        throw new IllegalArgumentException("Player is not part of this call.");
    }

    public void setVideoEnabled(UUID player, boolean enabled) {
        if (callerUUID.equals(player)) callerVideo = enabled;
        else if (calleeUUID.equals(player)) calleeVideo = enabled;
        else throw new IllegalArgumentException("Player is not part of this call.");
    }

    public boolean acceptVideoFrame(UUID player, long now) {
        if (callerUUID.equals(player)) {
            if (now - callerVideoFrameAt < 200L) return false;
            callerVideoFrameAt = now;
        } else if (calleeUUID.equals(player)) {
            if (now - calleeVideoFrameAt < 200L) return false;
            calleeVideoFrameAt = now;
        } else {
            throw new IllegalArgumentException("Player is not part of this call.");
        }
        return true;
    }

    public enum State {
        RINGING,
        ACTIVE,
        ENDED
    }
}

package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.voice.CallSession;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientCallCache {
    private static volatile String sessionId = "";
    private static volatile CallSession.State state;
    private static volatile String partnerName = "";
    private static volatile String reason = "";
    private static volatile long startedAtMillis;
    private static volatile boolean muted;
    private static volatile boolean incoming;
    private static volatile boolean videoCall;
    private static volatile long updatedAtMillis;

    private ClientCallCache() {
    }

    public static void update(CallStatePacket packet) {
        sessionId = packet.sessionId();
        state = CallSession.State.valueOf(packet.state());
        partnerName = packet.partnerName();
        reason = packet.reason();
        startedAtMillis = packet.startedAtMillis();
        muted = packet.muted();
        incoming = packet.incoming();
        videoCall = packet.videoCall();
        updatedAtMillis = System.currentTimeMillis();
    }

    public static String sessionId() { return sessionId; }
    public static CallSession.State state() { return state; }
    public static String partnerName() { return partnerName; }
    public static String reason() { return reason; }
    public static long startedAtMillis() { return startedAtMillis; }
    public static boolean muted() { return muted; }
    public static boolean incoming() { return incoming; }
    public static boolean videoCall() { return videoCall; }
    public static long updatedAtMillis() { return updatedAtMillis; }

    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        clearSensitiveData();
    }

    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearSensitiveData();
    }

    public static void clearSensitiveData() {
        sessionId = "";
        state = null;
        partnerName = "";
        reason = "";
        startedAtMillis = 0L;
        muted = false;
        incoming = false;
        videoCall = false;
        updatedAtMillis = 0L;
    }
}

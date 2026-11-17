package com.example.mobilecellnetwork.voice;

import com.mojang.logging.LogUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

public final class VoiceRouter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SERVER_API = "de.maxhenkel.voicechat.api.VoicechatServerApi";
    private static final String IMPLEMENTATION = "de.maxhenkel.voicechat.plugins.impl.VoicechatServerApiImpl";
    private static final Map<UUID, Object> GROUPS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ORIGINAL_DISABLED_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerGroupState> PLAYER_GROUP_STATES = new ConcurrentHashMap<>();
    private static volatile Object api;
    private static volatile boolean apiClassMissing;
    private static volatile long nextApiLookupAt;
    private static volatile boolean failureLogged;

    private VoiceRouter() {
    }

    public static boolean isAvailable() {
        return resolveApi() != null;
    }

    public static boolean start(CallSession session, ServerPlayer caller, ServerPlayer callee) {
        Object voiceApi = resolveApi();
        if (voiceApi == null) {
            return false;
        }
        try {
            Object callerConnection = invoke(voiceApi, "getConnectionOf", caller.getUUID());
            Object calleeConnection = invoke(voiceApi, "getConnectionOf", callee.getUUID());
            if (callerConnection == null || calleeConnection == null
                    || !(boolean) invoke(callerConnection, "isInstalled")
                    || !(boolean) invoke(calleeConnection, "isInstalled")) {
                return false;
            }
            rememberPlayerGroup(voiceApi, callerConnection, caller.getUUID());
            rememberPlayerGroup(voiceApi, calleeConnection, callee.getUUID());
            Object builder = voiceApi.getClass().getMethod("groupBuilder").invoke(voiceApi);
            invoke(builder, "setName", "Mobile Call " + session.id());
            invoke(builder, "setPersistent", false);
            Class<?> groupType = Class.forName("de.maxhenkel.voicechat.api.Group$Type");
            Object isolated = groupType.getField("ISOLATED").get(null);
            invoke(builder, "setType", isolated);
            Object group = invoke(builder, "build");
            invoke(callerConnection, "setGroup", group);
            invoke(calleeConnection, "setGroup", group);
            GROUPS.put(session.id(), group);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logFailure("Unable to create isolated Simple Voice Chat call group.", exception);
            return false;
        }
    }

    public static void end(CallSession session) {
        Object voiceApi = resolveApi();
        Object group = GROUPS.remove(session.id());
        if (voiceApi == null || group == null) {
            return;
        }
        try {
            restorePlayerGroup(voiceApi, session.callerUUID());
            restorePlayerGroup(voiceApi, session.calleeUUID());
            invoke(voiceApi, "removeGroup", invoke(group, "getId"));
        } catch (ReflectiveOperationException | LinkageError exception) {
            logFailure("Unable to remove isolated Simple Voice Chat call group.", exception);
        }
    }

    public static void setMuted(UUID playerId, boolean muted) {
        Object voiceApi = resolveApi();
        if (voiceApi == null) {
            return;
        }
        try {
            Object connection = invoke(voiceApi, "getConnectionOf", playerId);
            if (connection != null) {
                invoke(connection, "setDisabled", muted);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logFailure("Unable to change Simple Voice Chat microphone state.", exception);
        }
    }

    public static void enforceOutsideCallPolicy(Iterable<ServerPlayer> players, java.util.Set<UUID> inCalls) {
        if (!com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .BLOCK_PROXIMITY_VOICE_OUTSIDE_CALLS.get()) {
            restoreOutsideCallPolicy();
            return;
        }
        Object voiceApi = resolveApi();
        if (voiceApi == null) {
            return;
        }
        for (ServerPlayer player : players) {
            try {
                Object connection = invoke(voiceApi, "getConnectionOf", player.getUUID());
                if (connection == null) {
                    continue;
                }
                if (inCalls.contains(player.getUUID())) {
                    restoreDisabledState(connection, player.getUUID());
                } else {
                    rememberPlayerGroup(voiceApi, connection, player.getUUID());
                    ORIGINAL_DISABLED_STATES.computeIfAbsent(player.getUUID(), uuid -> {
                        try {
                            return (boolean) invoke(connection, "isDisabled");
                        } catch (ReflectiveOperationException exception) {
                            logFailure("Unable to inspect Simple Voice Chat connection state.", exception);
                            return false;
                        }
                    });
                    invoke(connection, "setDisabled", true);
                    PlayerGroupState groupState = PLAYER_GROUP_STATES.get(player.getUUID());
                    if (groupState != null && groupState.isolatedGroup() != null
                            && invoke(connection, "getGroup") != groupState.isolatedGroup()) {
                        invoke(connection, "setGroup", groupState.isolatedGroup());
                    }
                }
            } catch (ReflectiveOperationException | LinkageError exception) {
                logFailure("Unable to enforce Simple Voice Chat proximity voice policy.", exception);
                return;
            }
        }
    }

    public static void restoreOutsideCallPolicy() {
        Object voiceApi = resolveApi();
        if (voiceApi == null) {
            ORIGINAL_DISABLED_STATES.clear();
            return;
        }
        for (Map.Entry<UUID, Boolean> entry : ORIGINAL_DISABLED_STATES.entrySet()) {
            try {
                Object connection = invoke(voiceApi, "getConnectionOf", entry.getKey());
                if (connection != null) {
                    invoke(connection, "setDisabled", entry.getValue());
                }
            } catch (ReflectiveOperationException | LinkageError exception) {
                logFailure("Unable to restore Simple Voice Chat connection state.", exception);
            }
        }
        ORIGINAL_DISABLED_STATES.clear();
        for (Map.Entry<UUID, PlayerGroupState> entry : PLAYER_GROUP_STATES.entrySet()) {
            try {
                Object connection = invoke(voiceApi, "getConnectionOf", entry.getKey());
                if (connection != null) {
                    restoreOriginalGroup(connection, entry.getKey());
                }
            } catch (ReflectiveOperationException | LinkageError exception) {
                logFailure("Unable to restore original Simple Voice Chat group.", exception);
            }
            removeGroup(voiceApi, entry.getValue().isolatedGroup());
        }
        PLAYER_GROUP_STATES.clear();
    }

    public static void onPlayerLogout(UUID playerId) {
        Object voiceApi = resolveApi();
        if (voiceApi == null) {
            ORIGINAL_DISABLED_STATES.remove(playerId);
            PLAYER_GROUP_STATES.remove(playerId);
            return;
        }
        try {
            Object connection = invoke(voiceApi, "getConnectionOf", playerId);
            if (connection != null) {
                Boolean disabled = ORIGINAL_DISABLED_STATES.remove(playerId);
                if (disabled != null) {
                    invoke(connection, "setDisabled", disabled);
                }
                restoreOriginalGroup(connection, playerId);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            logFailure("Unable to restore voice settings when a player disconnects.", exception);
        }
        PlayerGroupState state = PLAYER_GROUP_STATES.remove(playerId);
        if (state != null) {
            removeGroup(voiceApi, state.isolatedGroup());
        }
    }

    private static void rememberPlayerGroup(Object voiceApi, Object connection, UUID playerId)
            throws ReflectiveOperationException {
        if (PLAYER_GROUP_STATES.containsKey(playerId)) {
            return;
        }
        Object originalGroup = invoke(connection, "getGroup");
        Object isolatedGroup = null;
        if (com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .BLOCK_PROXIMITY_VOICE_OUTSIDE_CALLS.get()) {
            isolatedGroup = createIsolatedGroup(voiceApi, "Mobile Private " + playerId);
        }
        PLAYER_GROUP_STATES.put(playerId, new PlayerGroupState(originalGroup, isolatedGroup));
    }

    private static Object createIsolatedGroup(Object voiceApi, String name) throws ReflectiveOperationException {
        Object builder = voiceApi.getClass().getMethod("groupBuilder").invoke(voiceApi);
        invoke(builder, "setName", name);
        invoke(builder, "setPersistent", false);
        Class<?> groupType = Class.forName("de.maxhenkel.voicechat.api.Group$Type");
        invoke(builder, "setType", groupType.getField("ISOLATED").get(null));
        return invoke(builder, "build");
    }

    private static void restorePlayerGroup(Object voiceApi, UUID playerId) throws ReflectiveOperationException {
        Object connection = invoke(voiceApi, "getConnectionOf", playerId);
        if (connection == null) {
            return;
        }
        PlayerGroupState state = PLAYER_GROUP_STATES.get(playerId);
        if (state == null) {
            invoke(connection, "setGroup", new Object[]{null});
            return;
        }
        Object group = com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .BLOCK_PROXIMITY_VOICE_OUTSIDE_CALLS.get()
                ? state.isolatedGroup() : state.originalGroup();
        invoke(connection, "setGroup", new Object[]{group});
    }

    private static void restoreOriginalGroup(Object connection, UUID playerId) throws ReflectiveOperationException {
        PlayerGroupState state = PLAYER_GROUP_STATES.get(playerId);
        if (state != null) {
            invoke(connection, "setGroup", new Object[]{state.originalGroup()});
        }
    }

    private static void removeGroup(Object voiceApi, Object group) {
        if (group == null) {
            return;
        }
        try {
            invoke(voiceApi, "removeGroup", invoke(group, "getId"));
        } catch (ReflectiveOperationException | LinkageError exception) {
            logFailure("Unable to remove private Simple Voice Chat group.", exception);
        }
    }

    private static void restoreDisabledState(Object connection, UUID playerId) throws ReflectiveOperationException {
        Boolean state = ORIGINAL_DISABLED_STATES.remove(playerId);
        if (state != null) {
            invoke(connection, "setDisabled", state);
        }
    }

    private static Object invoke(Object receiver, String methodName, Object... arguments)
            throws ReflectiveOperationException {
        for (Method method : receiver.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == arguments.length) {
                try {
                    return method.invoke(receiver, arguments);
                } catch (IllegalArgumentException ignored) {
                    continue;
                } catch (InvocationTargetException exception) {
                    Throwable cause = exception.getCause();
                    if (cause instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    }
                    if (cause instanceof Error error) {
                        throw error;
                    }
                    throw new ReflectiveOperationException(cause);
                }
            }
        }
        throw new NoSuchMethodException(receiver.getClass().getName() + "." + methodName);
    }

    private static Object resolveApi() {
        if (api != null || apiClassMissing) {
            return api;
        }
        long now = System.currentTimeMillis();
        if (now < nextApiLookupAt) {
            return null;
        }
        synchronized (VoiceRouter.class) {
            if (api != null || apiClassMissing) {
                return api;
            }
            if (System.currentTimeMillis() < nextApiLookupAt) {
                return null;
            }
            nextApiLookupAt = System.currentTimeMillis() + 1_000L;
            try {
                Class.forName(SERVER_API);
                Class<?> implementation = Class.forName(IMPLEMENTATION);
                api = implementation.getMethod("instance").invoke(null);
            } catch (ClassNotFoundException ignored) {
                apiClassMissing = true;
            } catch (ReflectiveOperationException | LinkageError exception) {
                logFailure("Simple Voice Chat API was found but could not be initialized.", exception);
            }
            return api;
        }
    }

    private static void logFailure(String message, Throwable exception) {
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.warn(message, exception);
        }
    }

    private record PlayerGroupState(Object originalGroup, Object isolatedGroup) {
    }
}

package com.example.mobilecellnetwork.voice;

import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import com.example.mobilecellnetwork.capabilities.PlanUsage;
import com.example.mobilecellnetwork.billing.BillingManager;
import com.example.mobilecellnetwork.data.CallDirection;
import com.example.mobilecellnetwork.data.CallLogEntry;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.network.CallControlPacket;
import com.example.mobilecellnetwork.network.CallLogEntryPacket;
import com.example.mobilecellnetwork.network.CallStatePacket;
import com.example.mobilecellnetwork.network.CoverageEngine;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.plans.PlanRegistry;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

public final class CallManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, CallSession> ACTIVE_CALLS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PLAYER_TO_PARTNER = new ConcurrentHashMap<>();

    private CallManager() {
    }

    public static CallResult startCall(ServerPlayer caller, ServerPlayer callee) {
        if (caller.getUUID().equals(callee.getUUID())) {
            return reject(caller, CallResult.DECLINED, "You cannot call yourself.", "CALL_FAILED");
        }
        if (phoneData(caller).isDead()) {
            return reject(caller, CallResult.PHONE_DEAD, "Your phone is dead.", "CALL_FAILED");
        }
        if (phoneData(caller).isLowPowerMode()) {
            return reject(caller, CallResult.LOW_POWER, "Calls are disabled in Low Power Mode.", "CALL_FAILED");
        }
        if (ACTIVE_CALLS.containsKey(caller.getUUID())) {
            return reject(caller, CallResult.BUSY, "You are already on a call.", "CALL_FAILED");
        }
        if (!hasActivePlan(caller)) {
            return reject(caller, CallResult.NO_SERVICE, "Choose an active plan to call.", "CALL_FAILED");
        }
        if (signal(caller) == SignalTier.NO_SERVICE) {
            return reject(caller, CallResult.NO_SERVICE, "You have no service.", "CALL_FAILED");
        }
        if (!hasMinutes(caller)) {
            return reject(caller, CallResult.PLAN_LIMIT_REACHED, "Plan limit reached", "PLAN_LIMIT_REACHED");
        }
        if (!hasActivePlan(callee) || phoneData(callee).isDead() || phoneData(callee).isLowPowerMode()
                || ACTIVE_CALLS.containsKey(callee.getUUID()) || signal(callee) == SignalTier.NO_SERVICE) {
            return reject(caller, CallResult.BUSY, "The player is unavailable.", "UNAVAILABLE");
        }
        if (!hasMinutes(callee)) {
            return reject(caller, CallResult.PLAN_LIMIT_REACHED, "Plan limit reached", "PLAN_LIMIT_REACHED");
        }
        if (remainingMinutes(caller) <= 5 || remainingMinutes(callee) <= 5) {
            warnLowMinutes(caller, callee);
        }

        CallSession session = new CallSession(caller.getUUID(), callee.getUUID(), System.currentTimeMillis());
        ACTIVE_CALLS.put(caller.getUUID(), session);
        ACTIVE_CALLS.put(callee.getUUID(), session);
        PLAYER_TO_PARTNER.put(caller.getUUID(), callee.getUUID());
        PLAYER_TO_PARTNER.put(callee.getUUID(), caller.getUUID());
        sendState(session, caller, callee, CallSession.State.RINGING, "");
        long now = System.currentTimeMillis();
        phoneData(callee).getNotifications().add(new PhoneData.Notification(
                "CALL", "Incoming call", "Call from " + caller.getGameProfile().getName(), now, false));
        PacketDistributor.sendToPlayer(callee, new com.example.mobilecellnetwork.network.PhoneNotificationPacket(
                "CALL", "Incoming call", "Call from " + caller.getGameProfile().getName(), false));
        if (phoneData(callee).isRingtoneEnabled() && phoneData(callee).areNotificationSoundsEnabled()
                && phoneData(callee).getAppNotifications().getOrDefault("call", true)) {
            playSound(callee, phoneData(callee).getRingtoneSound());
        }
        caller.displayClientMessage(Component.literal("Calling " + callee.getGameProfile().getName() + "..."), true);
        callee.displayClientMessage(Component.literal(
                "Incoming call from " + caller.getGameProfile().getName()), true);
        return CallResult.ACCEPTED;
    }

    public static CallResult startCall(ServerPlayer caller, String phoneNumber) {
        UUID calleeId = NetworkSavedData.get(caller.server.overworld()).getUUIDForNumber(phoneNumber);
        if (calleeId == null) {
            return reject(caller, CallResult.DECLINED, "This number is not in service.", "NOT_IN_SERVICE");
        }
        ServerPlayer callee = caller.server.getPlayerList().getPlayer(calleeId);
        if (callee == null) {
            return reject(caller, CallResult.DECLINED, "The player is unavailable.", "UNAVAILABLE");
        }
        return startCall(caller, callee);
    }

    public static void acceptCall(UUID playerId) {
        CallSession session = ACTIVE_CALLS.get(playerId);
        if (session == null || session.state() != CallSession.State.RINGING
                || !session.calleeUUID().equals(playerId)) {
            return;
        }
        MinecraftServer server = serverFor(playerId);
        if (server == null) {
            endCall(session, "TIMEOUT", true);
            return;
        }
        ServerPlayer caller = server.getPlayerList().getPlayer(session.callerUUID());
        ServerPlayer callee = server.getPlayerList().getPlayer(session.calleeUUID());
        if (caller == null || callee == null) {
            endCall(session, "MISSED_CALL", true);
            return;
        }
        session.setState(CallSession.State.ACTIVE);
        session.setStartTime(System.currentTimeMillis());
        boolean voiceRouted = VoiceRouter.start(session, caller, callee);
        sendState(session, caller, callee, CallSession.State.ACTIVE, "");
        if (!voiceRouted) {
            caller.displayClientMessage(Component.literal(
                    "Text-call connected to " + callee.getGameProfile().getName() + "."), true);
            callee.displayClientMessage(Component.literal(
                    "Text-call connected to " + caller.getGameProfile().getName() + "."), true);
        }
    }

    public static void declineCall(UUID playerId) {
        CallSession session = ACTIVE_CALLS.get(playerId);
        if (session != null && session.state() == CallSession.State.RINGING) {
            endCall(session, "DECLINED", false);
        }
    }

    public static void hangUp(UUID playerId) {
        CallSession session = ACTIVE_CALLS.get(playerId);
        if (session != null) {
            endCall(session, "HUNG_UP", false);
        }
    }

    public static void muteMic(UUID playerId, boolean muted) {
        CallSession session = ACTIVE_CALLS.get(playerId);
        if (session == null || session.state() != CallSession.State.ACTIVE) {
            return;
        }
        session.setMuted(playerId, muted);
        VoiceRouter.setMuted(playerId, muted);
        MinecraftServer server = serverFor(playerId);
        ServerPlayer caller = server == null ? null : server.getPlayerList().getPlayer(session.callerUUID());
        ServerPlayer callee = server == null ? null : server.getPlayerList().getPlayer(session.calleeUUID());
        if (caller != null && callee != null) {
            sendState(session, caller, callee, CallSession.State.ACTIVE, "");
        }
    }

    public static void handleControl(ServerPlayer player, CallControlPacket packet) {
        switch (packet.action()) {
            case ACCEPT -> acceptCall(player.getUUID());
            case DECLINE -> declineCall(player.getUUID());
            case HANG_UP -> hangUp(player.getUUID());
            case MUTE -> muteMic(player.getUUID(), packet.muted());
            case TOGGLE_RINGTONE -> { }
        }
    }

    public static void endForLowPower(ServerPlayer player) {
        CallSession session = ACTIVE_CALLS.get(player.getUUID());
        if (session != null) {
            endCall(session, "LOW_POWER_ENABLED:" + player.getGameProfile().getName(), false);
        }
    }

    public static void clear(ServerPlayer player) {
        CallSession session = ACTIVE_CALLS.get(player.getUUID());
        if (session != null) {
            endCall(session, "PLAYER_DISCONNECTED", session.state() == CallSession.State.RINGING);
        }
        VoiceRouter.onPlayerLogout(player.getUUID());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        Set<CallSession> sessions = new HashSet<>(ACTIVE_CALLS.values());
        for (CallSession session : sessions) {
            if (session.state() == CallSession.State.RINGING) {
                session.incrementTicksElapsed();
                if (session.ticksElapsed() >= MobileCellNetworkConfig.CALL_RING_TIMEOUT_SECONDS.get() * 20) {
                    endCall(session, "TIMEOUT", true);
                }
            } else if (session.state() == CallSession.State.ACTIVE) {
                session.incrementTicksElapsed();
                if (session.ticksElapsed() % 1200 == 0) {
                    consumeMinute(session, server);
                }
            }
        }
        Set<UUID> inActiveCalls = new HashSet<>();
        for (CallSession session : sessions) {
            if (session.state() == CallSession.State.ACTIVE) {
                inActiveCalls.add(session.callerUUID());
                inActiveCalls.add(session.calleeUUID());
            }
        }
        VoiceRouter.enforceOutsideCallPolicy(server.getPlayerList().getPlayers(), Set.copyOf(inActiveCalls));
    }

    public static Map<UUID, CallSession> activeCalls() {
        return Map.copyOf(ACTIVE_CALLS);
    }

    public static Map<UUID, UUID> playerToPartner() {
        return Map.copyOf(PLAYER_TO_PARTNER);
    }

    public static CallSession getCall(UUID playerId) {
        return ACTIVE_CALLS.get(playerId);
    }

    public static void handleVideo(ServerPlayer sender, com.example.mobilecellnetwork.network.VideoCallPacket packet) {
        CallSession session = getCall(sender.getUUID());
        if (session == null || session.state() != CallSession.State.ACTIVE || !session.id().equals(packet.session())) {
            return;
        }
        ServerPlayer recipient = sender.server.getPlayerList().getPlayer(session.partnerOf(sender.getUUID()));
        if (recipient == null) return;
        byte[] image = packet.image();
        if (image.length == 0) {
            session.setVideoEnabled(sender.getUUID(), packet.enabled());
            PacketDistributor.sendToPlayer(sender,
                    new com.example.mobilecellnetwork.network.VideoCallPacket(session.id(), packet.enabled(), true, new byte[0]));
            if (!packet.enabled()) {
                PacketDistributor.sendToPlayer(recipient,
                        new com.example.mobilecellnetwork.network.VideoCallPacket(session.id(), false, false, new byte[0]));
            }
            return;
        }
        if (!packet.enabled() || !session.videoEnabled(sender.getUUID())
                || !session.acceptVideoFrame(sender.getUUID(), System.currentTimeMillis())) return;
        try {
            com.example.mobilecellnetwork.network.VideoImage.validate(image);
        } catch (java.io.IOException exception) {
            LOGGER.warn("Rejected invalid video frame from {}", sender.getGameProfile().getName(), exception);
            return;
        }
        double megabytes = image.length / 1_000_000.0D;
        if (!com.example.mobilecellnetwork.plans.DataUsageService.spend(sender, megabytes)
                || !com.example.mobilecellnetwork.plans.DataUsageService.spend(recipient, megabytes)) {
            session.setVideoEnabled(sender.getUUID(), false);
            PacketDistributor.sendToPlayer(sender,
                    new com.example.mobilecellnetwork.network.VideoCallPacket(session.id(), false, true, new byte[0]));
            PacketDistributor.sendToPlayer(recipient,
                    new com.example.mobilecellnetwork.network.VideoCallPacket(session.id(), false, false, new byte[0]));
            sender.displayClientMessage(Component.literal("Video stopped: data unavailable."), true);
            return;
        }
        PacketDistributor.sendToPlayer(recipient,
                new com.example.mobilecellnetwork.network.VideoCallPacket(session.id(), true, false, image));
    }

    private static void consumeMinute(CallSession session, MinecraftServer server) {
        ServerPlayer caller = server.getPlayerList().getPlayer(session.callerUUID());
        ServerPlayer callee = server.getPlayerList().getPlayer(session.calleeUUID());
        if (caller == null || callee == null) {
            endCall(session, "PLAYER_DISCONNECTED", false);
            return;
        }
        if (!hasMinutes(caller) || !hasMinutes(callee)) {
            endCall(session, "PLAN_LIMIT_REACHED", false);
            return;
        }
        String roamingFailure = chargeRoamingMinute(caller, callee);
        if (roamingFailure != null) {
            endCall(session, roamingFailure, false);
            return;
        }
        PlanUsage callerUsage = caller.getData(ModCapabilities.PLAN_USAGE);
        PlanUsage calleeUsage = callee.getData(ModCapabilities.PLAN_USAGE);
        int callerRemainingBefore = remainingMinutes(caller);
        int calleeRemainingBefore = remainingMinutes(callee);
        callerUsage.setMinutesUsed(callerUsage.getMinutesUsed() + 1);
        calleeUsage.setMinutesUsed(calleeUsage.getMinutesUsed() + 1);
        session.incrementMinutesUsed();
        com.example.mobilecellnetwork.network.NetworkHandler.syncPlanData(caller);
        com.example.mobilecellnetwork.network.NetworkHandler.syncPlanData(callee);
        if ((callerRemainingBefore > 5 && remainingMinutes(caller) <= 5)
                || (calleeRemainingBefore > 5 && remainingMinutes(callee) <= 5)) {
            warnLowMinutes(caller, callee);
        }
        if (!hasMinutes(caller) || !hasMinutes(callee)) {
            endCall(session, "PLAN_LIMIT_REACHED", false);
        } else {
            sendState(session, caller, callee, CallSession.State.ACTIVE, "");
        }
    }

    private static void recordCallLog(CallSession session, CallSession.State oldState,
                                      ServerPlayer caller, ServerPlayer callee) {
        if (caller == null || callee == null) {
            return;
        }
        int duration = oldState == CallSession.State.ACTIVE
                ? (int) Math.max(0L, (System.currentTimeMillis() - session.startTime()) / 1000L) : 0;
        logEntry(caller, CallDirection.OUTGOING, callee, duration);
        logEntry(callee, oldState == CallSession.State.ACTIVE ? CallDirection.INCOMING : CallDirection.MISSED,
                caller, duration);
    }

    private static void logEntry(ServerPlayer owner, CallDirection direction, ServerPlayer other, int duration) {
        PhoneData data = phoneData(owner);
        String number = phoneData(other).getPhoneNumber();
        String contactName = data.getContacts().entrySet().stream()
                .filter(contact -> contact.getValue().equals(number))
                .map(Map.Entry::getKey).findFirst().orElse(null);
        CallLogEntry entry = data.addCallLogEntry(direction, number, contactName, duration);
        PacketDistributor.sendToPlayer(owner, new CallLogEntryPacket(entry));
    }

    private static void endCall(CallSession session, String reason, boolean missed) {
        if (session.state() == CallSession.State.ENDED) {
            return;
        }
        CallSession.State oldState = session.state();
        session.setState(CallSession.State.ENDED);
        ACTIVE_CALLS.remove(session.callerUUID(), session);
        ACTIVE_CALLS.remove(session.calleeUUID(), session);
        PLAYER_TO_PARTNER.remove(session.callerUUID());
        PLAYER_TO_PARTNER.remove(session.calleeUUID());
        MinecraftServer server = serverFor(session.callerUUID());
        ServerPlayer caller = server == null ? null : server.getPlayerList().getPlayer(session.callerUUID());
        ServerPlayer callee = server == null ? null : server.getPlayerList().getPlayer(session.calleeUUID());
        if (oldState == CallSession.State.ACTIVE) {
            VoiceRouter.end(session);
        }
        recordCallLog(session, oldState, caller, callee);
        if (missed && oldState == CallSession.State.RINGING && callee != null) {
            String callerName = caller == null ? "Unknown" : caller.getGameProfile().getName();
            String callerNumber = caller == null
                    ? NetworkSavedData.get(server.overworld()).getNumberFor(session.callerUUID())
                    : phoneData(caller).getPhoneNumber();
            phoneData(callee).getMissedCalls().add(new PhoneData.MissedCall(callerNumber, System.currentTimeMillis()));
            phoneData(callee).getNotifications().add(new PhoneData.Notification(
                    "MISSED_CALL", "Missed call", "Missed call from " + callerName,
                    System.currentTimeMillis(), false));
        }
        if (oldState == CallSession.State.RINGING && callee != null
                && (reason.equals("DECLINED") || reason.equals("TIMEOUT") || missed)) {
            String callerNumber = caller == null
                    ? NetworkSavedData.get(server.overworld()).getNumberFor(session.callerUUID())
                    : phoneData(caller).getPhoneNumber();
            phoneData(callee).getVoicemail().add(new PhoneData.Voicemail(
                    callerNumber == null ? "" : callerNumber, System.currentTimeMillis(), ""));
            phoneData(callee).getNotifications().add(new PhoneData.Notification(
                    "VOICEMAIL", "Voicemail", "Missed call from "
                    + (caller == null ? "Unknown" : caller.getGameProfile().getName()),
                    System.currentTimeMillis(), false));
            PacketDistributor.sendToPlayer(callee, new com.example.mobilecellnetwork.network.PhoneNotificationPacket(
                    "VOICEMAIL", "Voicemail", "Missed call from "
                    + (caller == null ? "Unknown" : caller.getGameProfile().getName()), false));
            com.example.mobilecellnetwork.network.NetworkHandler.syncPhoneData(callee);
        } else if (oldState == CallSession.State.RINGING && callee == null
                && (reason.equals("DECLINED") || reason.equals("TIMEOUT") || missed) && server != null) {
            var savedData = NetworkSavedData.get(server.overworld());
            String callerNumber = caller == null ? savedData.getNumberFor(session.callerUUID())
                    : phoneData(caller).getPhoneNumber();
            savedData.addPendingCall(session.calleeUUID(), new NetworkSavedData.PendingCall(
                    callerNumber == null ? "" : callerNumber, System.currentTimeMillis(), missed));
        }
        if (caller != null && callee != null) {
            sendState(session, caller, callee, CallSession.State.ENDED, reason);
            String message = endMessage(reason, caller.getGameProfile().getName(), callee.getGameProfile().getName());
            caller.displayClientMessage(Component.literal(message), true);
            callee.displayClientMessage(Component.literal(message), true);
            playEndSound(caller);
            playEndSound(callee);
        } else {
            sendStateToOnline(session, server, reason);
            String message = endMessage(reason, caller == null ? "Player" : caller.getGameProfile().getName(),
                    callee == null ? "Player" : callee.getGameProfile().getName());
            if (caller != null) {
                caller.displayClientMessage(Component.literal(message), true);
                playEndSound(caller);
            }
            if (callee != null) {
                callee.displayClientMessage(Component.literal(message), true);
                playEndSound(callee);
            }
        }
    }

    private static String endMessage(String reason, String caller, String callee) {
        if (reason.startsWith("LOW_POWER_ENABLED:")) {
            return "Call ended - " + reason.substring("LOW_POWER_ENABLED:".length())
                    + " enabled Low Power Mode";
        }
        return switch (reason) {
            case "PLAN_LIMIT_REACHED" -> "Call ended - Plan limit reached";
            case "PAYMENT_FAILED" -> "Call ended - insufficient funds for usage";
            case "OUT_OF_MINUTES" -> "Call ended - Out of minutes";
            case "ROAMING_NOT_ALLOWED" -> "Call ended - roaming is not allowed by the plan";
            case "ROAMING_FEE_FAILED" -> "Call ended - roaming charge failed";
            case "TIMEOUT", "MISSED_CALL" -> "Missed call";
            case "DECLINED" -> "Call declined";
            case "PLAYER_DISCONNECTED" -> "Call ended - player disconnected";
            default -> "Call ended";
        };
    }

    private static void sendState(CallSession session, ServerPlayer caller, ServerPlayer callee,
                                  CallSession.State state, String reason) {
        sendStateToPlayer(session, caller, callee.getGameProfile().getName(), state, reason,
                session.callerMuted(), false);
        sendStateToPlayer(session, callee, caller.getGameProfile().getName(), state, reason,
                session.calleeMuted(), true);
    }

    private static void sendStateToPlayer(CallSession session, ServerPlayer recipient, String partner,
                                          CallSession.State state, String reason, boolean muted,
                                          boolean incoming) {
        PacketDistributor.sendToPlayer(recipient, new com.example.mobilecellnetwork.network.CallStatePacket(
                session.id().toString(), state.name(), partner, reason, session.startTime(), muted, incoming));
    }

    private static void sendStateToOnline(CallSession session, MinecraftServer server, String reason) {
        if (server == null) {
            return;
        }
        ServerPlayer caller = server.getPlayerList().getPlayer(session.callerUUID());
        ServerPlayer callee = server.getPlayerList().getPlayer(session.calleeUUID());
        if (caller != null) {
            PacketDistributor.sendToPlayer(caller, new com.example.mobilecellnetwork.network.CallStatePacket(
                    session.id().toString(), CallSession.State.ENDED.name(), "Player", reason,
                    session.startTime(), session.callerMuted(), false));
        }
        if (callee != null) {
            PacketDistributor.sendToPlayer(callee, new com.example.mobilecellnetwork.network.CallStatePacket(
                    session.id().toString(), CallSession.State.ENDED.name(), "Player", reason,
                    session.startTime(), session.calleeMuted(), true));
        }
    }

    private static CallResult reject(ServerPlayer player, CallResult result, String message, String reason) {
        player.displayClientMessage(Component.literal(message), true);
        PacketDistributor.sendToPlayer(player, new com.example.mobilecellnetwork.network.CallStatePacket(
                "", CallSession.State.ENDED.name(), "", reason + ":" + message, 0L, false, false));
        return result;
    }

    private static SignalTier signal(ServerPlayer player) {
        return CoverageEngine.updateSignal(player, CoverageEngine.getStrongestAntennaFor(player)).tier();
    }

    private static boolean hasMinutes(ServerPlayer player) {
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        if (!hasActivePlan(player)) {
            return false;
        }
        PlanRegistry.Plan plan = PlanRegistry.findPlan(usage.getActivePlanId());
        int maximum = plan.callMinutes();
        return plan.allowOverage() || maximum < 0 || maximum > usage.getMinutesUsed();
    }

    private static boolean hasActivePlan(ServerPlayer player) {
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        return usage.isCycleActive() && !usage.getActivePlanId().isBlank()
                && PlanRegistry.findPlan(usage.getActivePlanId()) != null;
    }

    private static int remainingMinutes(ServerPlayer player) {
        int maximum = PlanRegistry.getPlan(player.getData(ModCapabilities.PLAN_USAGE)
                .getActivePlanId()).callMinutes();
        return maximum < 0 ? Integer.MAX_VALUE
                : Math.max(0, maximum - player.getData(ModCapabilities.PLAN_USAGE).getMinutesUsed());
    }

    private static String chargeRoamingMinute(ServerPlayer caller, ServerPlayer callee) {
        ServerPlayer[] participants = {caller, callee};
        long[] chargesByParticipant = new long[participants.length];
        long[] roamingChargesByParticipant = new long[participants.length];
        String[] hostNetworkIds = new String[participants.length];
        for (int i = 0; i < participants.length; i++) {
            ServerPlayer participant = participants[i];
            PlanUsage usage = participant.getData(ModCapabilities.PLAN_USAGE);
            PlanRegistry.Plan plan = PlanRegistry.getPlan(usage.getActivePlanId());
            boolean roaming = CoverageEngine.isRoamingConnection(participant);
            if (roaming && !plan.allowRoaming()) {
                notifyRoamingFailure(participant, "Your plan does not allow roaming; the call has ended.");
                return "ROAMING_NOT_ALLOWED";
            }
            BillingManager.ConnectionType connectionType = BillingManager.getConnectionType(participant, plan);
            var agreement = BillingManager.getRoamingAgreement(participant, plan);
            chargesByParticipant[i] = BillingManager.computeCharges(phoneData(participant), usage, 1L, 0L, 0L,
                    connectionType, agreement, plan);
            roamingChargesByParticipant[i] = BillingManager.computeRoamingCharges(1L, 0L, 0L,
                    connectionType, agreement, plan);
            hostNetworkIds[i] = BillingManager.getRoamingHostNetworkId(participant);
            if (participant.getData(ModCapabilities.BANK_ACCOUNT).getBalance() < chargesByParticipant[i]) {
                notifyRoamingFailure(participant, "Insufficient funds - usage was blocked");
                return "PAYMENT_FAILED";
            }
        }
        for (int i = 0; i < participants.length; i++) {
            if (!BillingManager.chargeUsage(participants[i], chargesByParticipant[i],
                    roamingChargesByParticipant[i], "Call usage", hostNetworkIds[i])) {
                return "PAYMENT_FAILED";
            }
        }
        return null;
    }
    private static void notifyRoamingFailure(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
        PacketDistributor.sendToPlayer(player, new com.example.mobilecellnetwork.network.PhoneNotificationPacket(
                "PLAN_WARNING", "Plan warning", message, false));
    }

    private static void warnLowMinutes(ServerPlayer caller, ServerPlayer callee) {
        Component message = Component.literal("Plan warning: 5 or fewer call minutes remaining.");
        caller.displayClientMessage(message, true);
        callee.displayClientMessage(message, true);
        PacketDistributor.sendToPlayer(caller, new com.example.mobilecellnetwork.network.PhoneNotificationPacket(
                "PLAN_WARNING", "Plan warning", message.getString(), false));
        PacketDistributor.sendToPlayer(callee, new com.example.mobilecellnetwork.network.PhoneNotificationPacket(
                "PLAN_WARNING", "Plan warning", message.getString(), false));
        ResourceLocation id = ResourceLocation.tryParse("minecraft:block.note_block.bell");
        SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound != null) {
            caller.playNotifySound(sound, SoundSource.PLAYERS, 0.7F, 1.0F);
            callee.playNotifySound(sound, SoundSource.PLAYERS, 0.7F, 1.0F);
        }
    }

    private static PhoneData phoneData(ServerPlayer player) {
        return player.getData(ModCapabilities.PHONE_DATA);
    }

    private static MinecraftServer serverFor(UUID playerId) {
        return net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
    }

    private static void playSound(ServerPlayer player, String soundName) {
        ResourceLocation id = ResourceLocation.tryParse(soundName);
        if (id == null) {
            LOGGER.warn("Ignoring invalid configured call sound '{}'.", soundName);
            return;
        }

        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound == null) {
            LOGGER.warn("Ignoring unknown configured call sound '{}'.", soundName);
            return;
        }
        player.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void playEndSound(ServerPlayer player) {
        PhoneData data = phoneData(player);
        if (data.areNotificationSoundsEnabled() && data.getAppNotifications().getOrDefault("call", true)) {
            playSound(player, MobileCellNetworkConfig.CALL_END_SOUND.get());
        }
    }

    public enum CallResult {
        ACCEPTED,
        BUSY,
        NO_SERVICE,
        NO_PLAN_MINUTES,
        PLAN_LIMIT_REACHED,
        DECLINED,
        TIMEOUT,
        PHONE_DEAD,
        LOW_POWER
    }
}

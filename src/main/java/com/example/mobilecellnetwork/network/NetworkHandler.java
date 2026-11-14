package com.example.mobilecellnetwork.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import com.example.mobilecellnetwork.capabilities.PlanUsage;
import com.example.mobilecellnetwork.billing.BillingManager;
import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.data.MapPointOfInterest;
import com.example.mobilecellnetwork.data.Chat;
import com.example.mobilecellnetwork.data.ChatType;
import com.example.mobilecellnetwork.data.EmergencyMessages;
import com.example.mobilecellnetwork.data.Message;
import com.example.mobilecellnetwork.data.TowerPreferenceMode;
import com.example.mobilecellnetwork.data.TowerPreferenceMode;
import com.example.mobilecellnetwork.gui.EmergencyDispatchMenu;
import com.example.mobilecellnetwork.gui.PhoneMenu;
import com.example.mobilecellnetwork.gui.ServerMenu;
import com.example.mobilecellnetwork.gui.PhoneStyle;
import com.example.mobilecellnetwork.network.packet.*;
import com.example.mobilecellnetwork.voice.CallManager;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.plans.PlanRegistry;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.util.CurrencyUtil;
import com.example.mobilecellnetwork.network.packet.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void receiveEmergencyMessage(ServerPlayer player, String body) {
        handleEmergencyMessage(player, body == null ? "" : body.strip());
    }

    public static void init() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(AntennaConfigActionPacket.TYPE, AntennaConfigActionPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        AntennaConfigHandler.handle(context.player(), packet)));
        registrar.playToClient(AntennaConfigSyncPacket.TYPE, AntennaConfigSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientAntennaConfigCache.update(packet)));
        registrar.playToClient(SignalUpdatePacket.TYPE, SignalUpdatePacket.STREAM_CODEC,
                (packet, context) -> {
                    context.enqueueWork(() -> ClientSignalCache.update(packet));
                });
        registrar.playToClient(OpenPhoneScreenPacket.TYPE, OpenPhoneScreenPacket.STREAM_CODEC,
                OpenPhoneScreenPacket::handle);
        registrar.playToClient(PhoneDeviceStatePacket.TYPE, PhoneDeviceStatePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    java.util.UUID priorPhoneId = PhoneDeviceClientCache.phoneId();
                    if (packet.clearSensitiveData() || packet.lockedAll()) {
                        clearClientPhoneData();
                    } else if (!java.util.Objects.equals(priorPhoneId, packet.phoneId())) {
                        clearClientPhoneData();
                    }
                    if (packet.lockedBank()) {
                        ClientBankAccountCache.clearSensitiveData();
                        ClientPlanCache.clearSensitiveData();
                    }
                    PhoneDeviceClientCache.update(packet);
                    com.example.mobilecellnetwork.gui.PhonePhotoLibrary.setActivePhoneId(packet.phoneId());
                    var current = net.minecraft.client.Minecraft.getInstance().screen;
                    if (packet.lockedAll() && current instanceof com.example.mobilecellnetwork.gui.PhoneNavigableScreen phoneScreen
                            && phoneScreen.phoneMenu() != null) {
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                                new com.example.mobilecellnetwork.gui.LockScreen(phoneScreen.phoneMenu()));
                    }
                }));
        registrar.playToClient(PhonePinResultPacket.TYPE, PhonePinResultPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> PhoneDeviceClientCache.recordPinResult(packet)));
        registrar.playToClient(PhonePhotoListPacket.TYPE, PhonePhotoListPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        com.example.mobilecellnetwork.gui.PhonePhotoLibrary.replacePhonePhotos(
                                packet.phoneId(), packet.names())));
        registrar.playToClient(PhonePhotoPayloadPacket.TYPE, PhonePhotoPayloadPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        com.example.mobilecellnetwork.gui.PhonePhotoLibrary.saveRemotePhoto(
                                packet.phoneId(), packet.name(), packet.image())));
        registrar.playToClient(KnownPhoneDevicesPacket.TYPE, KnownPhoneDevicesPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> PhoneDeviceClientCache.updateDevices(packet)));
        registrar.playToServer(UnlockPhonePacket.TYPE, UnlockPhonePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handlePhoneUnlock(context.player(), packet)));
        registrar.playToServer(SetPhonePinPacket.TYPE, SetPhonePinPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleSetPhonePin(context.player(), packet)));
        registrar.playToServer(RemovePhonePinPacket.TYPE, RemovePhonePinPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleRemovePhonePin(context.player(), packet)));
        registrar.playToServer(SetPhoneLockScreenPacket.TYPE, SetPhoneLockScreenPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleSetPhoneLockScreen(context.player(), packet)));
        registrar.playToServer(RequestKnownPhoneDevicesPacket.TYPE, RequestKnownPhoneDevicesPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> sendKnownPhoneDevices(context.player())));
        registrar.playToServer(LockPhoneFeaturePacket.TYPE, LockPhoneFeaturePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleLockPhoneFeature(context.player(), packet)));
        registrar.playToServer(WipePhonePacket.TYPE, WipePhonePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleWipePhone(context.player(), packet)));
        registrar.playToServer(RemoveKnownPhonePacket.TYPE, RemoveKnownPhonePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleRemoveKnownPhone(context.player(), packet)));
        registrar.playToServer(RequestPhonePhotosPacket.TYPE, RequestPhonePhotosPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        PhoneDeviceService.sendPhotos(player, packet.phoneId());
                    }
                }));
        registrar.playToServer(PhonePhotoUploadPacket.TYPE, PhonePhotoUploadPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player
                            && !PhoneDeviceService.savePhoto(player, packet.phoneId(), packet.name(), packet.image())) {
                        notifyPlayer(player, "Could not save this photo to the active phone.");
                    }
                }));
        registrar.playToClient(PhoneDataSyncPacket.TYPE, PhoneDataSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientPhoneCache.update(packet)));
        registrar.playToClient(PhoneStyleSyncPacket.TYPE, PhoneStyleSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientPhoneCache.setPhoneStyle(packet.style())));
        registrar.playToClient(BankAccountSyncPacket.TYPE, BankAccountSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientBankAccountCache.update(packet)));
        registrar.playToServer(ServerAdminRequestPacket.TYPE, ServerAdminRequestPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ServerAdminHandler.request(context.player(), packet)));
        registrar.playToServer(ServerAdminActionPacket.TYPE, ServerAdminActionPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ServerAdminHandler.action(context.player(), packet)));
        registrar.playToServer(SetNetworkPasswordPacket.TYPE, SetNetworkPasswordPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleSetNetworkPassword(context.player(), packet)));
        registrar.playToServer(ReconnectServerBlockPacket.TYPE, ReconnectServerBlockPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleServerReconnect(context.player(), packet)));
        registrar.playToServer(ClaimAntennaPacket.TYPE, ClaimAntennaPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleAntennaClaim(context.player(), packet)));
        registrar.playToServer(UnclaimAntennaPacket.TYPE, UnclaimAntennaPacket.STREAM_CODEC,
                        (packet, context) -> context.enqueueWork(() -> {
                            System.out.println("[MCN] Received " + packet.type().id() + " from " + context.player().getName().getString());
                            handleAntennaUnclaim(context.player(), packet);
                        }));
        registrar.playToServer(AddAntennaSharePacket.TYPE, AddAntennaSharePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleAntennaShare(context.player(), packet)));
        registrar.playToServer(RemoveAntennaSharePacket.TYPE, RemoveAntennaSharePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleAntennaUnshare(context.player(), packet)));
        registrar.playToClient(RoamingProposalPacket.TYPE, RoamingProposalPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleClientRoamingProposal(packet)));
        registrar.playToServer(RoamingProposalResponsePacket.TYPE, RoamingProposalResponsePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleRoamingProposalResponse(context.player(), packet)));
        registrar.playToClient(RoamingAgreementSyncPacket.TYPE, RoamingAgreementSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleClientRoamingAgreements(packet)));
        registrar.playToServer(RevokeAgreementPacket.TYPE, RevokeAgreementPacket.STREAM_CODEC,
                        (packet, context) -> context.enqueueWork(() -> {
                            System.out.println("[MCN] Received " + packet.type().id() + " from " + context.player().getName().getString());
                            handleRevokeAgreement(context.player(), packet);
                        }));
        registrar.playToServer(com.example.mobilecellnetwork.network.packet.SelectPlanPacket.TYPE,
                com.example.mobilecellnetwork.network.packet.SelectPlanPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handlePlanSubscription(context.player(), packet.planId())));
        registrar.playToServer(CancelPlanPacket.TYPE, CancelPlanPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handlePlanCancellation(context.player())));
        registrar.playToServer(SetTowerPreferencePacket.TYPE, SetTowerPreferencePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleTowerPreference(context.player(), packet)));
        registrar.playToServer(EmergencyMessagePacket.TYPE, EmergencyMessagePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && hasPhoneMenu(player)) {
                        receiveEmergencyMessage(player, packet.body());
                    }
                }));
        registrar.playToServer(EmergencyDispatchRequestPacket.TYPE, EmergencyDispatchRequestPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> sendEmergencyDispatchData(context.player())));
        registrar.playToClient(EmergencyDispatchDataPacket.TYPE, EmergencyDispatchDataPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientEmergencyDispatchCache.update(packet)));
        registrar.playToClient(ServerAdminDataPacket.TYPE, ServerAdminDataPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientServerAdminCache.update(packet)));
        registrar.playToClient(NetworkCreationResultPacket.TYPE, NetworkCreationResultPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (packet.success()) {
                        com.example.mobilecellnetwork.gui.admin.AdminToast.success(packet.message());
                    } else {
                        com.example.mobilecellnetwork.gui.admin.AdminToast.error(packet.message());
                    }
                }));
        registrar.playToClient(SmsDataSyncPacket.TYPE, SmsDataSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientSmsCache.update(packet)));
        registrar.playToClient(ChatListSyncPacket.TYPE, ChatListSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientChatCache.update(packet)));
        registrar.playToClient(SmsReceivePacket.TYPE, SmsReceivePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (packet.legacyMessage() != null) {
                        ClientSmsCache.receive(packet.legacyMessage());
                    } else {
                        ClientChatCache.receive(packet.chatId(), packet.message());
                    }
                }));
        registrar.playToClient(PlanSyncPacket.TYPE, PlanSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientPlanCache.update(packet)));
        registrar.playToClient(MapPointSyncPacket.TYPE, MapPointSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> MapPointCache.update(packet)));
        registrar.playToClient(ConnectionStatePacket.TYPE, ConnectionStatePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientConnectionCache.update(packet)));
        registrar.playToClient(MapLoadStatePacket.TYPE, MapLoadStatePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> MapLoadCache.update(packet)));
        registrar.playToClient(AntennaCachePacket.TYPE, AntennaCachePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientSignalCache.updateAntennas(packet)));
        registrar.playToServer(RequestPhoneDataPacket.TYPE, RequestPhoneDataPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> requestPhoneData(context.player())));
        registrar.playToServer(CallRequestPacket.TYPE, CallRequestPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if ("911".equals(packet.number())) {
                        handleCall(context.player(), packet);
                    } else {
                        ConnectionDelayService.submit(context.player(),
                                () -> handleCall(context.player(), packet));
                    }
                }));
        registrar.playToServer(CallControlPacket.TYPE, CallControlPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleCallControl(context.player(), packet)));
        registrar.playToClient(CallStatePacket.TYPE, CallStatePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientCallCache.update(packet)));
        registrar.playToServer(ContactActionPacket.TYPE, ContactActionPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleContact(context.player(), packet)));
        registrar.playToServer(ContactsActionPacket.TYPE, ContactsActionPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleContactsAction(context.player(), packet)));
        registrar.playToServer(AppInstallPacket.TYPE, AppInstallPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleAppInstall(context.player(), packet))));
        registrar.playToServer(BankTransferPacket.TYPE, BankTransferPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleBankTransfer(context.player(), packet))));
        registrar.playToServer(BankingOpenPacket.TYPE, BankingOpenPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleBankingOpen(context.player()))));
        registrar.playToServer(SettingsUpdatePacket.TYPE, SettingsUpdatePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleSettingsUpdate(context.player(), packet)));
        registrar.playToServer(SetPhoneStylePacket.TYPE, SetPhoneStylePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> setPhoneStyle(context.player(), packet.style())));
        registrar.playToServer(MapOpenPacket.TYPE, MapOpenPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleMapOpen(context.player()),
                        () -> setMapLoadState(context.player(), false, "No Service - map unavailable."))));
        registrar.playToServer(SocialFeedRequestPacket.TYPE, SocialFeedRequestPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleSocialFeedRequest(context.player(), packet.filter()))));
        registrar.playToServer(SocialPostPacket.TYPE, SocialPostPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleSocialPost(context.player(), packet))));
        registrar.playToServer(SocialLikePacket.TYPE, SocialLikePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleSocialLike(context.player(), packet))));
        registrar.playToServer(SocialFollowPacket.TYPE, SocialFollowPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleSocialFollow(context.player(), packet))));
        registrar.playToServer(SocialProfilePacket.TYPE, SocialProfilePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleSocialProfile(context.player(), packet)));
        registrar.playToServer(SocialImageRequestPacket.TYPE, SocialImageRequestPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleSocialImageRequest(context.player(), packet)));
        registrar.playToClient(SocialFeedPacket.TYPE, SocialFeedPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientSocialCache.update(packet)));
        registrar.playToClient(SocialProfileSyncPacket.TYPE, SocialProfileSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientSocialProfileCache.update(packet)));
        registrar.playToClient(SocialImagePacket.TYPE, SocialImagePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientSocialImageCache.update(packet)));
        registrar.playToServer(SetLowPowerPacket.TYPE, SetLowPowerPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleSettings(context.player(), packet)));
        registrar.playToServer(DismissLowPowerPromptPacket.TYPE, DismissLowPowerPromptPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> dismissLowPowerPrompt(context.player())));
        registrar.playToServer(MapInteractionPacket.TYPE, MapInteractionPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleMapInteraction(context.player(), packet))));
        registrar.playToServer(MapAntennaCacheRequestPacket.TYPE, MapAntennaCacheRequestPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleMapAntennaCacheRequest(context.player(), packet)));
        registrar.playToServer(MapPointActionPacket.TYPE, MapPointActionPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleMapPointAction(context.player(), packet))));
        registrar.playToServer(SmsSendPacket.TYPE, SmsSendPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleSms(context.player(), packet))));
        registrar.playToServer(StartChatPacket.TYPE, StartChatPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> startChat(context.player(), packet)));
        registrar.playToServer(CreateGroupChatPacket.TYPE, CreateGroupChatPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> createGroupChat(context.player(), packet)));
        registrar.playToServer(MarkChatReadPacket.TYPE, MarkChatReadPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> markChatRead(context.player(), packet)));
        registrar.playToServer(ChatActionPacket.TYPE, ChatActionPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleChatAction(context.player(), packet)));
        registrar.playToServer(SmsReadPacket.TYPE, SmsReadPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handleSmsRead(context.player(), packet))));
        registrar.playToServer(VoicemailNotePacket.TYPE, VoicemailNotePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleVoicemailNote(context.player(), packet)));
        registrar.playToServer(LeaveVoicemailPacket.TYPE, LeaveVoicemailPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleLeaveVoicemail(context.player(), packet)));
        registrar.playBidirectional(FlashlightPacket.TYPE, FlashlightPacket.STREAM_CODEC,
                new net.neoforged.neoforge.network.handling.DirectionalPayloadHandler<>(
                        (packet, context) -> context.enqueueWork(
                                () -> ClientPhoneCache.setFlashlightOn(packet.on())),
                        (packet, context) -> context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer sp && hasPhoneMenu(sp)) {
                                CoverageEngine.setFlashlight(sp, packet.on());
                            }
                        })));
        registrar.playToServer(TowerSelectPacket.TYPE, TowerSelectPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer sp) || !hasPhoneMenu(sp)) return;
                    if (packet.action() == TowerSelectPacket.AUTOMATIC) {
                        CoverageEngine.setPreferredTower(sp, null);
                    } else if (packet.action() == TowerSelectPacket.SELECT) {
                        CoverageEngine.setPreferredTower(sp, packet.pos());
                    }
                    CoverageEngine.getStrongestAntennaFor(sp);
                    CoverageEngine.sendTowerList(sp);
                }));
        registrar.playToClient(TowerListPacket.TYPE, TowerListPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientTowerCache.update(packet)));
        registrar.playToClient(CallLogSyncPacket.TYPE, CallLogSyncPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientCallLogCache.update(packet)));
        registrar.playToClient(CallLogEntryPacket.TYPE, CallLogEntryPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientCallLogCache.add(packet.entry())));
        registrar.playToServer(DeleteCallLogEntryPacket.TYPE, DeleteCallLogEntryPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && hasPhoneMenu(player)) {
                        player.getData(ModCapabilities.PHONE_DATA).getCallLog()
                                .removeIf(entry -> entry.id().equals(packet.entryId()));
                        PacketDistributor.sendToPlayer(player, new CallLogSyncPacket(
                                java.util.List.copyOf(player.getData(ModCapabilities.PHONE_DATA).getCallLog())));
                    }
                }));
        registrar.playToServer(DeleteVoicemailPacket.TYPE, DeleteVoicemailPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player && hasPhoneMenu(player)) {
                        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
                        if (packet.index() >= 0 && packet.index() < data.getVoicemail().size()) {
                            data.getVoicemail().remove(packet.index());
                            PacketDistributor.sendToPlayer(player, SmsDataSyncPacket.from(data));
                        }
                    }
                }));
        registrar.playToServer(PlanSelectPacket.TYPE, PlanSelectPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ConnectionDelayService.submit(context.player(),
                        () -> handlePlanSelect(context.player(), packet))));
        registrar.playToClient(LowPowerStatePacket.TYPE, LowPowerStatePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientPhoneCache.updateLowPowerState(packet.enabled())));
        registrar.playToClient(PhoneChargePacket.TYPE, PhoneChargePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientSignalCache.setCharge(packet.charge())));
        registrar.playToClient(PhoneDiedPacket.TYPE, PhoneDiedPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> ClientPhoneCache.setPhoneDead(true)));
        registrar.playToClient(PhoneNotificationPacket.TYPE, PhoneNotificationPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (packet.lowPowerPrompt()) {
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                                new com.example.mobilecellnetwork.gui.LowPowerPromptScreen(packet.title(), packet.message()));
                    } else if (net.minecraft.client.Minecraft.getInstance().player != null) {
                        ClientSmsCache.addToast(packet.title(), packet.message());
                    }
                }));
        registrar.playToServer(CreateNetworkPacket.TYPE, CreateNetworkPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        ServerAdminHandler.createNetwork(context.player(), packet.name(), packet.password())));
        registrar.playToServer(UpdateNetworkPacket.TYPE, UpdateNetworkPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleUpdateNetwork(context.player(), packet)));
        registrar.playToServer(ChangePasswordPacket.TYPE, ChangePasswordPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> handleSetNetworkPassword(context.player(),
                        new SetNetworkPasswordPacket(packet.networkId(), packet.newPassword()))));
        registrar.playToServer(DeleteEntityPacket.TYPE, DeleteEntityPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleDeleteEntity(context.player(), packet)));
        registrar.playToServer(AssignAntennaPacket.TYPE, AssignAntennaPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleAssignAntenna(context.player(), packet)));
        registrar.playToServer(ToggleNetworkRoamingPacket.TYPE, ToggleNetworkRoamingPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleToggleNetworkRoaming(context.player(), packet)));
        registrar.playToServer(RoamingProposalPacket.TYPE, RoamingProposalPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleRoamingProposal(context.player(), packet)));
        registrar.playToServer(RoamingResponsePacket.TYPE, RoamingResponsePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleRoamingResponse(context.player(), packet)));
        registrar.playToServer(UpdatePlanPacket.TYPE, UpdatePlanPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleUpdatePlan(context.player(), packet)));
        registrar.playToServer(SetPlayerNumberPacket.TYPE, SetPlayerNumberPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleSetPlayerNumber(context.player(), packet)));
        registrar.playToServer(AssignPlanPacket.TYPE, AssignPlanPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleAssignPlan(context.player(), packet)));
        registrar.playToServer(ForceBillingPacket.TYPE, ForceBillingPacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() ->
                        handleForceBilling(context.player(), packet)));
    }

    private static void handleUpdateNetwork(net.minecraft.world.entity.player.Player sender,
                                            UpdateNetworkPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasServerAdminMenu(player)) {
            return;
        }
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        NetworkSavedData.NetworkRecord network = data.getNetwork(packet.networkId());
        if (network == null || !(player.hasPermissions(2) || player.getUUID().equals(network.ownerUUID()))) {
            notifyPlayer(player, "You cannot manage that network.");
            return;
        }
        if (!data.renameNetwork(packet.networkId(), packet.name())) {
            notifyPlayer(player, "Could not update the network name.");
            return;
        }
        var settings = data.getNetworkSettings(packet.networkId());
        if (settings.isAllowRoaming() != packet.allowRoaming()) {
            settings.setAllowRoaming(packet.allowRoaming());
            data.setDirty();
        }
        notifyPlayer(player, "Network updated.");
        ServerAdminHandler.request(player, new ServerAdminRequestPacket("Networks"));
    }

    private static void handleDeleteEntity(net.minecraft.world.entity.player.Player sender,
                                           DeleteEntityPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !requireServerOperator(player)) {
            return;
        }
        String entityType = packet.entityType().toLowerCase(Locale.ROOT);
        switch (entityType) {
            case "network" -> {
                boolean deleted = NetworkSavedData.get(player.server.overworld())
                        .deleteNetwork(packet.entityId());
                notifyPlayer(player, deleted ? "Network deleted." : "Network not found.");
                if (deleted) {
                    ServerAdminHandler.request(player, new ServerAdminRequestPacket("Networks"));
                }
            }
            case "plan" -> {
                boolean deleted = PlanRegistry.deleteAdminPlan(packet.entityId());
                notifyPlayer(player, deleted ? "Plan deleted." : "Plan could not be deleted.");
                if (deleted) {
                    ServerAdminHandler.request(player, new ServerAdminRequestPacket("Plans"));
                }
            }
            case "antenna" -> {
                BlockPos position = parseBlockPos(packet.entityId());
                if (position == null) {
                    notifyPlayer(player, "Invalid antenna position.");
                    return;
                }
                AntennaClaimHandler.UnclaimResult result = AntennaClaimHandler.unclaim(player, position);
                notifyPlayer(player, switch (result) {
                    case SUCCESS -> "Antenna unclaimed; the block was not removed.";
                    case NOT_OWNER -> "You cannot unclaim that antenna.";
                    case ANTENNA_NOT_FOUND -> "No antenna at that position.";
                });
            }
            default -> notifyPlayer(player, "Unknown entity type.");
        }
    }

    private static void handleAssignAntenna(net.minecraft.world.entity.player.Player sender,
                                            AssignAntennaPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasServerAdminMenu(player)) {
            return;
        }
        BlockPos position = parseBlockPos(packet.antennaPos());
        if (position == null) {
            notifyPlayer(player, "Invalid antenna position.");
            return;
        }

        // TODO: AssignAntennaPacket has no password field, but claim requires one.
        System.out.println("[MCN] AssignAntennaPacket cannot provide the password required by AntennaClaimHandler.claim.");
        AntennaClaimHandler.ClaimResult result = AntennaClaimHandler.claim(
                player, position, packet.networkId(), "");
        notifyPlayer(player, switch (result) {
            case SUCCESS -> "Antenna assigned to network.";
            case WRONG_PASSWORD -> "A password is required to claim that network.";
            case NETWORK_NOT_FOUND -> "Unknown network.";
            case ANTENNA_ALREADY_CLAIMED -> "That antenna belongs to another network.";
            case ANTENNA_NOT_FOUND -> "No antenna at that position.";
        });
    }

    private static void handleToggleNetworkRoaming(net.minecraft.world.entity.player.Player sender,
                                                   ToggleNetworkRoamingPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasServerAdminMenu(player)) {
            return;
        }
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        NetworkSavedData.NetworkRecord network = data.getNetwork(packet.networkId());
        if (network == null || !(player.hasPermissions(2) || player.getUUID().equals(network.ownerUUID()))) {
            notifyPlayer(player, "You cannot manage that network.");
            return;
        }
        var settings = data.getNetworkSettings(packet.networkId());
        settings.setAllowRoaming(!settings.isAllowRoaming());
        data.setDirty();
        notifyPlayer(player, "Network roaming " + (settings.isAllowRoaming() ? "enabled." : "disabled."));
        ServerAdminHandler.request(player, new ServerAdminRequestPacket("Roaming"));
    }

    private static void handleRoamingProposal(net.minecraft.world.entity.player.Player sender,
                                              RoamingProposalPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasServerAdminMenu(player)) {
            return;
        }
        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        List<String> ownedNetworkIds = data.getNetworks().entrySet().stream()
                .filter(entry -> player.getUUID().equals(entry.getValue().ownerUUID()))
                .map(entry -> entry.getKey()).sorted().toList();
        if (ownedNetworkIds.size() != 1) {
            notifyPlayer(player, "A roaming proposal requires exactly one owned network.");
            return;
        }
        long perMinute;
        long perText;
        long perMb;
        try {
            perMinute = Long.parseLong(packet.rateMin());
            perText = Long.parseLong(packet.rateText());
            perMb = Long.parseLong(packet.rateMb());
        } catch (NumberFormatException exception) {
            notifyPlayer(player, "Enter valid whole-number roaming rates.");
            return;
        }
        if (!packet.message().isBlank()) {
            // TODO: RoamingManager.propose has no message parameter.
            System.out.println("[MCN] Roaming proposal message is not stored by RoamingManager.propose.");
        }
        boolean proposed = RoamingManager.propose(player, ownedNetworkIds.getFirst(),
                packet.targetNetworkId(), perMinute, perText, perMb);
        notifyPlayer(player, proposed ? "Roaming proposal sent." : "Could not send roaming proposal.");
    }

    private static void handleRoamingResponse(net.minecraft.world.entity.player.Player sender,
                                             RoamingResponsePacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasServerAdminMenu(player)) {
            return;
        }
        ProposalResponse response;
        try {
            response = ProposalResponse.valueOf(packet.response().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            notifyPlayer(player, "Unknown roaming proposal response.");
            return;
        }
        if (response == ProposalResponse.COUNTER) {
            notifyPlayer(player, "Counter-proposals require rate fields not provided by this packet.");
            return;
        }
        handleRoamingProposalResponse(player,
                new RoamingProposalResponsePacket(packet.proposalId(), response, 0L, 0L, 0L));
    }

    private static void handleSetPlayerNumber(net.minecraft.world.entity.player.Player sender,
                                              SetPlayerNumberPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !requireServerOperator(player)) {
            return;
        }
        UUID playerId;
        try {
            playerId = UUID.fromString(packet.playerUuid());
        } catch (IllegalArgumentException exception) {
            notifyPlayer(player, "Invalid player UUID.");
            return;
        }
        boolean registered = NetworkSavedData.get(player.server.overworld())
                .registerNumber(playerId, packet.number());
        notifyPlayer(player, registered ? "Player number updated."
                : "Number must be 1-9 digits and not assigned to another player.");
    }

    private static void handleAssignPlan(net.minecraft.world.entity.player.Player sender,
                                         AssignPlanPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !requireServerOperator(player)) {
            return;
        }
        UUID playerId;
        try {
            playerId = UUID.fromString(packet.playerUuid());
        } catch (IllegalArgumentException exception) {
            notifyPlayer(player, "Invalid player UUID.");
            return;
        }
        if (PlanRegistry.findPlan(packet.planId()) == null) {
            notifyPlayer(player, "Unknown plan.");
            return;
        }
        NetworkSavedData.get(player.server.overworld()).setPlanAssignment(playerId, packet.planId());
        notifyPlayer(player, "Plan assigned.");
    }

    private static void handleForceBilling(net.minecraft.world.entity.player.Player sender,
                                           ForceBillingPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !requireServerOperator(player)) {
            return;
        }
        ServerPlayer target = player.server.getPlayerList().getPlayerByName(packet.playerName());
        if (target == null) {
            notifyPlayer(player, "Player is not online.");
            return;
        }
        boolean processed = BillingManager.processPlayer(target, true);
        notifyPlayer(player, processed ? "Billing processed for " + target.getGameProfile().getName() + "."
                : "Billing could not be processed for " + target.getGameProfile().getName() + ".");
    }

    private static void handleUpdatePlan(net.minecraft.world.entity.player.Player sender,
                                         UpdatePlanPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !requireServerOperator(player)) {
            return;
        }
        PlanRegistry.Plan current = PlanRegistry.findPlan(packet.planId());
        if (current == null) {
            notifyPlayer(player, "Unknown plan.");
            return;
        }
        try {
            PlanRegistry.Plan updated = new PlanRegistry.Plan(current.id(), packet.displayName().trim(),
                    current.description(), Long.parseLong(packet.price()), Integer.parseInt(packet.cycleDays()),
                    Long.parseLong(packet.dataAllowanceMb()), current.dataSpeed(), current.maxSignalTier(),
                    Integer.parseInt(packet.callMinutes()), Integer.parseInt(packet.textMessages()),
                    current.allowedNetworks(), current.autoRenew(), current.isDefault(), current.allowRoaming(),
                    current.roamingFeePerMinute(), current.roamingFeePerMb(), current.carrierNetworkId(),
                    current.roamingMode(), current.allowOverage(), current.overageRatePerMinute(),
                    current.overageRatePerText(), current.overageRatePerMb());
            if (updated.displayName().isBlank() || !PlanRegistry.putAdminPlan(updated)) {
                notifyPlayer(player, "Could not update the plan.");
                return;
            }
        } catch (IllegalArgumentException exception) {
            notifyPlayer(player, "Plan values are invalid.");
            return;
        }
        notifyPlayer(player, "Plan updated.");
        ServerAdminHandler.request(player, new ServerAdminRequestPacket("Plans"));
    }

    private static BlockPos parseBlockPos(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length != 3) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean hasServerAdminMenu(ServerPlayer player) {
        return player.containerMenu instanceof ServerMenu menu && menu.stillValid(player);
    }

    private static boolean requireServerOperator(ServerPlayer player) {
        if (!player.hasPermissions(2) || !hasServerAdminMenu(player)) {
            notifyPlayer(player, "Only an operator using the Server Block menu can do that.");
            return false;
        }
        return true;
    }

    private static void handleSetNetworkPassword(net.minecraft.world.entity.player.Player sender,
                                                 SetNetworkPasswordPacket packet) {
        if (!(sender instanceof ServerPlayer player)) {
            return;
        }
        if (ServerBlockPasswordHandler.changePassword(player, packet.networkId(), packet.newPassword())) {
            notifyPlayer(player, "Network password updated.");
        } else {
            notifyPlayer(player, "Could not change that network password.");
        }
    }

    private static void handleServerReconnect(net.minecraft.world.entity.player.Player sender,
                                              ReconnectServerBlockPacket packet) {
        if (!(sender instanceof ServerPlayer player)
                || !player.serverLevel().hasChunkAt(packet.blockPos())
                || !(player.serverLevel().getBlockEntity(packet.blockPos())
                instanceof com.example.mobilecellnetwork.blockentity.ServerBlockEntity serverBlock)) {
            if (sender instanceof ServerPlayer player) {
                notifyPlayer(player, "Place a server block at the selected position first.");
            }
            return;
        }
        if (player.distanceToSqr(packet.blockPos().getX() + 0.5D,
                packet.blockPos().getY() + 0.5D, packet.blockPos().getZ() + 0.5D) > 64.0D) {
            notifyPlayer(player, "Move closer to the server block to reconnect it.");
            return;
        }
        var result = ServerBlockReconnectHandler.reconnect(player, packet.blockPos(),
                packet.networkId(), packet.password());
        switch (result) {
            case SUCCESS -> {
                serverBlock.addManagedNetwork(packet.networkId());
                notifyPlayer(player, "Server block reconnected.");
            }
            case WRONG_PASSWORD -> notifyPlayer(player, "Incorrect network password.");
            case NETWORK_ALREADY_HAS_SERVER -> notifyPlayer(player, "That network already has a server block.");
            case NETWORK_NOT_FOUND -> notifyPlayer(player, "Unknown network.");
        }
    }

    private static void handleAntennaClaim(net.minecraft.world.entity.player.Player sender,
                                           ClaimAntennaPacket packet) {
        if (!(sender instanceof ServerPlayer player)) {
            return;
        }
        var result = AntennaClaimHandler.claim(player, packet.antennaPos(),
                packet.networkId(), packet.password());
        notifyPlayer(player, switch (result) {
            case SUCCESS -> "Antenna claimed.";
            case WRONG_PASSWORD -> "Incorrect network password.";
            case NETWORK_NOT_FOUND -> "Unknown network.";
            case ANTENNA_ALREADY_CLAIMED -> "That antenna belongs to another network.";
            case ANTENNA_NOT_FOUND -> "No antenna at that position.";
        });
    }

    private static void handleAntennaUnclaim(net.minecraft.world.entity.player.Player sender,
                                             UnclaimAntennaPacket packet) {
        if (!(sender instanceof ServerPlayer player)) {
            return;
        }
        String[] apos = packet.antennaPos().split(",");
        BlockPos aposPos = new BlockPos(Integer.parseInt(apos[0]), Integer.parseInt(apos[1]), Integer.parseInt(apos[2]));
        var result = AntennaClaimHandler.unclaim(player, aposPos);
        notifyPlayer(player, switch (result) {
            case SUCCESS -> "Antenna unclaimed.";
            case NOT_OWNER -> "Only the network owner or an operator may unclaim this antenna.";
            case ANTENNA_NOT_FOUND -> "No antenna at that position.";
        });
    }

    private static void handleAntennaShare(net.minecraft.world.entity.player.Player sender,
                                           AddAntennaSharePacket packet) {
        if (sender instanceof ServerPlayer player) {
            notifyPlayer(player, AntennaShareHandler.addShare(player, packet.antennaPos(), packet.networkId())
                    ? "Antenna sharing enabled." : "Could not share the antenna with that network.");
        }
    }

    private static void handleAntennaUnshare(net.minecraft.world.entity.player.Player sender,
                                             RemoveAntennaSharePacket packet) {
        if (sender instanceof ServerPlayer player) {
            notifyPlayer(player, AntennaShareHandler.removeShare(player, packet.antennaPos(), packet.networkId())
                    ? "Antenna sharing removed." : "Could not remove that antenna share.");
        }
    }

    private static void handleRoamingProposalResponse(net.minecraft.world.entity.player.Player sender,
                                                     RoamingProposalResponsePacket packet) {
        if (!(sender instanceof ServerPlayer player)) {
            return;
        }
        if (!RoamingManager.respondToProposal(player, packet.proposalId(), packet.response(),
                packet.counterPerMinute(), packet.counterPerText(), packet.counterPerMb())) {
            notifyPlayer(player, "That roaming proposal is no longer available or you cannot manage it.");
        } else {
            notifyPlayer(player, switch (packet.response()) {
                case ACCEPT -> "Roaming proposal accepted.";
                case REJECT -> "Roaming proposal declined.";
                case COUNTER -> "Roaming counter-proposal sent.";
            });
        }
    }

    private static void handleRevokeAgreement(net.minecraft.world.entity.player.Player sender,
                                              RevokeAgreementPacket packet) {
        if (!(sender instanceof ServerPlayer player)) {
            return;
        }
        notifyPlayer(player, RoamingManager.revokeAgreement(player, packet.agreementId(), packet.agreementId())
                ? "Roaming agreement revoked." : "Could not revoke that roaming agreement.");
    }

    private static void handlePlanSubscription(net.minecraft.world.entity.player.Player sender, String planId) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        var result = com.example.mobilecellnetwork.plans.PlanSubscriptionHandler.selectPlan(player, planId);
        notifyPlayer(player, switch (result) {
            case SUCCESS -> "Plan activated.";
            case INSUFFICIENT_FUNDS -> "Insufficient funds to select that plan.";
            case PLAN_NOT_FOUND -> "That plan is not available.";
            case PLAN_ALREADY_ACTIVE -> "You already have this plan.";
        });
    }

    private static void handlePlanCancellation(net.minecraft.world.entity.player.Player sender) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        com.example.mobilecellnetwork.plans.PlanSubscriptionHandler.cancelPlan(player);
        notifyPlayer(player, "Plan canceled.");
    }

    private static void handleTowerPreference(net.minecraft.world.entity.player.Player sender,
                                              SetTowerPreferencePacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player) || packet.mode() == null) {
            return;
        }
        if (packet.mode() == TowerPreferenceMode.PINNED) {
            if (packet.pinnedTower() == null
                    || !(player.serverLevel().getBlockEntity(packet.pinnedTower())
                    instanceof com.example.mobilecellnetwork.blockentity.AntennaBlockEntity)) {
                notifyPlayer(player, "Select a loaded antenna to pin.");
                return;
            }
        } else if (packet.pinnedTower() != null) {
            notifyPlayer(player, "Only a pinned tower preference accepts a position.");
            return;
        }
        PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
        phone.setTowerMode(packet.mode());
        phone.setPinnedTowerPos(packet.pinnedTower());
        CoverageEngine.setPreferredTower(player,
                packet.pinnedTower() == null ? null : packet.pinnedTower().asLong());
        CoverageEngine.sendTowerList(player);
        sendPhoneData(player);
        notifyPlayer(player, "Tower preference updated.");
    }

    private static void handleClientRoamingProposal(RoamingProposalPacket packet) {
        ClientRoamingCache.updateProposal(packet);
        if (net.minecraft.client.Minecraft.getInstance().screen
                instanceof com.example.mobilecellnetwork.gui.ServerBlockScreen screen
                && screen.isRoamingTabSelected()) {
            screen.refreshRoamingTab();
        } else {
            ClientSmsCache.addToast("Roaming request", packet.message().isBlank()
                    ? "New roaming proposal received." : packet.message());
        }
    }

    private static void handleClientRoamingAgreements(RoamingAgreementSyncPacket packet) {
        ClientRoamingCache.updateAgreements(packet.networkId(), packet.agreements());
        if (net.minecraft.client.Minecraft.getInstance().screen
                instanceof com.example.mobilecellnetwork.gui.ServerBlockScreen screen
                && screen.isRoamingTabSelected()) {
            screen.refreshRoamingTab();
        } else {
            ClientSmsCache.addToast("Roaming", "Roaming agreements updated.");
        }
    }

    private static void requestPhoneData(net.minecraft.world.entity.player.Player sender) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        sendPhoneData(player);
    }

    private static void sendPhoneData(ServerPlayer player) {
        if (!PhoneDeviceService.isSessionUnlocked(player) || PhoneDeviceService.isAllLocked(player)) {
            return;
        }
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        PacketDistributor.sendToPlayer(player, new FlashlightPacket(data.isFlashlightOn()));
        PlanUsage plan = player.getData(ModCapabilities.PLAN_USAGE);
        java.util.List<PhoneDataSyncPacket.ContactEntry> contacts = data.getContacts().entrySet().stream()
                .map(entry -> new PhoneDataSyncPacket.ContactEntry(entry.getKey(), entry.getValue()))
                .toList();
        java.util.List<String> notifications = data.getNotifications().stream()
                .filter(notification -> !notification.read())
                .map(notification -> notification.title() + ": " + notification.message())
                .toList();
        java.util.List<PhoneDataSyncPacket.OnlinePlayer> online = player.server.getPlayerList().getPlayers().stream()
                .filter(other -> !other.getUUID().equals(player.getUUID()))
                .map(other -> new PhoneDataSyncPacket.OnlinePlayer(other.getUUID(), other.getGameProfile().getName(),
                        other.getData(ModCapabilities.PHONE_DATA).getPhoneNumber()))
                .toList();
        java.util.List<PhoneDataSyncPacket.FollowedPlayer> followed = data.getFollowedPlayers().stream()
                .map(id -> {
                    var profile = player.server.getProfileCache().get(id);
                    return new PhoneDataSyncPacket.FollowedPlayer(id,
                            profile.map(com.mojang.authlib.GameProfile::getName).orElse(id.toString()));
                }).toList();
        java.util.List<PhoneDataSyncPacket.AppNotification> appNotificationSettings =
                data.getAppNotifications().entrySet().stream()
                        .map(entry -> new PhoneDataSyncPacket.AppNotification(entry.getKey(), entry.getValue()))
                        .toList();
        PacketDistributor.sendToPlayer(player, new PhoneDataSyncPacket(
                data.getPhoneNumber(), plan.getActivePlanId(), data.isLowPowerMode(), data.isDead(),
                data.isRingtoneEnabled(), data.isNotificationSoundsEnabled(), data.isDoNotDisturb(),
                data.getRingtoneSound(), data.getTheme(), contacts, notifications, online,
                data.getInstalledApps().stream().toList(), followed, appNotificationSettings));
        PacketDistributor.sendToPlayer(player, new PhoneStyleSyncPacket(data.getPhoneStyle()));
        PhoneDeviceService.syncBankView(player);
        PacketDistributor.sendToPlayer(player, SmsDataSyncPacket.from(data));
        PacketDistributor.sendToPlayer(player, new CallLogSyncPacket(java.util.List.copyOf(data.getCallLog())));
        syncChats(player);
        syncPlanData(player);
        PacketDistributor.sendToPlayer(player, new LowPowerStatePacket(data.isLowPowerMode()));
        if (data.isDead()) {
            PacketDistributor.sendToPlayer(player, new PhoneDiedPacket());
        }
    }

    public static void syncBankAccount(ServerPlayer player) {
        var account = player.getData(com.example.mobilecellnetwork.capabilities.ModCapabilities.BANK_ACCOUNT);
        PhoneDeviceService.refreshOwnedBankBalances(player, account.getBalance());
        if (!PhoneDeviceService.isSessionUnlocked(player)
                || PhoneDeviceService.isAllLocked(player) || PhoneDeviceService.isBankLocked(player)) {
            PacketDistributor.sendToPlayer(player, new BankAccountSyncPacket(0L, List.of()));
            return;
        }
        if (PhoneDeviceService.activeDevice(player) != null
                && !PhoneDeviceService.activeOwner(player).equals(player.getUUID())) {
            PhoneDeviceService.syncBankView(player);
            return;
        }
        java.util.List<BankAccountSyncPacket.Transaction> transactions = account.getTransactionHistory().stream()
                .map(transaction -> new BankAccountSyncPacket.Transaction(
                        transaction.amount(), transaction.reason(), transaction.timestamp(), transaction.details()))
                .toList();
        PacketDistributor.sendToPlayer(player,
                new BankAccountSyncPacket(account.getBalance(), transactions));
    }

    private static void handleCall(net.minecraft.world.entity.player.Player sender, CallRequestPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        if ("911".equals(packet.number())) {
            player.displayClientMessage(Component.literal("Emergency calling is not implemented yet."), false);
            return;
        }
        CallManager.startCall(player, packet.number());
    }

    private static void handleCallControl(
            net.minecraft.world.entity.player.Player sender, CallControlPacket packet) {
        if (sender instanceof ServerPlayer player) {
            if (PhoneDeviceService.isAllLocked(player)
                    || (PhoneDeviceService.activePhoneId(player) != null
                    && !PhoneDeviceService.isSessionUnlocked(player))) {
                notifyPlayer(player, "This phone has been locked by its owner.");
                return;
            }
            if (packet.action() == CallControlPacket.Action.TOGGLE_RINGTONE) {
                if (!hasPhoneMenu(player)) {
                    return;
                }
                var data = player.getData(ModCapabilities.PHONE_DATA);
                data.setRingtoneEnabled(packet.muted());
                sendPhoneData(player);
            } else {
                CallManager.handleControl(player, packet);
            }
        }
    }

    private static void handleContact(net.minecraft.world.entity.player.Player sender, ContactActionPacket packet) {
        if (!(sender instanceof ServerPlayer player)
                || !hasPhoneMenu(player)
                || packet.name().isBlank()
                || packet.name().length() > 64
                || !packet.number().matches("[0-9]{1,9}")) {
            return;
        }
        player.getData(ModCapabilities.PHONE_DATA).getContacts().put(packet.name(), packet.number());
        sendPhoneData(player);
    }

    private static void handleContactsAction(net.minecraft.world.entity.player.Player sender,
                                                 ContactsActionPacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
                return;
            }
            PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
            if ("delete".equals(packet.action())) {
                data.getContacts().remove(packet.previousName());
            } else if (validContact(packet.name(), packet.number())) {
                if ("edit".equals(packet.action())) {
                    data.getContacts().remove(packet.previousName());
                } else if (!"add".equals(packet.action())) {
                    return;
                }
                data.getContacts().put(packet.name(), packet.number());
            } else {
                notifyPlayer(player, "Enter a valid contact name and phone number.");
                return;
            }
            sendPhoneData(player);
        }

    private static boolean validContact(String name, String number) {
        return name != null && !name.isBlank() && name.length() <= 64
                && number != null && number.matches("[0-9]{1,9}");
    }

    private static void handleAppInstall(net.minecraft.world.entity.player.Player sender, AppInstallPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        String appId = packet.appId();
        java.util.Map<String, Long> prices = java.util.Map.of(
                "map", 0L, "socials", 250L, "bank", 0L, "plan", 0L, "ringtones", 100L);
        Long price = prices.get(appId);
        if (price == null) {
            return;
        }
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        if (data.isDead() || data.isLowPowerMode()) {
            notifyPlayer(player, data.isDead() ? "Phone is dead." : "Unavailable in Low Power Mode.");
            return;
        }
        if (data.getInstalledApps().contains(appId)) {
            notifyPlayer(player, "App already installed.");
            sendPhoneData(player);
            return;
        }
        var account = player.getData(com.example.mobilecellnetwork.capabilities.ModCapabilities.BANK_ACCOUNT);
        if (account.getBalance() < price) {
            notifyPlayer(player, "Insufficient funds.");
            return;
        }
        if (price > 0) {
            account.setBalance(account.getBalance() - price);
            account.getTransactionHistory().add(new com.example.mobilecellnetwork.capabilities.BankAccount.Transaction(
                    -price, "App purchase: " + appId, player.serverLevel().getGameTime()));
            syncBankAccount(player);
        }
        data.getInstalledApps().add(appId);
        sendPhoneData(player);
    }

    private static void handleBankTransfer(net.minecraft.world.entity.player.Player sender,
                                           BankTransferPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)
                || !player.getData(ModCapabilities.PHONE_DATA).canUseApp("bank")
                || PhoneDeviceService.isBankLocked(player)
                || !PhoneDeviceService.activeOwner(player).equals(player.getUUID())
                || player.getData(ModCapabilities.PHONE_DATA).isDead()
                || CoverageEngine.getStrongestAntennaFor(player) == null
                || packet.amount() <= 0 || !packet.recipientNumber().matches("[0-9]{1,9}")) {
            if (sender instanceof ServerPlayer player && PhoneDeviceService.isBankLocked(player)) {
                notifyPlayer(player, "This app has been remotely locked.");
            } else if (sender instanceof ServerPlayer player
                    && !PhoneDeviceService.activeOwner(player).equals(player.getUUID())) {
                notifyPlayer(player, "This phone's bank account is read-only.");
            }
            return;
        }
        NetworkSavedData saved = NetworkSavedData.get(player.serverLevel().getServer().overworld());
        java.util.UUID recipientId = saved.getUUIDForNumber(packet.recipientNumber());
        if (recipientId == null || recipientId.equals(player.getUUID())) {
            notifyPlayer(player, "Unknown recipient.");
            return;
        }
        ServerPlayer recipient = player.server.getPlayerList().getPlayer(recipientId);
        if (recipient == null) {
            notifyPlayer(player, "Recipient must be online to receive transfers.");
            return;
        }
        var senderAccount = player.getData(com.example.mobilecellnetwork.capabilities.ModCapabilities.BANK_ACCOUNT);
        var recipientAccount =
                recipient.getData(com.example.mobilecellnetwork.capabilities.ModCapabilities.BANK_ACCOUNT);
        if (senderAccount.getBalance() < packet.amount()
                || recipientAccount.getBalance() > Long.MAX_VALUE - packet.amount()) {
            notifyPlayer(player, "Insufficient funds.");
            return;
        }
        senderAccount.setBalance(senderAccount.getBalance() - packet.amount());
        recipientAccount.setBalance(recipientAccount.getBalance() + packet.amount());
        long now = System.currentTimeMillis();
        senderAccount.getTransactionHistory().add(new com.example.mobilecellnetwork.capabilities.BankAccount.Transaction(
                -packet.amount(), "Transfer to " + packet.recipientNumber(), now,
                "Sent to " + recipient.getGameProfile().getName()
                        + " (phone " + packet.recipientNumber() + ")"));
        recipientAccount.getTransactionHistory().add(
                new com.example.mobilecellnetwork.capabilities.BankAccount.Transaction(
                        packet.amount(), "Transfer from " + player.getGameProfile().getName(), now,
                        "Received from " + player.getGameProfile().getName()
                                + " (phone " + saved.getNumberFor(player.getUUID()) + ")"));
        syncBankAccount(player);
        syncBankAccount(recipient);
        notifyPlayer(player, "Transfer sent.");
        notifyPlayer(recipient, "Received " + CurrencyUtil.formatLong(packet.amount()) + ".");
    }

    private static void handleBankingOpen(net.minecraft.world.entity.player.Player sender) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)
                || !player.getData(ModCapabilities.PHONE_DATA).canUseApp("bank")
                || player.getData(ModCapabilities.PHONE_DATA).isDead()
                || CoverageEngine.getStrongestAntennaFor(player) == null) {
            return;
        }
        if (PhoneDeviceService.isBankLocked(player)) {
            notifyPlayer(player, "This app has been remotely locked.");
            return;
        }
        if (!com.example.mobilecellnetwork.plans.DataUsageService.spend(player, 1.0D)) {
            return;
        }
    }

    private static void handleSettingsUpdate(net.minecraft.world.entity.player.Player sender,
                                                 SettingsUpdatePacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)
                    || packet.setting().length() > 32 || packet.value().length() > 128) {
                return;
            }
            PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
            try {
                switch (packet.setting()) {
                    case "ringtone" -> {
                        ResourceLocation soundId = ResourceLocation.parse(packet.value());
                        if (!BuiltInRegistries.SOUND_EVENT.containsKey(soundId)
                                || !packet.value().startsWith("minecraft:")) {
                            return;
                        }
                        if (java.util.Set.of("minecraft:block.note_block.chime", "minecraft:block.note_block.pling")
                                .contains(packet.value())
                                && !data.getInstalledApps().contains("ringtones")) {
                            notifyPlayer(player, "Install Extra Ringtones to use this sound.");
                            return;
                        }
                        data.setRingtoneSound(packet.value());
                    }
                    case "sounds" -> data.setNotificationSoundsEnabled(parseBoolean(packet.value()));
                    case "dnd" -> data.setDoNotDisturb(parseBoolean(packet.value()));
                    case "theme" -> {
                        if (!packet.value().equals("iphone") && !packet.value().equals("samsung")) {
                            return;
                        }
                        data.setTheme(packet.value());
                    }
                    default -> {
                        if (!packet.setting().startsWith("app:")) {
                            return;
                        }
                        String appId = packet.setting().substring(4);
                        if (!java.util.Set.of("sms", "call", "socials", "plan", "bank").contains(appId)) {
                            return;
                        }
                        data.getAppNotifications().put(appId, parseBoolean(packet.value()));
                    }
                }
                sendPhoneData(player);
            } catch (IllegalArgumentException exception) {
                notifyPlayer(player, "Invalid settings value.");
            }
        }

    private static boolean parseBoolean(String value) {
            if (!value.equals("true") && !value.equals("false")) {
                throw new IllegalArgumentException("Expected boolean setting.");
            }
            return Boolean.parseBoolean(value);
        }

    private static boolean hasPhoneMenu(ServerPlayer player) {
            if (!(player.containerMenu instanceof PhoneMenu menu) || !menu.stillValid(player)) {
                return false;
            }
            if (PhoneDeviceService.isAllLocked(player)) {
                notifyPlayer(player, "This phone has been locked by its owner.");
                return false;
            }
            if (!PhoneDeviceService.isSessionUnlocked(player)) {
                return false;
            }
            return true;
        }

        public static void clearClientPhoneData() {
            ClientPhoneCache.clearSensitiveData();
            ClientSmsCache.clearSensitiveData();
            ClientChatCache.clearSensitiveData();
            ClientCallLogCache.clearSensitiveData();
            ClientBankAccountCache.clearSensitiveData();
            ClientCallCache.clearSensitiveData();
            ClientSocialCache.clearSensitiveData();
            ClientSocialProfileCache.clearSensitiveData();
            ClientSocialImageCache.clearSensitiveData();
        }

        private static void handlePhoneUnlock(net.minecraft.world.entity.player.Player sender, UnlockPhonePacket packet) {
            if (!(sender instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof PhoneMenu menu) || !menu.stillValid(player)) {
                return;
            }
            boolean accepted = PhoneDeviceService.tryUnlock(player, packet.pin());
            PacketDistributor.sendToPlayer(player, new PhonePinResultPacket(accepted));
            if (!accepted) {
                notifyPlayer(player, "Incorrect PIN.");
            }
        }

        private static void handleSetPhonePin(net.minecraft.world.entity.player.Player sender, SetPhonePinPacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
                return;
            }
            if (!PhoneDeviceService.setActivePin(player, packet.pin())) {
                notifyPlayer(player, "PIN must contain 4 to 12 digits; only the phone's owner can set it.");
            } else {
                notifyPlayer(player, "Phone PIN updated.");
            }
        }

        private static void handleRemovePhonePin(net.minecraft.world.entity.player.Player sender,
                                                RemovePhonePinPacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
                return;
            }
            if (!PhoneDeviceService.removeActivePin(player, packet.currentPin())) {
                notifyPlayer(player, "PIN removal failed. Check your PIN and phone ownership.");
            } else {
                notifyPlayer(player, "Phone PIN removed.");
            }
        }

        private static void handleSetPhoneLockScreen(net.minecraft.world.entity.player.Player sender,
                                                     SetPhoneLockScreenPacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
                return;
            }
            if (!PhoneDeviceService.setLockScreen(player, packet.enabled())) {
                notifyPlayer(player, "Only the phone's owner can change its lock screen.");
            }
        }

        private static void sendKnownPhoneDevices(net.minecraft.world.entity.player.Player sender) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
                return;
            }
            PacketDistributor.sendToPlayer(player, PhoneDeviceService.knownDevicesPacket(player));
        }

        private static void handleLockPhoneFeature(net.minecraft.world.entity.player.Player sender,
                                                   LockPhoneFeaturePacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
                return;
            }
            if (!PhoneDeviceService.setRemoteLock(player, packet.targetPhoneId(), packet.feature(), packet.lock())) {
                notifyPlayer(player, "Could not change that device lock. Use a phone you own and select one of your devices.");
                return;
            }
            notifyPlayer(player, packet.lock() ? "Device lock applied." : "Device unlocked.");
            sendKnownPhoneDevices(player);
        }

        private static void handleWipePhone(net.minecraft.world.entity.player.Player sender, WipePhonePacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
                return;
            }
            if (!PhoneDeviceService.wipe(player, packet.targetPhoneId())) {
                notifyPlayer(player, "Could not wipe that device. Use a phone you own and select one of your devices.");
                return;
            }
            notifyPlayer(player, "Phone data wiped and device locked.");
            sendKnownPhoneDevices(player);
        }

        private static void handleRemoveKnownPhone(net.minecraft.world.entity.player.Player sender,
                                                   RemoveKnownPhonePacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
                return;
            }
            if (!PhoneDeviceService.removeKnownDevice(player, packet.phoneId())) {
                notifyPlayer(player, "Could not remove that device from your known list.");
            }
            sendKnownPhoneDevices(player);
        }

    private static void setPhoneStyle(net.minecraft.world.entity.player.Player sender, PhoneStyle style) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        data.setPhoneStyle(style);
        PacketDistributor.sendToPlayer(player, new PhoneStyleSyncPacket(data.getPhoneStyle()));
    }

    private static void handleMapOpen(net.minecraft.world.entity.player.Player sender) {
            if (!(sender instanceof ServerPlayer player)) {
                return;
            }
            if (!hasPhoneMenu(player)) {
                setMapLoadState(player, false, "Phone menu is closed.");
                return;
            }
            PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
            PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
            if (!phone.canUseApp("map") || phone.isDead()
                    || CoverageEngine.getStrongestAntennaFor(player) == null) {
                String message = phone.isDead() ? "Phone is dead." : "Map is unavailable.";
                notifyPlayer(player, message);
                setMapLoadState(player, false, message);
                return;
            }
            PlanRegistry.Plan plan = PlanRegistry.getPlan(usage.getActivePlanId());
            if (plan.maxSignalTier().ordinal() > SignalTier.LTE.ordinal()) {
                String message = "Your plan does not support high-speed map data.";
                notifyPlayer(player, message);
                setMapLoadState(player, false, message);
                return;
            }
            double divisor = phone.isLowPowerMode()
                    ? MobileCellNetworkConfig.MAP_RESOLUTION_DIVISOR.get() : 1.0D;
            if (!com.example.mobilecellnetwork.plans.DataUsageService.spend(player, 5.0D / divisor)) {
                setMapLoadState(player, false, "Map data unavailable.");
                return;
            }
            syncMapPoints(player);
            CoverageEngine.sendMapAntennaCache(player, player.blockPosition().getX(),
                    player.blockPosition().getZ(), 1);
            setMapLoadState(player, true, "");
        }

    private static void handleMapAntennaCacheRequest(
            net.minecraft.world.entity.player.Player sender, MapAntennaCacheRequestPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
        if (phone.isDead() || !phone.canUseApp("map")
                || CoverageEngine.getStrongestAntennaFor(player) == null) {
            return;
        }
        long dx = Math.abs((long) packet.centerX() - player.blockPosition().getX());
        long dz = Math.abs((long) packet.centerZ() - player.blockPosition().getZ());
        String dimension = player.serverLevel().dimension().location().toString();
        if (dx > 4096L || dz > 4096L) {
            PacketDistributor.sendToPlayer(player,
                    new AntennaCachePacket(player.getUUID(), dimension, List.of()));
            return;
        }
        CoverageEngine.sendMapAntennaCache(player, packet.centerX(), packet.centerZ(), packet.resolution());
    }

    private static void setMapLoadState(net.minecraft.world.entity.player.Player sender, boolean ready, String message) {
        if (sender instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new MapLoadStatePacket(ready, message));
        }
    }

    private static void handleSocialFeedRequest(net.minecraft.world.entity.player.Player sender, String filter) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
        PacketDistributor.sendToPlayer(player, new SocialProfileSyncPacket(
                phone.getSocialUsername(), phone.getSocialProfileColor(), phone.getSocialAvatar()));
        if (!phone.canUseApp("socials") || phone.isDead()
                || CoverageEngine.getStrongestAntennaFor(player) == null) {
            notifyPlayer(player, phone.isDead() ? "Phone is dead." : "Socials is unavailable.");
            return;
        }
        if (!com.example.mobilecellnetwork.plans.DataUsageService.spend(player, 1.0D)) {
            return;
        }
        sendSocialFeed(player, filter);
        }

    private static void handleSocialProfile(net.minecraft.world.entity.player.Player sender,
                                            SocialProfilePacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        String username = packet.username().strip();
        if (username.length() < 2 || username.length() > 24
                || !username.matches("[\\p{L}\\p{N}_ -]+")) {
            notifyPlayer(player, "Choose a username with 2-24 letters, numbers, spaces, _ or -.");
            return;
        }
        byte[] avatar = packet.avatar();
        if (avatar.length > SocialProfilePacket.MAX_AVATAR_BYTES
                || !isValidSocialImage(avatar)) {
            notifyPlayer(player, "Profile photos must be valid PNG/JPEG images no larger than 1 MiB.");
            return;
        }
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        data.setSocialUsername(username);
        data.setSocialProfileColor(packet.color());
        data.setSocialAvatar(avatar);
        sendPhoneData(player);
        PacketDistributor.sendToPlayer(player, new SocialProfileSyncPacket(
                data.getSocialUsername(), data.getSocialProfileColor(), data.getSocialAvatar()));
    }

    private static void handleSocialImageRequest(net.minecraft.world.entity.player.Player sender,
                                                 SocialImageRequestPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)
                || !isUuid(packet.postId())
                || !player.getData(ModCapabilities.PHONE_DATA).canUseApp("socials")) {
            return;
        }
        NetworkSavedData data = NetworkSavedData.get(player.serverLevel().getServer().overworld());
        NetworkSavedData.SocialPost post = data.getSocialPost(UUID.fromString(packet.postId()));
        if (post != null && post.image().length > 0) {
            PacketDistributor.sendToPlayer(player,
                    new SocialImagePacket(packet.postId(), post.image()));
        }
    }

    private static boolean isValidSocialImage(byte[] image) {
        if (image.length == 0) {
            return true;
        }
        try (javax.imageio.stream.ImageInputStream input = javax.imageio.ImageIO.createImageInputStream(
                new java.io.ByteArrayInputStream(image))) {
            if (input == null) {
                return false;
            }
            var readers = javax.imageio.ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return false;
            }
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                return width > 0 && height > 0 && (long) width * height <= 16_000_000L;
            } finally {
                reader.dispose();
            }
        } catch (java.io.IOException exception) {
            return false;
        }
    }

    private static void handleSocialPost(net.minecraft.world.entity.player.Player sender, SocialPostPacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)
                    || packet.text().isBlank() && packet.image().length == 0) {
                return;
            }
            PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
            if (!phone.canUseApp("socials") || phone.isDead()
                    || CoverageEngine.getStrongestAntennaFor(player) == null) {
                notifyPlayer(player, "Socials is unavailable.");
                return;
            }
            NetworkSavedData data = NetworkSavedData.get(player.serverLevel().getServer().overworld());
            if ("comment".equals(packet.action())) {
                if (packet.text().isBlank() || packet.text().length() > 140
                        || packet.image().length > 0 || !isUuid(packet.postId())) {
                    notifyPlayer(player, "Comment must be 1-140 characters.");
                    return;
                }
                data.addSocialComment(java.util.UUID.fromString(packet.postId()),
                        new NetworkSavedData.SocialComment(player.getUUID(), packet.text(),
                                player.serverLevel().getGameTime()));
            } else if ("post".equals(packet.action())) {
                byte[] image = packet.image();
                if (packet.text().length() > 280 || packet.attachment().length() > 128
                        || image.length > SocialPostPacket.MAX_IMAGE_BYTES || !isValidSocialImage(image)
                        || (!packet.attachment().isEmpty()
                        && (!packet.attachment().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                        || !BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(packet.attachment()))))) {
                    notifyPlayer(player, "Post is invalid or exceeds 280 characters.");
                    return;
                }
                if (!com.example.mobilecellnetwork.plans.DataUsageService.spend(player, 1.0D)) {
                    return;
                }
                String author = phone.getSocialUsername().isBlank()
                        ? player.getGameProfile().getName() : phone.getSocialUsername();
                data.addSocialPost(new NetworkSavedData.SocialPost(java.util.UUID.randomUUID(), player.getUUID(),
                        author, packet.text(), packet.attachment(), System.currentTimeMillis(),
                        java.util.List.of(), java.util.List.of(), image));
            } else {
                return;
            }
            sendSocialFeed(player, "ALL");
        }

    private static void handleSocialLike(net.minecraft.world.entity.player.Player sender, SocialLikePacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player) || !isUuid(packet.postId())
                    || !player.getData(ModCapabilities.PHONE_DATA).canUseApp("socials")
                    || player.getData(ModCapabilities.PHONE_DATA).isDead()
                    || CoverageEngine.getStrongestAntennaFor(player) == null) {
                return;
            }
            NetworkSavedData.get(player.serverLevel().getServer().overworld())
                    .toggleSocialLike(java.util.UUID.fromString(packet.postId()), player.getUUID());
            sendSocialFeed(player, packet.filter());
        }

    private static void handleSocialFollow(net.minecraft.world.entity.player.Player sender, SocialFollowPacket packet) {
            if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player) || !isUuid(packet.playerId())
                    || !player.getData(ModCapabilities.PHONE_DATA).canUseApp("socials")
                    || player.getData(ModCapabilities.PHONE_DATA).isDead()) {
                return;
            }
            java.util.UUID target = java.util.UUID.fromString(packet.playerId());
            if (target.equals(player.getUUID())) {
                return;
            }
            java.util.Set<java.util.UUID> follows = player.getData(ModCapabilities.PHONE_DATA).getFollowedPlayers();
            if (!follows.remove(target)) {
                follows.add(target);
            }
            sendPhoneData(player);
        }

    private static boolean isUuid(String value) {
            try {
                java.util.UUID.fromString(value);
                return true;
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }

    private static void sendSocialFeed(ServerPlayer player) {
        sendSocialFeed(player, "ALL");
    }

    private static void sendSocialFeed(ServerPlayer player, String filter) {
            NetworkSavedData data = NetworkSavedData.get(player.serverLevel().getServer().overworld());
            String selectedFilter = switch (filter == null ? "" : filter.toUpperCase(java.util.Locale.ROOT)) {
                case "FOLLOWING" -> "FOLLOWING";
                case "MINE" -> "MINE";
                default -> "ALL";
            };
            PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
            java.util.List<SocialFeedPacket.PostEntry> posts = data.getSocialPosts().stream()
                    .filter(post -> switch (selectedFilter) {
                        case "FOLLOWING" -> phone.getFollowedPlayers().contains(post.author());
                        case "MINE" -> post.author().equals(player.getUUID());
                        default -> true;
                    })
                    .sorted(java.util.Comparator.comparingLong(NetworkSavedData.SocialPost::createdAt).reversed())
                    .limit(100)
                    .map(post -> {
                        String author = post.authorName().isBlank()
                                ? player.server.getProfileCache().get(post.author())
                                .map(com.mojang.authlib.GameProfile::getName).orElse("Unknown")
                                : post.authorName();
                        java.util.List<String> comments = post.comments().stream().map(comment -> {
                            String name = player.server.getProfileCache().get(comment.author())
                                    .map(com.mojang.authlib.GameProfile::getName).orElse("Unknown");
                            return name + ": " + comment.text();
                        }).toList();
                        return new SocialFeedPacket.PostEntry(post.id().toString(), post.author().toString(),
                                author, post.text(), post.attachment(), post.hasImage(),
                                post.createdAt(), post.likes().size(),
                                post.likes().contains(player.getUUID()), comments);
                    }).toList();
            PacketDistributor.sendToPlayer(player, new SocialFeedPacket(posts));
        }

    private static void handleSettings(
            net.minecraft.world.entity.player.Player sender, SetLowPowerPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        var phoneData = player.getData(ModCapabilities.PHONE_DATA);
        if (packet.enabled() && !MobileCellNetworkConfig.LOW_POWER_ENABLED.get()) {
            notifyPlayer(player, "Low Power Mode is disabled on this server.");
            PacketDistributor.sendToPlayer(player, new LowPowerStatePacket(phoneData.isLowPowerMode()));
            return;
        }
        if (packet.enabled() && phoneData.isDead()) {
            notifyPlayer(player, "Cannot enable: phone is dead");
            PacketDistributor.sendToPlayer(player, new LowPowerStatePacket(false));
            return;
        }
        phoneData.setLowPowerMode(packet.enabled());
        if (packet.enabled() && MobileCellNetworkConfig.LOW_POWER_DROP_ACTIVE_CALL.get()) {
            CallManager.endForLowPower(player);
        }
        PacketDistributor.sendToPlayer(player, new LowPowerStatePacket(phoneData.isLowPowerMode()));
        sendPhoneData(player);
    }

    private static void dismissLowPowerPrompt(net.minecraft.world.entity.player.Player sender) {
        if (sender instanceof ServerPlayer player && hasPhoneMenu(player)) {
            player.getData(ModCapabilities.PHONE_DATA).setLowPowerPromptShownThisSession(true);
        }
    }

    private static void handleMapInteraction(
            net.minecraft.world.entity.player.Player sender, MapInteractionPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        if (phone.isDead() || !phone.canUseApp("map")
                || CoverageEngine.getStrongestAntennaFor(player) == null
                || PlanRegistry.getPlan(usage.getActivePlanId()).maxSignalTier().ordinal() > SignalTier.LTE.ordinal()) {
            notifyPlayer(player, "No Service.");
            return;
        }
        int resolution = Math.clamp(packet.resolution(), 1, 4);
        double lowPowerDivisor = phone.isLowPowerMode()
                ? MobileCellNetworkConfig.MAP_RESOLUTION_DIVISOR.get() : 1.0D;
        double resolutionFactor = 1.0D / (resolution * resolution);
        if (!com.example.mobilecellnetwork.plans.DataUsageService.spend(player,
                4.0D * resolutionFactor / lowPowerDivisor)) {
            return;
        }
    }

    private static void handleMapPointAction(
            net.minecraft.world.entity.player.Player sender, MapPointActionPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
        if (phone.isDead() || !phone.canUseApp("map")
                || CoverageEngine.getStrongestAntennaFor(player) == null) {
            notifyPlayer(player, "Map is unavailable.");
            return;
        }
        NetworkSavedData saved = NetworkSavedData.get(player.server.overworld());
        if ("create".equals(packet.action())) {
            String name = packet.name().trim();
            BlockPos pointPosition = new BlockPos(packet.x(), packet.y(), packet.z());
            if (name.isEmpty() || name.length() > 64 || name.contains("|")
                    || Math.abs((long) packet.x() - player.getX()) > 64
                    || Math.abs((long) packet.z() - player.getZ()) > 64
                    || packet.y() < player.serverLevel().getMinBuildHeight()
                    || packet.y() >= player.serverLevel().getMaxBuildHeight()
                    || !player.serverLevel().hasChunkAt(pointPosition)) {
                notifyPlayer(player, "Invalid map point.");
                return;
            }
            MapPointOfInterest.Visibility visibility;
            try {
                visibility = MapPointOfInterest.Visibility.valueOf(packet.visibility());
            } catch (IllegalArgumentException exception) {
                notifyPlayer(player, "Invalid map point visibility.");
                return;
            }
            MapPointOfInterest point = new MapPointOfInterest(UUID.randomUUID(), name,
                    player.serverLevel().dimension().location().toString(), packet.x(), packet.y(), packet.z(),
                    player.getUUID(), player.getGameProfile().getName(), visibility);
            if (visibility == MapPointOfInterest.Visibility.PUBLIC) {
                if (!saved.addPublicMapPoint(point)) {
                    notifyPlayer(player, "The public map point limit has been reached.");
                    return;
                }
            } else {
                if (phone.getMapPoints().size() >= 100) {
                    notifyPlayer(player, "You can keep up to 100 personal map points.");
                    return;
                }
                phone.getMapPoints().add(point);
            }
            notifyPlayer(player, "Map point added.");
            syncMapPoints(player);
            return;
        }
        if ("delete".equals(packet.action())) {
            UUID pointId;
            try {
                pointId = UUID.fromString(packet.pointId());
            } catch (IllegalArgumentException exception) {
                return;
            }
            boolean removed = phone.getMapPoints().removeIf(point ->
                    point.id().equals(pointId) && player.getUUID().equals(point.ownerUUID()));
            removed |= saved.removePublicMapPoint(pointId, player.getUUID());
            if (removed) {
                syncMapPoints(player);
            }
            return;
        }
        if ("share".equals(packet.action())) {
            UUID pointId;
            try {
                pointId = UUID.fromString(packet.pointId());
            } catch (IllegalArgumentException exception) {
                notifyPlayer(player, "Select a valid private point to share.");
                return;
            }
            MapPointOfInterest point = phone.getMapPoints().stream()
                    .filter(candidate -> candidate.id().equals(pointId)
                            && player.getUUID().equals(candidate.ownerUUID())
                            && candidate.visibility() != MapPointOfInterest.Visibility.PUBLIC)
                    .findFirst().orElse(null);
            if (point == null || !packet.recipientNumber().matches("[0-9]{1,9}")) {
                notifyPlayer(player, "Select a private point and enter a valid phone number.");
                return;
            }
            String share = MapPointShare.encode(point);
            if (share.length() > MobileCellNetworkConfig.SMS_MAX_LENGTH.get()) {
                notifyPlayer(player, "This map point share does not fit in one text.");
                return;
            }
            handleSms(player, new SmsSendPacket(packet.recipientNumber(), share));
        }
    }

    private static void syncMapPoints(ServerPlayer player) {
        if (!PhoneDeviceService.isSessionUnlocked(player)) {
            return;
        }
        PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
        List<MapPointOfInterest> points = new ArrayList<>();
        points.addAll(phone.getMapPoints());
        points.addAll(NetworkSavedData.get(player.server.overworld()).getPublicMapPoints());
        for (PhoneData.Message message : phone.getInbox()) {
            MapPointOfInterest shared = MapPointShare.decode(message.body(), message.senderNumber());
            if (shared != null) {
                points.add(shared);
            }
        }
        PacketDistributor.sendToPlayer(player, new MapPointSyncPacket(points.stream().limit(512).toList()));
    }

    private static void syncChats(ServerPlayer player) {
        if (!PhoneDeviceService.isSessionUnlocked(player)) {
            return;
        }
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        migrateLegacyMessages(data);
        ensureEmergencyChat(data);
        PacketDistributor.sendToPlayer(player, new ChatListSyncPacket(data.getChats()));
    }

    private static void ensureEmergencyChat(PhoneData phone) {
        Chat emergency = phone.getChats().stream()
                .filter(chat -> EmergencyMessages.CHAT_ID.equals(chat.id()))
                .findFirst().orElse(null);
        if (emergency == null) {
            emergency = new Chat(EmergencyMessages.CHAT_ID, ChatType.EMERGENCY, "Emergency",
                    List.of(phone.getPhoneNumber()), List.of(), 0L, true);
            phone.getChats().add(emergency);
        } else {
            emergency.setPinnedToTop(true);
        }
    }

    private static boolean isEmergencyChatId(String value) {
        try {
            return EmergencyMessages.CHAT_ID.equals(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void handleEmergencyMessage(ServerPlayer player, String body) {
        int maxLength = MobileCellNetworkConfig.EMERGENCY_MESSAGE_MAX_LENGTH.get();
        if (body.isBlank() || body.length() > maxLength) {
            notifyPlayer(player, "Emergency message must be 1-" + maxLength + " characters.");
            return;
        }
        PhoneData phone = player.getData(ModCapabilities.PHONE_DATA);
        if (phone.isDead() && !MobileCellNetworkConfig.DEAD_PHONE_ALLOWS_EMERGENCY.get()) {
            notifyPlayer(player, "Emergency messaging is unavailable while the phone is dead.");
            return;
        }
        long timestamp = System.currentTimeMillis();
        BlockPos position = player.blockPosition();
        String senderNumber = phone.getPhoneNumber();
        NetworkSavedData saved = NetworkSavedData.get(player.server.overworld());
        saved.addEmergencyMessage(new NetworkSavedData.EmergencyMessage(
                UUID.randomUUID(), player.getUUID(), player.getGameProfile().getName(), senderNumber,
                body, player.serverLevel().dimension().location().toString(),
                position.getX(), position.getY(), position.getZ(), timestamp));
        ensureEmergencyChat(phone);
        Chat emergency = phone.getChats().stream()
                .filter(chat -> EmergencyMessages.CHAT_ID.equals(chat.id())).findFirst().orElseThrow();
        emergency.addMessage(new Message(senderNumber, body, timestamp, true));
        syncChats(player);
        notifyPlayer(player, "Emergency message sent to dispatch.");
        for (ServerPlayer admin : player.server.getPlayerList().getPlayers()) {
            if (admin.hasPermissions(2)) {
                PacketDistributor.sendToPlayer(admin, new PhoneNotificationPacket(
                        "EMERGENCY", "New emergency message",
                        "Message from " + player.getGameProfile().getName(), false));
            }
            if (admin.containerMenu instanceof EmergencyDispatchMenu menu && menu.stillValid(admin)) {
                sendEmergencyDispatchData(admin);
            }
        }
    }

    public static void sendEmergencyDispatchData(net.minecraft.world.entity.player.Player sender) {
        if (!(sender instanceof ServerPlayer admin)
                || !(admin.containerMenu instanceof EmergencyDispatchMenu menu)
                || !menu.stillValid(admin)) {
            return;
        }
        List<EmergencyDispatchDataPacket.Entry> entries = NetworkSavedData.get(admin.server.overworld())
                .getEmergencyMessages().stream()
                .map(message -> new EmergencyDispatchDataPacket.Entry(message.id(), message.senderId(),
                        message.senderName(), message.senderNumber(), message.body(), message.dimension(),
                        message.x(), message.y(), message.z(), message.timestamp()))
                .toList();
        PacketDistributor.sendToPlayer(admin, new EmergencyDispatchDataPacket(entries));
    }

    private static void migrateLegacyMessages(PhoneData data) {
        for (PhoneData.Message legacy : java.util.stream.Stream.concat(
                data.getInbox().stream(), data.getSent().stream()).toList()) {
            boolean incoming = legacy.recipientNumber().equals(data.getPhoneNumber());
            String peer = incoming ? legacy.senderNumber() : legacy.recipientNumber();
            if (peer.isBlank() || peer.equals(data.getPhoneNumber())) {
                continue;
            }
            List<String> members = java.util.stream.Stream.of(data.getPhoneNumber(), peer).sorted().toList();
            UUID chatId = UUID.nameUUIDFromBytes(("mobilecellnetwork:direct:"
                    + String.join(":", members)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Chat chat = data.getChats().stream().filter(entry -> entry.id().equals(chatId))
                    .findFirst().orElseGet(() -> {
                        Chat created = new Chat(chatId, ChatType.DIRECT, peer, members);
                        data.getChats().add(created);
                        return created;
                    });
            UUID messageId = UUID.nameUUIDFromBytes((chatId + ":" + legacy.timestamp() + ":"
                    + legacy.senderNumber() + ":" + legacy.body()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (chat.messages().stream().noneMatch(message -> message.id().equals(messageId))) {
                chat.addMessage(new Message(messageId, legacy.senderNumber(), legacy.body(),
                        legacy.timestamp(), legacy.read()));
            }
        }
    }

    private static void startChat(net.minecraft.world.entity.player.Player sender, StartChatPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        if (!packet.recipientNumber().matches("[0-9]{1,9}")) {
            notifyPlayer(player, "Enter a valid phone number.");
            return;
        }
        PhoneData senderData = player.getData(ModCapabilities.PHONE_DATA);
        NetworkSavedData saved = NetworkSavedData.get(player.server.overworld());
        UUID recipientId = saved.getUUIDForNumber(packet.recipientNumber());
        if (recipientId == null || recipientId.equals(player.getUUID())) {
            notifyPlayer(player, "Unknown recipient.");
            return;
        }
        Chat chat = senderData.getChats().stream()
                .filter(entry -> entry.type() == ChatType.DIRECT
                        && entry.memberNumbers().contains(senderData.getPhoneNumber())
                        && entry.memberNumbers().contains(packet.recipientNumber()))
                .findFirst().orElse(null);
        if (chat == null) {
            chat = new Chat(UUID.randomUUID(), ChatType.DIRECT, packet.recipientNumber(),
                    List.of(senderData.getPhoneNumber(), packet.recipientNumber()));
            senderData.getChats().add(chat);
        }
        UUID directChatId = chat.id();
        syncChats(player);
        ServerPlayer recipient = player.server.getPlayerList().getPlayer(recipientId);
        if (recipient != null) {
            PhoneData recipientData = recipient.getData(ModCapabilities.PHONE_DATA);
            if (recipientData.getChats().stream().noneMatch(entry -> entry.id().equals(directChatId))) {
                recipientData.getChats().add(new Chat(directChatId, ChatType.DIRECT,
                        senderData.getPhoneNumber(),
                        List.of(senderData.getPhoneNumber(), packet.recipientNumber())));
            }
            syncChats(recipient);
        }
    }

    private static void createGroupChat(net.minecraft.world.entity.player.Player sender,
                                        CreateGroupChatPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        if (packet.groupName().isBlank() || packet.groupName().length() > 32
                || packet.memberNumbers().stream().distinct().count() < 2
                || packet.memberNumbers().stream().anyMatch(number -> !number.matches("[0-9]{1,9}"))) {
            notifyPlayer(player, "Enter a group name and select at least two valid members.");
            return;
        }
        PhoneData ownerData = player.getData(ModCapabilities.PHONE_DATA);
        List<String> numbers = new ArrayList<>(packet.memberNumbers().stream().distinct().toList());
        if (!numbers.contains(ownerData.getPhoneNumber())) {
            numbers.add(ownerData.getPhoneNumber());
        }
        if (numbers.size() > 64) {
            notifyPlayer(player, "A group can have at most 64 members.");
            return;
        }
        NetworkSavedData saved = NetworkSavedData.get(player.server.overworld());
        List<ServerPlayer> members = new ArrayList<>();
        for (String number : numbers) {
            UUID id = saved.getUUIDForNumber(number);
            ServerPlayer member = id == null ? null : player.server.getPlayerList().getPlayer(id);
            if (member != null && !members.contains(member)) {
                members.add(member);
            }
        }
        if (!members.contains(player)) {
            members.add(player);
        }
        UUID chatId = UUID.randomUUID();
        for (ServerPlayer member : members) {
            member.getData(ModCapabilities.PHONE_DATA).getChats().add(
                    new Chat(chatId, ChatType.GROUP, packet.groupName(), numbers));
            syncChats(member);
        }
    }

    private static void markChatRead(net.minecraft.world.entity.player.Player sender, MarkChatReadPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        Chat chat = data.getChats().stream().filter(entry -> entry.id().equals(packet.chatId()))
                .findFirst().orElse(null);
        if (chat == null) {
            return;
        }
        chat.setLastReadTimestamp(System.currentTimeMillis());
        for (int index = 0; index < chat.messages().size(); index++) {
            Message message = chat.messages().get(index);
            if (!"system".equals(message.senderNumber()) && !message.read()) {
                chat.messages().set(index, message.markRead());
            }
        }
        syncChats(player);
    }

    private static void handleChatAction(net.minecraft.world.entity.player.Player sender, ChatActionPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        Chat chat = data.getChats().stream().filter(entry -> entry.id().equals(packet.chatId()))
                .findFirst().orElse(null);
        if (chat == null || chat.type() == ChatType.GLOBAL || chat.type() == ChatType.EMERGENCY) {
            return;
        }
        switch (packet.action()) {
            case "delete", "leave" -> data.getChats().remove(chat);
            case "rename" -> {
                if (chat.type() != ChatType.GROUP || packet.value().isBlank() || packet.value().length() > 32) {
                    return;
                }
                chat.setDisplayName(packet.value());
            }
            default -> {
                return;
            }
        }
        syncChats(player);
    }

    private static void handleChatSms(net.minecraft.world.entity.player.Player sender, SmsSendPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        if (isEmergencyChatId(packet.chatId())) {
            handleEmergencyMessage(player, packet.body().strip());
            return;
        }
        if (packet.body().isBlank() || packet.body().length() > MobileCellNetworkConfig.SMS_MAX_LENGTH.get()) {
            notifyPlayer(player, "Invalid message.");
            return;
        }
        PhoneData senderData = player.getData(ModCapabilities.PHONE_DATA);
        if (senderData.isDead()
                || CoverageEngine.updateSignal(player, CoverageEngine.getStrongestAntennaFor(player)).tier()
                == SignalTier.NO_SERVICE) {
            notifyPlayer(player, "No Service.");
            return;
        }
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        if (!usage.isCycleActive()) {
            notifyPlayer(player, "Your plan is suspended.");
            return;
        }
        PlanRegistry.Plan plan = PlanRegistry.getPlan(usage.getActivePlanId());
        int maximumTexts = plan.textMessages();
        if (!plan.allowOverage() && maximumTexts >= 0 && usage.getTextsUsed() >= maximumTexts) {
            notifyPlayer(player, "Plan limit reached");
            return;
        }
        UUID chatId;
        try {
            chatId = UUID.fromString(packet.chatId());
        } catch (IllegalArgumentException exception) {
            notifyPlayer(player, "Invalid chat.");
            return;
        }
        Chat senderChat = senderData.getChats().stream().filter(chat -> chat.id().equals(chatId))
                .findFirst().orElse(null);
        boolean global = new UUID(0L, 0L).equals(chatId);
        if (!global && (senderChat == null || !senderChat.memberNumbers().contains(senderData.getPhoneNumber()))) {
            notifyPlayer(player, "Chat not found.");
            return;
        }
        if (!chargeTextUsage(player, senderData, usage, plan)) {
            return;
        }
        if (global && senderChat == null) {
            senderChat = new Chat(chatId, ChatType.GLOBAL, "Global", List.of());
            senderData.getChats().add(senderChat);
        }
        long now = System.currentTimeMillis();
        Message message = new Message(senderData.getPhoneNumber(), packet.body(), now, false);
        if (senderChat != null) {
            senderChat.addMessage(new Message(message.id(), message.senderNumber(), message.body(),
                    message.timestamp(), true));
        }
        List<ServerPlayer> recipients;
        if (global) {
            recipients = new ArrayList<>(player.server.getPlayerList().getPlayers());
        } else {
            NetworkSavedData saved = NetworkSavedData.get(player.server.overworld());
            recipients = senderChat.memberNumbers().stream()
                    .filter(number -> !number.equals(senderData.getPhoneNumber()))
                    .map(saved::getUUIDForNumber)
                    .filter(java.util.Objects::nonNull)
                    .map(player.server.getPlayerList()::getPlayer)
                    .filter(java.util.Objects::nonNull)
                    .distinct().toList();
        }
        for (ServerPlayer recipient : recipients) {
            if (recipient.getUUID().equals(player.getUUID())) {
                continue;
            }
            PhoneData receiverData = recipient.getData(ModCapabilities.PHONE_DATA);
            Chat receiverChat = receiverData.getChats().stream().filter(chat -> chat.id().equals(chatId))
                    .findFirst().orElse(null);
            if (receiverChat == null) {
                receiverChat = global
                        ? new Chat(chatId, ChatType.GLOBAL, "Global", List.of())
                        : new Chat(chatId, senderChat.type(), senderChat.displayName(), senderChat.memberNumbers());
                receiverData.getChats().add(receiverChat);
            }
            receiverChat.addMessage(message);
            if (PhoneDeviceService.isSessionUnlocked(recipient)) {
                PacketDistributor.sendToPlayer(recipient, new SmsReceivePacket(chatId, message));
            }
            syncChats(recipient);
        }
        usage.setTextsUsed(usage.getTextsUsed() + 1);
        syncChats(player);
        syncPlanData(player);
    }

    private static boolean chargeTextUsage(ServerPlayer player, PhoneData phone, PlanUsage usage,
                                            PlanRegistry.Plan plan) {
        var connectionType = com.example.mobilecellnetwork.billing.BillingManager
                .getConnectionType(player, plan);
        var agreement = com.example.mobilecellnetwork.billing.BillingManager
                .getRoamingAgreement(player, plan);
        long charges = com.example.mobilecellnetwork.billing.BillingManager.computeCharges(
                phone, usage, 0L, 1L, 0L, connectionType, agreement, plan);
        long roamingCharges = com.example.mobilecellnetwork.billing.BillingManager.computeRoamingCharges(
                0L, 1L, 0L, connectionType, agreement, plan);
        String hostNetworkId = com.example.mobilecellnetwork.billing.BillingManager
                .getRoamingHostNetworkId(player);
        return com.example.mobilecellnetwork.billing.BillingManager.chargeUsage(
                player, charges, roamingCharges, "SMS usage", hostNetworkId);
    }

    private static void handleSms(
            net.minecraft.world.entity.player.Player sender, SmsSendPacket packet) {
        try {
            UUID chatId = UUID.fromString(packet.chatId());
            if (EmergencyMessages.CHAT_ID.equals(chatId)
                    || new UUID(0L, 0L).equals(chatId)
                    || (sender instanceof ServerPlayer player
                    && player.getData(ModCapabilities.PHONE_DATA).getChats().stream()
                    .anyMatch(chat -> chat.id().equals(chatId)))) {
                handleChatSms(sender, packet);
                return;
            }
        } catch (IllegalArgumentException ignored) {
            // Legacy SMS callers pass a recipient phone number.
        }
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        if (!packet.number().matches("[0-9]{1,9}") || packet.message().isBlank()
                || packet.message().length() > MobileCellNetworkConfig.SMS_MAX_LENGTH.get()) {
            notifyPlayer(player, "Invalid SMS.");
            return;
        }
        PhoneData senderData = player.getData(ModCapabilities.PHONE_DATA);
        if (senderData.isDead()
                || CoverageEngine.updateSignal(player, CoverageEngine.getStrongestAntennaFor(player)).tier()
                == SignalTier.NO_SERVICE) {
            notifyPlayer(player, "No Service.");
            return;
        }
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        if (!usage.isCycleActive()) {
            notifyPlayer(player, "Your plan is suspended.");
            return;
        }
        PlanRegistry.Plan plan = PlanRegistry.getPlan(usage.getActivePlanId());
        int maximumTexts = plan.textMessages();
        if (!plan.allowOverage() && maximumTexts >= 0 && usage.getTextsUsed() >= maximumTexts) {
            notifyPlayer(player, "Plan limit reached");
            return;
        }
        NetworkSavedData savedData = NetworkSavedData.get(player.serverLevel().getServer().overworld());
        java.util.UUID recipientId = savedData.getUUIDForNumber(packet.number());
        if (recipientId == null || recipientId.equals(player.getUUID())) {
            notifyPlayer(player, "Unknown recipient.");
            return;
        }
        if (!chargeTextUsage(player, senderData, usage, plan)) {
            return;
        }
        long now = System.currentTimeMillis();
        PhoneData.Message sent = new PhoneData.Message(senderData.getPhoneNumber(), packet.number(),
                packet.message(), now, true);
        senderData.addMessage(senderData.getSent(), sent, MobileCellNetworkConfig.MAX_MESSAGES_PER_THREAD.get());
        ServerPlayer receiver = player.server.getPlayerList().getPlayer(recipientId);
        PhoneData.Message received = new PhoneData.Message(senderData.getPhoneNumber(), packet.number(),
                packet.message(), now, false);
        if (receiver == null) {
            savedData.addPendingSms(recipientId, received);
        } else {
            PhoneData receiverData = receiver.getData(ModCapabilities.PHONE_DATA);
            receiverData.addMessage(receiverData.getInbox(), received,
                    MobileCellNetworkConfig.MAX_MESSAGES_PER_THREAD.get());
            receiverData.getNotifications().add(new PhoneData.Notification("SMS", "SMS from "
                    + senderData.getPhoneNumber(),
                    MapPointShare.displayText(packet.message(), senderData.getPhoneNumber()), now, false));
            if (PhoneDeviceService.isSessionUnlocked(receiver)) {
                PacketDistributor.sendToPlayer(receiver,
                        new SmsReceivePacket(SmsDataSyncPacket.MessageEntry.from(received)));
            }
            syncChats(receiver);
            if (MapPointShare.isShare(packet.message())) {
                syncMapPoints(receiver);
            }
            PacketDistributor.sendToPlayer(receiver, new PhoneNotificationPacket(
                    "SMS", "New message", "SMS from " + senderData.getPhoneNumber(), false));
            playSmsNotificationSound(receiver);
        }
        usage.setTextsUsed(usage.getTextsUsed() + 1);
        if (maximumTexts > 0 && usage.getTextsUsed() >= Math.ceil(maximumTexts * 0.9D)
                && usage.getTextsUsed() - 1 < Math.ceil(maximumTexts * 0.9D)) {
            PacketDistributor.sendToPlayer(player, new PhoneNotificationPacket(
                    "PLAN_WARNING", "Plan warning", "You have used at least 90% of your texts.", false));
        }
        sendPhoneData(player);
        player.displayClientMessage(Component.literal("SMS sent."), true);
    }

    public static void deliverPendingSms(ServerPlayer player) {
        NetworkSavedData savedData = NetworkSavedData.get(player.serverLevel().getServer().overworld());
        PhoneData phoneData = player.getData(ModCapabilities.PHONE_DATA);
        for (PhoneData.Message message : savedData.takePendingSms(player.getUUID())) {
            phoneData.addMessage(phoneData.getInbox(), message,
                    MobileCellNetworkConfig.MAX_MESSAGES_PER_THREAD.get());
            phoneData.getNotifications().add(new PhoneData.Notification("SMS", "New message",
                    "SMS from " + message.senderNumber(), message.timestamp(), false));
            PacketDistributor.sendToPlayer(player, new SmsReceivePacket(SmsDataSyncPacket.MessageEntry.from(message)));
            if (MapPointShare.isShare(message.body())) {
                syncMapPoints(player);
            }
            PacketDistributor.sendToPlayer(player, new PhoneNotificationPacket(
                    "SMS", "New message", "SMS from " + message.senderNumber(), false));
            playSmsNotificationSound(player);
        }
        for (NetworkSavedData.PendingCall pending : savedData.takePendingCalls(player.getUUID())) {
            if (pending.missed()) {
                phoneData.getMissedCalls().add(new PhoneData.MissedCall(pending.callerNumber(), pending.timestamp()));
            }
            phoneData.getVoicemail().add(new PhoneData.Voicemail(
                    pending.callerNumber(), pending.timestamp(), pending.note()));
            phoneData.getNotifications().add(new PhoneData.Notification(
                    pending.missed() ? "MISSED_CALL" : "VOICEMAIL",
                    pending.missed() ? "Missed call" : "Voicemail",
                    "Missed call from " + pending.callerNumber(), pending.timestamp(), false));
            PacketDistributor.sendToPlayer(player, new PhoneNotificationPacket(
                    pending.missed() ? "MISSED_CALL" : "VOICEMAIL",
                    pending.missed() ? "Missed call" : "Voicemail",
                    "Missed call from " + pending.callerNumber(), false));
        }
        sendPhoneData(player);
    }

    public static int deliverPrivilegedAlert(ServerPlayer sender, NetworkSavedData savedData,
                                             java.util.Collection<java.util.UUID> recipientIds,
                                             String senderNumber, String body) {
        long timestamp = System.currentTimeMillis();
        int delivered = 0;
        for (java.util.UUID recipientId : new java.util.LinkedHashSet<>(recipientIds)) {
            String recipientNumber = savedData.getNumberFor(recipientId);
            if (recipientNumber == null) {
                continue;
            }
            PhoneData.Message received = new PhoneData.Message(
                    senderNumber, recipientNumber, body, timestamp, false);
            ServerPlayer recipient = sender.server.getPlayerList().getPlayer(recipientId);
            if (recipient == null) {
                savedData.addPendingSms(recipientId, received);
            } else {
                PhoneData data = recipient.getData(ModCapabilities.PHONE_DATA);
                data.addMessage(data.getInbox(), received,
                        MobileCellNetworkConfig.MAX_MESSAGES_PER_THREAD.get());
                data.getNotifications().add(new PhoneData.Notification(
                        "SMS", "New message", "SMS from " + senderNumber, timestamp, false));
                if (PhoneDeviceService.isSessionUnlocked(recipient)) {
                    PacketDistributor.sendToPlayer(recipient,
                            new SmsReceivePacket(SmsDataSyncPacket.MessageEntry.from(received)));
                    PacketDistributor.sendToPlayer(recipient, new PhoneNotificationPacket(
                            "SMS", "New message", "SMS from " + senderNumber, false));
                }
                playSmsNotificationSound(recipient);
                sendPhoneData(recipient);
            }
            delivered++;
        }
        return delivered;
    }

    private static void playSmsNotificationSound(ServerPlayer player) {
        PhoneData phoneData = player.getData(ModCapabilities.PHONE_DATA);
        if (!phoneData.areNotificationSoundsEnabled()
                || !phoneData.getAppNotifications().getOrDefault("sms", true)) {
            return;
        }
        ResourceLocation soundId = ResourceLocation.tryParse(MobileCellNetworkConfig.SMS_NOTIFICATION_SOUND.get());
        SoundEvent sound = soundId == null ? null : BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (sound != null) {
            player.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static void handleLeaveVoicemail(
            net.minecraft.world.entity.player.Player sender, LeaveVoicemailPacket packet) {
        if (!(sender instanceof ServerPlayer player)
                || !hasPhoneMenu(player)
                || packet.message().isBlank()
                || packet.message().length() > 160) {
            return;
        }
        NetworkSavedData saved = NetworkSavedData.get(player.server.overworld());
        java.util.UUID targetId = saved.getUUIDForNumber(packet.number());
        String fromNumber = player.getData(ModCapabilities.PHONE_DATA).getPhoneNumber();
        if (targetId == null || targetId.equals(player.getUUID()) || fromNumber == null) {
            return;
        }
        long now = System.currentTimeMillis();
        ServerPlayer recipient = player.server.getPlayerList().getPlayer(targetId);
        if (recipient == null) {
            saved.addPendingCall(targetId, new NetworkSavedData.PendingCall(fromNumber, now, false,
                    packet.message()));
            return;
        }
        PhoneData data = recipient.getData(ModCapabilities.PHONE_DATA);
        data.getVoicemail().add(new PhoneData.Voicemail(fromNumber, now, packet.message()));
        data.getNotifications().add(new PhoneData.Notification(
                "VOICEMAIL", "Voicemail", "New voicemail from " + fromNumber, now, false));
        if (PhoneDeviceService.isSessionUnlocked(recipient)) {
            PacketDistributor.sendToPlayer(recipient, new PhoneNotificationPacket(
                    "VOICEMAIL", "Voicemail", "New voicemail from " + fromNumber, false));
        }
        sendPhoneData(recipient);
        if (PhoneDeviceService.isSessionUnlocked(recipient)) {
            PacketDistributor.sendToPlayer(recipient, SmsDataSyncPacket.from(data));
        }
    }

    private static void handleVoicemailNote(
            net.minecraft.world.entity.player.Player sender, VoicemailNotePacket packet) {
        if (!(sender instanceof ServerPlayer player)
                || !hasPhoneMenu(player)
                || packet.note().length() > 160) {
            return;
        }
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        if (packet.index() >= 0 && packet.index() < data.getVoicemail().size()) {
            PhoneData.Voicemail voicemail = data.getVoicemail().get(packet.index());
            data.getVoicemail().set(packet.index(),
                    new PhoneData.Voicemail(voicemail.caller(), voicemail.timestamp(), packet.note()));
            PacketDistributor.sendToPlayer(player, SmsDataSyncPacket.from(data));
        }
    }

    private static void handleSmsRead(
            net.minecraft.world.entity.player.Player sender, SmsReadPacket packet) {
        if (!(sender instanceof ServerPlayer player)
                || !hasPhoneMenu(player)
                || !packet.number().matches("[0-9]{1,9}")) {
            return;
        }
        PhoneData data = player.getData(ModCapabilities.PHONE_DATA);
        for (int index = 0; index < data.getInbox().size(); index++) {
            PhoneData.Message message = data.getInbox().get(index);
            if (packet.number().equals(message.senderNumber()) && !message.read()) {
                data.getInbox().set(index, message.markRead());
            }
        }
        sendPhoneData(player);
    }

    private static void notifyPlayer(ServerPlayer player, String message) {
        PacketDistributor.sendToPlayer(player, new PhoneNotificationPacket("SYSTEM", "Mobile Cell Network", message, false));
    }

    public static void syncPhoneData(ServerPlayer player) {
        sendPhoneData(player);
    }

    public static void syncPlanData(ServerPlayer player) {
        if (PhoneDeviceService.activePhoneId(player) != null
                && (!PhoneDeviceService.isSessionUnlocked(player) || PhoneDeviceService.isAllLocked(player))) {
            return;
        }
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        java.util.Map<String, String> networkNames = NetworkSavedData.get(player.serverLevel().getServer().overworld())
                .getNetworks().entrySet().stream().collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey, entry -> entry.getValue().name()));
        NetworkSavedData saved = NetworkSavedData.get(player.serverLevel().getServer().overworld());
        java.util.List<PlanSyncPacket.NetworkEntry> networks = saved.getNetworks().entrySet().stream()
                .map(entry -> {
                    var settings = saved.getNetworkSettings(entry.getKey());
                    int antennas = (int) saved.getAntennaNetworks().values().stream()
                            .filter(entry.getKey()::equals).count();
                    return new PlanSyncPacket.NetworkEntry(entry.getKey(), entry.getValue().name(),
                            entry.getValue().ownerName(), antennas, settings.isAllowRoaming());
                }).toList();
        PacketDistributor.sendToPlayer(player, PlanSyncPacket.forPlayer(PlanRegistry.getAllPlans(), networkNames,
                usage.getActivePlanId(), usage.getCycleStartDay(), usage.getMinutesUsed(),
                usage.getTextsUsed(), usage.getDataUsedMb(), usage.getPartialDataUsedMb(), networks));
    }

    private static void handlePlanSelect(
            net.minecraft.world.entity.player.Player sender, PlanSelectPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !hasPhoneMenu(player)) {
            return;
        }
        PlanRegistry.Plan plan = PlanRegistry.findPlan(packet.planId());
        if (plan == null) {
            notifyPlayer(player, "That plan is no longer available.");
            syncPlanData(player);
            return;
        }
        if (player.getData(ModCapabilities.PLAN_USAGE).getActivePlanId().equals(plan.id())) {
            notifyPlayer(player, "You already have this plan.");
            return;
        }
        if (CoverageEngine.getStrongestAntennaFor(player) == null) {
            notifyPlayer(player, "No Service. Connect to a network before purchasing a plan.");
            return;
        }
        boolean bypassPlanCost = net.neoforged.neoforge.server.permission.PermissionAPI.getPermission(
                player, com.example.mobilecellnetwork.chat.ChatInterceptor.BYPASS_PLANS_PERMISSION);
        PlanUsage usage = player.getData(ModCapabilities.PLAN_USAGE);
        PlanRegistry.Plan previous = usage.getActivePlanId().isBlank()
                ? null : PlanRegistry.findPlan(usage.getActivePlanId());
        if (!bypassPlanCost) {
            if (player.getData(ModCapabilities.BANK_ACCOUNT).getBalance() < plan.price()) {
                notifyPlayer(player, "Insufficient bank balance.");
                return;
            }
        }
        com.example.mobilecellnetwork.billing.BillingManager.resetPlanRevenueCycles(player, previous, plan);
        if (!bypassPlanCost) {
            if (!com.example.mobilecellnetwork.billing.BillingManager.chargePlanSubscription(player, plan)) {
                notifyPlayer(player, "Insufficient bank balance.");
                return;
            }
        }
        if (PlanRegistry.isDowngrade(
                previous == null ? PlanRegistry.getDefaultPlan() : previous, plan)) {
            CallManager.hangUp(player.getUUID());
        }
        usage.setActivePlanId(plan.id());
        usage.resetDailyCounters(Math.floorDiv(player.serverLevel().getDayTime(), 24_000L));
        usage.setCycleStartDay(Math.floorDiv(player.serverLevel().getDayTime(), 24_000L));
        usage.setCycleActive(true);
        player.getData(ModCapabilities.PHONE_DATA).setLastKnownPlanId(plan.id());
        NetworkSavedData.get(player.serverLevel().getServer().overworld())
                .setPlanAssignment(player.getUUID(), plan.id());
        syncPlanData(player);
        sendPhoneData(player);
        notifyPlayer(player, "Plan changed to " + plan.displayName() + ".");
    }
}


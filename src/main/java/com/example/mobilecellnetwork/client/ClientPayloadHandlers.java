package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.gui.CallScreen;
import com.example.mobilecellnetwork.gui.DeviceSettingsScreen;
import com.example.mobilecellnetwork.gui.EditPlanModal;
import com.example.mobilecellnetwork.gui.HeldPhoneView;
import com.example.mobilecellnetwork.gui.LockScreen;
import com.example.mobilecellnetwork.gui.LowPowerPromptScreen;
import com.example.mobilecellnetwork.gui.NetworkEditorScreen;
import com.example.mobilecellnetwork.gui.PhoneMenu;
import com.example.mobilecellnetwork.gui.PhoneNavigableScreen;
import com.example.mobilecellnetwork.gui.PhonePhotoLibrary;
import com.example.mobilecellnetwork.gui.PhoneScreen;
import com.example.mobilecellnetwork.gui.PhoneSetupScreen;
import com.example.mobilecellnetwork.gui.PlanScreen;
import com.example.mobilecellnetwork.gui.ServerBlockScreen;
import com.example.mobilecellnetwork.gui.admin.AdminToast;
import com.example.mobilecellnetwork.network.*;
import com.example.mobilecellnetwork.network.packet.RoamingAgreementSyncPacket;
import com.example.mobilecellnetwork.network.packet.RoamingProposalPacket;
import net.minecraft.client.Minecraft;

public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {
    }

    public static void openPhone(OpenPhoneScreenPacket packet) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player != null
                && minecraft.player.containerMenu.containerId == packet.menuId()
                && minecraft.player.containerMenu instanceof PhoneMenu menu) {
            HeldPhoneView.setEnabled(packet.heldView());
            NetworkHandler.clearClientPhoneData();
            PhoneDeviceStatePacket state = PhoneDeviceClientCache.state();
            if (state != null) {
                ClientPhoneCache.setAppearance(state.appearance());
            }
            if (state != null && !state.setupComplete() && !state.lockedAll()) {
                minecraft.setScreen(new PhoneSetupScreen(menu));
            } else if (state != null && (state.lockScreenEnabled() || state.pinSet() || state.lockedAll())) {
                minecraft.setScreen(new LockScreen(menu));
            } else {
                PhoneDeviceClientCache.setUnlocked(true);
                if (ClientCallCache.incoming() && ClientCallCache.state() != null
                        && ClientCallCache.state() != com.example.mobilecellnetwork.voice.CallSession.State.ENDED) {
                    minecraft.setScreen(new CallScreen(menu, ""));
                } else {
                    PhoneScreen.openPhone(menu, minecraft.player.getInventory());
                }
            }
        }
    }

    public static void updatePhoneDevice(PhoneDeviceStatePacket packet) {
        java.util.UUID priorPhoneId = PhoneDeviceClientCache.phoneId();
        if (packet.clearSensitiveData() || packet.lockedAll()) {
            NetworkHandler.clearClientPhoneData();
        } else if (!java.util.Objects.equals(priorPhoneId, packet.phoneId())) {
            NetworkHandler.clearClientPhoneData();
        }
        if (packet.lockedBank()) {
            ClientBankAccountCache.clearSensitiveData();
            ClientPlanCache.clearSensitiveData();
        }
        PhoneDeviceClientCache.update(packet);
        ClientPhoneCache.setAppearance(packet.appearance());
        PhonePhotoLibrary.setActivePhoneId(packet.phoneId());
        var current = Minecraft.getInstance().screen;
        if (!packet.setupComplete() && !packet.lockedAll()
                && current instanceof DeviceSettingsScreen phoneScreen) {
            Minecraft.getInstance().setScreen(new PhoneSetupScreen(phoneScreen.phoneMenu()));
        } else if (packet.setupComplete() && current instanceof PhoneSetupScreen setup) {
            setup.completed();
        }
        if (packet.lockedAll() && current instanceof PhoneNavigableScreen phoneScreen
                && phoneScreen.phoneMenu() != null) {
            Minecraft.getInstance().setScreen(new LockScreen(phoneScreen.phoneMenu()));
        }
    }

    public static void replacePhotos(PhonePhotoListPacket packet) {
        PhonePhotoLibrary.replacePhonePhotos(packet.phoneId(), packet.names());
    }

    public static void savePhoto(PhonePhotoPayloadPacket packet) {
        PhonePhotoLibrary.saveRemotePhoto(packet.phoneId(), packet.name(), packet.image());
    }

    public static void removePhoto(PhonePhotoDeletedPacket packet) {
        PhonePhotoLibrary.removeRemotePhoto(packet.phoneId(), packet.name());
    }

    public static void networkCreationResult(NetworkCreationResultPacket packet) {
        if (Minecraft.getInstance().screen instanceof NetworkEditorScreen editor) {
            editor.creationResult(packet.success(), packet.message());
        }
        if (packet.success()) {
            AdminToast.success(packet.message());
        } else {
            AdminToast.error(packet.message());
        }
    }

    public static void updateCall(CallStatePacket packet) {
        ClientCallCache.update(packet);
        com.example.mobilecellnetwork.gui.CallOverlay.onCallStateUpdated(packet);
    }

    public static void receiveVideo(VideoCallPacket packet) {
        com.example.mobilecellnetwork.gui.VideoCallClient.receive(packet);
    }

    public static void notifyPhone(PhoneNotificationPacket packet) {
        if (packet.lowPowerPrompt()) {
            Minecraft.getInstance().setScreen(new LowPowerPromptScreen(packet.title(), packet.message()));
        } else if (Minecraft.getInstance().player != null) {
            if ("Plan editor".equals(packet.title())
                    && Minecraft.getInstance().screen instanceof EditPlanModal editor) {
                editor.saveResult("Plan saved.".equals(packet.message()), packet.message());
            } else if ("Plan selection".equals(packet.title())
                    && Minecraft.getInstance().screen instanceof PlanScreen screen) {
                screen.selectionResult(packet.message());
            }
            ClientSmsCache.addToast(packet.title(), packet.message());
        }
    }

    public static void roamingProposal(RoamingProposalPacket packet) {
        ClientRoamingCache.updateProposal(packet);
        if (Minecraft.getInstance().screen instanceof ServerBlockScreen screen
                && screen.isRoamingTabSelected()) {
            screen.refreshRoamingTab();
        } else {
            ClientSmsCache.addToast("Roaming request", packet.message().isBlank()
                    ? "New roaming proposal received." : packet.message());
        }
    }

    public static void roamingAgreements(RoamingAgreementSyncPacket packet) {
        ClientRoamingCache.updateAgreements(packet.networkId(), packet.agreements());
        if (Minecraft.getInstance().screen instanceof ServerBlockScreen screen
                && screen.isRoamingTabSelected()) {
            screen.refreshRoamingTab();
        } else {
            ClientSmsCache.addToast("Roaming", "Roaming agreements updated.");
        }
    }
}

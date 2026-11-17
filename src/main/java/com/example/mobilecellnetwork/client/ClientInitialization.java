package com.example.mobilecellnetwork.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientInitialization {
    private ClientInitialization() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(com.example.mobilecellnetwork.gui.ModScreens::register);
        modEventBus.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::registerKeys);
        modEventBus.addListener(ClientRenderers::register);
        modEventBus.addListener(TowerMeshCache::registerReloadListener);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.CallOverlay::onRender);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.NotificationOverlay::onRender);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.NotificationOverlay::onRenderScreen);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.PhoneStatusHud::onRender);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.CameraPreviewCapture::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.VideoCallClient::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.VideoCallClient::onTick);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.CameraPreviewCapture::onComputeFov);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.event.TowerPlacementPreview::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(PowerWireRenderer::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(TowerOutlineRenderer::onHighlight);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.CallOverlay::onMouseButton);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.PhoneNavigation::onMouseButton);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::tick);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onMousePressed);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onMouseDragged);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onMouseReleased);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onScreenClosing);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onRenderHand);
        NeoForge.EVENT_BUS.addListener(HeldTesterRenderer::onRenderHand);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientRoamingCache::onClientLogin);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientRoamingCache::onClientLogout);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientPlanCache::onClientLogin);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientPlanCache::onClientLogout);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientPlanCache::onClientClone);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientCallCache::onClientLogin);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientCallCache::onClientLogout);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientSmsCache::onClientLogin);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientSmsCache::onClientLogout);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientEmergencyDispatchCache::onClientLogin);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientEmergencyDispatchCache::onClientLogout);
    }
}

package com.example.mobilecellnetwork;

import com.example.mobilecellnetwork.billing.BillingManager;
import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.chat.ChatInterceptor;
import com.example.mobilecellnetwork.command.MobileCellCommands;
import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.event.PlayerDataEvents;
import com.example.mobilecellnetwork.network.CoverageEngine;
import com.example.mobilecellnetwork.network.NetworkHandler;
import com.example.mobilecellnetwork.plans.PlanRegistry;
import com.example.mobilecellnetwork.registry.ModBlockEntities;
import com.example.mobilecellnetwork.registry.ModBlocks;
import com.example.mobilecellnetwork.registry.ModCreativeTabs;
import com.example.mobilecellnetwork.registry.ModDataComponents;
import com.example.mobilecellnetwork.registry.ModItems;
import com.example.mobilecellnetwork.registry.ModMenus;
import com.example.mobilecellnetwork.voice.VoiceManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(MobileCellNetwork.MOD_ID)
public final class MobileCellNetwork {
    public static final String MOD_ID = "mobilecellnetwork";

    public MobileCellNetwork(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModCapabilities.ATTACHMENTS.register(modEventBus);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, MobileCellNetworkConfig.SPEC);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModCapabilities::register);
        modEventBus.addListener(NetworkHandler::registerPayloads);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(com.example.mobilecellnetwork.gui.ModScreens::register);
            modEventBus.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::registerKeys);
            modEventBus.addListener(com.example.mobilecellnetwork.client.ClientRenderers::register);
        }
        NeoForge.EVENT_BUS.addListener(PlayerDataEvents::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(PlayerDataEvents::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.event.AntennaLifecycleHandler::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.event.AntennaLifecycleHandler::onEntityPlace);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.event.AntennaLifecycleHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.billing.BillingManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(MobileCellCommands::register);
        NeoForge.EVENT_BUS.addListener(PlanRegistry::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(PlanRegistry::onServerStarted);
        NeoForge.EVENT_BUS.addListener(CoverageEngine::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.RoamingManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ConnectionDelayService::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.voice.CallManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.PhoneDeviceService::onServerTick);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.CallOverlay::onRender);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.NotificationOverlay::onRender);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.PhoneStatusHud::onRender);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.CameraPreviewCapture::onRenderLevel);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.VideoCallClient::onRenderLevel);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.VideoCallClient::onTick);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.CameraPreviewCapture::onComputeFov);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.event.TowerPlacementPreview::onRenderLevel);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.CallOverlay::onMouseButton);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.PhoneNavigation::onMouseButton);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::tick);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onKeyPressed);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onMousePressed);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onMouseDragged);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onMouseReleased);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onScreenClosing);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.gui.HeldPhoneView::onRenderHand);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientRoamingCache::onClientLogin);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientRoamingCache::onClientLogout);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientPlanCache::onClientLogin);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientPlanCache::onClientLogout);
            NeoForge.EVENT_BUS.addListener(com.example.mobilecellnetwork.network.ClientPlanCache::onClientClone);
            NeoForge.EVENT_BUS.addListener(
                    com.example.mobilecellnetwork.network.ClientEmergencyDispatchCache::onClientLogin);
            NeoForge.EVENT_BUS.addListener(
                    com.example.mobilecellnetwork.network.ClientEmergencyDispatchCache::onClientLogout);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.init();
            ChatInterceptor.init();
            VoiceManager.init();
            PlanRegistry.init();
            BillingManager.init();
        });
    }
}

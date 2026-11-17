package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.registry.ModBlockEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class ClientRenderers {
    private ClientRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TEST_ANTENNA.get(), TestAntennaRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SERVER.get(), ServerRackRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PHONE_CHARGER.get(), PhoneChargerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SIGNAL_BOOSTER.get(), PowerIndicatorRenderer.Booster::new);
        event.registerBlockEntityRenderer(ModBlockEntities.COPPER_ANTENNA.get(), PowerIndicatorRenderer.Antenna::new);
        event.registerBlockEntityRenderer(ModBlockEntities.IRON_ANTENNA.get(), PowerIndicatorRenderer.Antenna::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DIAMOND_ANTENNA.get(), PowerIndicatorRenderer.Antenna::new);
    }
}

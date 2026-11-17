package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class PowerIndicatorRenderer {
    private static final ResourceLocation LENS =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");

    private PowerIndicatorRenderer() {
    }

    private static void light(PoseStack pose, MultiBufferSource buffers, int ambient, int overlay,
                              boolean powered, int onColor, float x1, float y1, float z1,
                              float x2, float y2, float z2) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(LENS));
        TestAntennaRenderer.box(vertices, pose, powered ? LightTexture.FULL_BRIGHT : ambient, overlay,
                x1, y1, z1, x2, y2, z2, powered ? onColor : 0xFF344047);
    }

    public static void charger(boolean powered, PoseStack pose, MultiBufferSource buffers, int ambient, int overlay) {
        light(pose, buffers, ambient, overlay, powered, 0xFF68ECA4,
                7.1F / 16, 0.6F / 16, 1.95F / 16, 8.9F / 16, 1.0F / 16, 1.98F / 16);
    }

    public static final class Booster implements BlockEntityRenderer<SignalBoosterBlockEntity> {
        public Booster(BlockEntityRendererProvider.Context context) {
        }

        @Override public void render(SignalBoosterBlockEntity booster, float partialTick, PoseStack pose,
                                     MultiBufferSource buffers, int light, int overlay) {
            for (int i = 0; i < 3; i++) {
                float x = 6.5F + i * 1.2F;
                boolean active = booster.isPowered() && switch (i) {
                    case 0 -> true;
                    case 1 -> booster.isBypassIndicator();
                    default -> booster.isLowPowerIndicator() && booster.getLevel() != null
                            && Math.floorMod(booster.getLevel().getGameTime(), 40) < 20;
                };
                int color = switch (i) {
                    case 0 -> 0xFF68ECA4;
                    case 1 -> 0xFF69CFFF;
                    default -> 0xFFFFB342;
                };
                light(pose, buffers, light, overlay, active, color,
                        x / 16, 7.0F / 16, 3.95F / 16, (x + 0.6F) / 16, 7.6F / 16, 3.98F / 16);
            }
        }
    }

    public static final class Antenna<T extends AntennaBlockEntity> implements BlockEntityRenderer<T> {
        public Antenna(BlockEntityRendererProvider.Context context) {
        }

        @Override public void render(T antenna, float partialTick, PoseStack pose,
                                     MultiBufferSource buffers, int light, int overlay) {
            boolean on = antenna.isPowered() && antenna.getLevel() != null
                    && Math.floorMod(antenna.getLevel().getGameTime() + antenna.getBlockPos().asLong(), 40) < 20;
            light(pose, buffers, light, overlay, on, 0xFFFF5353,
                    6.9F / 16, 15.0F / 16, 6.9F / 16, 9.1F / 16, 16.0F / 16, 9.1F / 16);
        }
    }
}

package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.blockentity.ServerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class ServerRackRenderer implements BlockEntityRenderer<ServerBlockEntity> {
    private static final ResourceLocation LIGHT =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");

    public ServerRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ServerBlockEntity rack, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (rack.getLevel() == null) return;
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(LIGHT));
        long time = rack.getLevel().getGameTime();
        long offset = rack.getBlockPos().asLong();
        for (int row = 0; row < 3; row++) {
            for (int led = 0; led < 3; led++) {
                boolean on = rack.isPowered() && switch (led) {
                    case 0 -> true;
                    case 1 -> rack.hasManagedNetworkIndicator();
                    default -> rack.hasLowPowerIndicator()
                            && Math.floorMod(time + offset, 40) < 20;
                };
                int color = on ? switch (led) {
                    case 0 -> 0xFF63F6AA;
                    case 1 -> 0xFF70C7FF;
                    default -> 0xFFFFB342;
                } : 0xFF263B34;
                float x = 0.26F + led * 0.075F;
                float y = 0.22F + row * 0.25F;
                face(vertices, pose, overlay, on ? LightTexture.FULL_BRIGHT : light, color, x, y);
            }
        }
    }

    private static void face(VertexConsumer out, PoseStack pose, int overlay, int light,
                             int color, float x, float y) {
        for (int side = 0; side < 4; side++) {
            pose.pushPose();
            pose.translate(0.5F, 0, 0.5F);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side * 90.0F));
            pose.translate(-0.5F, 0, -0.5F);
            vertex(out, pose, overlay, light, color, x, y, 0.058F, 0, 1);
            vertex(out, pose, overlay, light, color, x, y + 0.05F, 0.058F, 0, 0);
            vertex(out, pose, overlay, light, color, x + 0.05F, y + 0.05F, 0.058F, 1, 0);
            vertex(out, pose, overlay, light, color, x + 0.05F, y, 0.058F, 1, 1);
            pose.popPose();
        }
    }

    private static void vertex(VertexConsumer out, PoseStack pose, int overlay, int light, int color,
                               float x, float y, float z, float u, float v) {
        out.addVertex(pose.last(), x, y, z).setColor(color).setUv(u, v)
                .setOverlay(overlay).setLight(light).setNormal(pose.last(), 0, 0, -1);
    }
}

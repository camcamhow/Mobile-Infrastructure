package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

public final class TestAntennaRenderer implements BlockEntityRenderer<TestAntennaBlockEntity> {
    private static final ResourceLocation STEEL =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/light_gray_concrete.png");
    private static final ResourceLocation DARK =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gray_concrete.png");
    private static final ResourceLocation PANEL =
            ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "textures/block/panel_diamond.png");
    private static final ResourceLocation LIGHT =
            ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "textures/block/antenna_light.png");

    public TestAntennaRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(TestAntennaBlockEntity antenna) {
        return new AABB(antenna.getBlockPos()).expandTowards(0, 9, 0);
    }

    @Override
    public void render(TestAntennaBlockEntity antenna, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer steel = buffers.getBuffer(RenderType.entityCutoutNoCull(STEEL));
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                float px = x == 0 ? 0.22F : 0.70F;
                float pz = z == 0 ? 0.22F : 0.70F;
                box(steel, pose, light, overlay, px, 1.25F, pz, px + 0.08F, 9.5F, pz + 0.08F);
            }
        }
        box(steel, pose, light, overlay, 0.45F, 1.25F, 0.45F, 0.55F, 9.75F, 0.55F);
        for (int level = 1; level < 8; level++) {
            for (int side = 0; side < 4; side++) {
                brace(steel, pose, light, overlay, level + 0.5F, side, 30.96F);
                brace(steel, pose, light, overlay, level + 0.5F, side, -30.96F);
            }
        }

        VertexConsumer dark = buffers.getBuffer(RenderType.entityCutoutNoCull(DARK));
        for (int level = 2; level <= 9; level++) {
            float y = level;
            box(dark, pose, light, overlay, 0.20F, y, 0.20F, 0.80F, y + 0.09F, 0.28F);
            box(dark, pose, light, overlay, 0.20F, y, 0.72F, 0.80F, y + 0.09F, 0.80F);
            box(dark, pose, light, overlay, 0.20F, y, 0.28F, 0.28F, y + 0.09F, 0.72F);
            box(dark, pose, light, overlay, 0.72F, y, 0.28F, 0.80F, y + 0.09F, 0.72F);
        }
        box(dark, pose, light, overlay, 0.20F, 9.40F, 0.20F, 0.80F, 9.52F, 0.80F);
        box(dark, pose, light, overlay, 0.36F, 9.68F, 0.36F, 0.64F, 9.75F, 0.64F);
        box(dark, pose, light, overlay, 0.26F, 8.00F, 0.16F, 0.74F, 9.23F, 0.23F);
        box(dark, pose, light, overlay, 0.26F, 8.00F, 0.77F, 0.74F, 9.23F, 0.84F);
        box(dark, pose, light, overlay, 0.16F, 8.00F, 0.26F, 0.23F, 9.23F, 0.74F);
        box(dark, pose, light, overlay, 0.77F, 8.00F, 0.26F, 0.84F, 9.23F, 0.74F);

        VertexConsumer panel = buffers.getBuffer(RenderType.entityCutoutNoCull(PANEL));
        box(panel, pose, light, overlay, 0.29F, 8.1F, 0.13F, 0.71F, 9.15F, 0.22F);
        box(panel, pose, light, overlay, 0.29F, 8.1F, 0.78F, 0.71F, 9.15F, 0.87F);
        box(panel, pose, light, overlay, 0.13F, 8.1F, 0.29F, 0.22F, 9.15F, 0.71F);
        box(panel, pose, light, overlay, 0.78F, 8.1F, 0.29F, 0.87F, 9.15F, 0.71F);
        VertexConsumer beacon = buffers.getBuffer(RenderType.entityCutoutNoCull(LIGHT));
        boolean on = antenna.isPowered() && antenna.getLevel() != null
                && Math.floorMod(antenna.getLevel().getGameTime() + antenna.getBlockPos().asLong(), 40) < 20;
        int beaconLight = on ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT : light;
        int beaconColor = on ? 0xFFFF4444 : 0xFF542020;
        box(beacon, pose, beaconLight, overlay, 0.40F, 9.75F, 0.40F, 0.60F, 10.0F, 0.60F, beaconColor);
    }

    private static void brace(VertexConsumer out, PoseStack pose, int light, int overlay,
                              float y, int side, float angle) {
        pose.pushPose();
        pose.translate(0.5F, y, 0.5F);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side * 90.0F));
        pose.translate(0, 0, -0.26F);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
        box(out, pose, light, overlay, -0.025F, -0.4665F, -0.025F,
                0.025F, 0.4665F, 0.025F);
        pose.popPose();
    }

    private static void box(VertexConsumer out, PoseStack pose, int light, int overlay,
                            float x1, float y1, float z1, float x2, float y2, float z2) {
        box(out, pose, light, overlay, x1, y1, z1, x2, y2, z2, 0xFFFFFFFF);
    }

    static void box(VertexConsumer out, PoseStack pose, int light, int overlay,
                            float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        face(out, pose, light, overlay, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, 0, 0, -1, color);
        face(out, pose, light, overlay, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, 0, 0, 1, color);
        face(out, pose, light, overlay, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, -1, 0, 0, color);
        face(out, pose, light, overlay, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, 1, 0, 0, color);
        face(out, pose, light, overlay, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, 0, 1, 0, color);
        face(out, pose, light, overlay, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, 0, -1, 0, color);
    }

    private static void face(VertexConsumer out, PoseStack pose, int light, int overlay,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, int color) {
        vertex(out, pose, light, overlay, ax, ay, az, 0, 1, nx, ny, nz, color);
        vertex(out, pose, light, overlay, bx, by, bz, 0, 0, nx, ny, nz, color);
        vertex(out, pose, light, overlay, cx, cy, cz, 1, 0, nx, ny, nz, color);
        vertex(out, pose, light, overlay, dx, dy, dz, 1, 1, nx, ny, nz, color);
    }

    private static void vertex(VertexConsumer out, PoseStack pose, int light, int overlay,
                               float x, float y, float z, float u, float v, float nx, float ny, float nz, int color) {
        out.addVertex(pose.last(), x, y, z).setColor(color)
                .setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose.last(), nx, ny, nz);
    }
}

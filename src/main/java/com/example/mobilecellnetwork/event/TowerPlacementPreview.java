package com.example.mobilecellnetwork.event;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.AntennaPowerState;
import com.example.mobilecellnetwork.blockentity.AntennaTier;
import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.network.ClientPlanCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class TowerPlacementPreview {
    private static final int GREEN = 0x8000FF00;
    private static final int CYAN = 0x8000FFFF;
    private static final int CIRCLE_SEGMENTS = 360;

    private TowerPlacementPreview() {
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || !MobileCellNetworkConfig.TOWER_PLACEMENT_PREVIEW_ENABLED.get()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null || minecraft.screen != null || player.isSpectator()) {
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        AntennaTier tier = antennaTier(heldItem);
        if (tier == null) {
            return;
        }

        HitResult hit = player.pick(MobileCellNetworkConfig.TOWER_PLACEMENT_PREVIEW_MAX_DISTANCE.get(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false), false);
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockPos target = blockHit.getBlockPos();
        Vec3 center = new Vec3(target.getX() + 0.5D, target.getY() + 0.01D, target.getZ() + 0.5D);
        int color = isOwnNetworkCoverage(level, target, player) ? GREEN : CYAN;
        drawCircle(event, center, tier.range(), color);
    }

    private static AntennaTier antennaTier(ItemStack stack) {
        if (stack.is(ModItems.COPPER_ANTENNA.get())) {
            return AntennaTier.COPPER;
        }
        if (stack.is(ModItems.IRON_ANTENNA.get())) {
            return AntennaTier.IRON;
        }
        if (stack.is(ModItems.DIAMOND_ANTENNA.get()) || stack.is(ModItems.TEST_ANTENNA.get())) {
            return AntennaTier.DIAMOND;
        }
        return null;
    }

    private static boolean isOwnNetworkCoverage(Level level, BlockPos target, Player player) {
        String carrierNetworkId = ClientPlanCache.getActiveCarrierId();
        if (carrierNetworkId == null || carrierNetworkId.isBlank()) {
            carrierNetworkId = ClientSignalCache.activeNetworkId();
        }
        if (carrierNetworkId == null || carrierNetworkId.isBlank()) {
            return false;
        }
        double targetX = target.getX() + 0.5D;
        double targetY = target.getY() + 0.01D;
        double targetZ = target.getZ() + 0.5D;
        for (AntennaBlockEntity antenna : AntennaBlockEntity.loadedAntennas(level)) {
            if (!antenna.isEnabled() || antenna.getPowerState() == AntennaPowerState.UNPOWERED) {
                continue;
            }
            if (!carrierNetworkId.equals(antenna.getNetworkId())) {
                continue;
            }
            double dx = targetX - (antenna.getBlockPos().getX() + 0.5D);
            double dy = targetY - (antenna.getBlockPos().getY() + 0.5D);
            double dz = targetZ - (antenna.getBlockPos().getZ() + 0.5D);
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            double range = antenna.getRange();
            if (antenna.getPowerState() == AntennaPowerState.LOW_POWER) {
                range *= MobileCellNetworkConfig.LOW_POWER_RANGE_MULTIPLIER.get();
            }
            double meshRange = antenna.getTier().meshRange();
            if (distanceSquared <= range * range || distanceSquared <= meshRange * meshRange) {
                return true;
            }
        }
        return false;
    }

    private static void drawCircle(RenderLevelStageEvent event, Vec3 center, double radius, int color) {
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        int alpha = color >>> 24;
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        for (int segment = 0; segment < CIRCLE_SEGMENTS; segment++) {
            double angle = Math.PI * 2.0D * segment / CIRCLE_SEGMENTS;
            double nextAngle = Math.PI * 2.0D * (segment + 1) / CIRCLE_SEGMENTS;
            addLineVertex(lines, poseStack, center, radius, angle, red, green, blue, alpha);
            addLineVertex(lines, poseStack, center, radius, nextAngle, red, green, blue, alpha);
        }
        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void addLineVertex(VertexConsumer lines, PoseStack poseStack, Vec3 center,
                                      double radius, double angle, int red, int green, int blue, int alpha) {
        float x = (float) (center.x + Math.cos(angle) * radius);
        float y = (float) center.y;
        float z = (float) (center.z + Math.sin(angle) * radius);
        lines.addVertex(poseStack.last(), x, y, z)
                .setColor(red, green, blue, alpha)
                .setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
    }
}

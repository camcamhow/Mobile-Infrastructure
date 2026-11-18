package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity;
import com.example.mobilecellnetwork.infrastructure.Installation;
import com.example.mobilecellnetwork.infrastructure.WirePath;
import com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity;
import com.example.mobilecellnetwork.blockentity.PowerTapBlockEntity;
import com.example.mobilecellnetwork.infrastructure.CablePorts;
import com.example.mobilecellnetwork.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.client.renderer.LightTexture;
import java.util.ArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PowerWireRenderer {
    private static final ResourceLocation INSULATION =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/white_concrete.png");
    private PowerWireRenderer() {}

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        var buffers = minecraft.renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityCutoutNoCull(INSULATION);
        VertexConsumer cable = buffers.getBuffer(renderType);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        Vec3 camera = event.getCamera().getPosition();
        pose.translate(-camera.x, -camera.y, -camera.z);
        var sourcePorts = new java.util.HashSet<BlockPos>();
        for (var antenna : AntennaBlockEntity.loadedAntennas(minecraft.level)) {
            draw(cable, pose, antenna, antenna.installation(), camera, sourcePorts);
        }
        for (var booster : SignalBoosterBlockEntity.loaded(minecraft.level)) {
            draw(cable, pose, booster, booster.installation(), camera, sourcePorts);
        }
        for (var charger : PhoneChargerBlockEntity.loaded(minecraft.level)) {
            draw(cable, pose, charger, charger.installation(), camera, sourcePorts);
        }
        preview(cable, pose);
        buffers.endBatch(renderType);
        pose.popPose();
    }

    private static void draw(VertexConsumer cable, PoseStack pose, BlockEntity device, Installation installation,
                             Vec3 camera, java.util.Set<BlockPos> sourcePorts) {
        BlockPos target = device.getBlockPos();
        if (Vec3.atCenterOf(target).distanceToSqr(camera) > 192 * 192) return;
        int light = LevelRenderer.getLightColor(Minecraft.getInstance().level, target);
        socket(cable, pose, target, CablePorts.socket(device), light);
        BlockPos source = installation.source();
        if (source == null) return;
        if (Minecraft.getInstance().level.hasChunkAt(source)
                && Minecraft.getInstance().level.getBlockEntity(source) instanceof PowerTapBlockEntity
                && sourcePorts.add(source)) {
            socket(cable, pose, source, CablePorts.Socket.POWER_TAP,
                    LevelRenderer.getLightColor(Minecraft.getInstance().level, source));
        }
        Vec3 from = installation.sourcePoint() != null ? installation.sourcePoint()
                : Vec3.atCenterOf(source).add(Vec3.atLowerCornerOf(installation.sourceFace().getNormal()).scale(0.5));
        Vec3 to = installation.targetPoint() != null ? installation.targetPoint() : CablePorts.point(device);
        renderPath(cable, pose, from, installation.snaps(), to, light, 0xFF252930);
    }

    private static void preview(VertexConsumer cable, PoseStack pose) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        for (var hand : net.minecraft.world.InteractionHand.values()) {
            var stack = minecraft.player.getItemInHand(hand);
            if (!stack.is(ModItems.POWER_CABLE_SPOOL.get())) continue;
            var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (!tag.contains("wireSource") || !minecraft.level.dimension().location().toString()
                    .equals(tag.getString("wireDimension"))) continue;
            Vec3 from = tag.contains("wireStart") ? WirePath.loadPoint(tag.getCompound("wireStart"))
                    : Vec3.atCenterOf(BlockPos.of(tag.getLong("wireSource")));
            var snaps = WirePath.load(tag, "wireSnaps");
            Vec3 to = minecraft.hitResult == null ? minecraft.player.getEyePosition()
                    .add(minecraft.player.getLookAngle().scale(4)) : minecraft.hitResult.getLocation();
            if (minecraft.hitResult instanceof BlockHitResult hit) {
                to = CablePorts.previewPoint(minecraft.level, hit);
            }
            BlockPos source = BlockPos.of(tag.getLong("wireSource"));
            if (minecraft.level.hasChunkAt(source)
                    && minecraft.level.getBlockEntity(source) instanceof PowerTapBlockEntity) {
                socket(cable, pose, source, CablePorts.Socket.POWER_TAP, LightTexture.FULL_BRIGHT);
            }
            int color = WirePath.valid(from, snaps, to) ? 0xFFE6BD45 : 0xFFDC4949;
            renderPath(cable, pose, from, snaps, to, LightTexture.FULL_BRIGHT, color);
        }
    }

    private static void socket(VertexConsumer cable, PoseStack pose, BlockPos pos,
                               CablePorts.Socket socket, int light) {
        pose.pushPose();
        Vec3 mount = Vec3.atLowerCornerOf(pos).add(socket.mount());
        pose.translate(mount.x, mount.y, mount.z);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1),
                Vec3.atLowerCornerOf(socket.facing().getNormal()).toVector3f()));
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        TestAntennaRenderer.box(cable, pose, light, overlay,
                -0.05F, -0.05F, -0.008F, 0.05F, 0.05F, 0.022F, 0xFF929A9E);
        TestAntennaRenderer.box(cable, pose, light, overlay,
                -0.035F, -0.035F, 0.022F, 0.035F, 0.035F, 0.05F, 0xFF252930);
        TestAntennaRenderer.box(cable, pose, light, overlay,
                -0.027F, -0.027F, 0.05F, 0.027F, 0.027F, 0.065F, 0xFFB88A53);
        pose.popPose();
    }

    private static void renderPath(VertexConsumer cable, PoseStack pose, Vec3 from,
                                   java.util.List<Vec3> snaps, Vec3 to, int light, int color) {
        var points = new ArrayList<Vec3>();
        points.add(from);
        points.addAll(snaps);
        points.add(to);
        for (int i = 1; i < points.size(); i++) {
            segment(cable, pose, points.get(i - 1), points.get(i), 0.045F, light, color);
        }
        for (Vec3 point : points) {
            pose.pushPose();
            pose.translate(point.x, point.y, point.z);
            TestAntennaRenderer.box(cable, pose, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    -0.04F, -0.04F, -0.04F, 0.04F, 0.04F, 0.04F, 0xFF929A9E);
            pose.popPose();
        }
    }

    private static void segment(VertexConsumer cable, PoseStack pose, Vec3 from, Vec3 to,
                                float thickness, int light, int color) {
        Vector3f direction = to.subtract(from).toVector3f();
        float length = direction.length();
        if (length < 0.00001F) return;
        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), direction.normalize()));
        float half = thickness / 2;
        TestAntennaRenderer.box(cable, pose, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                -half, 0, -half, half, length, half, color);
        pose.popPose();
    }
}

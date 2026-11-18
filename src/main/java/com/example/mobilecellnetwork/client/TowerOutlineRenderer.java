package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.block.TestTowerBlock;
import com.example.mobilecellnetwork.block.TestTowerSectionBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

public final class TowerOutlineRenderer {
    private static final Map<VoxelShape, List<Edge>> EDGES = new IdentityHashMap<>();

    record Edge(float x1, float y1, float z1, float x2, float y2, float z2,
                float nx, float ny, float nz) {}

    private TowerOutlineRenderer() {}

    static List<Edge> edges(VoxelShape shape) {
        return EDGES.computeIfAbsent(shape, key -> {
            List<Edge> edges = new ArrayList<>();
            key.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                double dx = x2 - x1;
                double dy = y2 - y1;
                double dz = z2 - z1;
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                edges.add(new Edge((float) x1, (float) y1, (float) z1,
                        (float) x2, (float) y2, (float) z2,
                        (float) (dx / length), (float) (dy / length), (float) (dz / length)));
            });
            return List.copyOf(edges);
        });
    }

    public static void onHighlight(RenderHighlightEvent.Block event) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var pos = event.getTarget().getBlockPos();
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TestTowerBlock || state.getBlock() instanceof TestTowerSectionBlock)
                || !state.getValue(TestTowerBlock.WIDE) || !level.getWorldBorder().isWithinBounds(pos)) return;
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        pose.pushPose();
        try {
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            render(state.getShape(level, pos), pose,
                    event.getMultiBufferSource().getBuffer(RenderType.lines()));
        } finally {
            pose.popPose();
        }
        event.setCanceled(true);
    }

    static void render(VoxelShape shape, PoseStack pose, VertexConsumer out) {
        for (Edge edge : edges(shape)) {
            out.addVertex(pose.last(), edge.x1, edge.y1, edge.z1)
                    .setColor(0, 0, 0, 0.4F).setNormal(pose.last(), edge.nx, edge.ny, edge.nz);
            out.addVertex(pose.last(), edge.x2, edge.y2, edge.z2)
                    .setColor(0, 0, 0, 0.4F).setNormal(pose.last(), edge.nx, edge.ny, edge.nz);
        }
    }
}

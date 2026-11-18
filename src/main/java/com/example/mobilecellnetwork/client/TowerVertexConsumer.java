package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.block.TowerVariant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

final class TowerVertexConsumer implements VertexConsumer {
    private final VertexConsumer target;
    private final TowerVariant variant;
    private final PoseStack.Pose pose;
    private double slope = 1;

    TowerVertexConsumer(VertexConsumer target, TowerVariant variant, PoseStack.Pose pose) {
        this.target = target;
        this.variant = variant;
        this.pose = pose;
    }

    @Override public VertexConsumer addVertex(float x, float y, float z) {
        slope = !variant.monopole() && y > 3 && y < 14 ? variant.mastScale() : 1;
        target.addVertex(pose, x, (float) variant.y(y), z);
        return this;
    }

    @Override public VertexConsumer setColor(int r, int g, int b, int a) {
        target.setColor(r, g, b, a);
        return this;
    }
    @Override public VertexConsumer setUv(float u, float v) { target.setUv(u, v); return this; }
    @Override public VertexConsumer setUv1(int u, int v) { target.setUv1(u, v); return this; }
    @Override public VertexConsumer setUv2(int u, int v) { target.setUv2(u, v); return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) {
        float adjustedY = (float) (y / slope);
        float length = (float) Math.sqrt(x * x + adjustedY * adjustedY + z * z);
        target.setNormal(pose, x / length, adjustedY / length, z / length);
        return this;
    }
}

package com.example.mobilecellnetwork.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.joml.Matrix4f;
import com.example.mobilecellnetwork.block.TowerVariant;

public final class TowerMeshCache {
    private static final int MAX_ENTRIES = 8;
    private static final Map<Key, List<Mesh>> CACHE = new LinkedHashMap<>(16, 0.75F, true);

    private TowerMeshCache() {}

    private record Key(TowerVariant variant, int light, int overlay) {}
    private record Mesh(ResourceLocation texture, VertexBuffer buffer) implements AutoCloseable {
        @Override public void close() { buffer.close(); }
    }

    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            if (RenderSystem.isOnRenderThread()) clear();
            else RenderSystem.recordRenderCall(TowerMeshCache::clear);
        });
    }

    private static void clear() {
        CACHE.values().forEach(meshes -> meshes.forEach(Mesh::close));
        CACHE.clear();
    }

    public static void render(TowerVariant variant, PoseStack pose, int light, int overlay) {
        RenderSystem.assertOnRenderThread();
        Key key = new Key(variant, light, overlay);
        List<Mesh> meshes = CACHE.get(key);
        if (meshes == null) {
            if (CACHE.size() >= MAX_ENTRIES) {
                var oldest = CACHE.entrySet().iterator();
                oldest.next().getValue().forEach(Mesh::close);
                oldest.remove();
            }
            meshes = build(variant, light, overlay);
            CACHE.put(key, meshes);
        }
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
        for (Mesh mesh : meshes) {
            RenderType type = RenderType.entityCutoutNoCull(mesh.texture());
            type.setupRenderState();
            try {
                mesh.buffer().bind();
                mesh.buffer().drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            } finally {
                VertexBuffer.unbind();
                type.clearRenderState();
            }
        }
    }

    private static List<Mesh> build(TowerVariant variant, int light, int overlay) {
        Map<ResourceLocation, ByteBufferBuilder> storage = new LinkedHashMap<>();
        Map<ResourceLocation, BufferBuilder> builders = new LinkedHashMap<>();
        List<Mesh> meshes = new ArrayList<>();
        try {
            PoseStack local = new PoseStack();
            TestAntennaRenderer.renderVariantStatic(variant, 20, local, texture ->
                    new TowerVertexConsumer(builders.computeIfAbsent(
                            TestAntennaRenderer.textureFor(variant, texture), name -> {
                        ByteBufferBuilder bytes = new ByteBufferBuilder(64 * 1024);
                        storage.put(name, bytes);
                        return new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
                    }), variant, local.last()), light, overlay);
            for (var entry : builders.entrySet()) {
                VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                meshes.add(new Mesh(entry.getKey(), buffer));
                buffer.bind();
                buffer.upload(entry.getValue().buildOrThrow());
            }
            return List.copyOf(meshes);
        } catch (RuntimeException | Error failure) {
            meshes.forEach(Mesh::close);
            throw failure;
        } finally {
            VertexBuffer.unbind();
            storage.values().forEach(ByteBufferBuilder::close);
        }
    }
}

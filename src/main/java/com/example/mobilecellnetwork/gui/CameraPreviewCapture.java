package com.example.mobilecellnetwork.gui;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.joml.Matrix4f;
import org.slf4j.Logger;

public final class CameraPreviewCapture {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation PREVIEW =
            ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "camera_preview");
    private static TextureTarget target;
    private static CameraScreen owner;
    private static long lastFailureLogNanos;

    private CameraPreviewCapture() {
    }

    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (event.usedConfiguredFov() && Minecraft.getInstance().screen instanceof CameraScreen camera) {
            double radians = Math.toRadians(event.getFOV()) / 2.0D;
            event.setFOV(Math.clamp(Math.toDegrees(2.0D * Math.atan(Math.tan(radians) / camera.zoom())),
                    1.0D, 150.0D));
        }
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof CameraScreen camera) || minecraft.player == null || minecraft.level == null) {
            return;
        }
        var source = minecraft.getMainRenderTarget();
        if (source.width <= 0 || source.height <= 0) {
            camera.cameraPreviewFailed(camera.previewGeneration(), "Camera view is not ready yet.");
            return;
        }
        RenderSystem.assertOnRenderThread();
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int alignment = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
        IntBuffer viewport = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        try {
            double scale = Math.min(1.0D, 960.0D / Math.max(source.width, source.height));
            int width = Math.max(1, (int) Math.round(source.width * scale));
            int height = Math.max(1, (int) Math.round(source.height * scale));
            if (owner != camera || target == null || target.width != width || target.height != height) {
                close();
                target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
                owner = camera;
                minecraft.getTextureManager().register(PREVIEW, new PreviewTexture(target));
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, source.width, source.height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
            camera.cameraPreviewReady();
            if (camera.takeCaptureRequest()) {
                int generation = camera.previewGeneration();
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
                GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
                GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
                ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
                GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
                Util.backgroundExecutor().execute(() -> encode(camera, generation, width, height, pixels));
            }
        } catch (RuntimeException exception) {
            long now = System.nanoTime();
            if (lastFailureLogNanos == 0 || now - lastFailureLogNanos >= 10_000_000_000L) {
                LOGGER.warn("Unable to update the camera view.", exception);
                lastFailureLogNanos = now;
            }
            camera.cameraPreviewFailed(camera.previewGeneration(), "Camera preview failed; reopen the camera.");
        } finally {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, alignment);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
        }
    }

    private static void encode(CameraScreen camera, int generation, int width, int height, ByteBuffer pixels) {
        try {
            byte[] image = PhonePhotoLibrary.encodeCameraFrame(width, height, pixels);
            Minecraft.getInstance().execute(() -> camera.finishCapture(generation, image));
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Unable to capture a camera photo.", exception);
            Minecraft.getInstance().execute(() -> camera.captureFailed(generation));
        }
    }

    public static boolean draw(CameraScreen camera, GuiGraphics graphics, int x, int y, int width, int height) {
        if (owner != camera || target == null || !camera.previewReady()) {
            return false;
        }
        double scale = Math.min((double) width / target.width, (double) height / target.height);
        int drawWidth = Math.max(1, (int) Math.round(target.width * scale));
        int drawHeight = Math.max(1, (int) Math.round(target.height * scale));
        int left = x + (width - drawWidth) / 2;
        int top = y + (height - drawHeight) / 2;
        graphics.flush();
        boolean blending = GL11.glIsEnabled(GL11.GL_BLEND);
        RenderSystem.disableBlend();
        try {
            RenderSystem.setShaderTexture(0, PREVIEW);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Matrix4f matrix = graphics.pose().last().pose();
            BufferBuilder vertices = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            vertices.addVertex(matrix, left, top, 0).setUv(0, 1);
            vertices.addVertex(matrix, left, top + drawHeight, 0).setUv(0, 0);
            vertices.addVertex(matrix, left + drawWidth, top + drawHeight, 0).setUv(1, 0);
            vertices.addVertex(matrix, left + drawWidth, top, 0).setUv(1, 1);
            BufferUploader.drawWithShader(vertices.buildOrThrow());
        } finally {
            if (blending) {
                RenderSystem.enableBlend();
            }
        }
        return true;
    }

    public static void close(CameraScreen camera) {
        if (owner == camera) {
            close();
        }
    }

    private static void close() {
        if (target != null) {
            int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int removedFramebuffer = target.frameBufferId;
            Minecraft.getInstance().getTextureManager().release(PREVIEW);
            target.destroyBuffers();
            target = null;
            owner = null;
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,
                    readFramebuffer == removedFramebuffer ? 0 : readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,
                    drawFramebuffer == removedFramebuffer ? 0 : drawFramebuffer);
        }
    }

    private static final class PreviewTexture extends AbstractTexture {
        private final TextureTarget target;

        private PreviewTexture(TextureTarget target) {
            this.target = target;
        }

        @Override
        public int getId() {
            return target.getColorTextureId();
        }

        @Override
        public void releaseId() {
        }

        @Override
        public void load(ResourceManager resources) {
        }
    }
}

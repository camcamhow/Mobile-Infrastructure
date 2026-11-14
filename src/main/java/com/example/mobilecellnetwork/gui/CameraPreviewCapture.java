package com.example.mobilecellnetwork.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

public final class CameraPreviewCapture {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long CAPTURE_INTERVAL_NANOS = 200_000_000L;
    private static volatile byte[] latestFrame = new byte[0];
    private static volatile long latestFrameNumber;
    private static long lastCaptureNanos;

    private CameraPreviewCapture() {
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof CameraScreen) || minecraft.player == null || minecraft.level == null) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastCaptureNanos < CAPTURE_INTERVAL_NANOS) {
            return;
        }
        lastCaptureNanos = now;

        var target = minecraft.getMainRenderTarget();
        int width = target.width;
        int height = target.height;
        if (width <= 0 || height <= 0
                || (long) width * height > PhonePhotoLibrary.MAX_IMAGE_PIXELS) {
            return;
        }

        RenderSystem.assertOnRenderThread();
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
            GL30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            latestFrame = PhonePhotoLibrary.encodeCameraFrame(width, height, pixels);
            latestFrameNumber++;
            if (latestFrameNumber == 1) {
                LOGGER.info("Camera preview captured first frame ({}x{}).", width, height);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Unable to update the in-game camera preview.", exception);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
        }
    }
    public static byte[] latestFrame() {
        return latestFrame;
    }

    public static long latestFrameNumber() {
        return latestFrameNumber;
    }
}

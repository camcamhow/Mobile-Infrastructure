package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientCallCache;
import com.example.mobilecellnetwork.network.VideoCallPacket;
import com.example.mobilecellnetwork.network.VideoImage;
import com.example.mobilecellnetwork.voice.CallSession;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

public final class VideoCallClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean ENCODING = new AtomicBoolean();
    private static UUID session;
    private static boolean enabled;
    private static CameraType previousCamera;
    private static byte[] remote = new byte[0];
    private static volatile byte[] local = new byte[0];
    private static volatile long localVersion;
    private static long remoteVersion;
    private static long remoteAt;
    private static String remoteSession = "";
    private static long lastFrame;
    private static TextureTarget target;
    private static int generation;

    private VideoCallClient() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static boolean hasRemoteVideo() {
        return remote.length > 0 && remoteSession.equals(ClientCallCache.sessionId())
                && System.currentTimeMillis() - remoteAt < 3_000L;
    }

    public static boolean hasVideo() {
        return enabled || hasRemoteVideo();
    }

    public static void toggle() {
        if (ClientCallCache.state() != CallSession.State.ACTIVE) return;
        UUID id = UUID.fromString(ClientCallCache.sessionId());
        PacketDistributor.sendToServer(new VideoCallPacket(id, !enabled, false, new byte[0]));
    }

    public static void receive(VideoCallPacket packet) {
        if (ClientCallCache.state() != CallSession.State.ACTIVE
                || !packet.session().toString().equals(ClientCallCache.sessionId())) return;
        if (packet.self()) {
            if (packet.enabled() && !(Minecraft.getInstance().screen instanceof CallScreen)) {
                PacketDistributor.sendToServer(new VideoCallPacket(packet.session(), false, false, new byte[0]));
                return;
            }
            if (packet.enabled() && !enabled) {
                session = packet.session();
                enabled = true;
                previousCamera = Minecraft.getInstance().options.getCameraType();
                Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            } else if (!packet.enabled()) {
                stopLocal();
            }
        } else if (!packet.enabled()) {
            remote = new byte[0];
        } else {
            byte[] image = packet.image();
            try {
                VideoImage.validate(image);
                remote = image;
                remoteSession = packet.session().toString();
                remoteVersion++;
                remoteAt = System.currentTimeMillis();
            } catch (IOException exception) {
                LOGGER.warn("Rejected invalid incoming video frame", exception);
            }
        }
    }

    public static void drawRemote(GuiGraphics graphics, int x, int y, int width, int height) {
        if (hasRemoteVideo()) {
            graphics.flush();
            PhonePhotoLibrary.drawVersionedThumbnail(graphics, "video-call-" + remoteVersion,
                    remote, x, y, width, height);
        } else {
            graphics.drawCenteredString(Minecraft.getInstance().font, "Their camera is off", x + width / 2,
                    y + height / 2 - 4, 0xFFFFFFFF);
        }
    }

    public static void drawLocal(GuiGraphics graphics, int x, int y, int width, int height) {
        byte[] image = local;
        if (image.length > 0) {
            graphics.flush();
            PhonePhotoLibrary.drawVersionedThumbnail(graphics, "video-call-local-" + localVersion,
                    image, x, y, width, height);
        } else {
            graphics.drawCenteredString(Minecraft.getInstance().font, "Starting camera...",
                    x + width / 2, y + height / 2 - 4, 0xFFFFFFFF);
        }
    }

    public static void onTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (enabled && (minecraft.player == null || minecraft.level == null
                || !(minecraft.screen instanceof CallScreen)
                || minecraft.options.getCameraType() != CameraType.THIRD_PERSON_FRONT
                || ClientCallCache.state() != CallSession.State.ACTIVE
                || !session.toString().equals(ClientCallCache.sessionId()))) {
            if (minecraft.player != null && ClientCallCache.state() == CallSession.State.ACTIVE
                    && session.toString().equals(ClientCallCache.sessionId())) {
                PacketDistributor.sendToServer(new VideoCallPacket(session, false, false, new byte[0]));
            }
            stopLocal();
        }
        if (ClientCallCache.state() != CallSession.State.ACTIVE) remote = new byte[0];
    }

    public static void stopForScreen() {
        if (enabled && session != null && ClientCallCache.state() == CallSession.State.ACTIVE) {
            PacketDistributor.sendToServer(new VideoCallPacket(session, false, false, new byte[0]));
        }
        stopLocal();
    }

    private static void stopLocal() {
        generation++;
        if (previousCamera != null && Minecraft.getInstance().options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            Minecraft.getInstance().options.setCameraType(previousCamera);
        }
        previousCamera = null;
        enabled = false;
        session = null;
        local = new byte[0];
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !enabled
                || !(Minecraft.getInstance().screen instanceof CallScreen) || ENCODING.get()) return;
        long now = System.nanoTime();
        if (now - lastFrame < 250_000_000L || !ENCODING.compareAndSet(false, true)) return;
        lastFrame = now;
        Minecraft minecraft = Minecraft.getInstance();
        var source = minecraft.getMainRenderTarget();
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int alignment = GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
        IntBuffer viewport = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        try {
            if (source.width <= 0 || source.height <= 0) {
                throw new IllegalStateException("Video source has no size");
            }
            int width = 160;
            int height = Math.clamp((int) ((long) source.height * width / source.width), 1, 160);
            if (target == null || target.height != height) {
                if (target != null) target.destroyBuffers();
                target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            }
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            GL30.glBlitFramebuffer(0, 0, source.width, source.height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            UUID capturedSession = session;
            int capturedGeneration = generation;
            Util.backgroundExecutor().execute(() -> encode(pixels, width, height, capturedSession, capturedGeneration));
        } catch (RuntimeException exception) {
            ENCODING.set(false);
            LOGGER.warn("Video capture failed", exception);
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("Video capture failed; camera turned off."), true);
            }
            stopForScreen();
        } finally {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, alignment);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDraw);
            RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
        }
    }

    private static void encode(ByteBuffer pixels, int width, int height, UUID id, int capturedGeneration) {
        try {
            byte[] image = PhonePhotoLibrary.encodeCameraFrame(width, height, pixels);
            if (image.length > VideoCallPacket.MAX_BYTES) throw new IOException("Video frame too large");
            Minecraft.getInstance().execute(() -> {
                if (enabled && generation == capturedGeneration && id.equals(session)
                        && ClientCallCache.state() == CallSession.State.ACTIVE) {
                    local = image;
                    localVersion++;
                    PacketDistributor.sendToServer(new VideoCallPacket(id, true, false, image));
                }
            });
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Unable to encode video frame", exception);
            Minecraft.getInstance().execute(() -> {
                if (generation == capturedGeneration && enabled) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("Video encoding failed; camera turned off."), true);
                    stopForScreen();
                }
            });
        } finally {
            ENCODING.set(false);
        }
    }
}

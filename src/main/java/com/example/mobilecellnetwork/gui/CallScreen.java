package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.CallControlPacket;
import com.example.mobilecellnetwork.network.ClientCallCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.LeaveVoicemailPacket;
import com.example.mobilecellnetwork.voice.CallSession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.gui.NoServiceOverlay;

public final class CallScreen extends Screen implements PhoneNavigableScreen {
    private static final int BUTTON_SIZE = 48;
    private static final float[] TONES = {1.5F, 1.2F, 0.9F};

    private final PhoneMenu menu;
    private final String number;
    private final long openedAtMillis = System.currentTimeMillis();
    private boolean speaker;
    private EditBox voicemailBox;
    private boolean voicemailSent;
    private int toneIndex;
    private int toneDelay;
    private long endedSeenAt;

    public CallScreen(PhoneMenu menu, String number) {
        super(Component.literal("Call"));
        this.menu = menu;
        this.number = number;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        PhoneNavigation.openHome(this);
    }

    private boolean fresh() {
        return ClientCallCache.state() != null && (ClientCallCache.state() == CallSession.State.ACTIVE
                || ClientCallCache.updatedAtMillis() >= openedAtMillis - 50L);
    }

    private CallSession.State state() {
        return fresh() ? ClientCallCache.state() : null;
    }

    private String reasonCode() {
        String reason = ClientCallCache.reason();
        int split = reason.indexOf(':');
        return split < 0 ? reason : reason.substring(0, split);
    }

    private String reasonMessage() {
        String reason = ClientCallCache.reason();
        int split = reason.indexOf(':');
        return split < 0 ? "" : reason.substring(split + 1);
    }

    private boolean canLeaveVoicemail() {
        if (state() != CallSession.State.ENDED) {
            return false;
        }
        String code = reasonCode();
        return code.equals("UNAVAILABLE") || code.equals("TIMEOUT") || code.equals("DECLINED")
                || code.equals("MISSED_CALL");
    }

    @Override
    public void tick() {
        CallSession.State state = state();
        if (state == CallSession.State.ENDED) {
            if (endedSeenAt == 0L) {
                endedSeenAt = System.currentTimeMillis();
            }
            if (reasonCode().equals("NOT_IN_SERVICE") && toneIndex < TONES.length && --toneDelay <= 0) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                        SoundEvents.NOTE_BLOCK_BELL.value(), TONES[toneIndex++], 1.0F));
                toneDelay = 6;
            }
            if (!canLeaveVoicemail() && !reasonCode().equals("NOT_IN_SERVICE")
                    && !reasonCode().equals("CALL_FAILED")
                    && System.currentTimeMillis() - endedSeenAt > 2000L) {
                close();
            }
        }
        if (canLeaveVoicemail() && voicemailBox == null && !voicemailSent) {
            int left = PhoneFrame.getScreenX(this);
            int top = PhoneFrame.getContentTop(this);
            voicemailBox = PhoneTextField.create(font, left + 14, top + 160, 176, 24,
                    "Leave a message", 160, ClientPhoneCache.phoneStyle());
            addRenderableWidget(voicemailBox);
            setFocused(voicemailBox);
            voicemailBox.setFocused(true);
        }
    }

    private void close() {
        minecraft.setScreen(new DialerScreen(menu));
    }

    private void sendVoicemail() {
        if (voicemailBox == null || voicemailBox.getValue().isBlank()) {
            return;
        }
        PacketDistributor.sendToServer(new LeaveVoicemailPacket(number, voicemailBox.getValue().trim()));
        voicemailSent = true;
        removeWidget(voicemailBox);
        voicemailBox = null;
    }

    private String title() {
        String partner = ClientCallCache.partnerName();
        return fresh() && !partner.isBlank() ? partner : PhoneRenderer.formatPhoneNumber(number);
    }

    private String status() {
        CallSession.State state = state();
        if (state == null) {
            return "Connecting...";
        }
        return switch (state) {
            case RINGING -> "Calling...";
            case ACTIVE -> formatDuration(System.currentTimeMillis() - ClientCallCache.startedAtMillis());
            case ENDED -> endedText();
        };
    }

    private String endedText() {
        if (voicemailSent) {
            return "Voicemail sent";
        }
        return switch (reasonCode()) {
            case "NOT_IN_SERVICE" -> "This number is not in service";
            case "UNAVAILABLE" -> "Unavailable";
            case "TIMEOUT", "MISSED_CALL" -> "No answer";
            case "DECLINED" -> "Call declined";
            case "CALL_FAILED" -> reasonMessage();
            default -> "Call ended";
        };
    }

    private static String formatDuration(long millis) {
        long total = Math.max(0L, millis / 1000L);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", total / 60L, total % 60L);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // No service gating: show overlay if no active plan and screen is not exempt
            if (ClientPhoneCache.planId().isBlank()) {
                PhoneFrame.render(graphics, this);
                NoServiceOverlay.render(graphics, ClientPhoneCache.phoneStyle(), PhoneFrame.getScreenX(this),
                        PhoneFrame.getContentTop(this), PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24, "Call", font);
                PhoneFrame.renderStatusBar(graphics, this);
                return;
            }
            PhoneFrame.render(graphics, this);
            PhoneStyle style = ClientPhoneCache.phoneStyle();
            int left = PhoneFrame.getScreenX(this);
            int top = PhoneFrame.getContentTop(this);
        int centerX = left + PhoneFrame.getScreenW() / 2;
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);

        CallSession.State state = state();
        if (state == CallSession.State.ACTIVE) {
            graphics.fill(left + 14, top + 20, left + 190, top + 102, 0xFF17191D);
            VideoCallClient.drawRemote(graphics, left + 14, top + 20, 176, 82);
            PhoneRenderer.drawSecondaryButton(graphics, style, left + 38, top + 153, 128, 22,
                    VideoCallClient.enabled() ? "Camera off" : "Camera on", font);
        } else {
            PhoneRenderer.fillRoundedRect(graphics, style, centerX - 28, top + 44, 56, 56, 28,
                    PhoneColors.accent(style));
            String initial = title().isEmpty() ? "?" : title().substring(0, 1).toUpperCase(java.util.Locale.ROOT);
            graphics.drawCenteredString(font, initial, centerX, top + 68, 0xFFFFFFFF);
        }
        graphics.drawCenteredString(font, font.plainSubstrByWidth(title(), 180), centerX, top + 110,
                PhoneColors.textPrimary(style));
        boolean error = state == CallSession.State.ENDED && !voicemailSent;
        String status = status();
        int statusColor = error ? PhoneColors.error(style) : PhoneColors.textSecondary(style);
        if (font.width(status) > 188) {
            var lines = font.split(Component.literal(status), 188);
            for (int i = 0; i < lines.size(); i++) {
                graphics.drawCenteredString(font, lines.get(i), centerX, top + 126 + i * 11, statusColor);
            }
        } else {
            graphics.drawCenteredString(font, status, centerX, top + 126, statusColor);
        }

        if (state == CallSession.State.ENDED) {
            if (canLeaveVoicemail() && !voicemailSent) {
                graphics.drawCenteredString(font, "Leave a voicemail?", centerX, top + 146,
                        PhoneColors.textSecondary(style));
                PhoneRenderer.drawPrimaryButton(graphics, style, left + 14, top + 192, 86, 22,
                        "Send", font);
                PhoneRenderer.drawSecondaryButton(graphics, style, left + 106, top + 192, 84, 22,
                        "Close", font);
            } else {
                PhoneRenderer.drawPrimaryButton(graphics, style, left + 60, top + 200, 84, 22,
                        "Done", font);
            }
        } else {
            int muteX = left + 18;
            int speakerX = left + 78;
            int endX = left + 138;
            int y = top + 200;
            boolean muted = state == CallSession.State.ACTIVE && ClientCallCache.muted();
            drawRound(graphics, style, muteX, y, muted ? PhoneColors.accent(style) : PhoneColors.bgTertiary(style),
                    "M", muted ? 0xFFFFFFFF : PhoneColors.textPrimary(style));
            drawRound(graphics, style, speakerX, y,
                    speaker ? PhoneColors.accent(style) : PhoneColors.bgTertiary(style),
                    "S", speaker ? 0xFFFFFFFF : PhoneColors.textPrimary(style));
            drawRound(graphics, style, endX, y, PhoneColors.error(style), "X", 0xFFFFFFFF);
            graphics.drawCenteredString(font, muted ? "Unmute" : "Mute", muteX + BUTTON_SIZE / 2, y + 54,
                    PhoneColors.textSecondary(style));
            graphics.drawCenteredString(font, "Speaker", speakerX + BUTTON_SIZE / 2, y + 54,
                    PhoneColors.textSecondary(style));
            graphics.drawCenteredString(font, "End", endX + BUTTON_SIZE / 2, y + 54,
                    PhoneColors.textSecondary(style));
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void drawRound(GuiGraphics graphics, PhoneStyle style, int x, int y, int fill, String glyph, int color) {
        PhoneRenderer.fillRoundedRect(graphics, style, x, y, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE / 2, fill);
        graphics.drawCenteredString(font, glyph, x + BUTTON_SIZE / 2, y + (BUTTON_SIZE - font.lineHeight) / 2, color);
    }

    private static boolean inside(double x, double y, int bx, int by, int w, int h) {
        return x >= bx && x < bx + w && y >= by && y < by + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        if (button == 0) {
            CallSession.State state = state();
            if (state == CallSession.State.ENDED) {
                if (canLeaveVoicemail() && !voicemailSent) {
                    if (inside(x, y, left + 14, top + 192, 86, 22)) {
                        sendVoicemail();
                        return true;
                    }
                    if (inside(x, y, left + 106, top + 192, 84, 22)) {
                        close();
                        return true;
                    }
                } else if (inside(x, y, left + 60, top + 200, 84, 22)) {
                    close();
                    return true;
                }
            } else {
                if (state == CallSession.State.ACTIVE && inside(x, y, left + 38, top + 153, 128, 22)) {
                    VideoCallClient.toggle();
                    return true;
                }
                int by = top + 200;
                if (inside(x, y, left + 18, by, BUTTON_SIZE, BUTTON_SIZE)) {
                    if (state == CallSession.State.ACTIVE) {
                        PacketDistributor.sendToServer(new CallControlPacket(
                                CallControlPacket.Action.MUTE, !ClientCallCache.muted()));
                    }
                    return true;
                }
                if (inside(x, y, left + 78, by, BUTTON_SIZE, BUTTON_SIZE)) {
                    speaker = !speaker;
                    return true;
                }
                if (inside(x, y, left + 138, by, BUTTON_SIZE, BUTTON_SIZE)) {
                    PacketDistributor.sendToServer(new CallControlPacket(CallControlPacket.Action.HANG_UP, false));
                    if (state == null) {
                        close();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public void removed() {
        VideoCallClient.stopForScreen();
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER && voicemailBox != null && voicemailBox.isFocused()) {
            sendVoicemail();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

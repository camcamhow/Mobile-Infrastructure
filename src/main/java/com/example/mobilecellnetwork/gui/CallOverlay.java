package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.CallControlPacket;
import com.example.mobilecellnetwork.network.ClientCallCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.voice.CallSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class CallOverlay {
    private static final int PANEL_X = 8;
    private static final int PANEL_WIDTH = 176;
    private CallOverlay() {
    }

    public static void onRender(RenderGuiEvent.Post event) {
        CallSession.State state = ClientCallCache.state();
        if (state == null) {
            return;
        }
        if (state == CallSession.State.ENDED
                && System.currentTimeMillis() - ClientCallCache.updatedAtMillis() > 3_000L) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen instanceof CallScreen) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int panelY = screenHeight / 2 - 45;
        int panelHeight = state == CallSession.State.RINGING ? 104 : 88;
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        PhoneRenderer.drawCard(graphics, style, PANEL_X, panelY, PANEL_WIDTH, panelHeight);
        var font = minecraft.font;

        if (state == CallSession.State.RINGING && ClientCallCache.incoming()) {
            graphics.drawString(font, "Incoming call from", PANEL_X + 10, panelY + 12,
                    PhoneColors.textSecondary(style), false);
            graphics.drawString(font, ClientCallCache.partnerName(), PANEL_X + 10, panelY + 25,
                    PhoneColors.textPrimary(style), false);
            drawButton(graphics, PANEL_X + 9, panelY + 58, 72, 23, "Answer", PhoneColors.success(style));
            drawButton(graphics, PANEL_X + 91, panelY + 58, 72, 23, "Decline", PhoneColors.error(style));
        } else if (state == CallSession.State.RINGING) {
            graphics.drawString(font, "Calling", PANEL_X + 10, panelY + 13,
                    PhoneColors.textSecondary(style), false);
            graphics.drawString(font, ClientCallCache.partnerName(), PANEL_X + 10, panelY + 27,
                    PhoneColors.textPrimary(style), false);
            drawButton(graphics, PANEL_X + 47, panelY + 55, 82, 23, "Cancel", PhoneColors.error(style));
        } else if (state == CallSession.State.ACTIVE) {
            graphics.drawString(font, "Call with " + ClientCallCache.partnerName(),
                    PANEL_X + 10, panelY + 12, PhoneColors.textPrimary(style), false);
            graphics.drawString(font, formatDuration(System.currentTimeMillis() - ClientCallCache.startedAtMillis()),
                    PANEL_X + 10, panelY + 30, PhoneColors.textSecondary(style), false);
            drawButton(graphics, PANEL_X + 9, panelY + 53, 72, 23,
                    ClientCallCache.muted() ? "Unmute" : "Mute", PhoneColors.accent(style));
            drawButton(graphics, PANEL_X + 91, panelY + 53, 72, 23, "Hang up", PhoneColors.error(style));
        } else {
            graphics.drawString(font, endedMessage(ClientCallCache.reason()),
                    PANEL_X + 10, panelY + 24, 0xFFFFFFFF, false);
        }
    }

    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        CallSession.State state = ClientCallCache.state();
        if (state == null || state == CallSession.State.ENDED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CallScreen) {
            return;
        }
        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight()
                / minecraft.getWindow().getHeight();
        int panelY = minecraft.getWindow().getGuiScaledHeight() / 2 - 45;
        if (state == CallSession.State.RINGING) {
            if (mouseY >= panelY + 58 && mouseY <= panelY + 81) {
                if (ClientCallCache.incoming() && mouseX >= PANEL_X + 9 && mouseX <= PANEL_X + 81) {
                    send(CallControlPacket.Action.ACCEPT, false);
                    event.setCanceled(true);
                } else if ((ClientCallCache.incoming() && mouseX >= PANEL_X + 91 && mouseX <= PANEL_X + 163)
                        || (!ClientCallCache.incoming() && mouseX >= PANEL_X + 47 && mouseX <= PANEL_X + 129)) {
                    send(ClientCallCache.incoming()
                            ? CallControlPacket.Action.DECLINE : CallControlPacket.Action.HANG_UP, false);
                    event.setCanceled(true);
                }
            }
        } else if (mouseY >= panelY + 53 && mouseY <= panelY + 76) {
            if (mouseX >= PANEL_X + 9 && mouseX <= PANEL_X + 81) {
                send(CallControlPacket.Action.MUTE, !ClientCallCache.muted());
                event.setCanceled(true);
            } else if (mouseX >= PANEL_X + 91 && mouseX <= PANEL_X + 163) {
                send(CallControlPacket.Action.HANG_UP, false);
                event.setCanceled(true);
            }
        }
    }

    private static void send(CallControlPacket.Action action, boolean muted) {
        PacketDistributor.sendToServer(new CallControlPacket(action, muted));
    }

    private static void drawButton(
            GuiGraphics graphics, int x, int y, int width, int height, String label, int color) {
        PhoneRenderer.fillRoundedRect(graphics, ClientPhoneCache.phoneStyle(),
                x, y, width, height, 10, color);
        graphics.drawCenteredString(Minecraft.getInstance().font, label, x + width / 2, y + 7, 0xFFFFFFFF);
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }

    private static String endedMessage(String reason) {
        if (reason.startsWith("LOW_POWER_ENABLED:")) {
            return "Call ended - " + reason.substring("LOW_POWER_ENABLED:".length())
                    + " enabled Low Power Mode";
        }
        return switch (reason) {
            case "TIMEOUT" -> "Missed call";
            case "DECLINED" -> "Call declined";
            case "OUT_OF_MINUTES" -> "Call ended - Out of minutes";
            case "PLAYER_DISCONNECTED" -> "Call ended - player disconnected";
            default -> "Call ended";
        };
    }
}

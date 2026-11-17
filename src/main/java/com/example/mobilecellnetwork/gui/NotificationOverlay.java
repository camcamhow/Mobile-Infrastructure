package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientSmsCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class NotificationOverlay {
    private NotificationOverlay() {
    }

    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null) {
            render(event.getGuiGraphics());
        }
    }

    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        render(event.getGuiGraphics());
    }

    private static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        try {
            int index = 0;
            for (var toast : ClientSmsCache.activeToasts(now)) {
                int width = Math.min(180, minecraft.getWindow().getGuiScaledWidth() - 16);
                long age = now - toast.createdAt();
                int x = minecraft.getWindow().getGuiScaledWidth() - width - 8
                        + (age < 250 ? (int) ((250 - age) / 250.0 * 24) : 0);
                int y = 8 + index++ * 38;
                int alpha = age < 250 ? (int) (age / 250.0 * 217) : 217;
                int color = alpha << 24 | 0x101820;
                graphics.fill(x, y, x + width, y + 34, color);
                graphics.fill(x, y, x + 2, y + 34, 0xFF47A7E8);
                graphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(toast.title(), width - 16),
                        x + 8, y + 5, 0xFFFFFFFF, false);
                graphics.drawString(minecraft.font, minecraft.font.plainSubstrByWidth(toast.message(), width - 16),
                        x + 8, y + 18, 0xFFD8E2EF, false);
            }
        } finally {
            graphics.pose().popPose();
        }
    }
}

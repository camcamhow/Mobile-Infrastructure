package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientSmsCache;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class NotificationOverlay {
    private NotificationOverlay() {
    }

    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        int index = 0;
        for (var toast : ClientSmsCache.activeToasts(System.currentTimeMillis())) {
            int width = 180;
            long age = System.currentTimeMillis() - toast.createdAt();
            int x = minecraft.getWindow().getGuiScaledWidth() - width - 8
                    + (age < 250 ? (int) ((250 - age) / 250.0 * 24) : 0);
            int y = 8 + index++ * 38;
            int alpha = age < 250 ? (int) (age / 250.0 * 217) : 217;
            int color = alpha << 24 | 0x101820;
            event.getGuiGraphics().fill(x, y, x + width, y + 34, color);
            event.getGuiGraphics().fill(x, y, x + 2, y + 34, 0xFF47A7E8);
            event.getGuiGraphics().drawString(minecraft.font, toast.title(), x + 8, y + 5, 0xFFFFFFFF, false);
            event.getGuiGraphics().drawString(minecraft.font, toast.message(), x + 8, y + 18, 0xFFD8E2EF, false);
        }
    }
}

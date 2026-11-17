package com.example.mobilecellnetwork.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.example.mobilecellnetwork.network.ClientPhoneCache;

public final class SnappleText {
    private static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "snapple");
    private static final ResourceLocation LIGHT_FONT =
            ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "snapple_light");
    private static final ResourceLocation ANDROID_FONT =
            ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "android_bold");

    private SnappleText() {
    }

    public static Component text(String text) {
        ResourceLocation selected = ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI ? ANDROID_FONT
                : PhoneColors.dark(ClientPhoneCache.phoneStyle()) ? FONT : LIGHT_FONT;
        return Component.literal(text).withStyle(style -> style.withFont(selected));
    }

    public static void draw(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawString(font, text(text), x, y, color, false);
    }

    public static void centered(GuiGraphics graphics, Font font, String text, int centerX, int y, int color) {
        Component label = text(text);
        graphics.drawString(font, label, centerX - font.width(label) / 2, y, color, false);
    }
}

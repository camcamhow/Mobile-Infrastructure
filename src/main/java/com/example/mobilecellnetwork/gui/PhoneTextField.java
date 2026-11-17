package com.example.mobilecellnetwork.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class PhoneTextField {
    private PhoneTextField() {
    }

    public static EditBox create(Font font, int x, int y, int width, int height,
                                 String hint, int maxLength, PhoneStyle style) {
        int horizontalPadding = Math.min(6, Math.max(0, width / 4));
        int verticalPadding = Math.min(2, Math.max(0, height / 4));
        EditBox field = new EditBox(font, x + horizontalPadding, y + verticalPadding + 1,
                width - horizontalPadding * 2, height - verticalPadding * 2, Component.literal(hint));
        field.setMaxLength(maxLength);
        field.setHint(Component.literal(hint));
        field.setBordered(false);
        field.setTextColor(PhoneColors.textPrimary(style));
        field.setTextColorUneditable(PhoneColors.textSecondary(style));
        return field;
    }

    public static void renderBackground(GuiGraphics graphics, PhoneStyle style,
                                        int x, int y, int width, int height) {
        PhoneRenderer.fillRoundedRect(graphics, style, x, y, width, height, 8, PhoneColors.divider(style));
        PhoneRenderer.fillRoundedRect(graphics, style, x + 1, y + 1, width - 2, height - 2, 7,
                PhoneColors.bgSecondary(style));
    }
}

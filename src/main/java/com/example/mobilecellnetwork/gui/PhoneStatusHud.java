package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class PhoneStatusHud {
    private static final ResourceLocation HUD_FONT = ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "android");
    private static final ResourceLocation ANDROID_BOLD_FONT =
            ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "android_bold");
    private PhoneStatusHud() {
    }

    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen()
                || minecraft.screen != null || !hasPhone(player)) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        var font = minecraft.font;
        SignalTier tier = ClientSignalCache.tier();
        boolean noService = tier == SignalTier.NO_SERVICE;
        String network = noService || ClientSignalCache.networkName().isBlank()
                ? "No Service" : ClientSignalCache.networkName();
        int percent = PhoneFrame.phoneChargePercent();
        int strength = noService ? 0 : (int) Math.round(Math.clamp(ClientSignalCache.strength(), 0.0D, 1.0D) * 100.0D);
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        String signal = noService ? "--" : tier.displayName() + " " + strength + "%";
        int displayedPercent = ClientPhoneCache.phoneDead() ? 0 : percent;
        String battery = displayedPercent + "%";
        int batteryColor = percent < 20 ? 0xFFE34B4B : percent <= 50 ? 0xFFFFD447 : 0xFF49D66B;
        int bars = noService ? 0 : Math.clamp(ClientSignalCache.bars(), 0, 4);

        int gap = 9;
        int iconGap = 5;
        int padding = 10;
        int panelHeight = 22;
        int barsWidth = 14;
        int batteryIconWidth = 15;
        int batteryWidth = batteryIconWidth + iconGap + font.width(label(battery));
        int fixedWidth = barsWidth + iconGap + font.width(label(signal))
                + batteryWidth + font.width(label(time)) + gap * 3;
        int availableNetworkWidth = Math.max(0, graphics.guiWidth() - padding * 2 - 8 - fixedWidth);
        if (font.width(label(network)) > availableNetworkWidth) {
            String suffix = "...";
            while (!network.isEmpty() && font.width(label(network + suffix)) > availableNetworkWidth) {
                network = network.substring(0, network.offsetByCodePoints(network.length(), -1));
            }
            network = font.width(label(network + suffix)) <= availableNetworkWidth ? network + suffix : "";
        }
        int total = font.width(label(network)) + fixedWidth;
        int x = (event.getGuiGraphics().guiWidth() - total) / 2;
        int panelY = Math.max(4, graphics.guiHeight() - 100);
        int textY = panelY + (panelHeight - font.lineHeight) / 2;
        int iconY = panelY + (panelHeight - 9) / 2;
        McnPrimitives.fillRoundedRect(graphics, x - padding, panelY,
                total + padding * 2, panelHeight, panelHeight / 2, 0xD91B2635);
        graphics.drawString(font, label(network), x, textY, noService ? 0xFFFF9999 : 0xFFFFFFFF, false);
        x += font.width(label(network)) + gap;
        for (int i = 0; i < 4; i++) {
            int h = 3 + i * 2;
            graphics.fill(x + i * 4, iconY + 9 - h, x + i * 4 + 2, iconY + 9,
                    i < bars ? 0xFFFFFFFF : 0xFF637080);
        }
        x += barsWidth + iconGap;
        graphics.drawString(font, label(signal), x, textY, noService ? 0xFFFF9999 : 0xFFE2EAF4, false);
        x += font.width(label(signal)) + gap;
        McnPrimitives.fillRoundedRectOutline(graphics, x, iconY, 13, 9, 2, 1, 0xFFE6EDF5);
        graphics.fill(x + 13, iconY + 3, x + 15, iconY + 6, 0xFFE6EDF5);
        graphics.fill(x + 2, iconY + 2, x + 2 + displayedPercent * 9 / 100, iconY + 7, batteryColor);
        graphics.drawString(font, label(battery), x + batteryIconWidth + iconGap, textY, 0xFFE6EDF5, false);
        x += batteryWidth + gap;
        graphics.drawString(font, label(time), x, textY, 0xFFC9D6E4, false);
    }

    private static Component label(String text) {
        ResourceLocation selected = ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI
                ? ANDROID_BOLD_FONT : HUD_FONT;
        return Component.literal(text).withStyle(style -> style.withFont(selected));
    }

    private static boolean hasPhone(net.minecraft.world.entity.player.Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack)) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack)) return true;
        }
        return false;
    }
}

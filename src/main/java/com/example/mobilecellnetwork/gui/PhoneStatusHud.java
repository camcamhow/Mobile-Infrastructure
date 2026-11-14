package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class PhoneStatusHud {
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
        String battery = (ClientPhoneCache.phoneDead() ? 0 : percent) + "%";
        int batteryColor = percent < 20 ? 0xFFE34B4B : percent <= 50 ? 0xFFFFD447 : 0xFF49D66B;
        int bars = noService ? 0 : Math.clamp(ClientSignalCache.bars(), 0, 4);

        int gap = 10;
        int barsWidth = 18;
        int batteryWidth = 20 + font.width(battery);
        int total = font.width(network) + gap + barsWidth + 4 + font.width(signal) + gap
                + batteryWidth + gap + font.width(time);
        int x = (event.getGuiGraphics().guiWidth() - total) / 2;
        int y = event.getGuiGraphics().guiHeight() - 66;
        graphics.fill(x - 6, y - 4, x + total + 6, y + 14, 0x99000000);
        graphics.drawString(font, network, x, y + 1, noService ? 0xFFE34B4B : 0xFFFFFFFF, true);
        x += font.width(network) + gap;
        for (int i = 0; i < 4; i++) {
            int h = 3 + i * 2;
            graphics.fill(x + i * 5, y + 10 - h, x + i * 5 + 3, y + 10, i < bars ? 0xFFFFFFFF : 0xFF555B63);
        }
        x += barsWidth + 4;
        graphics.drawString(font, signal, x, y + 1, noService ? 0xFFE34B4B : 0xFFE6E6E6, true);
        x += font.width(signal) + gap;
        graphics.fill(x, y, x + 16, y + 10, 0xFFE6E6E6);
        graphics.fill(x + 1, y + 1, x + 15, y + 9, 0xFF1A1D22);
        graphics.fill(x + 2, y + 2, x + 2 + Math.max(0, percent) * 12 / 100, y + 8, batteryColor);
        graphics.fill(x + 16, y + 3, x + 18, y + 7, 0xFFE6E6E6);
        graphics.drawString(font, battery, x + 21, y + 1, batteryColor, true);
        x += batteryWidth + gap;
        graphics.drawString(font, time, x, y + 1, 0xFFBFC5CC, true);
    }

    private static boolean hasPhone(net.minecraft.world.entity.player.Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.CELLPHONE.get())) return true;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(ModItems.CELLPHONE.get())) return true;
        }
        return false;
    }
}

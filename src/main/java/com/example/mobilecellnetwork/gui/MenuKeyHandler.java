package com.example.mobilecellnetwork.gui;

import net.minecraft.client.Minecraft;

final class MenuKeyHandler {
    private MenuKeyHandler() {
    }

    static boolean isInventoryKey(int keyCode, int scanCode) {
        return Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode);
    }
}

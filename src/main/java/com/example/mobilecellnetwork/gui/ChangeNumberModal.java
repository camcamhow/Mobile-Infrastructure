package com.example.mobilecellnetwork.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

public final class ChangeNumberModal extends ModalScreen {
    public ChangeNumberModal(Screen parent, String player, Consumer<List<String>> onConfirm) {
        super(parent, "Change Phone Number", new String[]{"Player name", "New number"},
                new String[]{player, ""}, onConfirm);
    }
}

package com.example.mobilecellnetwork.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

public final class ChangePlanModal extends ModalScreen {
    public ChangePlanModal(Screen parent, String player, Consumer<List<String>> onConfirm) {
        super(parent, "Change Player Plan", new String[]{"Player name", "Plan ID"},
                new String[]{player, ""}, onConfirm);
    }
}

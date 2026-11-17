package com.example.mobilecellnetwork.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;

public final class NewNetworkModal extends ModalScreen {
    public NewNetworkModal(Screen parent, Consumer<List<String>> onConfirm) {
        super(parent, "New Network", new String[]{"Network name"}, new String[]{""}, onConfirm);
    }
}

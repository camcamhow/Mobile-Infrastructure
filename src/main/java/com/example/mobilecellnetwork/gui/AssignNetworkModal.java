package com.example.mobilecellnetwork.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AssignNetworkModal extends Screen {
    private final Screen parent;
    private final String antennaLabel;
    private final String currentNetwork;
    private final List<NetworkOption> networks;
    private final Consumer<String> onConfirm;
    private int selected = -1;
    private int scroll;
    private int modalX;
    private int modalY;
    private int modalW;
    private int modalH;
    private int listTop;
    private int listBottom;

    public AssignNetworkModal(Screen parent, String antennaLabel, String currentNetwork,
                              String networkOptions, Consumer<String> onConfirm) {
        super(Component.literal("Assign Network"));
        this.parent = parent;
        this.antennaLabel = antennaLabel;
        this.currentNetwork = currentNetwork;
        this.onConfirm = onConfirm;
        List<NetworkOption> parsed = new ArrayList<>();
        for (String entry : networkOptions.split(";")) {
            int delimiter = entry.indexOf('=');
            if (delimiter > 0) {
                parsed.add(new NetworkOption(entry.substring(0, delimiter), entry.substring(delimiter + 1)));
            }
        }
        this.networks = List.copyOf(parsed);
        for (int index = 0; index < networks.size(); index++) {
            if (networks.get(index).name().equals(currentNetwork)) {
                selected = index;
                break;
            }
        }
    }

    @Override
    protected void init() {
        modalW = Math.min(300, width - 20);
        modalH = Math.min(220, height - 20);
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;
        listTop = modalY + 60;
        listBottom = modalY + modalH - 30;
        int rowHeight = 18;
        int firstVisible = Math.max(0, scroll / rowHeight);
        int visibleRows = Math.max(0, (listBottom - listTop - 20) / rowHeight);
        int lastVisible = Math.min(networks.size(), firstVisible + visibleRows);
        for (int index = firstVisible; index < lastVisible; index++) {
            int item = index;
            int y = listTop + (index - firstVisible) * rowHeight;
            addRenderableWidget(Button.builder(Component.literal(
                            (selected == index ? "(*) " : "( ) ") + networks.get(index).name()),
                            button -> {
                                selected = item;
                                rebuildWidgets();
                            })
                    .bounds(modalX + 12, y, modalW - 24, 16).build());
        }
        int noneY = listTop + visibleRows * rowHeight;
        addRenderableWidget(Button.builder(Component.literal(selected < 0 ? "(*) None (unassign)" : "( ) None (unassign)"),
                        button -> {
                            selected = -1;
                            rebuildWidgets();
                        })
                .bounds(modalX + 12, noneY, modalW - 24, 16).build());
        int footerY = modalY + modalH - 25;
        addRenderableWidget(Button.builder(Component.literal("OK"), button -> {
                    onConfirm.accept(selected < 0 ? "default" : networks.get(selected).id());
                    returnToParent();
                }).bounds(modalX + modalW - 116, footerY, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(modalX + modalW - 62, footerY, 54, 18).build());
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        init();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF1E2A38);
        graphics.fill(modalX + 2, modalY + 2, modalX + modalW - 2, modalY + modalH - 2, 0xFF0F1620);
        graphics.renderOutline(modalX, modalY, modalW, modalH, 0xFF3A4A5A);
        graphics.fill(modalX, modalY, modalX + modalW, modalY + 2, PhoneColors.ACCENT_TEXT);
        graphics.drawString(font, "Assign Network", modalX + 12, modalY + 10,
                PhoneColors.PRIMARY_TEXT, true);
        graphics.drawString(font, fit(antennaLabel, modalW - 24), modalX + 12, modalY + 27,
                PhoneColors.SECONDARY_TEXT, true);
        for (var child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, networks.size() * 18 - Math.max(0, listBottom - listTop - 20));
        int next = Math.clamp(scroll - (int) Math.signum(scrollY) * 18, 0, max);
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    private void returnToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record NetworkOption(String id, String name) {
    }
}

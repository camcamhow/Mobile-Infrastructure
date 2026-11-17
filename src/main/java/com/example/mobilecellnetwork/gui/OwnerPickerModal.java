package com.example.mobilecellnetwork.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OwnerPickerModal extends Screen {
    private final Screen parent;
    private final Consumer<String> onSelect;
    private final List<String> owners = new ArrayList<>();
    private final String currentOwner;
    private int scroll;
    private int selected;
    private int modalX;
    private int modalY;
    private int modalW;
    private int modalH;

    public OwnerPickerModal(Screen parent, Consumer<String> onSelect) {
        this(parent, "Unknown", onSelect);
    }

    public OwnerPickerModal(Screen parent, String currentOwner, Consumer<String> onSelect) {
        super(Component.literal("Set Owner"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.currentOwner = currentOwner;
    }

    @Override
    protected void init() {
        owners.clear();
        owners.add("Keep current owner");
        owners.add("Server");
        if (minecraft != null && minecraft.getConnection() != null) {
            minecraft.getConnection().getOnlinePlayers().stream()
                    .map(info -> info.getProfile().getName())
                    .filter(name -> !name.isBlank())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(owners::add);
        }
        selected = Math.clamp(selected, 0, owners.size() - 1);
        modalW = Math.min(300, width - 20);
        modalH = Math.min(220, height - 20);
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;
        int listTop = modalY + 36;
        int listBottom = modalY + modalH - 30;
        int first = Math.max(0, scroll / 18);
        int visible = Math.max(1, (listBottom - listTop) / 18);
        for (int index = first; index < Math.min(owners.size(), first + visible); index++) {
            int item = index;
            String name = owners.get(index);
            addRenderableWidget(Button.builder(Component.literal(
                            (selected == index ? "(*) " : "( ) ") + name),
                            button -> {
                                selected = item;
                                rebuildWidgets();
                            })
                    .bounds(modalX + 12, listTop + (index - first) * 18, modalW - 24, 16).build());
        }
        addRenderableWidget(Button.builder(Component.literal("OK"), button -> {
                    String owner = owners.get(selected);
                    if (!owner.equals("Keep current owner")) {
                        onSelect.accept(owner);
                    }
                    returnToParent();
                }).bounds(modalX + modalW - 116, modalY + modalH - 26, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(modalX + modalW - 62, modalY + modalH - 26, 54, 18).build());
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
        graphics.drawString(font, "Set Owner", modalX + 12, modalY + 10, PhoneColors.PRIMARY_TEXT, true);
        graphics.drawString(font, "Current owner: " + currentOwner,
                modalX + 12, modalY + 23, PhoneColors.SECONDARY_TEXT, true);
        for (var child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listHeight = modalH - 66;
        int max = Math.max(0, owners.size() * 18 - listHeight);
        int next = Math.clamp(scroll - (int) Math.signum(scrollY) * 18, 0, max);
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
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
}

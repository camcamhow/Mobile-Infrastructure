package com.example.mobilecellnetwork.gui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RoamingPolicyModal extends Screen {
    private static final List<String> POLICIES = List.of(
            "INHERIT", "ALLOW_ALL", "DENY_ALL", "WHITELIST_PLAYERS", "WHITELIST_NETWORKS", "PAID_ROAMING");
    private final Screen parent;
    private final String initialPolicy;
    private final String initialWhitelist;
    private final Consumer<String> onSelect;
    private int selected;
    private EditBox whitelist;
    private int modalX;
    private int modalY;
    private int modalW;
    private int modalH;

    public RoamingPolicyModal(Screen parent, String policy, String whitelist, Consumer<String> onSelect) {
        super(Component.literal("Roaming Policy"));
        this.parent = parent;
        this.initialPolicy = policy;
        this.initialWhitelist = whitelist;
        this.onSelect = onSelect;
        this.selected = Math.max(0, POLICIES.indexOf(policy));
    }

    @Override
    protected void init() {
        modalW = Math.min(300, width - 20);
        modalH = Math.min(220, height - 20);
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> cycle(-1))
                .bounds(modalX + 12, modalY + 50, 24, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> cycle(1))
                .bounds(modalX + modalW - 36, modalY + 50, 24, 18).build());
        whitelist = new EditBox(font, modalX + 12, modalY + 112, modalW - 24, 18,
                Component.literal("Whitelist"));
        whitelist.setMaxLength(256);
        whitelist.setValue(initialWhitelist);
        whitelist.setHint(Component.literal("Players or network IDs, comma separated"));
        addRenderableWidget(whitelist);
        addRenderableWidget(Button.builder(Component.literal("OK"), button -> {
                    onSelect.accept(POLICIES.get(selected) + "|" + whitelist.getValue());
                    returnToParent();
                }).bounds(modalX + modalW - 116, modalY + modalH - 26, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(modalX + modalW - 62, modalY + modalH - 26, 54, 18).build());
    }

    private void cycle(int amount) {
        selected = Math.floorMod(selected + amount, POLICIES.size());
        if (whitelist != null) {
            String current = whitelist.getValue();
            rebuildWidgets();
            whitelist.setValue(current);
        }
    }

    @Override
    protected void rebuildWidgets() {
        String current = whitelist == null ? initialWhitelist : whitelist.getValue();
        clearWidgets();
        init();
        if (whitelist != null) {
            whitelist.setValue(current);
        }
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
        graphics.drawString(font, "Roaming Policy", modalX + 12, modalY + 12, PhoneColors.PRIMARY_TEXT, true);
        graphics.drawCenteredString(font, POLICIES.get(selected), width / 2, modalY + 55,
                PhoneColors.ACCENT_TEXT);
        graphics.drawString(font, "Whitelist entries (optional)", modalX + 12, modalY + 96,
                PhoneColors.SECONDARY_TEXT, true);
        for (var child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
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

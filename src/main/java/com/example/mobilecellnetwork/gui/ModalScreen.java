package com.example.mobilecellnetwork.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModalScreen extends Screen {
    private int modalX;
    private int modalY;
    private int modalW;
    private int modalH;
    private int scroll;
    private int contentTop;
    private int contentBottom;
    private final Screen parent;
    private final String[] labels;
    private final String[] initialValues;
    private final Consumer<List<String>> onConfirm;
    private final List<EditBox> inputs = new ArrayList<>();

    public ModalScreen(Screen parent, String title, String[] labels, String[] initialValues,
                       Consumer<List<String>> onConfirm) {
        super(Component.literal(title));
        this.parent = parent;
        this.labels = labels.clone();
        this.initialValues = initialValues.clone();
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        modalW = Math.min(300, width - 20);
        modalH = Math.min(220, height - 20);
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;
        contentTop = modalY + 42;
        contentBottom = modalY + modalH - 34;
        inputs.clear();
        int rowHeight = 38;
        int first = Math.max(0, scroll / rowHeight);
        int visible = Math.max(1, (contentBottom - contentTop) / rowHeight);
        int last = Math.min(labels.length, first + visible);
        for (int index = first; index < last; index++) {
            int y = contentTop + (index - first) * rowHeight;
            EditBox input = new EditBox(font, modalX + 12, y + 13, modalW - 24, 18,
                    Component.literal(labels[index]));
            input.setMaxLength(128);
            input.setValue(initialValues[index]);
            input.setHint(Component.literal(labels[index]));
            addRenderableWidget(input);
            inputs.add(input);
        }
        int buttonY = modalY + modalH - 26;
        addRenderableWidget(Button.builder(Component.literal("OK"), button -> confirm())
                .bounds(modalX + modalW - 116, buttonY, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(modalX + modalW - 62, buttonY, 54, 18).build());
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
        graphics.drawString(font, title, modalX + 12, modalY + 12, PhoneColors.PRIMARY_TEXT, true);
        int first = Math.max(0, scroll / 38);
        int visible = Math.max(1, (contentBottom - contentTop) / 38);
        for (int index = first; index < Math.min(labels.length, first + visible); index++) {
            graphics.drawString(font, labels[index], modalX + 12,
                    contentTop + (index - first) * 38,
                    PhoneColors.SECONDARY_TEXT, true);
        }
        for (var child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, labels.length * 38 - (contentBottom - contentTop));
        int next = Math.clamp(scroll - (int) Math.signum(scrollY) * 38, 0, maxScroll);
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
        return true;
    }

    protected void confirm() {
        onConfirm.accept(inputValues());
        returnToParent();
    }

    protected List<String> inputValues() {
        return inputs.stream().map(EditBox::getValue).toList();
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    private void returnToParent() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

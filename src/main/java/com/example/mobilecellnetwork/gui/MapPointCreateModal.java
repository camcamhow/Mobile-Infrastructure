package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.data.MapPointOfInterest;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MapPointCreateModal extends Screen {
    private final Screen parent;
    private final BiConsumer<String, MapPointOfInterest.Visibility> onConfirm;
    private int modalX;
    private int modalY;
    private int modalW;
    private int modalH;
    private EditBox nameInput;
    private MapPointOfInterest.Visibility visibility = MapPointOfInterest.Visibility.PERSONAL;

    public MapPointCreateModal(Screen parent,
            BiConsumer<String, MapPointOfInterest.Visibility> onConfirm) {
        super(Component.literal("Add Map Point"));
        this.parent = parent;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        modalW = Math.min(300, width - 20);
        modalH = Math.min(220, height - 20);
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;
        nameInput = new EditBox(font, modalX + 12, modalY + 58, modalW - 24, 20,
                Component.literal("Point name"));
        nameInput.setMaxLength(64);
        nameInput.setHint(Component.literal("Point name"));
        addRenderableWidget(nameInput);
        addRenderableWidget(Button.builder(Component.literal("Visibility: " + visibilityName()), button -> {
            visibility = switch (visibility) {
                case PERSONAL -> MapPointOfInterest.Visibility.PUBLIC;
                case PUBLIC -> MapPointOfInterest.Visibility.PRIVATE;
                case PRIVATE -> MapPointOfInterest.Visibility.PERSONAL;
            };
            button.setMessage(Component.literal("Visibility: " + visibilityName()));
        }).bounds(modalX + 12, modalY + 91, modalW - 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Create"), button -> {
            String name = nameInput.getValue().trim();
            if (!name.isEmpty()) {
                onConfirm.accept(name, visibility);
                returnToParent();
            }
        }).bounds(modalX + modalW - 120, modalY + modalH - 28, 52, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(modalX + modalW - 62, modalY + modalH - 28, 54, 20).build());
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
        graphics.drawString(font, "Add Map Point", modalX + 12, modalY + 14, PhoneColors.PRIMARY_TEXT, true);
        graphics.drawString(font, "Choose who can see this location.", modalX + 12, modalY + 38,
                PhoneColors.SECONDARY_TEXT, true);
        for (var child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private String visibilityName() {
        return switch (visibility) {
            case PERSONAL -> "Personal";
            case PUBLIC -> "Public";
            case PRIVATE -> "Private";
        };
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

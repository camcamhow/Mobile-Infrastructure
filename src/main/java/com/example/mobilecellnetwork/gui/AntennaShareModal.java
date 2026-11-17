package com.example.mobilecellnetwork.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AntennaShareModal extends Screen {
    private final Screen parent;
    private final String title;
    private final List<NetworkOption> networks;
    private final Consumer<String> onSelect;
    private int selectedIndex;
    private int scroll;
    private int modalX;
    private int modalY;
    private int modalW;
    private int modalH;

    public AntennaShareModal(Screen parent, String title, String options,
                             String filterIds, Consumer<String> onSelect) {
        super(Component.literal(title));
        this.parent = parent;
        this.title = title;
        this.onSelect = onSelect;
        java.util.Set<String> included = filterIds == null || filterIds.isBlank()
                ? java.util.Set.of() : java.util.Set.of(filterIds.split(","));
        List<NetworkOption> parsed = new ArrayList<>();
        for (String entry : options.split(";")) {
            int delimiter = entry.indexOf('=');
            if (delimiter > 0) {
                NetworkOption option = new NetworkOption(entry.substring(0, delimiter),
                        entry.substring(delimiter + 1));
                if (included.isEmpty() || included.contains(option.id())) {
                    parsed.add(option);
                }
            }
        }
        networks = List.copyOf(parsed);
    }

    @Override
    protected void init() {
        modalW = Math.min(300, width - 20);
        modalH = Math.min(220, height - 20);
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;
        int visible = Math.max(1, (modalH - 90) / 28);
        int first = Math.clamp(scroll, 0, Math.max(0, networks.size() - visible));
        int last = Math.min(networks.size(), first + visible);
        for (int i = first; i < last; i++) {
            int index = i;
            int y = modalY + 42 + (i - first) * 26;
            addRenderableWidget(Button.builder(Component.literal(networks.get(index).name()),
                            button -> {
                                selectedIndex = index;
                                rebuildWidgets();
                            })
                    .bounds(modalX + 12, y, modalW - 24, 22).build());
        }
        int footer = modalY + modalH - 26;
        Button select = Button.builder(Component.literal("Select"), button -> {
                    if (!networks.isEmpty()) {
                        onSelect.accept(networks.get(selectedIndex).id());
                        returnToParent();
                    }
                }).bounds(modalX + modalW - 116, footer, 48, 18).build();
        select.active = !networks.isEmpty();
        addRenderableWidget(select);
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(modalX + modalW - 62, footer, 54, 18).build());
    }

    @Override
    public void rebuildWidgets() {
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
        if (networks.isEmpty()) {
            graphics.drawString(font, "No matching networks.", modalX + 12, modalY + 44,
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
        int visible = Math.max(1, (modalH - 90) / 28);
        int next = Math.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, networks.size() - visible));
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

    private record NetworkOption(String id, String name) { }
}

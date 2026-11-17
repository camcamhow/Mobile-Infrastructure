package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.gui.admin.AdminColors;
import com.example.mobilecellnetwork.gui.admin.AdminPrimitives;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SignalBoosterScreen extends AbstractContainerScreen<SignalBoosterMenu> {
    private final List<BoosterButton> rows = new ArrayList<>();
    private BoosterButton previous;
    private BoosterButton next;
    private BoosterMenuLayout layout;

    public SignalBoosterScreen(SignalBoosterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        layout = BoosterMenuLayout.fit(width, height);
        imageWidth = layout.width();
        imageHeight = layout.height();
        super.init();
        rows.clear();
        addRenderableWidget(new BoosterButton(leftPos + imageWidth - 66, topPos + 5, 54, 18,
                text("close"), button -> onClose()));
        for (int i = 0; i < SignalBoosterMenu.PAGE_SIZE; i++) {
            final int buttonId = i + 3;
            rows.add(addRenderableWidget(new BoosterButton(leftPos + 12, topPos + layout.rowTop(i),
                    imageWidth - 24, layout.rowHeight(), "", button -> send(buttonId))));
        }
        int navigationWidth = layout.navigationWidth();
        previous = addRenderableWidget(new BoosterButton(leftPos + 12, topPos + layout.footerY(),
                navigationWidth, 20, text("previous"), button -> send(0)));
        next = addRenderableWidget(new BoosterButton(leftPos + 16 + navigationWidth,
                topPos + layout.footerY(), navigationWidth, 20, text("next"), button -> send(1)));
        addRenderableWidget(new BoosterButton(leftPos + imageWidth - navigationWidth - 12,
                topPos + layout.footerY(), navigationWidth, 20, text("refresh"), button -> send(2)));
        updateButtons();
    }

    private static String text(String key) {
        return Component.translatable("gui.mobilecellnetwork.booster." + key).getString();
    }

    private void send(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        int page = menu.value(5);
        previous.active = page > 0;
        next.active = page + 1 < menu.pageCount();
        for (int row = 0; row < rows.size(); row++) {
            int index = page * SignalBoosterMenu.PAGE_SIZE + row;
            var button = rows.get(row);
            button.visible = index < menu.options().size();
            if (button.visible) {
                var option = menu.options().get(index);
                button.setMessage(Component.literal((index == menu.value(2) ? "> " : "")
                        + option.label().getString()));
                button.active = option.id().isEmpty() || option.percent() > 0;
            }
        }
    }

    private void drawFitted(GuiGraphics graphics, String text, int x, int y, int width, int height,
                            int color, boolean centered, float maximumScale) {
        float scale = Math.min(maximumScale, (height - 2) / (float) font.lineHeight);
        int available = Math.max(0, (int) ((width - 8) / scale));
        if (font.width(text) > available) {
            text = font.plainSubstrByWidth(text, Math.max(0, available - font.width("..."))) + "...";
        }
        graphics.pose().pushPose();
        graphics.pose().translate(centered ? x + (width - font.width(text) * scale) / 2 : x + 4,
                y + (height - font.lineHeight * scale) / 2, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AdminPrimitives.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        AdminPrimitives.drawHeader(graphics, leftPos, topPos, imageWidth, 28);
        AdminPrimitives.drawContentArea(graphics, leftPos + 1, topPos + 28, imageWidth - 2, imageHeight - 29);
        AdminPrimitives.drawCard(graphics, leftPos + 10, topPos + 33, imageWidth - 20, 63);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawFitted(graphics, title.getString(), 8, 5, imageWidth - 78, 18,
                AdminColors.TEXT_PRIMARY, false, 1);
        String state = menu.value(1) == 2 ? text("incomplete")
                : menu.value(4) == 1 ? text("bypass") : menu.value(1) == 1 ? text("powered") : text("no_power");
        drawFitted(graphics, menu.value(0) + " / 10000 FE | " + state, 14, 36, imageWidth - 28, 14,
                AdminColors.TEXT_PRIMARY, false, 0.9F);
        String upstream = Component.translatable("gui.mobilecellnetwork.booster.upstream", menu.value(3)).getString();
        drawFitted(graphics, upstream, 14, 50, imageWidth - 28, 14,
                AdminColors.TEXT_SECONDARY, false, 0.9F);
        int selected = menu.value(2);
        Component network = selected >= 0 && selected < menu.options().size()
                ? menu.options().get(selected).label() : Component.translatable("gui.mobilecellnetwork.booster.refresh");
        String tuned = Component.translatable("gui.mobilecellnetwork.booster.selected", network).getString();
        drawFitted(graphics, tuned, 14, 64, imageWidth - 28, 14, AdminColors.TEXT_PRIMARY, false, 0.9F);
        drawFitted(graphics, text("range"), 14, 78, imageWidth - 28, 14,
                AdminColors.TEXT_SECONDARY, false, 0.9F);
        int pageX = 20 + 2 * layout.navigationWidth();
        drawFitted(graphics, (menu.value(5) + 1) + "/" + menu.pageCount(), pageX, layout.footerY(),
                imageWidth - layout.navigationWidth() - 12 - pageX, 20,
                AdminColors.TEXT_SECONDARY, true, 0.85F);
        if (menu.options().stream().noneMatch(option -> option.percent() > 0)) {
            drawFitted(graphics, text("no_signal"), 8, imageHeight - 12, imageWidth - 16, 11,
                    AdminColors.TEXT_SECONDARY, false, 0.75F);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int row = 0; row < rows.size(); row++) {
            int index = menu.value(5) * SignalBoosterMenu.PAGE_SIZE + row;
            if (rows.get(row).visible && rows.get(row).isHoveredOrFocused() && index < menu.options().size()) {
                var option = menu.options().get(index);
                String tooltip = option.label().getString();
                if (!option.id().isEmpty()) tooltip += " [" + option.id() + "]";
                graphics.renderTooltip(font, font.split(Component.literal(tooltip), Math.min(280, width - 24)),
                        mouseX, mouseY);
            }
        }
    }

    private final class BoosterButton extends Button {
        private BoosterButton(int x, int y, int width, int height, String label, OnPress action) {
            super(x, y, width, height, Component.literal(label), action, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            AdminPrimitives.drawListRow(graphics, getX(), getY(), getWidth(), getHeight(),
                    active && isHoveredOrFocused(), active && isFocused());
            drawFitted(graphics, getMessage().getString(), getX(), getY(), getWidth(), getHeight(),
                    active ? AdminColors.TEXT_PRIMARY : AdminColors.TEXT_MUTED, true, 0.9F);
        }
    }
}

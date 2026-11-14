package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientTowerCache;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.network.TowerListPacket;
import com.example.mobilecellnetwork.network.TowerSelectPacket;
import com.example.mobilecellnetwork.data.TowerPreferenceMode;
import com.example.mobilecellnetwork.network.packet.SetTowerPreferencePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class TowerSelectScreen extends Screen implements PhoneNavigableScreen {
    private static final int VISIBLE = 7;
    private final PhoneMenu menu;
    private int scroll;
    private int ticks;

    public TowerSelectScreen(PhoneMenu menu) {
        super(Component.literal("Cell Tower"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(new TowerSelectPacket(TowerSelectPacket.REQUEST_LIST, 0L));
    }

    @Override
    public void tick() {
        if (++ticks % 40 == 0) {
            PacketDistributor.sendToServer(new TowerSelectPacket(TowerSelectPacket.REQUEST_LIST, 0L));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getScreenY(this);
        PhoneFrame.renderHeader(graphics, this, "Cell Tower", true);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        TowerListPacket data = ClientTowerCache.latest();
        int mx = (int) PhoneFrame.displayMouseX(this, mouseX);
        int my = (int) PhoneFrame.displayMouseY(this, mouseY);
        boolean auto = data.preferredPos() == Long.MIN_VALUE;
        int total = data.towers().size() + 1;
        scroll = Math.clamp(scroll, 0, Math.max(0, total - VISIBLE));
        for (int i = scroll; i < Math.min(total, scroll + VISIBLE); i++) {
            int y = top + 64 + (i - scroll) * 38;
            boolean selected;
            String title;
            String subtitle;
            if (i == 0) {
                selected = auto;
                title = "Automatic";
                subtitle = "Connect to the strongest tower";
            } else {
                TowerListPacket.Entry entry = data.towers().get(i - 1);
                selected = entry.pos() == data.preferredPos() || (auto && entry.pos() == data.connectedPos());
                title = entry.name() + (entry.roaming() ? " (Roaming)" : "");
                subtitle = SignalTier.displayName(entry.tier()) + " - " + entry.strengthPercent() + "%";
            }
            PhoneRenderer.drawCard(graphics, style, left + 8, y, PhoneFrame.getScreenW() - 16, 34);
            if (selected) {
                graphics.renderOutline(left + 8, y, PhoneFrame.getScreenW() - 16, 34,
                        PhoneColors.accent(style));
            }
            graphics.drawString(font, font.plainSubstrByWidth(title, 150), left + 14, y + 6,
                    PhoneColors.textPrimary(style), false);
            graphics.drawString(font, font.plainSubstrByWidth(subtitle, 150), left + 14, y + 19,
                    PhoneColors.textSecondary(style), false);
            if (selected) {
                graphics.drawString(font, "\u2713", left + PhoneFrame.getScreenW() - 24, y + 13,
                        PhoneColors.accent(style), false);
            }
        }
        if (data.towers().isEmpty()) {
            graphics.drawCenteredString(font, "No towers in range", left + PhoneFrame.getScreenW() / 2,
                    top + 112, PhoneColors.textSecondary(style));
        }
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getScreenY(this);
        TowerListPacket data = ClientTowerCache.latest();
        int total = data.towers().size() + 1;
        if (button == 0 && x >= left + 8 && x < left + PhoneFrame.getScreenW() - 8) {
            for (int i = scroll; i < Math.min(total, scroll + VISIBLE); i++) {
                int rowY = top + 64 + (i - scroll) * 38;
                if (y >= rowY && y < rowY + 34) {
                    if (i == 0) {
                        PacketDistributor.sendToServer(new SetTowerPreferencePacket(
                                TowerPreferenceMode.AUTOMATIC_FASTEST, null));
                    } else {
                        PacketDistributor.sendToServer(new SetTowerPreferencePacket(
                                TowerPreferenceMode.PINNED,
                                net.minecraft.core.BlockPos.of(data.towers().get(i - 1).pos())));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll + (scrollY < 0 ? 1 : -1));
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

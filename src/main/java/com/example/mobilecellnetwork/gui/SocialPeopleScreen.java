package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.SocialFollowPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SocialPeopleScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private int scroll;

    public SocialPeopleScreen(PhoneMenu menu) {
        super(Component.literal("Find people"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        minecraft.setScreen(new SocialsScreen(menu));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneFrame.renderHeader(graphics, this, "Find people", true);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        var people = ClientPhoneCache.onlinePlayers();
        int first = Math.min(scroll, Math.max(0, people.size() - 7));
        if (people.isEmpty()) {
            PhoneRenderer.drawCard(graphics, style, left + 8, top + 66, 188, 48);
            graphics.drawCenteredString(font, "No other players online",
                    left + 102, top + 83, PhoneColors.textSecondary(style));
        }
        for (int index = first; index < Math.min(people.size(), first + 7); index++) {
            var person = people.get(index);
            boolean following = ClientPhoneCache.followedPlayers().stream()
                    .anyMatch(entry -> entry.id().equals(person.id()));
            int y = top + 66 + (index - first) * 32;
            PhoneRenderer.drawCard(graphics, style, left + 8, y, 188, 28);
            graphics.drawString(font, font.plainSubstrByWidth(person.name(), 112),
                    left + 14, y + 4, PhoneColors.textPrimary(style), true);
            graphics.drawString(font, "#" + person.number(), left + 14, y + 16,
                    PhoneColors.textSecondary(style), true);
            PhoneRenderer.drawSecondaryButton(graphics, style, left + 138, y + 4,
                    52, 20, following ? "Following" : "Follow", font);
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        if (button == 0) {
            int left = PhoneFrame.getScreenX(this);
            int top = PhoneFrame.getContentTop(this);
            var people = ClientPhoneCache.onlinePlayers();
            int first = Math.min(scroll, Math.max(0, people.size() - 7));
            for (int index = first; index < Math.min(people.size(), first + 7); index++) {
                int rowY = top + 66 + (index - first) * 32;
                if (x >= left + 138 && x < left + 190 && y >= rowY + 4 && y < rowY + 24) {
                    PacketDistributor.sendToServer(new SocialFollowPacket(people.get(index).id().toString()));
                    return true;
                }
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1), 0,
                    Math.max(0, ClientPhoneCache.onlinePlayers().size() - 7));
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

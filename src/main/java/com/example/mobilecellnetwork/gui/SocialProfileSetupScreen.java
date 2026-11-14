package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientSocialProfileCache;
import com.example.mobilecellnetwork.network.SocialProfilePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SocialProfileSetupScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private EditBox username;
    private byte[] avatar = new byte[0];
    private int profileColor = 0xFF4F7CAC;
    private String photoLabel = "No photo: using your initial";
    private String error = "";

    public SocialProfileSetupScreen(PhoneMenu menu) {
        super(Component.literal("Set up Socials"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    protected void init() {
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        username = PhoneTextField.create(font, left + 18, top + 158, 168, 22,
                "Username", 24, com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle());
        username.setValue(ClientSocialProfileCache.username());
        addRenderableWidget(username);
        if (profileColor == 0xFF4F7CAC) {
            profileColor = java.util.concurrent.ThreadLocalRandom.current().nextInt(0xFF000000, 0xFFFFFFFF);
        }
        addRenderableWidget(new PhoneActionButton(left + 24, top + 193, 156, 22,
                "Choose photo (optional)", button -> choosePhoto()));
        addRenderableWidget(new PhoneActionButton(left + 24, top + 263, 156, 24,
                "Continue", button -> saveProfile()));
    }

    private void choosePhoto() {
        minecraft.setScreen(new PhotoImportScreen(this, menu, photo -> {
            avatar = photo.bytes();
            photoLabel = photo.name();
            error = "";
        }));
    }

    private void saveProfile() {
        String value = username.getValue().strip();
        if (value.length() < 2 || value.length() > 24
                || !value.matches("[\\p{L}\\p{N}_ -]+")) {
            error = "Use 2-24 letters, numbers, spaces, _ or -.";
            return;
        }
        PacketDistributor.sendToServer(new SocialProfilePacket(value, profileColor, avatar));
        ClientSocialProfileCache.update(new com.example.mobilecellnetwork.network.SocialProfileSyncPacket(
                value, profileColor, avatar));
        minecraft.setScreen(new SocialsScreen(menu));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneFrame.renderHeader(graphics, this, "Create profile", true);
        PhoneStyle style = com.example.mobilecellnetwork.network.ClientPhoneCache.phoneStyle();
        PhoneRenderer.drawCard(graphics, style, left + 16, top + 64, 172, 78);
        if (avatar.length > 0) {
            PhonePhotoLibrary.drawThumbnail(graphics, "social-profile-avatar", avatar,
                    left + 25, top + 76, 46, 46);
        } else {
            PhoneRenderer.fillRoundedRect(graphics, style, left + 25, top + 76,
                    46, 46, 23, profileColor);
            String initial = username == null || username.getValue().isBlank()
                    ? "?" : username.getValue().strip().substring(0, 1).toUpperCase(java.util.Locale.ROOT);
            graphics.drawCenteredString(font, initial, left + 48, top + 94, 0xFFFFFFFF);
        }
        graphics.drawString(font, "Choose a username", left + 80, top + 83,
                PhoneColors.textPrimary(style), true);
        graphics.drawString(font, "Photo optional", left + 80, top + 99,
                PhoneColors.textSecondary(style), true);
        PhoneTextField.renderBackground(graphics, style, left + 18, top + 158, 168, 22);
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 24, top + 193, 156, 22,
                "Choose photo (optional)", font);
        graphics.drawCenteredString(font, font.plainSubstrByWidth(photoLabel, 172),
                left + 102, top + 224, PhoneColors.textSecondary(style));
        PhoneRenderer.drawPrimaryButton(graphics, style, left + 24, top + 263, 156, 24,
                "Continue", font);
        if (!error.isBlank()) {
            graphics.drawCenteredString(font, font.plainSubstrByWidth(error, 180),
                    left + 102, top + 244, PhoneColors.error(style));
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.AppInstallPacket;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.gui.theme.McnIconRegistry;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.gui.NoServiceOverlay;

public final class StoreScreen extends Screen implements PhoneNavigableScreen {
    private static final int PANEL_WIDTH = 204;
    private static final int PANEL_HEIGHT = 360;
    private static final int ROW_TOP = 66;
    private static final int ROW_HEIGHT = 44;
    private static final int ROW_GAP = 5;
    private static final String[][] APPS = {
            {"Map", "map", "Free"}, {"Socials Pro", "socials", "250"},
            {"Bank", "bank", "Free"}, {"Plan", "plan", "Free"},
            {"Extra Ringtones", "ringtones", "100"}
    };
    private final PhoneMenu menu;
    private final Button[] installButtons = new Button[APPS.length];
    private final boolean[] pendingInstalls = new boolean[APPS.length];
    private final int[] installStartedTicks = new int[APPS.length];
    private int ticks;

    public StoreScreen(PhoneMenu menu) {
        super(Component.literal("App Store"));
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
        for (int i = 0; i < APPS.length; i++) {
            int index = i;
            int y = top + ROW_TOP + i * (ROW_HEIGHT + ROW_GAP);
            boolean installed = ClientPhoneCache.installedApps().contains(APPS[i][1]);
            Button installButton = new PhoneActionButton(left + PANEL_WIDTH - 62, y + 11, 54, 22,
                    installed ? "Installed" : "Install", button -> requestInstall(index));
            installButton.active = !installed;
            installButtons[i] = installButton;
            addRenderableWidget(installButton);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;
        for (int i = 0; i < APPS.length; i++) {
            Button button = installButtons[i];
            if (button == null) {
                continue;
            }
            if (ClientPhoneCache.installedApps().contains(APPS[i][1])) {
                pendingInstalls[i] = false;
                button.setMessage(buttonLabel("Installed", PhoneColors.SUCCESS_TEXT));
                button.active = false;
            } else if (pendingInstalls[i] && ticks - installStartedTicks[i] >= 100) {
                pendingInstalls[i] = false;
                button.setMessage(buttonLabel("Try again", 0xFF202020));
                button.active = true;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // No service gating: show overlay if no active plan and screen is not exempt
            if (ClientPhoneCache.planId().isBlank()) {
                NoServiceOverlay.render(graphics, ClientPhoneCache.phoneStyle(), PhoneFrame.getScreenX(this),
                        PhoneFrame.getContentTop(this), PhoneFrame.getScreenW(), PhoneFrame.getScreenH(), "Store", font);
                return;
            }
            PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PANEL_WIDTH, PANEL_HEIGHT);
        PhoneFrame.renderHeader(graphics, this, "App Store", true);
        graphics.drawString(font, "Discover apps", left + 12, top + 48,
                PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()), true);
        for (int i = 0; i < APPS.length; i++) {
            int y = top + ROW_TOP + i * (ROW_HEIGHT + ROW_GAP);
            PhoneRenderer.drawCard(graphics, ClientPhoneCache.phoneStyle(), left + 8, y,
                    PANEL_WIDTH - 16, ROW_HEIGHT);
            String appId = APPS[i][1];
            int appColor = McnIconRegistry.getBrandColor(appId);
            var png = McnIconRegistry.hasPngTexture(appId)
                    ? McnIconRegistry.getPngLocation(appId) : null;
            McnPrimitives.drawIconTile(graphics, ClientPhoneCache.phoneStyle(), font,
                    left + 14, y + 6, McnTheme.ICON_TILE_MD, appColor, png,
                    McnIconRegistry.getGlyph(appId));
            graphics.drawString(font, APPS[i][0], left + 52, y + 7,
                    PhoneColors.textPrimary(ClientPhoneCache.phoneStyle()), true);
            boolean installed = ClientPhoneCache.installedApps().contains(APPS[i][1]);
            String subtitle = "Free".equals(APPS[i][2]) ? "Free to install" : APPS[i][2] + " credits";
            int priceColor = "Free".equals(APPS[i][2])
                    ? PhoneColors.textSecondary(ClientPhoneCache.phoneStyle())
                    : PhoneColors.warn(ClientPhoneCache.phoneStyle());
            graphics.drawString(font, subtitle, left + 52, y + 23,
                    priceColor, true);
        }
        int layoutMouseX = PhoneFrame.displayMouseX(this, mouseX);
        int layoutMouseY = PhoneFrame.displayMouseY(this, mouseY);
        super.render(graphics, layoutMouseX, layoutMouseY, partialTick);
        for (int i = 0; i < APPS.length; i++) {
            Button button = installButtons[i];
            if (button != null) {
                boolean installed = ClientPhoneCache.installedApps().contains(APPS[i][1]);
                boolean hovered = layoutMouseX >= button.getX() && layoutMouseX < button.getX() + button.getWidth()
                        && layoutMouseY >= button.getY() && layoutMouseY < button.getY() + button.getHeight();
                String label;
                if (installed) {
                    label = "Installed";
                } else if (pendingInstalls[i]) {
                    label = "Installing...";
                } else {
                    label = button.getMessage().getString();
                }
                int color = installed ? PhoneColors.success(ClientPhoneCache.phoneStyle())
                        : pendingInstalls[i] ? PhoneColors.bgTertiary(ClientPhoneCache.phoneStyle())
                        : PhoneColors.accent(ClientPhoneCache.phoneStyle());
                PhoneRenderer.fillRoundedRect(graphics, ClientPhoneCache.phoneStyle(),
                        button.getX(), button.getY(), button.getWidth(), button.getHeight(), 10, color);
                graphics.drawCenteredString(font, label, button.getX() + button.getWidth() / 2,
                        button.getY() + (button.getHeight() - font.lineHeight) / 2,
                        PhoneColors.textPrimary(ClientPhoneCache.phoneStyle()));
            }
        }
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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button,
                PhoneFrame.layoutMouseDeltaX(this, dragX), PhoneFrame.layoutMouseDeltaY(this, dragY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void requestInstall(int index) {
        if (pendingInstalls[index] || ClientPhoneCache.installedApps().contains(APPS[index][1])) {
            return;
        }
        pendingInstalls[index] = true;
        installStartedTicks[index] = ticks;
        installButtons[index].setMessage(buttonLabel("Installing...", PhoneColors.PRIMARY_TEXT));
        installButtons[index].active = false;
        PacketDistributor.sendToServer(new AppInstallPacket(APPS[index][1]));
    }

    private static Component buttonLabel(String label, int color) {
        return Component.literal(label).withStyle(style -> style.withColor(color));
    }

    private void back() {
        if (minecraft != null && minecraft.player != null) {
            Minecraft.getInstance().setScreen(
                    new PhoneScreen(menu, minecraft.player.getInventory(), Component.literal("Phone")));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}

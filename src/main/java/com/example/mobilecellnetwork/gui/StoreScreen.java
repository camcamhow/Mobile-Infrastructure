package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.AppInstallPacket;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.gui.theme.McnIconRegistry;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
    private static final int SCROLLBAR_X = 198;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SNAPPLE_ROW_PITCH = 42;
    private static final String[][] APPS = {
            {"Map", "map", "Free"}, {"Socials Pro", "socials", "250"},
            {"Bank", "bank", "Free"}, {"Plan", "plan", "Free"},
            {"Extra Ringtones", "ringtones", "100"}
    };
    private static final String[] DESCRIPTIONS = {
            "Explore your world", "Share your moments", "Manage your credits",
            "Connect your phone", "More sounds for your phone"
    };
    private static final String[] CATEGORIES = {"All", "Social", "Utilities"};
    private final PhoneMenu menu;
    private final Button[] installButtons = new Button[APPS.length];
    private final boolean[] pendingInstalls = new boolean[APPS.length];
    private final boolean[] installTimedOut = new boolean[APPS.length];
    private final int[] installStartedTicks = new int[APPS.length];
    private int ticks;
    private int category;
    private int scroll;
    private boolean searchMode;
    private boolean draggingScrollbar;
    private double scrollbarGrabOffset;
    private EditBox search;

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
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            search = PhoneTextField.create(font, left + 10, PhoneFrame.getScreenY(this) + 70,
                    184, 24, "Search apps", 64, PhoneStyle.IOS);
            search.setResponder(value -> scroll = 0);
            search.visible = searchMode;
            addRenderableWidget(search);
            return;
        }
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
            if (ClientPhoneCache.installedApps().contains(APPS[i][1])) {
                pendingInstalls[i] = false;
                installTimedOut[i] = false;
                if (button != null) {
                    button.setMessage(buttonLabel("Installed", PhoneColors.SUCCESS_TEXT));
                    button.active = false;
                }
            } else if (pendingInstalls[i] && ticks - installStartedTicks[i] >= 100) {
                pendingInstalls[i] = false;
                installTimedOut[i] = true;
                if (button != null) {
                    button.setMessage(buttonLabel("Try again", 0xFF202020));
                    button.active = true;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // No service gating: show overlay if no active plan and screen is not exempt
            if (ClientPhoneCache.planId().isBlank()) {
                PhoneFrame.render(graphics, this);
                NoServiceOverlay.render(graphics, ClientPhoneCache.phoneStyle(), PhoneFrame.getScreenX(this),
                        PhoneFrame.getContentTop(this), PhoneFrame.getScreenW(),
                        PhoneFrame.getScreenH() - 24, "Store", font);
                PhoneFrame.renderStatusBar(graphics, this);
                return;
            }
            PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        boolean snapple = ClientPhoneCache.phoneStyle() == PhoneStyle.IOS;
        if (snapple) {
            renderSnapple(graphics, left, PhoneFrame.getScreenY(this), mouseX, mouseY, partialTick);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
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
            String appName = APPS[i][0];
            PhoneStyle style = ClientPhoneCache.phoneStyle();
            int textWidth = 82;
            graphics.drawString(font, style == PhoneStyle.SAMSUNG_ONE_UI
                            ? font.plainSubstrByWidth(appName, textWidth) : appName,
                    left + 52, y + 7, PhoneColors.textPrimary(style), style != PhoneStyle.SAMSUNG_ONE_UI);
            boolean owned = ClientPhoneCache.ownsApp(appId);
            String subtitle = owned ? "Owned - free to reinstall"
                    : "Free".equals(APPS[i][2]) ? "Free to install" : APPS[i][2] + " credits";
            int priceColor = owned || "Free".equals(APPS[i][2])
                    ? PhoneColors.textSecondary(ClientPhoneCache.phoneStyle())
                    : PhoneColors.warn(ClientPhoneCache.phoneStyle());
            graphics.drawString(font, style == PhoneStyle.SAMSUNG_ONE_UI
                            ? font.plainSubstrByWidth(subtitle, textWidth) : subtitle,
                    left + 52, y + 23, priceColor, style != PhoneStyle.SAMSUNG_ONE_UI);
        }
        int layoutMouseX = PhoneFrame.displayMouseX(this, mouseX);
        int layoutMouseY = PhoneFrame.displayMouseY(this, mouseY);
        super.render(graphics, layoutMouseX, layoutMouseY, partialTick);
        for (int i = 0; i < APPS.length; i++) {
            Button button = installButtons[i];
            if (button != null) {
                PhoneStyle style = ClientPhoneCache.phoneStyle();
                boolean installed = ClientPhoneCache.installedApps().contains(APPS[i][1]);
                String label;
                if (installed) {
                    label = "Installed";
                } else if (pendingInstalls[i]) {
                    label = "Installing...";
                } else {
                    String message = button.getMessage().getString();
                    label = message;
                }
                int color = installed ? PhoneColors.success(ClientPhoneCache.phoneStyle())
                        : pendingInstalls[i] ? PhoneColors.bgTertiary(ClientPhoneCache.phoneStyle())
                        : PhoneColors.accent(ClientPhoneCache.phoneStyle());
                PhoneRenderer.fillRoundedRect(graphics, ClientPhoneCache.phoneStyle(),
                        button.getX(), button.getY(), button.getWidth(), button.getHeight(), 10, color);
                if (style == PhoneStyle.SAMSUNG_ONE_UI) {
                    String fittedLabel = font.plainSubstrByWidth(label, button.getWidth() - 8);
                    int labelColor = installed || pendingInstalls[i]
                            ? PhoneColors.textPrimary(style) : PhoneColors.buttonTextOnAccent(style);
                    graphics.drawString(font, fittedLabel,
                            button.getX() + (button.getWidth() - font.width(fittedLabel)) / 2,
                            button.getY() + (button.getHeight() - font.lineHeight) / 2, labelColor, false);
                } else {
                    graphics.drawCenteredString(font, label, button.getX() + button.getWidth() / 2,
                            button.getY() + (button.getHeight() - font.lineHeight) / 2,
                            PhoneColors.textPrimary(style));
                }
            }
        }
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private void renderSnapple(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float partialTick) {
        PhoneStyle style = PhoneStyle.IOS;
        SnappleAppLayout.screen(graphics, font, x, y, searchMode ? "Search" : "Apps");
        if (searchMode) {
            PhoneTextField.renderBackground(graphics, style, x + 10, y + 70, 184, 24);
            SnappleText.draw(graphics, font, "Apps", x + 11, y + 104, PhoneColors.textPrimary(style));
        } else {
            int[] starts = {10, 56, 125};
            int[] widths = {40, 63, 69};
            for (int i = 0; i < CATEGORIES.length; i++) {
                SnappleAppLayout.pill(graphics, font, x + starts[i], y + 63, widths[i], 20,
                        CATEGORIES[i], i == category ? PhoneColors.accent(style) : PhoneColors.bgTertiary(style),
                        i == category ? 0xFFFFFFFF : PhoneColors.textPrimary(style));
            }
            int featured = featuredApp();
            SnappleText.draw(graphics, font, "FEATURED", x + 11, y + 91, PhoneColors.accent(style));
            SnappleText.draw(graphics, font, fit(APPS[featured][0], 180), x + 11, y + 103,
                    PhoneColors.textPrimary(style));
            SnappleText.draw(graphics, font, fit(DESCRIPTIONS[featured], 180), x + 11, y + 116,
                    PhoneColors.textSecondary(style));
            McnPrimitives.fillRoundedRect(graphics, x + 10, y + 132, 184, 66, 12,
                    PhoneColors.bgSecondary(style));
            McnPrimitives.fillRoundedRect(graphics, x + 20, y + 142, 41, 46, 9,
                    PhoneColors.bgTertiary(style));
            drawIcon(graphics, featured, x + 24, y + 149, 32);
            SnappleText.draw(graphics, font, fit(APPS[featured][0], 117), x + 69, y + 146,
                    PhoneColors.textPrimary(style));
            SnappleText.draw(graphics, font, price(featured), x + 69, y + 162,
                    PhoneColors.textSecondary(style));
            drawSnappleInstall(graphics, featured, x + 140, y + 172, 46, 19);
            SnappleText.draw(graphics, font, "Discover more", x + 11, y + 208,
                    PhoneColors.textPrimary(style));
        }
        List<Integer> apps = displayedApps();
        int visible = searchMode ? 4 : 2;
        scroll = Math.clamp(scroll, 0, Math.max(0, apps.size() - visible));
        int listY = searchMode ? 118 : 222;
        if (apps.isEmpty()) {
            SnappleText.centered(graphics, font, searchMode ? "No matching apps" : "No more apps",
                    x + PANEL_WIDTH / 2,
                    y + listY + 28, PhoneColors.textSecondary(style));
        }
        for (int row = 0; row < visible && row + scroll < apps.size(); row++) {
            int index = apps.get(row + scroll);
            int rowY = y + listY + row * SNAPPLE_ROW_PITCH;
            drawIcon(graphics, index, x + 11, rowY + 4, 32);
            SnappleText.draw(graphics, font, fit(APPS[index][0], 92), x + 49, rowY + 5,
                    PhoneColors.textPrimary(style));
            SnappleText.draw(graphics, font, fit(DESCRIPTIONS[index], 92), x + 49, rowY + 18,
                    PhoneColors.textSecondary(style));
            drawSnappleInstall(graphics, index, x + 149, rowY + 5, 46, 19);
            SnappleText.centered(graphics, font, price(index), x + 172, rowY + 27,
                    PhoneColors.textSecondary(style));
            if (row + scroll + 1 < apps.size()) {
                SnappleAppLayout.separator(graphics, x + 49, rowY + 39, 144);
            }
        }
        if (apps.size() > visible) {
            int trackHeight = visible * SNAPPLE_ROW_PITCH;
            int thumbHeight = scrollbarThumbHeight(apps.size(), visible);
            int thumbY = listY + scrollbarThumbOffset(apps.size(), visible);
            McnPrimitives.fillRoundedRect(graphics, x + SCROLLBAR_X, y + listY,
                    SCROLLBAR_WIDTH, trackHeight, 2, PhoneColors.bgTertiary(style));
            McnPrimitives.fillRoundedRect(graphics, x + SCROLLBAR_X, y + thumbY,
                    SCROLLBAR_WIDTH, thumbHeight, 2, PhoneColors.textSecondary(style));
        }
        McnPrimitives.fillRoundedRect(graphics, x + 22, y + 322, 160, 29, 14,
                PhoneColors.bgSecondary(style));
        SnappleAppLayout.pill(graphics, font, x + (searchMode ? 103 : 25), y + 324, 76, 25,
                searchMode ? "Search" : "Apps", PhoneColors.bgTertiary(style), PhoneColors.accent(style));
        SnappleText.centered(graphics, font, searchMode ? "Apps" : "Search",
                x + (searchMode ? 63 : 141), y + 332, PhoneColors.textSecondary(style));
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
    }

    private void drawIcon(GuiGraphics graphics, int index, int x, int y, int size) {
        String id = APPS[index][1];
        McnPrimitives.drawIconTile(graphics, PhoneStyle.IOS, font, x, y, size,
                McnIconRegistry.getBrandColor(id),
                McnIconRegistry.hasPngTexture(id) ? McnIconRegistry.getPngLocation(id) : null,
                McnIconRegistry.getGlyph(id));
    }

    private void drawSnappleInstall(GuiGraphics graphics, int index, int x, int y, int width, int height) {
        boolean installed = ClientPhoneCache.installedApps().contains(APPS[index][1]);
        String label = installed ? "Added" : pendingInstalls[index] ? "Adding" : installTimedOut[index] ? "Retry"
                : ClientPhoneCache.ownsApp(APPS[index][1]) || "Free".equals(APPS[index][2]) ? "GET" : "BUY";
        SnappleAppLayout.pill(graphics, font, x, y, width, height, label,
                PhoneColors.bgTertiary(PhoneStyle.IOS),
                installed ? PhoneColors.textSecondary(PhoneStyle.IOS) : PhoneColors.accent(PhoneStyle.IOS));
    }

    private static String price(int index) {
        return ClientPhoneCache.ownsApp(APPS[index][1]) ? "Owned - free reinstall"
                : "Free".equals(APPS[index][2]) ? "Free" : APPS[index][2] + " credits";
    }

    private String fit(String text, int width) {
        return SnappleAppLayout.fit(font, text, width);
    }

    private int featuredApp() {
        return category == 2 ? 0 : 1;
    }

    private List<Integer> displayedApps() {
        List<Integer> result = new ArrayList<>();
        String query = search == null ? "" : search.getValue().strip().toLowerCase(Locale.ROOT);
        for (int i = 0; i < APPS.length; i++) {
            if (!searchMode && (i == featuredApp() || (category == 1 && i != 1)
                    || (category == 2 && i == 1))) {
                continue;
            }
            if (searchMode && !APPS[i][0].toLowerCase(Locale.ROOT).contains(query)
                    && !DESCRIPTIONS[i].toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            result.add(i);
        }
        return result;
    }

    private int visibleRows() {
        return searchMode ? 4 : 2;
    }

    private int listTop() {
        return searchMode ? 118 : 222;
    }

    private static int scrollbarThumbHeight(int count, int visible) {
        int trackHeight = visible * SNAPPLE_ROW_PITCH;
        return Math.max(12, trackHeight * visible / count);
    }

    private int scrollbarThumbOffset(int count, int visible) {
        int maximum = count - visible;
        int travel = visible * SNAPPLE_ROW_PITCH - scrollbarThumbHeight(count, visible);
        return Math.round((float) Math.clamp(scroll, 0, maximum) * travel / maximum);
    }

    private void updateScrollbar(double layoutY, int count, int visible) {
        int travel = visible * SNAPPLE_ROW_PITCH - scrollbarThumbHeight(count, visible);
        int maximum = count - visible;
        double position = Math.clamp(layoutY - listTop() - scrollbarGrabOffset, 0.0D, (double) travel);
        scroll = (int) Math.round(position * maximum / travel);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ClientPhoneCache.planId().isBlank()) {
            return false;
        }
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS && button == 0) {
            double x = PhoneFrame.layoutMouseX(this, mouseX) - PhoneFrame.getScreenX(this);
            double y = PhoneFrame.layoutMouseY(this, mouseY) - PhoneFrame.getScreenY(this);
            if (inside(x, y, 22, 322, 160, 29)) {
                searchMode = x >= 102;
                scroll = 0;
                search.visible = searchMode;
                search.setFocused(false);
                setFocused(null);
                return true;
            }
            if (!searchMode) {
                int[] starts = {10, 56, 125};
                int[] widths = {40, 63, 69};
                for (int i = 0; i < starts.length; i++) {
                    if (inside(x, y, starts[i], 63, widths[i], 20)) {
                        category = i;
                        scroll = 0;
                        return true;
                    }
                }
                if (inside(x, y, 140, 172, 46, 19)) {
                    requestInstall(featuredApp());
                    return true;
                }
            }
            List<Integer> apps = displayedApps();
            int visible = visibleRows();
            int first = Math.clamp(scroll, 0, Math.max(0, apps.size() - visible));
            int listY = listTop();
            if (apps.size() > visible
                    && inside(x, y, SCROLLBAR_X - 2, listY, 8, visible * SNAPPLE_ROW_PITCH)) {
                int thumbTop = listY + scrollbarThumbOffset(apps.size(), visible);
                int thumbHeight = scrollbarThumbHeight(apps.size(), visible);
                scrollbarGrabOffset = y >= thumbTop && y < thumbTop + thumbHeight
                        ? y - thumbTop : thumbHeight / 2.0D;
                draggingScrollbar = true;
                updateScrollbar(y, apps.size(), visible);
                return true;
            }
            for (int row = 0; row < visible && first + row < apps.size(); row++) {
                if (inside(x, y, 149, listY + row * SNAPPLE_ROW_PITCH + 5, 46, 19)) {
                    requestInstall(apps.get(first + row));
                    return true;
                }
            }
        }
        return super.mouseClicked(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingScrollbar) {
            int count = displayedApps().size();
            int visible = visibleRows();
            if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS
                    && !ClientPhoneCache.planId().isBlank() && count > visible) {
                updateScrollbar(PhoneFrame.layoutMouseY(this, mouseY) - PhoneFrame.getScreenY(this),
                        count, visible);
            } else {
                draggingScrollbar = false;
            }
            return true;
        }
        return super.mouseDragged(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button,
                PhoneFrame.layoutMouseDeltaX(this, dragX), PhoneFrame.layoutMouseDeltaY(this, dragY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS && !ClientPhoneCache.planId().isBlank()
                && scrollY != 0) {
            double x = PhoneFrame.layoutMouseX(this, mouseX) - PhoneFrame.getScreenX(this);
            double y = PhoneFrame.layoutMouseY(this, mouseY) - PhoneFrame.getScreenY(this);
            int listY = listTop();
            if (inside(x, y, 8, listY, 196, visibleRows() * SNAPPLE_ROW_PITCH)) {
                int max = Math.max(0, displayedApps().size() - visibleRows());
                scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1), 0, max);
                return true;
            }
        }
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
        if (ClientPhoneCache.planId().isBlank() || pendingInstalls[index]
                || ClientPhoneCache.installedApps().contains(APPS[index][1])) {
            return;
        }
        pendingInstalls[index] = true;
        installTimedOut[index] = false;
        installStartedTicks[index] = ticks;
        if (installButtons[index] != null) {
            installButtons[index].setMessage(buttonLabel("Installing...", PhoneColors.PRIMARY_TEXT));
            installButtons[index].active = false;
        }
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

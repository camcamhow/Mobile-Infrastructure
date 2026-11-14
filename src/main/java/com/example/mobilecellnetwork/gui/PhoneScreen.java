package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.CallRequestPacket;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSmsCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.MapInteractionPacket;
import com.example.mobilecellnetwork.network.MapOpenPacket;
import com.example.mobilecellnetwork.network.RequestPhoneDataPacket;
import com.example.mobilecellnetwork.network.PhoneDeviceClientCache;
import com.example.mobilecellnetwork.network.HomeAppActionPacket;
import com.example.mobilecellnetwork.network.RequestPhonePhotosPacket;
import com.example.mobilecellnetwork.network.SetLowPowerPacket;
import com.example.mobilecellnetwork.network.StartChatPacket;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.gui.theme.McnIconRegistry;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PhoneScreen extends AbstractContainerScreen<PhoneMenu> implements PhoneNavigableScreen {
    private static final String[] APPS = {
            "Phone", "SMS", "Contacts", "Map", "Store", "Socials", "Bank", "Settings",
            "Plan", "Voicemail", "Camera", "Gallery", "Flashlight"
    };
    private static PhoneApp rememberedApp;

    private PhoneApp currentApp = PhoneApp.HOME;
    private boolean lastNoService;
    private boolean lastPhoneDead;
    private boolean lastLowPower;
    private int mapOffsetX;
    private int mapOffsetZ;
    private int mapZoomLevel;
    private int phoneScroll;
    private java.util.List<AppShortcut> homeShortcuts = java.util.List.of();
    private final java.util.List<Button> homeAppButtons = new java.util.ArrayList<>();
    private net.minecraft.client.gui.components.EditBox searchBox;
    private String appQuery = "";
    private boolean pullingControlCenter;
    private double controlCenterStartY;
    private boolean editingHome;
    private String pressedApp = "";
    private String pendingRemoval = "";
    private long appPressedAt;
    private double pressX, pressY, dragX, dragY;
    private boolean movedApp;
    private java.util.List<String> lastHomeOrder = java.util.List.of();
    private final java.util.Map<String, float[]> homeIconPositions = new java.util.HashMap<>();
    private long lastIconFrame;
    // Track installed apps to detect changes and refresh home screen
    private java.util.Set<String> lastKnownInstalledApps = java.util.Set.of();

    public PhoneScreen(PhoneMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        rememberedApp = null;
        imageWidth = PhoneFrame.getScreenW();
        imageHeight = PhoneFrame.getScreenH();
        titleLabelX = 0;
        titleLabelY = -1000;
        inventoryLabelY = -1000;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        if (editingHome) {
            editingHome = false;
            pendingRemoval = "";
            return;
        }
        if (currentApp != PhoneApp.HOME) {
            showHome();
        }
    }

    @Override
    public void navigateHome() {
        showHome();
    }

    private void showHome() {
        currentApp = PhoneApp.HOME;
        refreshAppWidgets();
    }

    private void addEmergencyButton() {
        addRenderableWidget(Button.builder(Component.literal("Emergency"), button ->
                        PacketDistributor.sendToServer(new CallRequestPacket("911")))
                .bounds(leftPos + (imageWidth - 100) / 2, topPos + 145, 100, 24)
                .build());
    }

    private void addSettingsButton() {
        addRenderableWidget(Button.builder(Component.literal("Settings"), button ->
                        Minecraft.getInstance().setScreen(new SettingsScreen(menu)))
                .bounds(leftPos + (imageWidth - 100) / 2, topPos + 175, 100, 22)
                .build());
    }

    private void addSettingsButtons() {
        addRenderableWidget(Button.builder(
                        Component.literal("Low power: " + (ClientPhoneCache.lowPowerMode() ? "On" : "Off")),
                        button -> {
                            PacketDistributor.sendToServer(
                                    new SetLowPowerPacket(!ClientPhoneCache.lowPowerMode()));
                            ClientPhoneCache.setLowPowerMode(!ClientPhoneCache.lowPowerMode());
                            refreshAppWidgets();
                        })
                .bounds(leftPos + 20, topPos + 75, imageWidth - 40, 22)
                .build());
    }

    @Override
    protected void init() {
        super.init();
        PacketDistributor.sendToServer(new RequestPhoneDataPacket());
        java.util.UUID phoneId = PhoneDeviceClientCache.phoneId();
        if (phoneId != null) {
            PacketDistributor.sendToServer(new RequestPhonePhotosPacket(phoneId));
        }
        lastNoService = ClientSignalCache.tier() == SignalTier.NO_SERVICE;
        lastPhoneDead = ClientPhoneCache.phoneDead();
        lastLowPower = ClientPhoneCache.lowPowerMode();
        refreshAppWidgets();
    }

    @Override
    protected void setInitialFocus() {
        if (!HeldPhoneView.active()) {
            super.setInitialFocus();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean noService = ClientSignalCache.tier() == SignalTier.NO_SERVICE;
        boolean dead = ClientPhoneCache.phoneDead();
        boolean lowPower = ClientPhoneCache.lowPowerMode();
        if (noService != lastNoService || dead != lastPhoneDead || lowPower != lastLowPower) {
            lastNoService = noService;
            lastPhoneDead = dead;
            lastLowPower = lowPower;
            if (lowPower && !isAllowedInLowPower(currentApp)) {
                currentApp = PhoneApp.HOME;
            }
            refreshAppWidgets();
        }
        boolean homeScreen = currentApp == PhoneApp.HOME;
                // Check if installed apps have changed and refresh home screen if needed
                if (homeScreen && !ClientPhoneCache.phoneDead()) {
                    java.util.Set<String> currentInstalledApps = ClientPhoneCache.installedApps();
                    if (!currentInstalledApps.equals(lastKnownInstalledApps)) {
                        lastKnownInstalledApps = java.util.Set.copyOf(currentInstalledApps);
                        layoutHomeApps();
                    }
                    if (!ClientPhoneCache.homeAppOrder().equals(lastHomeOrder)) {
                        lastHomeOrder = ClientPhoneCache.homeAppOrder();
                        layoutHomeApps();
                    }
                }
                if (homeScreen) {
            beginHomePhoneFrame(graphics);
        } else {
            PhoneFrame.render(graphics, this);
        }
        if (currentApp != PhoneApp.HOME) {
            PhoneFrame.renderHeader(graphics, this, currentApp.label, true);
        }
        if (homeScreen) {
            renderHomeHeader(graphics);
            if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
                McnPrimitives.fillRoundedRect(graphics, homeSearchX(), homeSearchY(),
                        homeSearchWidth(), McnTheme.TEXT_FIELD_HEIGHT, 12, 0x78404558);
                if (searchBox != null && searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
                    SnappleText.centered(graphics, font, "Search",
                            homeSearchX() + homeSearchWidth() / 2,
                            homeSearchY() + (McnTheme.TEXT_FIELD_HEIGHT - font.lineHeight) / 2, 0xFFD8D8E0);
                }
                if (appQuery.isBlank()) {
                    McnPrimitives.fillRoundedRect(graphics, leftPos + 9, topPos + 300,
                            imageWidth - 18, 53, 17, 0x704D5068);
                }
            } else if (ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI) {
                McnPrimitives.fillRoundedRect(graphics, homeSearchX(), homeSearchY(),
                        homeSearchWidth(), McnTheme.TEXT_FIELD_HEIGHT, 12, 0xB02C3746);
            } else {
                McnPrimitives.fillRoundedRect(graphics,
                        leftPos + McnTheme.SPACE_M, homeSearchY(),
                        imageWidth - McnTheme.SPACE_M * McnTheme.SPACE_XXS,
                        McnTheme.TEXT_FIELD_HEIGHT, 12, 0xA020293A);
            }
            if (homeShortcuts.isEmpty() && !ClientPhoneCache.phoneDead()) {
                graphics.drawCenteredString(font, "No apps found", leftPos + imageWidth / 2,
                        homeGridY(), PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()));
            }
        }
        int layoutMouseX = PhoneFrame.displayMouseX(this, mouseX);
        int layoutMouseY = PhoneFrame.displayMouseY(this, mouseY);
        super.render(graphics, layoutMouseX, layoutMouseY, partialTick);
        if (homeScreen && !ClientPhoneCache.phoneDead()) {
            renderHomeApps(graphics, layoutMouseX, layoutMouseY);
        }
        if (homeScreen) {
            renderHomeStatusBar(graphics);
        } else {
            PhoneFrame.renderStatusBar(graphics, this);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double layoutX = PhoneFrame.layoutMouseX(this, mouseX);
        double layoutY = PhoneFrame.layoutMouseY(this, mouseY);
        if (button == 1 && currentApp == PhoneApp.HOME && !ClientPhoneCache.phoneDead()
                && !ClientPhoneCache.lowPowerMode() && appQuery.isBlank()) {
            for (AppShortcut shortcut : homeShortcuts) {
                if (layoutX >= leftPos + shortcut.x() && layoutX < leftPos + shortcut.x() + McnTheme.ICON_TILE_GRID
                        && layoutY >= topPos + shortcut.y() && layoutY < topPos + shortcut.y() + McnTheme.ICON_TILE_GRID) {
                    editingHome = true;
                    pendingRemoval = "";
                    if (searchBox != null) searchBox.setFocused(false);
                    return true;
                }
            }
        }
        if (button == 0 && currentApp == PhoneApp.HOME && !ClientPhoneCache.phoneDead()) {
            if (editingHome && layoutX >= leftPos + 145 && layoutX < leftPos + 198
                    && layoutY >= topPos + 31 && layoutY < topPos + 52) {
                editingHome = false;
                pendingRemoval = "";
                return true;
            }
            for (AppShortcut shortcut : homeShortcuts) {
                int x = leftPos + shortcut.x();
                int y = topPos + shortcut.y();
                if (editingHome && removableApp(shortcut.id()) && layoutX >= x - 5 && layoutX < x + 12
                        && layoutY >= y - 5 && layoutY < y + 12) {
                    if (pendingRemoval.equals(shortcut.id())) {
                        PacketDistributor.sendToServer(new HomeAppActionPacket("uninstall", shortcut.id(), java.util.List.of()));
                        pendingRemoval = "";
                    } else {
                        pendingRemoval = shortcut.id();
                    }
                    return true;
                }
                if (layoutX >= x && layoutX < x + McnTheme.ICON_TILE_GRID
                        && layoutY >= y && layoutY < y + McnTheme.ICON_TILE_GRID) {
                    pendingRemoval = "";
                    pressedApp = shortcut.id();
                    appPressedAt = net.minecraft.Util.getMillis();
                    pressX = dragX = layoutX;
                    pressY = dragY = layoutY;
                    movedApp = false;
                    return true;
                }
            }
            if (editingHome) {
                pendingRemoval = "";
                return true;
            }
        }
        pullingControlCenter = button == 0 && currentApp == PhoneApp.HOME
                && (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS
                    || ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI)
                && !ClientPhoneCache.phoneDead()
                && layoutX >= PhoneFrame.getScreenX(this) + PhoneFrame.getScreenW() - 65
                && layoutX < PhoneFrame.getScreenX(this) + PhoneFrame.getScreenW()
                && layoutY >= PhoneFrame.getScreenY(this)
                && layoutY < PhoneFrame.getScreenY(this) + 30;
        if (pullingControlCenter) {
            controlCenterStartY = layoutY;
            return true;
        }
        if (currentApp == PhoneApp.PHONE && button == 0) {
            double localX = layoutX - leftPos;
            double localY = layoutY - topPos;
            if (localY >= 78 && localY < 318) {
                int row = (int) ((localY - 78) / 48) + phoneScroll;
                var players = ClientPhoneCache.onlinePlayers();
                if (row >= 0 && row < players.size() && row < phoneScroll + 5) {
                    ClientPhoneCache.OnlinePlayer player = players.get(row);
                    if (localX >= 103 && localX < 143) {
                        if (ClientSignalCache.tier() != SignalTier.NO_SERVICE) {
                            PacketDistributor.sendToServer(new CallRequestPacket(player.number()));
                        } else if (Minecraft.getInstance().player != null) {
                            Minecraft.getInstance().player.displayClientMessage(
                                    Component.literal("No Service"), true);
                        }
                        return true;
                    }
                    if (localX >= 146 && localX < 198) {
                        PacketDistributor.sendToServer(new StartChatPacket(player.number()));
                        Minecraft.getInstance().setScreen(new SmsScreen(menu, player.number()));
                        return true;
                    }
                }
            }
            if (localY >= 322 && localY < 346) {
                Minecraft.getInstance().setScreen(new VoicemailScreen(menu));
                return true;
            }
        }
        return super.mouseClicked(layoutX, layoutY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && !pressedApp.isEmpty()) {
            activateHomeEditingIfHeld();
            String id = pressedApp;
            pressedApp = "";
            if (editingHome) {
                double x = PhoneFrame.layoutMouseX(this, mouseX);
                double y = PhoneFrame.layoutMouseY(this, mouseY);
                AppShortcut source = homeShortcuts.stream().filter(app -> app.id().equals(id)).findFirst().orElse(null);
                if (source != null && movedApp) {
                    double centerX = leftPos + source.x() + McnTheme.ICON_TILE_GRID / 2.0D + x - pressX;
                    double centerY = topPos + source.y() + McnTheme.ICON_TILE_GRID / 2.0D + y - pressY;
                    AppShortcut target = homeShortcuts.stream().min(java.util.Comparator.comparingDouble(app ->
                            Math.hypot(centerX - (leftPos + app.x() + McnTheme.ICON_TILE_GRID / 2.0D),
                                    centerY - (topPos + app.y() + McnTheme.ICON_TILE_GRID / 2.0D)))).orElse(source);
                    double distance = Math.hypot(centerX - (leftPos + target.x() + McnTheme.ICON_TILE_GRID / 2.0D),
                            centerY - (topPos + target.y() + McnTheme.ICON_TILE_GRID / 2.0D));
                    if (!target.id().equals(id) && distance <= McnTheme.ICON_TILE_GRID) {
                        java.util.List<String> order = new java.util.ArrayList<>(orderedAppIds());
                        java.util.Collections.swap(order, order.indexOf(id), order.indexOf(target.id()));
                        for (String saved : ClientPhoneCache.homeAppOrder()) {
                            if (!order.contains(saved)) order.add(saved);
                        }
                        homeIconPositions.put(id, new float[] {
                                (float) (leftPos + source.x() + x - pressX),
                                (float) (topPos + source.y() + y - pressY)
                        });
                        ClientPhoneCache.setHomeAppOrder(order);
                        lastHomeOrder = java.util.List.copyOf(order);
                        PacketDistributor.sendToServer(new HomeAppActionPacket("order", "", order));
                        layoutHomeApps();
                    }
                }
            } else if (!movedApp) {
                for (PhoneApp app : PhoneApp.values()) {
                    if (app.id.equals(id)) {
                        openApp(app);
                        break;
                    }
                }
            }
            return true;
        }
        if (pullingControlCenter) {
            pullingControlCenter = false;
            if (button == 0 && PhoneFrame.layoutMouseY(this, mouseY) >= controlCenterStartY + 24) {
                Minecraft.getInstance().setScreen(new ControlCenterScreen(menu));
            }
            return true;
        }
        return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && !pressedApp.isEmpty()) {
            activateHomeEditingIfHeld();
            this.dragX = PhoneFrame.layoutMouseX(this, mouseX);
            this.dragY = PhoneFrame.layoutMouseY(this, mouseY);
            if (Math.hypot(this.dragX - pressX, this.dragY - pressY) > 5.0D) movedApp = true;
            return true;
        }
        if (pullingControlCenter && button == 0) {
            return true;
        }
        return super.mouseDragged(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), button,
                PhoneFrame.layoutMouseDeltaX(this, dragX), PhoneFrame.layoutMouseDeltaY(this, dragY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentApp == PhoneApp.PHONE && scrollY != 0) {
            phoneScroll = Math.clamp(phoneScroll + (scrollY < 0 ? 1 : -1),
                    0, Math.max(0, ClientPhoneCache.onlinePlayers().size() - 5));
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!pressedApp.isEmpty()) {
            activateHomeEditingIfHeld();
            dragX = PhoneFrame.layoutMouseX(this, mouseX);
            dragY = PhoneFrame.layoutMouseY(this, mouseY);
            if (Math.hypot(dragX - pressX, dragY - pressY) > 5.0D) movedApp = true;
        }
        super.mouseMoved(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY));
    }

    private void setupAppWidgets() {
        switch (currentApp) {
            case HOME -> addHomeButtons();
            case PHONE -> addPhoneButtons();
            case SETTINGS -> addSettingsButtons();
            case SMS -> {
            }
            case MAP -> addMapButtons();
            case CONTACTS, STORE, SOCIALS, BANK, PLAN, VOICEMAIL -> {
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (ClientPhoneCache.phoneDead()) {
            return;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (ClientPhoneCache.phoneDead()) {
            drawCenteredShadow(graphics, "Phone Off", imageWidth / 2, imageHeight / 2,
                    PhoneColors.PRIMARY_TEXT);
            return;
        }
        switch (currentApp) {
            case HOME -> {
            }
            case PHONE -> renderPhoneList(graphics);
            case SETTINGS -> {
                drawCenteredShadow(graphics, "Settings", imageWidth / 2, 43, PhoneColors.PRIMARY_TEXT);
                drawCenteredShadow(graphics, "Number: " + ClientPhoneCache.phoneNumber(), imageWidth / 2,
                        76, PhoneColors.SECONDARY_TEXT);
                drawCenteredShadow(graphics, "Plan: " + ClientPhoneCache.planId(),
                        imageWidth / 2, 95, PhoneColors.SECONDARY_TEXT);
            }
            case SMS -> {
                drawCenteredShadow(graphics, "Send an SMS", imageWidth / 2, 43, PhoneColors.PRIMARY_TEXT);
                drawCenteredShadow(graphics, "SMS messages use your plan allowance.", imageWidth / 2, 260,
                        PhoneColors.SECONDARY_TEXT);
            }
            case CONTACTS -> drawCenteredShadow(graphics, "Contacts app coming soon",
                    imageWidth / 2, 48, PhoneColors.PRIMARY_TEXT);
            case MAP -> {
                if (ClientSignalCache.tier() == SignalTier.NO_SERVICE) {
                    drawCenteredShadow(graphics, "Map unavailable offline",
                            imageWidth / 2, 65, PhoneColors.SECONDARY_TEXT);
                } else {
                    renderMap(graphics);
                }
            }
            case STORE, SOCIALS, BANK, PLAN, VOICEMAIL ->
                    drawCenteredShadow(graphics, currentApp.label + " app coming soon",
                            imageWidth / 2, 48, PhoneColors.PRIMARY_TEXT);
        }
    }

    private void renderPhoneList(GuiGraphics graphics) {
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        graphics.drawString(font, "Online players", 12, 62,
                PhoneColors.textSecondary(style), true);
        var players = ClientPhoneCache.onlinePlayers();
        int first = Math.clamp(phoneScroll, 0, Math.max(0, players.size() - 5));
        for (int index = first; index < Math.min(players.size(), first + 5); index++) {
            ClientPhoneCache.OnlinePlayer player = players.get(index);
            int rowY = 78 + (index - first) * 48;
            PhoneRenderer.drawCard(graphics, style, 8, rowY, imageWidth - 16, 42);
            graphics.drawString(font, font.plainSubstrByWidth(player.name(), 82), 14, rowY + 7,
                    PhoneColors.textPrimary(style), true);
            graphics.drawString(font, font.plainSubstrByWidth(player.number(), 82), 14, rowY + 23,
                    PhoneColors.textSecondary(style), true);
            PhoneRenderer.drawPrimaryButton(graphics, style, 103, rowY + 11, 40, 20, "Call", font);
            PhoneRenderer.drawSecondaryButton(graphics, style, 147, rowY + 11, 48, 20, "Message", font);
        }
        if (players.isEmpty()) {
            graphics.drawCenteredString(font, "No other players online", imageWidth / 2, 146,
                    PhoneColors.textSecondary(style));
        }
        PhoneRenderer.drawSecondaryButton(graphics, style, 12, 322, imageWidth - 24, 22,
                "Voicemail (" + ClientSmsCache.voicemail().size() + ")", font);
    }

    private void addHomeButtons() {
        if (ClientPhoneCache.phoneDead()) {
            return;
        }
        int searchX = homeSearchX();
        int searchY = homeSearchY();
        int searchWidth = homeSearchWidth();
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        int textY = style == PhoneStyle.IOS || style == PhoneStyle.SAMSUNG_ONE_UI
                ? searchY + (McnTheme.TEXT_FIELD_HEIGHT - font.lineHeight) / 2 : searchY;
        searchBox = new net.minecraft.client.gui.components.EditBox(font,
                searchX + McnTheme.SPACE_S, textY,
                searchWidth - McnTheme.SPACE_S * McnTheme.SPACE_XXS,
                McnTheme.TEXT_FIELD_HEIGHT, Component.literal("Search apps"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(24);
        searchBox.setHint(style == PhoneStyle.IOS
                ? Component.empty() : Component.literal("Search apps"));
        searchBox.setTextShadow(false);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setTextColorUneditable(0xFFD8D8E0);
        searchBox.setValue(appQuery);
        searchBox.setResponder(value -> {
            appQuery = value;
            layoutHomeApps();
        });
        addRenderableWidget(searchBox);
        layoutHomeApps();
    }

    private void layoutHomeApps() {
        homeAppButtons.forEach(this::removeWidget);
        homeAppButtons.clear();
        homeShortcuts = new java.util.ArrayList<>();
        String query = appQuery.trim().toLowerCase(java.util.Locale.ROOT);
        int shown = 0;
        int columns = McnTheme.SPACE_XXS * McnTheme.SPACE_XXS;
        int tileSize = McnTheme.ICON_TILE_GRID;
        int gap = McnTheme.SPACE_XS;
        int gridWidth = columns * tileSize + (columns - McnTheme.DIVIDER_THICKNESS) * gap;
        int gridX = (imageWidth - gridWidth) / McnTheme.SPACE_XXS;
        int gridY = homeGridY() - topPos;
        int cellHeight = tileSize + McnTheme.SPACE_XXS + McnTheme.TEXT_CAPTION;
        boolean hasDock = ClientPhoneCache.phoneStyle() == PhoneStyle.IOS
                || ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI;
        int dockIndex = 0;
        int gridIndex = 0;
        java.util.List<String> ordered = orderedAppIds();
        int dockStart = Math.max(0, ordered.size() - 4);
        for (String id : ordered) {
            PhoneApp app = java.util.Arrays.stream(PhoneApp.values())
                    .filter(value -> value != PhoneApp.HOME && value.id.equals(id)).findFirst().orElseThrow();
            if (!isAllowedInLowPower(app) || !isAppAvailable(app)) {
                continue;
            }
            if (!query.isEmpty() && !app.label.toLowerCase(java.util.Locale.ROOT).contains(query)) {
                continue;
            }
            boolean dock = hasDock && query.isEmpty() && ordered.indexOf(id) >= dockStart;
            int position = hasDock && query.isEmpty() ? (dock ? dockIndex++ : gridIndex++) : shown;
            int column = position % columns;
            int row = position / columns;
            shown++;
            int x = gridX + column * (tileSize + gap);
            int y = dock ? 305 : gridY + row * cellHeight;
            homeShortcuts.add(new AppShortcut(app.id, app.label, x, y,
                    McnIconRegistry.getBrandColor(app.id)));
            InvisibleAppButton appButton = new InvisibleAppButton(leftPos + x, topPos + y,
                    tileSize, dock ? tileSize : cellHeight,
                    button -> openApp(app));
            homeAppButtons.add(appButton);
            addRenderableWidget(appButton);
        }
    }

    private java.util.List<String> orderedAppIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (PhoneApp app : PhoneApp.values()) {
            if (app != PhoneApp.HOME && isAllowedInLowPower(app) && isAppAvailable(app)) ids.add(app.id);
        }
        java.util.List<String> order = ClientPhoneCache.homeAppOrder();
        if (order.isEmpty() && (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS
                || ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI)) {
            ids.sort(java.util.Comparator.comparing(id -> java.util.Arrays.stream(PhoneApp.values())
                    .anyMatch(app -> app.id.equals(id) && isSnappleDockApp(app))));
        } else {
            ids.sort(java.util.Comparator.comparingInt(id -> {
                int position = order.indexOf(id);
                return position < 0 ? Integer.MAX_VALUE : position;
            }));
        }
        return ids;
    }

    private static boolean removableApp(String id) {
        return "map".equals(id) || "socials".equals(id) || "bank".equals(id);
    }

    private static boolean isAppAvailable(PhoneApp app) {
        if (app == PhoneApp.FACETIME) {
            return ClientPhoneCache.phoneStyle() == PhoneStyle.IOS;
        }
        return app == PhoneApp.VOICEMAIL || ClientPhoneCache.canUseApp(app.id);
    }

    private static boolean isSnappleDockApp(PhoneApp app) {
        return app == PhoneApp.PHONE || app == PhoneApp.SMS
                || app == PhoneApp.MAP || app == PhoneApp.CAMERA;
    }

    private int homeGreetingY() {
        return McnPrimitives.getScreenY(height) + McnTheme.STATUS_BAR_HEIGHT
                + McnTheme.PHONE_NOTCH_HEIGHT + McnTheme.SPACE_S;
    }

    private int homeSubtitleY() {
        return homeGreetingY() + McnTheme.TEXT_HEADER + McnTheme.SPACE_XS;
    }

    private int homeSearchY() {
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS
                || ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI) {
            return topPos + 263;
        }

        return homeSubtitleY() + McnTheme.TEXT_CAPTION + McnTheme.SPACE_S;
    }

    private int homeSearchWidth() {
        return ClientPhoneCache.phoneStyle() == PhoneStyle.IOS
                ? 94 : imageWidth - McnTheme.SPACE_M * McnTheme.SPACE_XXS;
    }

    private int homeSearchX() {
        return leftPos + (imageWidth - homeSearchWidth()) / 2;
    }

    private int homeGridY() {
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS
                || ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI) {
            return topPos + (appQuery.isBlank() ? 90 : 38);
        }
        return homeSearchY() + McnTheme.TEXT_FIELD_HEIGHT + McnTheme.SPACE_XS + McnTheme.SPACE_S;
    }

    private void beginHomePhoneFrame(GuiGraphics graphics) {
        PhoneColors.applyTheme();
        PhoneFrame.pushPhoneTransform(graphics, this);
        McnPrimitives.drawPhoneFrame(graphics, ClientPhoneCache.phoneStyle(), width, height);
        if (!ClientPhoneCache.phoneDead()) {
            McnPrimitives.drawPhoneWallpaper(graphics, ClientPhoneCache.phoneStyle(),
                    PhoneFrame.getScreenX(this), PhoneFrame.getScreenY(this),
                    PhoneFrame.getScreenW(), PhoneFrame.getScreenH());
            if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
                renderWallpaperAnimation(graphics);
            } else if (ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI) {
                renderAndroidWallpaperAnimation(graphics);
            }
        }
    }

    private void renderAndroidWallpaperAnimation(GuiGraphics graphics) {
        int x = PhoneFrame.getScreenX(this);
        int y = PhoneFrame.getScreenY(this);
        int width = PhoneFrame.getScreenW();
        int height = PhoneFrame.getScreenH();
        double time = net.minecraft.Util.getMillis() / 1000.0D;
        int cx = x + width / 2 + (int) (Math.sin(time * 0.18D) * 20);
        int cy = y + height / 2 + (int) (Math.cos(time * 0.14D) * 35);
        for (int radius = 60; radius >= 20; radius -= 10) {
            McnPrimitives.fillRoundedRect(graphics, cx - radius, cy - radius, radius * 2, radius * 2,
                    radius, 0x0619C4DD);
        }
        for (int i = 0; i < 10; i++) {
            int px = x + 24 + i * (width - 48) / 10;
            double travel = i * 41 - time * (2 + i % 3);
            double offset = travel - Math.floor(travel / (height - 64)) * (height - 64);
            int py = y + 32 + (int) offset;
            graphics.fill(px, py, px + 2, py + 2, 0x50C8F6FF);
        }
    }

    private void renderWallpaperAnimation(GuiGraphics graphics) {
            int x = PhoneFrame.getScreenX(this), y = PhoneFrame.getScreenY(this);
            int w = PhoneFrame.getScreenW(), h = PhoneFrame.getScreenH();
            double time = net.minecraft.Util.getMillis() / 1000.0D;
            int cx = x + w / 2;
            int cy = y + h / 2;
            graphics.pose().pushPose();
            graphics.pose().translate(Math.sin(time * 0.22D) * 32, Math.cos(time * 0.17D) * 45, 0);
            for (int radius = 65; radius >= 15; radius -= 10) {
                McnPrimitives.fillRoundedRect(graphics, cx - radius, cy - radius, radius * 2, radius * 2,
                        radius, 0x080D70D8);
            }
            graphics.pose().popPose();
            for (int i = 0; i < 12; i++) {
                int px = x + 16 + (i * 43 % (w - 32));
                double travel = i * 53 - time * (3 + i % 3);
                double offset = travel - Math.floor(travel / (h - 48)) * (h - 48);
                graphics.pose().pushPose();
                graphics.pose().translate(0, offset, 0);
                graphics.fill(px, y + 24, px + 2, y + 26, 0x80DAEFFF);
                graphics.pose().popPose();
            }
    }

    private void renderHomeHeader(GuiGraphics graphics) {
            PhoneStyle style = ClientPhoneCache.phoneStyle();
            if (style == PhoneStyle.IOS) {
                if (!appQuery.isBlank()) {
                    return;
                }
                int x = leftPos + 14;
                int y = topPos + 29;
                java.time.LocalTime time = java.time.LocalTime.now();
                drawHomeText(graphics, time.format(java.time.format.DateTimeFormatter.ofPattern("HH")),
                        x, y, 25, 0xFFFFFFFF, true);
                drawHomeText(graphics, time.format(java.time.format.DateTimeFormatter.ofPattern("mm")),
                        x, y + 24, 25, 0xFFFFFFFF, true);
                McnPrimitives.fillRoundedRect(graphics, x + 91, y + 4, 79, 45, 13, 0x70312A47);
                SnappleText.draw(graphics, font, java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")),
                        x + 98, y + 11, 0xFFFFFFFF);
                String network = ClientSignalCache.tier() == SignalTier.NO_SERVICE
                        ? "No Service" : ClientSignalCache.networkName();
                SnappleText.draw(graphics, font, font.plainSubstrByWidth(network, 65),
                        x + 98, y + 28, 0xFFE7E6F2);
                return;
            }
            if (style == PhoneStyle.SAMSUNG_ONE_UI) {
                if (!appQuery.isBlank() || editingHome) return;
                drawHomeCenteredText(graphics, java.time.LocalTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                        leftPos + imageWidth / 2, topPos + 31, 24, 0xFFFFFFFF);
                drawHomeCenteredText(graphics, java.time.LocalDate.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d")),
                        leftPos + imageWidth / 2, topPos + 59, 10, 0xFFE7F0F7);
                return;
            }
            int contentLeft = McnPrimitives.getContentLeft(McnPrimitives.getScreenX(width));
            drawHomeText(graphics, java.time.LocalTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                    contentLeft, homeGreetingY(),
                    McnTheme.TEXT_HEADER, 0xFFFFFFFF, true);
            drawHomeText(graphics, java.time.LocalDate.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    contentLeft, homeSubtitleY(),
                    McnTheme.TEXT_CAPTION, 0xFFE7E6F2, false);
    }

    private void activateHomeEditingIfHeld() {
        if (!pressedApp.isEmpty() && !editingHome && !movedApp
                && net.minecraft.Util.getMillis() - appPressedAt >= 500L
                && appQuery.isBlank() && !ClientPhoneCache.lowPowerMode()) {
            editingHome = true;
            pendingRemoval = "";
            if (searchBox != null) searchBox.setFocused(false);
        }
    }

    private void renderHomeApps(GuiGraphics graphics, int mouseX, int mouseY) {
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        activateHomeEditingIfHeld();
        long now = net.minecraft.Util.getMillis();
        float delta = lastIconFrame == 0 ? 1.0F : Math.min(0.1F, Math.max(0, now - lastIconFrame) / 1000.0F);
        lastIconFrame = now;
        float blend = 1.0F - (float) Math.exp(-delta / 0.10F);
        homeIconPositions.keySet().retainAll(homeShortcuts.stream().map(AppShortcut::id).toList());
        java.util.List<AppShortcut> drawOrder = new java.util.ArrayList<>(homeShortcuts);
        drawOrder.sort(java.util.Comparator.comparing(app -> app.id().equals(pressedApp)));
        for (AppShortcut shortcut : drawOrder) {
            int x = leftPos + shortcut.x();
            int y = topPos + shortcut.y();
            float[] position = homeIconPositions.computeIfAbsent(shortcut.id(), id -> new float[] {x, y});
            position[0] += (x - position[0]) * blend;
            position[1] += (y - position[1]) * blend;
            graphics.pose().pushPose();
            graphics.pose().translate(position[0] - x, position[1] - y, 0.0F);
            if (editingHome && pressedApp.equals(shortcut.id())) {
                graphics.pose().translate(dragX - pressX, dragY - pressY, 20.0D);
            } else if (editingHome && style == PhoneStyle.IOS) {
                float phase = (shortcut.id().hashCode() & 255) * 0.037F;
                float angle = (float) Math.sin(now / 155.0D + phase) * 1.7F;
                graphics.pose().translate(x + McnTheme.ICON_TILE_GRID / 2.0F,
                        y + McnTheme.ICON_TILE_GRID / 2.0F, 0.0F);
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
                graphics.pose().translate(-x - McnTheme.ICON_TILE_GRID / 2.0F,
                        -y - McnTheme.ICON_TILE_GRID / 2.0F, 0.0F);
            }
            int tileSize = McnTheme.ICON_TILE_GRID;
            int labelWidth = tileSize + McnTheme.SPACE_XS;
            boolean hovered = mouseX >= x && mouseX < x + tileSize
                    && mouseY >= y && mouseY < y + tileSize + McnTheme.SPACE_XXS + McnTheme.TEXT_CAPTION;
            boolean hasTexture = McnIconRegistry.hasPngTexture(shortcut.id());
            var png = hasTexture ? McnIconRegistry.getPngLocation(shortcut.id()) : null;
            if (style == PhoneStyle.IOS) {
                var snappleIcon = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        "mobilecellnetwork", "textures/gui/snapple_apps/" + shortcut.id() + ".png");
                graphics.blit(snappleIcon, x, y, tileSize, tileSize,
                        0.0F, 0.0F, 256, 256, 256, 256);
            } else {
                McnPrimitives.drawIconTile(graphics, style, font, x, y, tileSize,
                        shortcut.color(), png, McnIconRegistry.getGlyph(shortcut.id()));
            }
            String label = shortcut.label();
            int labelX = x;
            int labelY = y + tileSize + McnTheme.SPACE_XXS;
            boolean dock = (style == PhoneStyle.IOS || style == PhoneStyle.SAMSUNG_ONE_UI)
                    && appQuery.isBlank() && shortcut.y() == 305;
            if (!dock) {
                if (style == PhoneStyle.IOS) {
                    float naturalScale = 9.0F / font.lineHeight;
                    int measuredWidth = font.width(SnappleText.text(label));
                    float scale = Math.min(naturalScale, (tileSize + 6.0F) / Math.max(1, measuredWidth));
                    graphics.pose().pushPose();
                    graphics.pose().translate(labelX + tileSize / 2.0F, labelY, 0.0F);
                    graphics.pose().scale(scale, scale, 1.0F);
                    SnappleText.centered(graphics, font, label, 0, 0, 0xFFFFFFFF);
                    graphics.pose().popPose();
                } else {
                    float scale = Math.min(9.0F / font.lineHeight,
                            (labelWidth - 2.0F) / Math.max(1, font.width(label)));
                    graphics.pose().pushPose();
                    graphics.pose().translate(labelX + tileSize / 2.0F, labelY, 0.0F);
                    graphics.pose().scale(scale, scale, 1.0F);
                    graphics.drawCenteredString(font, label, 0, 0, 0xFFFFFFFF);
                    graphics.pose().popPose();
                }
            }
            if ("phone".equals(shortcut.id()) && ClientSmsCache.missedCallCount() > 0) {
                McnPrimitives.drawBadge(graphics, style, font,
                        x + tileSize - McnTheme.ICON_TILE_SM, y,
                        McnTheme.ICON_TILE_SM,                         McnTheme.BADGE_HEIGHT,
                        ClientSmsCache.missedCallCount());
            }
            if (editingHome && removableApp(shortcut.id())) {
                McnPrimitives.fillRoundedRect(graphics, x - 5, y - 5, 14, 14, 7, 0xFFB32C36);
                graphics.drawCenteredString(font, "-", x + 2, y - 3, 0xFFFFFFFF);
            }
            graphics.pose().popPose();
        }
        if (editingHome) {
            McnPrimitives.fillRoundedRect(graphics, leftPos + 145, topPos + 31, 53, 21, 10, 0xDD263B62);
            graphics.drawCenteredString(font, "Done", leftPos + 171, topPos + 37, 0xFFFFFFFF);
            if (!pendingRemoval.isEmpty()) {
                graphics.drawCenteredString(font, "Tap - again to delete", leftPos + imageWidth / 2,
                        topPos + 244, 0xFFFFFFFF);
            }
        }
    }

    private void renderHomeStatusBar(GuiGraphics graphics) {
            PhoneStyle style = ClientPhoneCache.phoneStyle();
            boolean hasService = ClientSignalCache.tier() != SignalTier.NO_SERVICE;
            String carrier = hasService && !ClientSignalCache.networkName().isBlank()
                    ? ClientSignalCache.networkName() : "No Service";
            String time = java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            if (style == PhoneStyle.SAMSUNG_ONE_UI && !ClientPhoneCache.phoneDead()) {
                McnPrimitives.drawAndroidStatusBar(graphics, font, leftPos, topPos, imageWidth, time,
                        hasService ? ClientSignalCache.bars() : 0, PhoneFrame.phoneChargePercent(),
                        ClientSignalCache.roaming(), hasService, true);
            } else if (style == PhoneStyle.IOS && !ClientPhoneCache.phoneDead()) {
                McnPrimitives.drawSnappleStatusBar(graphics, font, leftPos, topPos, imageWidth, time,
                        hasService ? ClientSignalCache.bars() : 0, PhoneFrame.phoneChargePercent(),
                        ClientSignalCache.roaming(), hasService, true);
            } else {
                McnPrimitives.drawStatusBar(graphics, style, font, leftPos, topPos, imageWidth,
                        carrier, time, hasService ? ClientSignalCache.bars() : 0,
                        PhoneFrame.phoneChargePercent(), ClientSignalCache.roaming(), hasService);
            }
            PhoneNavigation.render(graphics, this);
            PhoneFrame.finishPhoneTransform(graphics);
        }

    private void drawHomeText(GuiGraphics graphics, String text, int x, int y,
                              int textSize, int color, boolean shadow) {
        float scale = textSize / (float) font.lineHeight;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, McnTheme.ELEV_0);
        graphics.pose().scale(scale, scale, McnTheme.ELEV_1);
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            SnappleText.draw(graphics, font, text, 0, 0, color);
        } else {
            graphics.drawString(font, text, 0, 0, color, shadow);
        }
        graphics.pose().popPose();
    }

    private void drawHomeCenteredText(GuiGraphics graphics, String text, int centerX, int y,
                                      int textSize, int color) {
        float scale = textSize / (float) font.lineHeight;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, McnTheme.ELEV_0);
        graphics.pose().scale(scale, scale, McnTheme.ELEV_1);
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            SnappleText.centered(graphics, font, text, 0, 0, color);
        } else {
            graphics.drawCenteredString(font, text, 0, 0, color);
        }
        graphics.pose().popPose();
    }

    private void addPhoneButtons() {
    }

    public static void openPhone(PhoneMenu menu, Inventory inventory) {
        PhoneApp last = rememberedApp;
        PhoneScreen screen = new PhoneScreen(menu, inventory,
                Component.translatable("gui.mobilecellnetwork.phone"));
        Minecraft.getInstance().setScreen(screen);
        if (last != null) {
            screen.openApp(last);
        }
    }

    private void openApp(PhoneApp app) {
        if (ClientPhoneCache.phoneDead()) {
            return;
        }
        if (app == PhoneApp.FACETIME && !isAppAvailable(app)) {
            return;
        }
        if (!isAllowedInLowPower(app)) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("Unavailable in Low Power Mode."), true);
            return;
        }
        rememberedApp = app;
        if (app == PhoneApp.FLASHLIGHT) {
            Minecraft.getInstance().setScreen(new FlashlightScreen(menu));
            return;
        }
        if (app == PhoneApp.SETTINGS) {
            Minecraft.getInstance().setScreen(new SettingsScreen(menu));
            return;
        }
        if (app == PhoneApp.SMS) {
            Minecraft.getInstance().setScreen(new SmsScreen(menu));
            return;
        }
        if (app == PhoneApp.PLAN) {
            Minecraft.getInstance().setScreen(new PlanScreen(menu));
            return;
        }
        if (app == PhoneApp.CONTACTS) {
            Minecraft.getInstance().setScreen(new ContactsScreen(menu));
            return;
        }
        if (app == PhoneApp.MAP) {
            if (com.example.mobilecellnetwork.network.ClientPlanCache.snapshot().plans().stream()
                    .filter(plan -> plan.id().equals(ClientPhoneCache.planId()))
                    .anyMatch(plan -> {
                        try {
                            return SignalTier.valueOf(plan.maxSignalTier()).ordinal() > SignalTier.LTE.ordinal();
                        } catch (IllegalArgumentException exception) {
                            return true;
                        }
                    })) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("Your plan does not support high-speed map data."), true);
                return;
            }
            Minecraft.getInstance().setScreen(new MapScreen(menu));
            return;
        }
        if (app == PhoneApp.STORE) {
            Minecraft.getInstance().setScreen(new StoreScreen(menu));
            return;
        }
        if (app == PhoneApp.BANK) {
            Minecraft.getInstance().setScreen(new BankingScreen(menu));
            return;
        }
        if (app == PhoneApp.VOICEMAIL) {
            Minecraft.getInstance().setScreen(new VoicemailScreen(menu));
            return;
        }
        if (app == PhoneApp.SOCIALS) {
            Minecraft.getInstance().setScreen(new SocialsScreen(menu));
            return;
        }
        if (app == PhoneApp.CAMERA) {
            Minecraft.getInstance().setScreen(new CameraScreen(menu));
            return;
        }
        if (app == PhoneApp.GALLERY) {
            Minecraft.getInstance().setScreen(new GalleryScreen(menu));
            return;
        }
        if (app == PhoneApp.PHONE) {
            Minecraft.getInstance().setScreen(new DialerScreen(menu));
            return;
        }
        if (app == PhoneApp.FACETIME) {
            Minecraft.getInstance().setScreen(new FaceTimeScreen(menu));
            return;
        }
        currentApp = app;
        refreshAppWidgets();
    }

    private void refreshAppWidgets() {
        clearWidgets();
        homeAppButtons.clear();
        searchBox = null;
        if (ClientPhoneCache.phoneDead()) {
            return;
        } else {
            setupAppWidgets();
        }
    }

    private boolean isAllowedInLowPower(PhoneApp app) {
        if (!ClientPhoneCache.lowPowerMode()
                || !com.example.mobilecellnetwork.config.MobileCellNetworkConfig.LOW_POWER_ENABLED.get()) {
            return true;
        }
        if (app == PhoneApp.HOME) {
            return true;
        }
        if (app == PhoneApp.SETTINGS) {
            return true;
        }
        return com.example.mobilecellnetwork.config.MobileCellNetworkConfig.LOW_POWER_ALLOWED_APPS.get()
                .stream().anyMatch(value -> value.equalsIgnoreCase(app.id));
    }

    private void addMapButtons() {
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    mapOffsetX--;
                    PacketDistributor.sendToServer(new MapInteractionPacket());
                })
                .bounds(leftPos + 8, topPos + imageHeight - 48, 28, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    mapOffsetX++;
                    PacketDistributor.sendToServer(new MapInteractionPacket());
                })
                .bounds(leftPos + 38, topPos + imageHeight - 48, 28, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Zoom"), button -> {
                    mapZoomLevel = Math.min(4, mapZoomLevel + 1);
                    PacketDistributor.sendToServer(new MapInteractionPacket());
                })
                .bounds(leftPos + 68, topPos + imageHeight - 48, 55, 18)
                .build());
    }

    private void renderMap(GuiGraphics graphics) {
        var level = Minecraft.getInstance().level;
        var player = Minecraft.getInstance().player;
        if (level == null || player == null) {
            return;
        }
        int divisor = Math.min(64,
                com.example.mobilecellnetwork.config.MobileCellNetworkConfig.MAP_RESOLUTION_DIVISOR.get()
                        * (1 << mapZoomLevel));
        int tileSize = 12;
        int columns = 20;
        int rows = 18;
        int startX = (imageWidth - columns * tileSize) / 2;
        int startY = 72;
        int centerX = player.blockPosition().getX() + mapOffsetX * divisor;
        int centerZ = player.blockPosition().getZ() + mapOffsetZ * divisor;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int worldX = centerX + (column - columns / 2) * divisor + divisor / 2;
                int worldZ = centerZ + (row - rows / 2) * divisor + divisor / 2;
                var pos = new net.minecraft.core.BlockPos(worldX, player.blockPosition().getY(), worldZ);
                var state = level.getBlockState(pos);
                int color = state.isAir() ? 0xFF526A42
                        : state.getFluidState().isEmpty() ? 0xFF827D72 : 0xFF397AA5;
                graphics.fill(leftPos + startX + column * tileSize, topPos + startY + row * tileSize,
                        leftPos + startX + (column + 1) * tileSize - 1,
                        topPos + startY + (row + 1) * tileSize - 1, color);
            }
        }
    }

    private void drawCenteredShadow(GuiGraphics graphics, String text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingHome && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            editingHome = false;
            pendingRemoval = "";
            return true;
        }
        if (searchBox != null && searchBox.isFocused()) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER && !homeAppButtons.isEmpty()) {
                homeAppButtons.get(0).onPress();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (MenuKeyHandler.isInventoryKey(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private enum PhoneApp {
        HOME("Home", PhoneAppColors.SETTINGS), PHONE("Phone", PhoneAppColors.PHONE),
        SMS("SMS", PhoneAppColors.SMS), CONTACTS("Contacts", PhoneAppColors.CONTACTS),
        MAP("Map", PhoneAppColors.MAP), STORE("App Store", "store", PhoneAppColors.STORE),
        SOCIALS("Socials", PhoneAppColors.SOCIALS), BANK("Bank", PhoneAppColors.BANK),
        SETTINGS("Settings", PhoneAppColors.SETTINGS), PLAN("Plan", PhoneAppColors.PLAN),
        VOICEMAIL("Voicemail", PhoneAppColors.VOICEMAIL), FACETIME("FaceTime", "facetime", PhoneAppColors.PHONE),
        CAMERA("Camera", PhoneAppColors.CAMERA), GALLERY("Gallery", PhoneAppColors.GALLERY),
        FLASHLIGHT("Flashlight", PhoneAppColors.FLASHLIGHT);

        private final String label;
        private final String id;
        private final int iconColor;

        PhoneApp(String label, int iconColor) {
            this.label = label;
            this.id = label.toLowerCase(java.util.Locale.ROOT);
            this.iconColor = iconColor;
        }

        PhoneApp(String label, String id, int iconColor) {
            this.label = label;
            this.id = id;
            this.iconColor = iconColor;
        }
    }

    public record AppShortcut(String id, String label, int x, int y, int color) {
    }

    private static final class InvisibleAppButton extends Button {
        private InvisibleAppButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }
    }
}

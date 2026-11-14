package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.CallRequestPacket;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSmsCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.MapInteractionPacket;
import com.example.mobilecellnetwork.network.MapOpenPacket;
import com.example.mobilecellnetwork.network.RequestPhoneDataPacket;
import com.example.mobilecellnetwork.network.PhoneDeviceClientCache;
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
import com.example.mobilecellnetwork.gui.NoServiceOverlay;

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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (ClientPhoneCache.planId().isBlank()) {
            PhoneFrame.render(graphics, this);
            int x = PhoneFrame.getScreenX(this);
            int y = PhoneFrame.getContentTop(this);
            int screenW = PhoneFrame.getScreenW();
            int screenH = PhoneFrame.getScreenH() - 24;
            PhoneStyle style = ClientPhoneCache.phoneStyle();
            NoServiceOverlay.render(graphics, style, x, y, screenW, screenH, "Phone", font);
            int buttonX = x + (screenW - 120) / 2;
            int planButtonY = y + screenH - 54;
            PhoneRenderer.drawPrimaryButton(graphics, style, buttonX, planButtonY, 120, 20,
                    "Choose a plan", font);
            PhoneRenderer.drawSecondaryButton(graphics, style, buttonX, planButtonY + 25, 120, 20,
                    "Emergency SMS", font);
            PhoneFrame.renderStatusBar(graphics, this);
            return;
        }
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
            McnPrimitives.drawCard(graphics, ClientPhoneCache.phoneStyle(),
                    leftPos + McnTheme.SPACE_M, homeSearchY(),
                    imageWidth - McnTheme.SPACE_M * McnTheme.SPACE_XXS,
                    McnTheme.TEXT_FIELD_HEIGHT);
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
        if (ClientPhoneCache.planId().isBlank()) {
            if (button == 0) {
                int x = PhoneFrame.getScreenX(this);
                int y = PhoneFrame.getContentTop(this);
                int screenW = PhoneFrame.getScreenW();
                int planButtonY = y + PhoneFrame.getScreenH() - 24 - 54;
                int buttonX = x + (screenW - 120) / 2;
                if (layoutX >= buttonX && layoutX < buttonX + 120
                        && layoutY >= planButtonY && layoutY < planButtonY + 20) {
                    Minecraft.getInstance().setScreen(new PlanScreen(menu));
                    return true;
                }
                if (layoutX >= buttonX && layoutX < buttonX + 120
                        && layoutY >= planButtonY + 25 && layoutY < planButtonY + 45) {
                    Minecraft.getInstance().setScreen(SmsScreen.emergency(menu));
                    return true;
                }
            }
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
        int searchX = leftPos + McnTheme.SPACE_M;
        int searchY = homeSearchY();
        int searchWidth = imageWidth - McnTheme.SPACE_M * McnTheme.SPACE_XXS;
        searchBox = new net.minecraft.client.gui.components.EditBox(font,
                searchX + McnTheme.SPACE_S, searchY,
                searchWidth - McnTheme.SPACE_S * McnTheme.SPACE_XXS,
                McnTheme.TEXT_FIELD_HEIGHT, Component.literal("Search apps"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(24);
        searchBox.setHint(Component.literal("Search apps"));
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        searchBox.setTextColor(PhoneColors.textPrimary(style));
        searchBox.setTextColorUneditable(PhoneColors.textSecondary(style));
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
        for (int index = 0; index < APPS.length; index++) {
            final PhoneApp app = PhoneApp.values()[index + 1];
            if (!isAllowedInLowPower(app)
                    || (app != PhoneApp.VOICEMAIL && !ClientPhoneCache.canUseApp(app.id))) {
                continue;
            }
            if (!query.isEmpty() && !app.label.toLowerCase(java.util.Locale.ROOT).contains(query)) {
                continue;
            }
            int column = shown % columns;
            int row = shown / columns;
            shown++;
            int x = gridX + column * (tileSize + gap);
            int y = gridY + row * cellHeight;
            homeShortcuts.add(new AppShortcut(app.id, app.label, x, y,
                    McnIconRegistry.getBrandColor(app.id)));
            InvisibleAppButton appButton = new InvisibleAppButton(leftPos + x, topPos + y,
                    tileSize, cellHeight,
                    button -> openApp(app));
            homeAppButtons.add(appButton);
            addRenderableWidget(appButton);
        }
    }

    private int homeGreetingY() {
        return McnPrimitives.getScreenY(height) + McnTheme.STATUS_BAR_HEIGHT
                + McnTheme.PHONE_NOTCH_HEIGHT + McnTheme.SPACE_S;
    }

    private int homeSubtitleY() {
        return homeGreetingY() + McnTheme.TEXT_HEADER + McnTheme.SPACE_XS;
    }

    private int homeSearchY() {
        return homeSubtitleY() + McnTheme.TEXT_CAPTION + McnTheme.SPACE_S;
    }

    private int homeGridY() {
        return homeSearchY() + McnTheme.TEXT_FIELD_HEIGHT + McnTheme.SPACE_XS + McnTheme.SPACE_S;
    }

    private void beginHomePhoneFrame(GuiGraphics graphics) {
        PhoneColors.applyTheme();
        double scale = Math.clamp(Math.min((width - McnTheme.SPACE_L) / (double) McnTheme.PHONE_WIDTH,
                (height - McnTheme.SPACE_L) / (double) McnTheme.PHONE_HEIGHT), 0.1D, 1.0D);
        double centerX = width / 2.0D;
        double centerY = height / 2.0D;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0D);
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0D);
        McnPrimitives.drawPhoneFrame(graphics, ClientPhoneCache.phoneStyle(), width, height);
    }

    private void renderHomeHeader(GuiGraphics graphics) {
            PhoneStyle style = ClientPhoneCache.phoneStyle();
            int contentLeft = McnPrimitives.getContentLeft(McnPrimitives.getScreenX(width));
            drawHomeText(graphics, "Good " + timeGreeting(), contentLeft, homeGreetingY(),
                    McnTheme.TEXT_HEADER, PhoneColors.textPrimary(style), true);
            drawHomeText(graphics, "Your mobile network", contentLeft, homeSubtitleY(),
                    McnTheme.TEXT_CAPTION, PhoneColors.textSecondary(style), false);
    }

    private void renderHomeApps(GuiGraphics graphics, int mouseX, int mouseY) {
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        for (AppShortcut shortcut : homeShortcuts) {
            int x = leftPos + shortcut.x();
            int y = topPos + shortcut.y();
            int tileSize = McnTheme.ICON_TILE_GRID;
            int labelWidth = tileSize + McnTheme.SPACE_XS;
            boolean hovered = mouseX >= x && mouseX < x + tileSize
                    && mouseY >= y && mouseY < y + tileSize + McnTheme.SPACE_XXS + McnTheme.TEXT_CAPTION;
            boolean hasTexture = McnIconRegistry.hasPngTexture(shortcut.id());
            var png = hasTexture ? McnIconRegistry.getPngLocation(shortcut.id()) : null;
            McnPrimitives.drawIconTile(graphics, style, font, x, y, tileSize,
                    shortcut.color(), png, McnIconRegistry.getGlyph(shortcut.id()));
            String label = shortcut.label();
            int maxFontWidth = (int) (labelWidth * (font.lineHeight / (float) McnTheme.TEXT_CAPTION));
            if (font.width(label) > maxFontWidth) {
                label = font.plainSubstrByWidth(label,
                        Math.max(McnTheme.RADIUS_NONE, maxFontWidth - font.width("..."))) + "...";
            }
            int labelX = x;
            int labelY = y + tileSize + McnTheme.SPACE_XXS;
            drawHomeCenteredText(graphics, label, labelX + tileSize / McnTheme.SPACE_XXS,
                    labelY, McnTheme.TEXT_CAPTION,
                    PhoneColors.textPrimary(style));
            if ("phone".equals(shortcut.id()) && ClientSmsCache.missedCallCount() > 0) {
                McnPrimitives.drawBadge(graphics, style, font,
                        x + tileSize - McnTheme.ICON_TILE_SM, y,
                        McnTheme.ICON_TILE_SM,                         McnTheme.BADGE_HEIGHT,
                        ClientSmsCache.missedCallCount());
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
            McnPrimitives.drawStatusBar(graphics, style, font, leftPos, topPos, imageWidth,
                    carrier, time, hasService ? ClientSignalCache.bars() : 0,
                    PhoneFrame.phoneChargePercent(), ClientSignalCache.roaming(), hasService);
            PhoneNavigation.render(graphics, this);
            graphics.pose().popPose();
        }

    private void drawHomeText(GuiGraphics graphics, String text, int x, int y,
                              int textSize, int color, boolean shadow) {
        float scale = textSize / (float) font.lineHeight;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, McnTheme.ELEV_0);
        graphics.pose().scale(scale, scale, McnTheme.ELEV_1);
        graphics.drawString(font, text, McnTheme.RADIUS_NONE, McnTheme.RADIUS_NONE, color, shadow);
        graphics.pose().popPose();
    }

    private void drawHomeCenteredText(GuiGraphics graphics, String text, int centerX, int y,
                                      int textSize, int color) {
        float scale = textSize / (float) font.lineHeight;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, McnTheme.ELEV_0);
        graphics.pose().scale(scale, scale, McnTheme.ELEV_1);
        graphics.drawCenteredString(font, text, McnTheme.RADIUS_NONE, McnTheme.RADIUS_NONE, color);
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

    private String timeGreeting() {
        long ticks = Minecraft.getInstance().level == null ? 0L
                : Math.floorMod(Minecraft.getInstance().level.getDayTime(), 24_000L);
        int hour = (int) (ticks / 1_000L);
        return hour < 6 ? "evening" : hour < 12 ? "morning" : hour < 18 ? "afternoon" : "evening";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
        VOICEMAIL("Voicemail", PhoneAppColors.VOICEMAIL),
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

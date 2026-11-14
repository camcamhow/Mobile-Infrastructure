package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.PhoneDeviceClientCache;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.network.UnlockPhonePacket;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LockScreen extends Screen implements PhoneNavigableScreen {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMM d");
    private final PhoneMenu menu;
    private boolean pinPadVisible;
    private boolean draggingUp;
    private double dragStartY;
    private String pin = "";
    private String error = "";
    private int observedPinResultRevision;
    private Button unlockButton;
    // Phone screen area coordinates (phone space)
    private int screenX;
    private int screenY;
    private int screenWidth;
    private int screenHeight;

    public LockScreen(PhoneMenu menu) {
        super(Component.literal("Phone Lock Screen"));
        this.menu = menu;
        observedPinResultRevision = PhoneDeviceClientCache.pinResultRevision();
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Do nothing - prevent the default dark overlay from covering the phone UI
    }

    @Override
    protected void init() {
        unlockButton = null;
        clearWidgets();
        
        // Calculate phone screen area coordinates (phone space)
        screenX = McnPrimitives.getScreenX(width);
        screenY = McnPrimitives.getScreenY(height);
        screenWidth = McnPrimitives.getScreenWidth();
        screenHeight = McnPrimitives.getScreenHeight();

        if (PhoneDeviceClientCache.pinAccepted()) {
            unlock();
            return;
        }
        if (pinPadVisible && PhoneDeviceClientCache.pinSet() && !PhoneDeviceClientCache.lockedAll()) {
            addPinButtons();
            return;
        }
        if (!PhoneDeviceClientCache.lockedAll()) {
            // Position unlock button in phone space coordinates (relative to screen area)
            int buttonWidth = 96;
            int buttonHeight = 20;
            int buttonX = screenX + (screenWidth - buttonWidth) / 2;
            int buttonY = screenY + screenHeight - McnTheme.HOME_INDICATOR_HEIGHT - McnTheme.SPACE_L - buttonHeight - McnTheme.SPACE_S;
            unlockButton = Button.builder(Component.literal(
                            PhoneDeviceClientCache.pinSet() ? "Enter PIN" : "Unlock"),
                    button -> beginUnlock()).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();
            // Add as widget - it will be rendered by super.render with phone space coordinates
            addRenderableWidget(unlockButton);
        }
    }

    private void addPinButtons() {
        int cell = 34;
        int gap = 4;
        int totalWidth = 3 * cell + 2 * gap;
        int startX = screenX + (screenWidth - totalWidth) / 2;
        int startY = screenY + screenHeight / 2;
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "DEL", "0", "OK"};
        for (int index = 0; index < labels.length; index++) {
            int row = index / 3;
            int column = index % 3;
            String label = labels[index];
            addRenderableWidget(Button.builder(Component.literal(label), button -> pinButton(label))
                    .bounds(startX + column * (cell + gap), startY + row * (cell + gap), cell, cell).build());
        }
    }

    private void beginUnlock() {
        if (PhoneDeviceClientCache.lockedAll()) {
            return;
        }
        if (PhoneDeviceClientCache.pinSet()) {
            pinPadVisible = true;
            clearWidgets();
            init();
            return;
        }
        PacketDistributor.sendToServer(new UnlockPhonePacket(""));
    }

    private void pinButton(String value) {
        error = "";
        if ("DEL".equals(value)) {
            if (!pin.isEmpty()) {
                pin = pin.substring(0, pin.length() - 1);
            }
        } else if ("OK".equals(value)) {
            if (!pin.isEmpty()) {
                PacketDistributor.sendToServer(new UnlockPhonePacket(pin));
            }
        } else if (pin.length() < 12) {
            pin += value;
        }
    }

    private void unlock() {
        if (PhoneDeviceClientCache.lockedAll() || minecraft == null || minecraft.player == null) {
            return;
        }
        PhoneDeviceClientCache.setUnlocked(true);
        minecraft.setScreen(new PhoneScreen(menu, minecraft.player.getInventory(),
                Component.translatable("gui.mobilecellnetwork.phone")));
    }

    @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (observedPinResultRevision != PhoneDeviceClientCache.pinResultRevision()) {
                observedPinResultRevision = PhoneDeviceClientCache.pinResultRevision();
                if (!PhoneDeviceClientCache.pinAccepted()) {
                    error = "Incorrect PIN";
                }
            }
            if (PhoneDeviceClientCache.pinAccepted() && !PhoneDeviceClientCache.lockedAll()) {
                PhoneDeviceClientCache.setPinAccepted(false);
                unlock();
                return;
            }
            PhoneStyle style = ClientPhoneCache.phoneStyle();
            beginPhoneFrame(graphics, style);
        
            // Use stored phone screen area coordinates
            int centerX = screenX + screenWidth / McnTheme.SPACE_XXS;
            int timeY = screenY + screenHeight / 3;
            drawCenteredTextAtSize(graphics, LocalDateTime.now().format(TIME_FORMAT), centerX, timeY,
                    McnTheme.TEXT_HERO, PhoneColors.textPrimary(style), true);
            int dateY = timeY + McnTheme.TEXT_HERO / McnTheme.SPACE_XXS
                    + McnTheme.SPACE_S + McnTheme.TEXT_HEADER / McnTheme.SPACE_XXS;
            drawCenteredTextAtSize(graphics, LocalDateTime.now().format(DATE_FORMAT), centerX, dateY,
                    McnTheme.TEXT_HEADER, PhoneColors.textSecondary(style), false);

            if (PhoneDeviceClientCache.lockedAll()) {
                drawCenteredTextAtSize(graphics, "This phone has been locked by its owner",
                        centerX, screenY + screenHeight / McnTheme.SPACE_XXS,
                        McnTheme.TEXT_BODY, PhoneColors.error(style), true);
            } else if (PhoneDeviceClientCache.pinSet() && pinPadVisible) {
                drawCenteredTextAtSize(graphics, "Enter PIN", centerX,
                        screenY + screenHeight / McnTheme.SPACE_XXS - McnTheme.SPACE_XL,
                        McnTheme.TEXT_HEADER, PhoneColors.textPrimary(style), true);
                drawCenteredTextAtSize(graphics, "* ".repeat(pin.length()), centerX,
                        screenY + screenHeight / McnTheme.SPACE_XXS,
                        McnTheme.TEXT_BODY, PhoneColors.accent(style), false);
                if (!error.isBlank()) {
                    drawCenteredTextAtSize(graphics, error, centerX,
                            screenY + screenHeight / McnTheme.SPACE_XXS + McnTheme.SPACE_XL,
                            McnTheme.TEXT_CAPTION, PhoneColors.error(style), false);
                }
            } else {
                int promptY = McnPrimitives.getContentBottom(screenY, screenHeight)
                        - McnTheme.TEXT_CAPTION / McnTheme.SPACE_XXS - McnTheme.SPACE_XS;
                if (PhoneDeviceClientCache.hasUnreadNotifications()) {
                    int dotSize = McnTheme.PROGRESS_BAR_HEIGHT;
                    int dotX = centerX - dotSize / McnTheme.SPACE_XXS;
                    int dotY = promptY - McnTheme.TEXT_CAPTION / McnTheme.SPACE_XXS
                            - McnTheme.SPACE_S - dotSize;
                    McnPrimitives.fillRoundedRect(graphics, dotX, dotY, dotSize, dotSize,
                            McnTheme.RADIUS_ROUND, PhoneColors.accent(style));
                }
                drawCenteredTextAtSize(graphics,
                        PhoneDeviceClientCache.pinSet() ? "Swipe up or press Home to enter PIN" : "Swipe up to unlock",
                        centerX, promptY, McnTheme.TEXT_CAPTION, PhoneColors.textSecondary(style), false);
            }
            boolean hasService = ClientSignalCache.tier() != SignalTier.NO_SERVICE
                                                    && !ClientSignalCache.networkName().isBlank();
                                    McnPrimitives.drawStatusBar(graphics, style, font, screenX, screenY, screenWidth,
                                            hasService ? ClientSignalCache.networkName() : "No Service",
                                            LocalDateTime.now().format(TIME_FORMAT), hasService ? ClientSignalCache.bars() : 0,
                                            PhoneFrame.phoneChargePercent(), ClientSignalCache.roaming(), hasService);
                                    graphics.pose().popPose();

            // Render widgets (unlock button, pin pad) in phone space coordinates
            super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                    PhoneFrame.displayMouseY(this, mouseY), partialTick);
        }

    private void beginPhoneFrame(GuiGraphics graphics, PhoneStyle style) {
        double scale = Math.clamp(Math.min((width - McnTheme.SPACE_L) / (double) McnTheme.PHONE_WIDTH,
                (height - McnTheme.SPACE_L) / (double) McnTheme.PHONE_HEIGHT), 0.1D, 1.0D);
        double centerX = width / 2.0D;
        double centerY = height / 2.0D;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, McnTheme.ELEV_0);
        graphics.pose().scale((float) scale, (float) scale, McnTheme.ELEV_1);
        graphics.pose().translate(-centerX, -centerY, McnTheme.ELEV_0);
        McnPrimitives.drawPhoneFrame(graphics, style, width, height);
    }

    private void drawCenteredTextAtSize(GuiGraphics graphics, String text, int centerX, int centerY,
                                        int textSize, int color, boolean shadow) {
        float scale = textSize / (float) font.lineHeight;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, McnTheme.ELEV_0);
        graphics.pose().scale(scale, scale, McnTheme.ELEV_1);
        graphics.drawString(font, text, -font.width(text) / McnTheme.SPACE_XXS,
                -font.lineHeight / McnTheme.SPACE_XXS, color, shadow);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double layoutX = PhoneFrame.layoutMouseX(this, mouseX);
            double layoutY = PhoneFrame.layoutMouseY(this, mouseY);
            dragStartY = layoutY;
            draggingUp = button == 0 && layoutY > PhoneFrame.getScreenY(this) + PhoneFrame.getScreenH() - 48;
            return super.mouseClicked(layoutX, layoutY, button) || draggingUp;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            double layoutY = PhoneFrame.layoutMouseY(this, mouseY);
            boolean shouldUnlock = draggingUp && layoutY < dragStartY - 24;
            draggingUp = false;
            if (shouldUnlock) {
                beginUnlock();
                return true;
            }
            return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX), layoutY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return super.mouseDragged(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY),
                    button, PhoneFrame.layoutMouseDeltaX(this, dragX), PhoneFrame.layoutMouseDeltaY(this, dragY));
        }

        @Override
        public void navigateHome() {
            beginUnlock();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
        }

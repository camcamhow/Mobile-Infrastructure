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
        draggingUp = false;
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
    }

    private void addPinButtons() {
        int cell = 34;
        int gap = 4;
        int cellWidth = ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI
                ? PhoneFrame.circularElementWidth(cell) : cell;
        int totalWidth = 3 * cellWidth + 2 * gap;
        int startX = screenX + (screenWidth - totalWidth) / 2;
        int startY = screenY + screenHeight / 2;
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "DEL", "0", "OK"};
        for (int index = 0; index < labels.length; index++) {
            int row = index / 3;
            int column = index % 3;
            String label = labels[index];
            addRenderableWidget(new LockButton(startX + column * (cellWidth + gap),
                    startY + row * (cell + gap), cellWidth, cell, label, button -> pinButton(label)));
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
            boolean snapple = style == PhoneStyle.IOS;
            int timeY = screenY + (pinPadVisible ? screenHeight / 4 : snapple ? 113 : screenHeight / 3);
            int foreground = 0xFFFFFFFF;
            int secondary = 0xFFE7E6F2;
            if (snapple || !pinPadVisible) {
                drawCenteredTextAtSize(graphics, LocalDateTime.now().format(TIME_FORMAT), centerX, timeY,
                        snapple && !pinPadVisible ? 46 : McnTheme.TEXT_HERO, foreground, true);
            }
            int dateY = timeY + McnTheme.TEXT_HERO / McnTheme.SPACE_XXS
                    + McnTheme.SPACE_S + McnTheme.TEXT_HEADER / McnTheme.SPACE_XXS;
            if (snapple && !pinPadVisible) {
                dateY = timeY - 38;
                McnPrimitives.fillRoundedRectOutline(graphics, centerX - 4, screenY + 40, 8, 9, 4, 1, foreground);
                McnPrimitives.fillRoundedRect(graphics, centerX - 6, screenY + 46, 12, 9, 3, foreground);
            }
            if (snapple || !pinPadVisible) {
                drawCenteredTextAtSize(graphics, LocalDateTime.now().format(DATE_FORMAT), centerX, dateY,
                        McnTheme.TEXT_HEADER, secondary, false);
            }

            if (PhoneDeviceClientCache.lockedAll()) {
                drawCenteredTextAtSize(graphics, "This phone has been locked by its owner",
                        centerX, screenY + screenHeight / McnTheme.SPACE_XXS,
                        McnTheme.TEXT_BODY, PhoneColors.error(style), true);
            } else if (PhoneDeviceClientCache.pinSet() && pinPadVisible) {
                drawCenteredTextAtSize(graphics, "Enter PIN", centerX,
                        screenY + screenHeight / McnTheme.SPACE_XXS - (snapple ? 47 : 88),
                        McnTheme.TEXT_HEADER, foreground, true);
                drawCenteredTextAtSize(graphics, "* ".repeat(pin.length()), centerX,
                        screenY + screenHeight / McnTheme.SPACE_XXS - (snapple ? 29 : 61),
                        McnTheme.TEXT_BODY, snapple ? PhoneColors.accent(style) : foreground, false);
                if (!error.isBlank()) {
                    drawCenteredTextAtSize(graphics, error, centerX,
                            screenY + screenHeight / McnTheme.SPACE_XXS - (snapple ? 14 : 32),
                            McnTheme.TEXT_CAPTION, PhoneColors.error(style), false);
                }
            } else {
                int promptY = McnPrimitives.getContentBottom(screenY, screenHeight)
                        - McnTheme.TEXT_CAPTION / McnTheme.SPACE_XXS - McnTheme.SPACE_XS;
                if (snapple) {
                    promptY = screenY + screenHeight - 22;
                }
                if (PhoneDeviceClientCache.hasUnreadNotifications()) {
                    int dotSize = McnTheme.PROGRESS_BAR_HEIGHT;
                    int dotX = centerX - dotSize / McnTheme.SPACE_XXS;
                    int dotY = promptY - McnTheme.TEXT_CAPTION / McnTheme.SPACE_XXS
                            - McnTheme.SPACE_S - dotSize;
                    McnPrimitives.fillRoundedRect(graphics, dotX, dotY, dotSize, dotSize,
                            McnTheme.RADIUS_ROUND, PhoneColors.accent(style));
                }
                drawCenteredTextAtSize(graphics, "Swipe up to unlock",
                        centerX, promptY, McnTheme.TEXT_CAPTION, secondary, false);
            }
            boolean hasService = ClientSignalCache.tier() != SignalTier.NO_SERVICE
                                                    && !ClientSignalCache.networkName().isBlank();
            if (snapple) {
                McnPrimitives.drawSnappleStatusBar(graphics, font, screenX, screenY, screenWidth,
                        LocalDateTime.now().format(TIME_FORMAT), hasService ? ClientSignalCache.bars() : 0,
                        PhoneFrame.phoneChargePercent(), ClientSignalCache.roaming(), hasService, true);
                McnPrimitives.fillRoundedRect(graphics, centerX - 28, screenY + screenHeight - 9,
                        56, 4, 2, 0xFFFFFFFF);
            } else if (style == PhoneStyle.SAMSUNG_ONE_UI) {
                McnPrimitives.drawAndroidStatusBar(graphics, font, screenX, screenY, screenWidth,
                        LocalDateTime.now().format(TIME_FORMAT), hasService ? ClientSignalCache.bars() : 0,
                        PhoneFrame.phoneChargePercent(), ClientSignalCache.roaming(), hasService, true);
            } else {
                McnPrimitives.drawStatusBar(graphics, style, font, screenX, screenY, screenWidth,
                        hasService ? ClientSignalCache.networkName() : "No Service",
                        LocalDateTime.now().format(TIME_FORMAT), hasService ? ClientSignalCache.bars() : 0,
                        PhoneFrame.phoneChargePercent(), ClientSignalCache.roaming(), hasService);
            }
            super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                    PhoneFrame.displayMouseY(this, mouseY), partialTick);
            PhoneFrame.finishPhoneTransform(graphics);
        }

    private void beginPhoneFrame(GuiGraphics graphics, PhoneStyle style) {
        PhoneFrame.pushPhoneTransform(graphics, this);
        McnPrimitives.drawPhoneFrame(graphics, style, width, height);
        McnPrimitives.drawPhoneWallpaper(graphics, style, screenX, screenY, screenWidth, screenHeight);
    }

    private void drawCenteredTextAtSize(GuiGraphics graphics, String text, int centerX, int centerY,
                                        int textSize, int color, boolean shadow) {
        float scale = textSize / (float) font.lineHeight;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, McnTheme.ELEV_0);
        graphics.pose().scale(scale, scale, McnTheme.ELEV_1);
        if (ClientPhoneCache.phoneStyle() == PhoneStyle.IOS) {
            SnappleText.centered(graphics, font, text, 0, -font.lineHeight / 2, color);
        } else {
            graphics.drawString(font, text, -font.width(text) / McnTheme.SPACE_XXS,
                    -font.lineHeight / McnTheme.SPACE_XXS, color, shadow);
        }
        graphics.pose().popPose();
    }

    private static final class LockButton extends Button {
        private LockButton(int x, int y, int width, int height, String label, OnPress onPress) {
            super(x, y, width, height, Component.literal(label), onPress, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean snapple = ClientPhoneCache.phoneStyle() == PhoneStyle.IOS;
            McnPrimitives.fillRoundedRect(graphics, getX(), getY(), getWidth(), getHeight(),
                    getHeight() / 2, isHovered() ? 0xB0FFFFFF : snapple ? 0x704D5068 : 0x80576879);
            if (snapple) {
                SnappleText.centered(graphics, Minecraft.getInstance().font, getMessage().getString(),
                        getX() + getWidth() / 2, getY() + (getHeight() - 9) / 2, 0xFFFFFFFF);
            } else {
                graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                        getX() + getWidth() / 2, getY() + (getHeight() - 9) / 2, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double layoutX = PhoneFrame.layoutMouseX(this, mouseX);
            double layoutY = PhoneFrame.layoutMouseY(this, mouseY);
            draggingUp = button == 0 && !pinPadVisible && !PhoneDeviceClientCache.lockedAll()
                    && layoutX >= screenX && layoutX < screenX + screenWidth
                    && layoutY >= screenY + screenHeight - 48 && layoutY < screenY + screenHeight;
            if (draggingUp) {
                dragStartY = layoutY;
                return true;
            }
            return super.mouseClicked(layoutX, layoutY, button);
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            double layoutY = PhoneFrame.layoutMouseY(this, mouseY);
            boolean shouldUnlock = draggingUp && button == 0 && layoutY < dragStartY - 24;
            draggingUp = false;
            if (shouldUnlock) {
                beginUnlock();
                return true;
            }
            return super.mouseReleased(PhoneFrame.layoutMouseX(this, mouseX), layoutY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            if (draggingUp && button == 0) {
                return true;
            }
            return super.mouseDragged(PhoneFrame.layoutMouseX(this, mouseX), PhoneFrame.layoutMouseY(this, mouseY),
                    button, PhoneFrame.layoutMouseDeltaX(this, dragX), PhoneFrame.layoutMouseDeltaY(this, dragY));
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
        }

package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.registry.ModDataComponents;
import com.example.mobilecellnetwork.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public final class PhoneFrame {
    private static boolean drawingPhone;
    private static final int BODY_WIDTH = McnTheme.PHONE_WIDTH;
    private static final int BODY_HEIGHT = McnTheme.PHONE_HEIGHT;
    private static final double SNAPPLE_WIDTH_FACTOR = (71.6D / 147.6D)
            / ((double) BODY_WIDTH / BODY_HEIGHT);
    private static final double G24_WIDTH_FACTOR = (70.6D / 147.0D)
            / ((double) BODY_WIDTH / BODY_HEIGHT);

    private PhoneFrame() {
    }

    public static void render(GuiGraphics graphics, Screen screen) {
        PhoneColors.applyTheme();
        pushPhoneTransform(graphics, screen);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        McnPrimitives.drawPhoneFrame(graphics, style, screen.width, screen.height);
    }

    public static void pushPhoneTransform(GuiGraphics graphics, Screen screen) {
        beginPhoneText();
        double scale = scale(screen);
        double centerX = screen.width / 2.0D;
        double centerY = screen.height / 2.0D;
        graphics.pose().pushPose();
        graphics.pose().translate(displayCenterX(screen), displayCenterY(screen), 0.0D);
        graphics.pose().scale((float) (scale * widthFactor()), (float) scale, 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0D);
    }

    public static boolean isDrawingPhone() {
        return drawingPhone;
    }

    public static void beginPhoneText() {
        drawingPhone = true;
    }

    public static void endPhoneText() {
        drawingPhone = false;
    }

    public static void finishPhoneTransform(GuiGraphics graphics) {
        endPhoneText();
        graphics.pose().popPose();
    }

    public static String displayNetworkName() {
        // TODO: consolidate into McnPrimitives
        return ClientSignalCache.networkName();
    }

    public static void renderStatusBar(GuiGraphics graphics, Screen screen) {
        renderStatusBar(graphics, screen, false);
    }

    public static void renderStatusBar(GuiGraphics graphics, Screen screen, boolean onWallpaper) {
        int x = getScreenX(screen);
        int y = getScreenY(screen);
        var font = Minecraft.getInstance().font;
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        boolean hasService = !ClientSignalCache.networkName().isBlank()
                && ClientSignalCache.tier() != SignalTier.NO_SERVICE;
        String network = hasService ? displayNetworkName() : "No Service";
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        if (style == PhoneStyle.SAMSUNG_ONE_UI) {
            McnPrimitives.drawAndroidStatusBar(graphics, font, x, y, getScreenW(), time,
                    hasService ? ClientSignalCache.bars() : 0, phoneChargePercent(),
                    ClientSignalCache.roaming(), hasService, onWallpaper);
        } else if (style == PhoneStyle.IOS && onWallpaper) {
            McnPrimitives.drawSnappleStatusBar(graphics, font, x, y, getScreenW(), time,
                    hasService ? ClientSignalCache.bars() : 0, phoneChargePercent(),
                    ClientSignalCache.roaming(), hasService, true);
        } else {
            McnPrimitives.drawStatusBar(graphics, style, font, x, y, getScreenW(), network,
                    time, hasService ? ClientSignalCache.bars() : 0, phoneChargePercent(),
                    ClientSignalCache.roaming(), hasService);
        }
        if (com.example.mobilecellnetwork.network.ClientConnectionCache.connecting()) {
            McnPrimitives.drawChip(graphics, style, font,
                    x + getScreenW() - McnTheme.PHONE_NOTCH_WIDTH - McnTheme.SPACE_M,
                    y + McnTheme.STATUS_BAR_HEIGHT,
                    McnTheme.PHONE_NOTCH_WIDTH + McnTheme.SPACE_M, McnTheme.BADGE_HEIGHT,
                    "Connecting...", PhoneColors.bgTertiary(style), PhoneColors.warn(style));
        }
        // TODO: consolidate phone navigation controls into McnPrimitives
        PhoneNavigation.render(graphics, screen);
        finishPhoneTransform(graphics);
    }

    // TODO: consolidate into McnPrimitives
    private static double scale(Screen screen) {
        double base = Math.clamp(Math.min((screen.width - 16.0D) / (BODY_WIDTH * widthFactor()),
                (screen.height - 16.0D) / BODY_HEIGHT), 0.1D, 1.0D);
        return HeldPhoneView.active() ? base * 0.90D : base;
    }

    private static double displayCenterX(Screen screen) {
        return HeldPhoneView.active() ? screen.width - 8.0D - BODY_WIDTH * widthFactor() * scale(screen) / 2
                : screen.width / 2.0D;
    }

    private static double displayCenterY(Screen screen) {
        return HeldPhoneView.active() ? screen.height - 8.0D - BODY_HEIGHT * scale(screen) / 2
                : screen.height / 2.0D;
    }

    private static double widthFactor() {
        return switch (ClientPhoneCache.phoneStyle()) {
            case IOS -> SNAPPLE_WIDTH_FACTOR;
            case SAMSUNG_ONE_UI -> G24_WIDTH_FACTOR;
            default -> 1.0D;
        };
    }

    public static int circularElementWidth(int height) {
        return (int) Math.round(height / widthFactor());
    }

    public static boolean containsPhone(Screen screen, double mouseX, double mouseY) {
        double x = layoutMouseX(screen, mouseX);
        double y = layoutMouseY(screen, mouseY);
        double left = (screen.width - BODY_WIDTH) / 2.0D;
        double top = (screen.height - BODY_HEIGHT) / 2.0D;
        return x >= left && x < left + BODY_WIDTH && y >= top && y < top + BODY_HEIGHT;
    }

    public static double layoutMouseX(Screen screen, double mouseX) {
        // TODO: consolidate into McnPrimitives
        double center = screen.width / 2.0D;
        return center + (mouseX - displayCenterX(screen)) / (scale(screen) * widthFactor());
    }

    public static double layoutMouseY(Screen screen, double mouseY) {
        // TODO: consolidate into McnPrimitives
        double center = screen.height / 2.0D;
        return center + (mouseY - displayCenterY(screen)) / scale(screen);
    }

    public static double layoutMouseDeltaX(Screen screen, double deltaX) {
        // TODO: consolidate into McnPrimitives
        return deltaX / (scale(screen) * widthFactor());
    }

    public static double layoutMouseDeltaY(Screen screen, double deltaY) {
        // TODO: consolidate into McnPrimitives
        return deltaY / scale(screen);
    }

    public static int displayMouseX(Screen screen, double mouseX) {
        // TODO: consolidate into McnPrimitives
        return (int) Math.round(layoutMouseX(screen, mouseX));
    }

    public static int displayMouseY(Screen screen, double mouseY) {
        // TODO: consolidate into McnPrimitives
        return (int) Math.round(layoutMouseY(screen, mouseY));
    }

    // TODO: consolidate into McnPrimitives
    private static int phoneCharge() {
        if (com.example.mobilecellnetwork.network.ClientSignalCache.charge() >= 0) {
            return com.example.mobilecellnetwork.network.ClientSignalCache.charge();
        }
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(mainHand)) {
            return mainHand.getOrDefault(ModDataComponents.PHONE_CHARGE.get(), 0);
        }
        if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(offHand)) {
            return offHand.getOrDefault(ModDataComponents.PHONE_CHARGE.get(), 0);
        }
        for (ItemStack stack : player.getInventory().items) {
            if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack)) {
                return stack.getOrDefault(ModDataComponents.PHONE_CHARGE.get(), 0);
            }
        }
        return 0;
    }

    public static int getScreenX(Screen screen) {
        return McnPrimitives.getScreenX(screen.width);
    }

    public static int getScreenY(Screen screen) {
        return McnPrimitives.getScreenY(screen.height);
    }

    public static int getContentTop(Screen screen) {
        return McnPrimitives.getScreenY(screen.height) + McnTheme.STATUS_BAR_HEIGHT;
    }

    public static int getScreenW() {
        return McnPrimitives.getScreenWidth();
    }

    public static int getScreenH() {
        return McnPrimitives.getScreenHeight();
    }

    public static boolean isIphone() {
        // TODO: consolidate into McnPrimitives
        return ClientPhoneCache.phoneStyle() == PhoneStyle.IOS;
    }

    public static void renderHeader(GuiGraphics graphics, Screen screen, String title, boolean showBack) {
        int x = getScreenX(screen);
        int y = getScreenY(screen) + 36;
            // Always hide back button at top-left per user request (bottom nav handles navigation)
            McnPrimitives.drawHeader(graphics, ClientPhoneCache.phoneStyle(), Minecraft.getInstance().font,
                    x, y, getScreenW(), title, false);
        }

    public static int phoneChargePercent() {
        // TODO: consolidate into McnPrimitives
        int maximum = Math.max(1, MobileCellNetworkConfig.PHONE_MAX_CHARGE.get());
        return Math.clamp((int) Math.round(phoneCharge() * 100.0D / maximum), 0, 100);
    }

    public static void renderPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        if (style == PhoneStyle.SAMSUNG_ONE_UI) {
            Screen screen = Minecraft.getInstance().screen;
            if (screen != null) {
                height = Math.min(height, Math.max(0, getScreenY(screen) + getScreenH() - 24 - y));
            }
        }
        McnPrimitives.drawCard(graphics, style, x, y, width, height);
    }

    public static void renderCard(GuiGraphics graphics, int x, int y, int width, int height) {
        McnPrimitives.drawCard(graphics, ClientPhoneCache.phoneStyle(), x, y, width, height);
    }
}

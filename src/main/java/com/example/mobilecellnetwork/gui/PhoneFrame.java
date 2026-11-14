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
    private static final int BODY_WIDTH = McnTheme.PHONE_WIDTH;
    private static final int BODY_HEIGHT = McnTheme.PHONE_HEIGHT;

    private PhoneFrame() {
    }

    public static void render(GuiGraphics graphics, Screen screen) {
        PhoneColors.applyTheme();
        double scale = scale(screen);
        double centerX = screen.width / 2.0D;
        double centerY = screen.height / 2.0D;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0D);
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0D);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        McnPrimitives.drawPhoneFrame(graphics, style, screen.width, screen.height);
    }

    public static String displayNetworkName() {
        // TODO: consolidate into McnPrimitives
        return ClientSignalCache.networkName();
    }

    public static void renderStatusBar(GuiGraphics graphics, Screen screen) {
        int x = getScreenX(screen);
        int y = getScreenY(screen);
        var font = Minecraft.getInstance().font;
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        boolean hasService = !ClientSignalCache.networkName().isBlank()
                && ClientSignalCache.tier() != SignalTier.NO_SERVICE;
        String network = hasService ? displayNetworkName() : "No Service";
        McnPrimitives.drawStatusBar(graphics, style, font, x, y, getScreenW(), network,
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                hasService ? ClientSignalCache.bars() : 0, phoneChargePercent(),
                ClientSignalCache.roaming(), hasService);
        if (com.example.mobilecellnetwork.network.ClientConnectionCache.connecting()) {
            McnPrimitives.drawChip(graphics, style, font,
                    x + getScreenW() - McnTheme.PHONE_NOTCH_WIDTH - McnTheme.SPACE_M,
                    y + McnTheme.STATUS_BAR_HEIGHT,
                    McnTheme.PHONE_NOTCH_WIDTH + McnTheme.SPACE_M, McnTheme.BADGE_HEIGHT,
                    "Connecting...", PhoneColors.bgTertiary(style), PhoneColors.warn(style));
        }
        // TODO: consolidate phone navigation controls into McnPrimitives
        PhoneNavigation.render(graphics, screen);
        graphics.pose().popPose();
    }

    // TODO: consolidate into McnPrimitives
    private static double scale(Screen screen) {
        return Math.clamp(Math.min((screen.width - 16.0D) / BODY_WIDTH,
                (screen.height - 16.0D) / BODY_HEIGHT), 0.1D, 1.0D);
    }

    public static double layoutMouseX(Screen screen, double mouseX) {
        // TODO: consolidate into McnPrimitives
        double center = screen.width / 2.0D;
        return center + (mouseX - center) / scale(screen);
    }

    public static double layoutMouseY(Screen screen, double mouseY) {
        // TODO: consolidate into McnPrimitives
        double center = screen.height / 2.0D;
        return center + (mouseY - center) / scale(screen);
    }

    public static double layoutMouseDeltaX(Screen screen, double deltaX) {
        // TODO: consolidate into McnPrimitives
        return deltaX / scale(screen);
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
        if (mainHand.is(ModItems.CELLPHONE.get())) {
            return mainHand.getOrDefault(ModDataComponents.PHONE_CHARGE.get(), 0);
        }
        if (offHand.is(ModItems.CELLPHONE.get())) {
            return offHand.getOrDefault(ModDataComponents.PHONE_CHARGE.get(), 0);
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.CELLPHONE.get())) {
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
        McnPrimitives.drawCard(graphics, ClientPhoneCache.phoneStyle(), x, y, width, height);
    }

    public static void renderCard(GuiGraphics graphics, int x, int y, int width, int height) {
        McnPrimitives.drawCard(graphics, ClientPhoneCache.phoneStyle(), x, y, width, height);
    }
}

package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPlanCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.packet.CancelPlanPacket;
import com.example.mobilecellnetwork.network.packet.SelectPlanPacket;
import com.example.mobilecellnetwork.network.PlanSyncPacket;
import com.example.mobilecellnetwork.network.RequestPhoneDataPacket;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.plans.PlanSubscriptionHandler;
import com.example.mobilecellnetwork.gui.theme.McnPrimitives;
import com.example.mobilecellnetwork.gui.theme.McnTheme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlanScreen extends Screen implements PhoneNavigableScreen {
    private enum ChooserStep { NETWORK, PLAN }

    private static final int NETWORK_ROW = 46;
    private static final int NETWORK_VISIBLE = 5;
    private static final int PLAN_ROW = 80;
    private static final int PLAN_VISIBLE = 3;
    private static final long SELECT_TIMEOUT_MS = 6_000L;

    private final PhoneMenu menu;
    private boolean choosing;
    private boolean chooserInitialized;
    private ChooserStep chooserStep = ChooserStep.NETWORK;
    private String selectedNetworkId = "";
    private int scroll;
    private boolean confirmingCancel;
    private String awaitingPlanId;
    private long awaitingSince;
    private String selectionMessage = "";

    public PlanScreen(PhoneMenu menu) {
        super(Component.literal("Plan"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        if (confirmingCancel) {
            confirmingCancel = false;
        } else if (chooserMode() && chooserStep == ChooserStep.PLAN) {
            chooserStep = ChooserStep.NETWORK;
            scroll = 0;
        } else if (choosing && hasActivePlan()) {
            choosing = false;
        } else {
            PhoneNavigableScreen.super.navigateBack();
        }
    }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(new RequestPhoneDataPacket());
    }

    private static boolean hasActivePlan() {
        return !ClientPlanCache.snapshot().activePlanId().isBlank();
    }

    private boolean chooserMode() {
        return choosing || !hasActivePlan();
    }

    public void selectionResult(String message) {
        if (awaitingPlanId != null && !"Plan activated.".equals(message)) {
            awaitingPlanId = null;
            selectionMessage = message;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PlanSyncPacket state = ClientPlanCache.snapshot();
        if (awaitingPlanId != null) {
            if (awaitingPlanId.equals(state.activePlanId())) {
                awaitingPlanId = null;
                choosing = false;
                chooserStep = ChooserStep.NETWORK;
                scroll = 0;
            } else if (System.currentTimeMillis() - awaitingSince > SELECT_TIMEOUT_MS) {
                awaitingPlanId = null;
                selectionMessage = "Not activated. Check notifications.";
            }
        }
        if (!hasActivePlan() && !chooserInitialized) {
            chooserInitialized = true;
        }
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        if (chooserMode()) {
            PhoneFrame.renderHeader(graphics, this,
                    chooserStep == ChooserStep.NETWORK ? "Choose network" : "Choose plan", true);
            if (chooserStep == ChooserStep.NETWORK) {
                renderNetworks(graphics, style, state, left, top);
            } else {
                renderPlans(graphics, style, state, left, top);
            }
        } else {
            PhoneFrame.renderHeader(graphics, this, "Plan", true);
            renderUsage(graphics, style, state, left, top);
        }
        if (confirmingCancel) {
            renderCancelModal(graphics, style, left, top);
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private static String networkName(PlanSyncPacket state, String networkId) {
        return state.networks().stream().filter(network -> network.id().equals(networkId))
                .map(PlanSyncPacket.NetworkEntry::name).findFirst().orElse(networkId);
    }

    private void renderNetworks(GuiGraphics graphics, PhoneStyle style, PlanSyncPacket state, int left, int top) {
        List<PlanSyncPacket.NetworkEntry> networks = state.networks();
        drawText(graphics, style, "SELECT YOUR CARRIER", left + 14, top + 48, 174,
                PhoneColors.textSecondary(style));
        if (networks.isEmpty()) {
            PhoneRenderer.drawCard(graphics, style, left + 8, top + 70, 188, 72);
            drawText(graphics, style, "No networks available", left + 18, top + 84, 168,
                    PhoneColors.textPrimary(style));
            drawText(graphics, style, "Create a network at a server.", left + 18, top + 103, 168,
                    PhoneColors.textSecondary(style));
            return;
        }
        int first = clampScroll(networks.size(), NETWORK_VISIBLE);
        for (int index = first; index < Math.min(networks.size(), first + NETWORK_VISIBLE); index++) {
            PlanSyncPacket.NetworkEntry network = networks.get(index);
            int y = top + 62 + (index - first) * NETWORK_ROW;
            PhoneRenderer.drawCard(graphics, style, left + 8, y, 188, 40);
            McnPrimitives.fillRoundedRect(graphics, left + 14, y + 8, 24, 24, 8, PhoneColors.accent(style));
            drawText(graphics, style, "NET", left + 16, y + 15, 20, PhoneColors.buttonTextOnAccent(style));
            drawText(graphics, style, network.name(), left + 44, y + 7, 132, PhoneColors.textPrimary(style));
            drawText(graphics, style, network.antennaCount() + " towers  |  "
                    + (network.allowRoaming() ? "Roaming" : "Local"), left + 44, y + 23, 132,
                    PhoneColors.textSecondary(style));
            drawText(graphics, style, ">", left + 181, y + 15, 8, PhoneColors.textSecondary(style));
        }
        renderPager(graphics, style, left, top, first, networks.size(), NETWORK_VISIBLE);
    }

    private List<PlanSyncPacket.PlanEntry> plansOnSelectedNetwork(PlanSyncPacket state) {
        return state.plans().stream().filter(plan -> plan.carrierNetworkId().equals(selectedNetworkId)).toList();
    }

    private void renderPlans(GuiGraphics graphics, PhoneStyle style, PlanSyncPacket state, int left, int top) {
        List<PlanSyncPacket.PlanEntry> plans = plansOnSelectedNetwork(state);
        drawText(graphics, style, "Carrier: " + networkName(state, selectedNetworkId),
                left + 14, top + 48, 174, PhoneColors.textSecondary(style));
        if (plans.isEmpty()) {
            PhoneRenderer.drawCard(graphics, style, left + 8, top + 70, 188, 58);
            drawText(graphics, style, "No plans on this network", left + 18, top + 86, 168,
                    PhoneColors.textPrimary(style));
            drawText(graphics, style, "Go back to choose another.", left + 18, top + 103, 168,
                    PhoneColors.textSecondary(style));
            return;
        }
        int first = clampScroll(plans.size(), PLAN_VISIBLE);
        for (int index = first; index < Math.min(plans.size(), first + PLAN_VISIBLE); index++) {
            PlanSyncPacket.PlanEntry plan = plans.get(index);
            int y = top + 62 + (index - first) * PLAN_ROW;
            PhoneRenderer.drawCard(graphics, style, left + 8, y, 188, 74);
            drawText(graphics, style, plan.displayName(), left + 14, y + 5, 174, PhoneColors.textPrimary(style));
            drawText(graphics, style, PlanSubscriptionHandler.formatCurrency(plan.price()) + " / "
                    + plan.billingCycleDays() + " days", left + 14, y + 18, 174, PhoneColors.success(style));
            drawText(graphics, style, allowance(plan.callMinutes(), " min") + " | "
                    + allowance(plan.textMessages(), " texts"), left + 14, y + 31, 174,
                    PhoneColors.textSecondary(style));
            drawText(graphics, style, allowance(plan.dataAllowanceMb(), " MB") + " | "
                    + SignalTier.displayName(plan.maxSignalTier()), left + 14, y + 44, 174,
                    PhoneColors.textSecondary(style));
            boolean active = plan.id().equals(state.activePlanId());
            boolean waiting = awaitingPlanId != null;
            String label = active ? "Active" : plan.id().equals(awaitingPlanId) ? "Wait..." : "Select";
            if (active || waiting) {
                PhoneRenderer.drawSecondaryButton(graphics, style, left + 138, y + 54, 52, 17, label, font);
            } else {
                PhoneRenderer.drawPrimaryButton(graphics, style, left + 138, y + 54, 52, 17, label, font);
            }
        }
        renderPager(graphics, style, left, top, first, plans.size(), PLAN_VISIBLE);
        if (!selectionMessage.isEmpty()) {
            drawText(graphics, style, selectionMessage, left + 14, top + 299, 174, PhoneColors.error(style));
        }
    }

    private void drawText(GuiGraphics graphics, PhoneStyle style, String text, int x, int y, int width, int color) {
        if (style == PhoneStyle.IOS) {
            SnappleText.draw(graphics, font, SnappleAppLayout.fit(font, text, width), x, y, color);
        } else {
            String fitted = font.plainSubstrByWidth(text, width);
            graphics.drawString(font, fitted, x, y, color, true);
        }
    }

    private void renderPager(GuiGraphics graphics, PhoneStyle style, int left, int top,
                             int first, int size, int visible) {
        if (size <= visible) return;
        if (first > 0) PhoneRenderer.drawSecondaryButton(graphics, style, left + 12, top + 313, 62, 20, "Back", font);
        if (first + visible < size) PhoneRenderer.drawSecondaryButton(graphics, style, left + 130, top + 313, 62, 20, "Next", font);
        drawText(graphics, style, (first + 1) + "-" + Math.min(size, first + visible) + "/" + size,
                left + 80, top + 319, 46, PhoneColors.textSecondary(style));
    }

    private int clampScroll(int size, int visible) {
        scroll = Math.clamp(scroll, 0, Math.max(0, size - visible));
        return scroll;
    }

    private static String allowance(long amount, String unit) {
        return amount < 0 ? "Unlimited" : amount + unit;
    }

    private void renderUsage(GuiGraphics graphics, PhoneStyle style, PlanSyncPacket state, int left, int top) {
        PlanSyncPacket.PlanEntry plan = state.plans().stream()
                .filter(entry -> entry.id().equals(state.activePlanId())).findFirst().orElse(null);

        PhoneRenderer.drawCard(graphics, style, left + 8, top + 62, 188, 56);
        drawText(graphics, style, "CURRENT PLAN", left + 14, top + 68, 174,
                PhoneColors.textSecondary(style));
        String planName = plan == null || plan.displayName().isBlank() ? state.activePlanId() : plan.displayName();
        drawText(graphics, style, planName, left + 14, top + 81, 174, PhoneColors.textPrimary(style));
        if (plan != null) {
                    String carrier;
                    if (plan.carrierNetworkId().isBlank()) {
                        carrier = "Any carrier";
                    } else if (!plan.carrierNetworkName().isBlank()) {
                        carrier = plan.carrierNetworkName();
                    } else {
                        // Look up carrier name from network list using carrierNetworkId
                        String carrierId = plan.carrierNetworkId();
                        String carrierName = state.networks().stream()
                                .filter(network -> network.id().equals(carrierId))
                                .map(PlanSyncPacket.NetworkEntry::name)
                                .findFirst().orElse(null);
                        carrier = carrierName != null ? carrierName : "Unknown carrier";
                    }
                    drawText(graphics, style, PlanSubscriptionHandler.formatCurrency(plan.price()) + " / "
                                    + plan.billingCycleDays() + " days - " + carrier,
                            left + 14, top + 99, 174, PhoneColors.textSecondary(style));

            PhoneRenderer.drawCard(graphics, style, left + 8, top + 124, 188, 120);
            drawText(graphics, style, "Usage this cycle", left + 14, top + 130, 174,
                    PhoneColors.textPrimary(style));
            drawUsage(graphics, style, left + 14, top + 146, "Minutes", state.minutesUsed(), plan.callMinutes());
            drawUsage(graphics, style, left + 14, top + 176, "Texts", state.textsUsed(), plan.textMessages());
            drawUsage(graphics, style, left + 14, top + 206, "Data",
                    state.dataUsedMb(), plan.dataAllowanceMb(), " MB");

            PhoneRenderer.drawCard(graphics, style, left + 8, top + 250, 188, 46);
            String renews = "Cycle day " + state.cycleStartDay() + " - renews day "
                    + (state.cycleStartDay() + plan.billingCycleDays());
            drawText(graphics, style, renews, left + 14, top + 256, 174, PhoneColors.textSecondary(style));
            String roaming = plan.allowRoaming() ? "Roaming " + plan.roamingMode().toLowerCase(java.util.Locale.ROOT)
                    : "Roaming unavailable";
            drawText(graphics, style, "Signal cap " + SignalTier.displayName(plan.maxSignalTier())
                            + " - " + roaming, left + 14, top + 274, 174, PhoneColors.textSecondary(style));
        }
        int[] change = planActionBounds(true);
        int[] cancel = planActionBounds(false);
        PhoneRenderer.drawPrimaryButton(graphics, style, change[0], change[1], change[2], change[3],
                "Change plan", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, cancel[0], cancel[1], cancel[2], cancel[3],
                "Cancel", font);
    }

    private int[] planActionBounds(boolean changeButton) {
        int screenX = McnPrimitives.getScreenX(width);
        int screenY = McnPrimitives.getScreenY(height);
        int screenWidth = McnPrimitives.getScreenWidth();
        int screenHeight = McnPrimitives.getScreenHeight();
        int contentLeft = McnPrimitives.getContentLeft(screenX);
        int contentRight = McnPrimitives.getContentRight(screenX, screenWidth);
        int buttonWidth = (contentRight - contentLeft - McnTheme.SPACE_S) / McnTheme.SPACE_XXS;
        int buttonY = McnPrimitives.getContentBottom(screenY, screenHeight)
                - McnTheme.BUTTON_HEIGHT_LG - McnTheme.SPACE_S;
        int buttonX = changeButton ? contentLeft : contentRight - buttonWidth;
        return new int[] {buttonX, buttonY, buttonWidth, McnTheme.BUTTON_HEIGHT_LG};
    }

    private void renderCancelModal(GuiGraphics graphics, PhoneStyle style, int left, int top) {
        PhoneRenderer.fillRoundedRect(graphics, style, left, top, PhoneFrame.getScreenW(),
                PhoneFrame.getScreenH() - 24, 4, 0xB0000000);
        PhoneRenderer.drawCard(graphics, style, left + 14, top + 100, 176, 122);
        drawText(graphics, style, "Cancel your plan?", left + 22, top + 108, 160, PhoneColors.textPrimary(style));
        drawText(graphics, style, "Network services will stop", left + 22, top + 128, 160,
                PhoneColors.textSecondary(style));
        drawText(graphics, style, "until you choose a new plan.", left + 22, top + 141, 160,
                PhoneColors.textSecondary(style));
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 20, top + 194, 78, 20, "Keep plan", font);
        PhoneRenderer.drawPrimaryButton(graphics, style, left + 106, top + 194, 78, 20, "Cancel", font);
    }

    private void drawUsage(GuiGraphics graphics, PhoneStyle style, int x, int y, String label,
                          long used, long maximum) {
        drawUsage(graphics, style, x, y, label, used, maximum, "");
    }

    private void drawUsage(GuiGraphics graphics, PhoneStyle style, int x, int y, String label,
                          long used, long maximum, String unit) {
        String maxText = maximum < 0 ? "Unlimited" : Long.toString(maximum) + unit;
        String value = Math.max(0L, used) + unit + " / " + maxText;
        drawText(graphics, style, label, x, y, 65, PhoneColors.textPrimary(style));
        drawText(graphics, style, value, x + 70, y, 104, PhoneColors.textSecondary(style));
        int barY = y + 12;
        PhoneRenderer.fillRoundedRect(graphics, style, x, barY, 174, 7, 4, PhoneColors.bgTertiary(style));
        double ratio = maximum < 0 ? 0.0D
                : Math.clamp((double) Math.max(0L, used) / Math.max(1L, maximum), 0.0D, 1.0D);
        if (ratio > 0.0D) {
            PhoneRenderer.fillRoundedRect(graphics, style, x, barY,
                    Math.max(4, (int) (174 * ratio)), 7, 4, PhoneColors.accent(style));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        if (button == 0 && confirmingCancel) {
            if (inside(x, y, left + 20, top + 194, 78, 20)) {
                confirmingCancel = false;
            } else if (inside(x, y, left + 106, top + 194, 78, 20)) {
                confirmingCancel = false;
                PacketDistributor.sendToServer(new CancelPlanPacket());
            }
            return true;
        }
        if (button == 0) {
            PlanSyncPacket state = ClientPlanCache.snapshot();
            if (chooserMode()) {
                int size = chooserStep == ChooserStep.NETWORK ? state.networks().size()
                        : plansOnSelectedNetwork(state).size();
                int visible = chooserStep == ChooserStep.NETWORK ? NETWORK_VISIBLE : PLAN_VISIBLE;
                int first = clampScroll(size, visible);
                if (size > visible && inside(x, y, left + 12, top + 313, 62, 20) && first > 0) {
                    scroll = Math.max(0, first - visible);
                    return true;
                }
                if (size > visible && inside(x, y, left + 130, top + 313, 62, 20) && first + visible < size) {
                    scroll = Math.min(size - visible, first + visible);
                    return true;
                }
            }
            if (!chooserMode()) {
                int[] change = planActionBounds(true);
                int[] cancel = planActionBounds(false);
                if (inside(x, y, change[0], change[1], change[2], change[3])) {
                    PlanSyncPacket.PlanEntry current = state.plans().stream()
                            .filter(entry -> entry.id().equals(state.activePlanId())).findFirst().orElse(null);
                    choosing = true;
                    chooserStep = ChooserStep.NETWORK;
                    selectedNetworkId = current == null ? "" : current.carrierNetworkId();
                    scroll = 0;
                    return true;
                }
                if (inside(x, y, cancel[0], cancel[1], cancel[2], cancel[3])) {
                    confirmingCancel = true;
                    return true;
                }
            } else if (chooserStep == ChooserStep.NETWORK) {
                List<PlanSyncPacket.NetworkEntry> networks = state.networks();
                int first = clampScroll(networks.size(), NETWORK_VISIBLE);
                for (int index = first; index < Math.min(networks.size(), first + NETWORK_VISIBLE); index++) {
                    if (inside(x, y, left + 8, top + 62 + (index - first) * NETWORK_ROW, 188, 40)) {
                        selectedNetworkId = networks.get(index).id();
                        chooserStep = ChooserStep.PLAN;
                        scroll = 0;
                        selectionMessage = "";
                        return true;
                    }
                }
            } else {
                List<PlanSyncPacket.PlanEntry> plans = plansOnSelectedNetwork(state);
                int first = clampScroll(plans.size(), PLAN_VISIBLE);
                for (int index = first; index < Math.min(plans.size(), first + PLAN_VISIBLE); index++) {
                    PlanSyncPacket.PlanEntry plan = plans.get(index);
                    if (inside(x, y, left + 138, top + 62 + (index - first) * PLAN_ROW + 54, 52, 17)) {
                        if (awaitingPlanId == null && !plan.id().equals(state.activePlanId())) {
                            awaitingPlanId = plan.id();
                            awaitingSince = System.currentTimeMillis();
                            selectionMessage = "";
                            PacketDistributor.sendToServer(new SelectPlanPacket(plan.id()));
                        }
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(x, y, button);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
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
        if (chooserMode() && !confirmingCancel) {
            scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
            return true;
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

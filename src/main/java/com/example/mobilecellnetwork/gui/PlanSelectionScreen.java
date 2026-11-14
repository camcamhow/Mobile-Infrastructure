package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPlanCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.PlanSelectPacket;
import com.example.mobilecellnetwork.network.PlanSyncPacket;
import com.example.mobilecellnetwork.network.RequestPhoneDataPacket;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.util.CurrencyUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlanSelectionScreen extends Screen implements PhoneNavigableScreen {
    private static final int VISIBLE_PLANS = 4;
    private final PhoneMenu menu;
    private int scroll;

    public PlanSelectionScreen(PhoneMenu menu) {
        super(Component.literal("Select a plan"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() {
        return menu;
    }

    @Override
    public void navigateBack() {
        if (minecraft != null) {
            minecraft.setScreen(new PlanScreen(menu));
        }
    }

    @Override
    protected void init() {
        PacketDistributor.sendToServer(new RequestPhoneDataPacket());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int left = PhoneFrame.getScreenX(this);
        int top = PhoneFrame.getContentTop(this);
        PhoneFrame.renderPanel(graphics, left, top, PhoneFrame.getScreenW(), PhoneFrame.getScreenH() - 24);
        PhoneFrame.renderHeader(graphics, this, "Choose a plan", true);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        List<PlanSyncPacket.PlanEntry> plans = ClientPlanCache.snapshot().plans();
        boolean hasSignal = ClientSignalCache.tier() != SignalTier.NO_SERVICE;
        if (!hasSignal) {
            graphics.drawString(font, "Purchases require network service", left + 10, top + 22,
                    PhoneColors.error(style), true);
        }

        int first = Math.min(scroll, Math.max(0, plans.size() - VISIBLE_PLANS));
        for (int index = first; index < Math.min(plans.size(), first + VISIBLE_PLANS); index++) {
            PlanSyncPacket.PlanEntry plan = plans.get(index);
            int row = index - first;
            int y = top + 62 + row * 58;
            PhoneRenderer.drawCard(graphics, style, left + 8, y, 188, 52);
            String price = CurrencyUtil.format(plan.price()) + " / " + plan.billingCycleDays() + "d";
            graphics.drawString(font, font.plainSubstrByWidth(plan.displayName(), 118),
                    left + 14, y + 6, PhoneColors.textPrimary(style), true);
            graphics.drawString(font, price, left + 190 - font.width(price), y + 6,
                    PhoneColors.success(style), true);
            String carrier;
                        if (plan.carrierNetworkId().isBlank()) {
                            carrier = "Any carrier";
                        } else if (!plan.carrierNetworkName().isBlank()) {
                            carrier = plan.carrierNetworkName();
                        } else {
                            // Look up carrier name from network list using carrierNetworkId
                            String carrierId = plan.carrierNetworkId();
                            var state = ClientPlanCache.snapshot();
                            String carrierName = state.networks().stream()
                                    .filter(network -> network.id().equals(carrierId))
                                    .map(PlanSyncPacket.NetworkEntry::name)
                                    .findFirst().orElse(null);
                            carrier = carrierName != null ? carrierName : "Unknown carrier";
                        }
                        String coverage = allowance(plan.dataAllowanceMb(), " MB") + " data · "
                    + allowance(plan.callMinutes(), " min") + " · "
                    + allowance(plan.textMessages(), " texts");
            graphics.drawString(font, font.plainSubstrByWidth(carrier + " · " + coverage, 174),
                    left + 14, y + 21, PhoneColors.textSecondary(style), true);
            String features = plan.dataSpeed() + " · "
                    + com.example.mobilecellnetwork.network.SignalTier.displayName(plan.maxSignalTier())
                    + (plan.allowRoaming() ? " · Roaming" : "");
            graphics.drawString(font, font.plainSubstrByWidth(features, 112),
                    left + 14, y + 36, PhoneColors.textSecondary(style), true);
            boolean active = plan.id().equals(ClientPlanCache.snapshot().activePlanId());
            String action = active ? "Active" : "Select";
            if (active) {
                PhoneRenderer.drawSecondaryButton(graphics, style,
                        left + 138, y + 31, 52, 17, action, font);
            } else {
                PhoneRenderer.drawPrimaryButton(graphics, style,
                        left + 138, y + 31, 52, 17, action, font);
            }
        }
        if (plans.isEmpty()) {
            PhoneRenderer.drawCard(graphics, style, left + 8, top + 70, 188, 58);
            graphics.drawCenteredString(font, "No plans are available",
                    left + 102, top + 91, PhoneColors.textSecondary(style));
        }

        boolean canPrevious = first > 0;
        boolean canNext = first + VISIBLE_PLANS < plans.size();
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 16, top + 300,
                72, 22, "Previous", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, left + 116, top + 300,
                72, 22, "Next", font);
        if (!canPrevious) {
            PhoneRenderer.fillRoundedRect(graphics, style, left + 16, top + 300,
                    72, 22, 10, PhoneColors.bgTertiary(style));
            graphics.drawCenteredString(font, "Previous", left + 52, top + 307,
                    PhoneColors.textSecondary(style));
        }
        if (!canNext) {
            PhoneRenderer.fillRoundedRect(graphics, style, left + 116, top + 300,
                    72, 22, 10, PhoneColors.bgTertiary(style));
            graphics.drawCenteredString(font, "Next", left + 152, top + 307,
                    PhoneColors.textSecondary(style));
        }

        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneFrame.renderStatusBar(graphics, this);
    }

    private static String allowance(long amount, String unit) {
        return amount < 0 ? "Unlimited" : amount + unit;
    }

    private static String allowance(int amount, String unit) {
        return amount < 0 ? "Unlimited" : amount + unit;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = PhoneFrame.layoutMouseX(this, mouseX);
        double y = PhoneFrame.layoutMouseY(this, mouseY);
        if (button == 0) {
            int left = PhoneFrame.getScreenX(this);
            int top = PhoneFrame.getContentTop(this);
            List<PlanSyncPacket.PlanEntry> plans = ClientPlanCache.snapshot().plans();
            int first = Math.min(scroll, Math.max(0, plans.size() - VISIBLE_PLANS));
            for (int index = first; index < Math.min(plans.size(), first + VISIBLE_PLANS); index++) {
                int rowY = top + 62 + (index - first) * 58;
                if (x >= left + 138 && x < left + 190 && y >= rowY + 31 && y < rowY + 48) {
                    PlanSyncPacket.PlanEntry plan = plans.get(index);
                    if (!plan.id().equals(ClientPlanCache.snapshot().activePlanId())
                            && ClientSignalCache.tier() != SignalTier.NO_SERVICE) {
                        PacketDistributor.sendToServer(new PlanSelectPacket(plan.id()));
                    }
                    return true;
                }
            }
            if (x >= left + 16 && x < left + 88 && y >= top + 300 && y < top + 322) {
                scroll = Math.max(0, scroll - VISIBLE_PLANS);
                return true;
            }
            if (x >= left + 116 && x < left + 188 && y >= top + 300 && y < top + 322) {
                scroll = Math.min(Math.max(0, plans.size() - VISIBLE_PLANS),
                        scroll + VISIBLE_PLANS);
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
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
        if (scrollY != 0) {
            scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1), 0,
                    Math.max(0, ClientPlanCache.snapshot().plans().size() - VISIBLE_PLANS));
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

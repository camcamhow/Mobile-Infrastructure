package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.blockentity.AntennaPowerState;
import com.example.mobilecellnetwork.blockentity.AntennaTier;
import com.example.mobilecellnetwork.network.AntennaConfigActionPacket;
import com.example.mobilecellnetwork.network.AntennaConfigSyncPacket;
import com.example.mobilecellnetwork.network.ClientAntennaConfigCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.registry.ModBlocks;
import com.example.mobilecellnetwork.gui.admin.AdminColors;
import com.example.mobilecellnetwork.gui.admin.AdminPrimitives;
import com.example.mobilecellnetwork.gui.admin.AdminButton;
import com.example.mobilecellnetwork.gui.admin.AdminTheme;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class AntennaScreen extends AbstractContainerScreen<AntennaMenu> {
    private Button networkButton;
    private Button peerNetworkButton;
    private Button saveButton;
    private Button withdrawButton;
    private EditBox feeInput;
    private String selectedNetworkId = "";
    private String peerNetworkId = "";
    private boolean editable;
    private boolean agreementMatches;
    private boolean configReceived;
    private long cacheRevision = -1L;
    private final boolean testTower;

    public AntennaScreen(AntennaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        testTower = inventory.player.level().getBlockState(menu.getBlockPos()).is(ModBlocks.TEST_ANTENNA.get());
        imageWidth = 300;
        imageHeight = 230;
    }

    @Override
    protected void init() {
        super.init();
        cacheRevision = ClientAntennaConfigCache.revision();
        configReceived = false;
        editable = false;
        addRenderableWidget(new AdminButton(leftPos + 232, topPos + 7, 56, 18, "Close", button -> onClose()));
        if (testTower) {
            networkButton = addRenderableWidget(new AntennaHitbox(leftPos + 14, topPos + 101,
                    imageWidth - 28, 22, "Network: Loading...", this::cycleNetwork));
            saveButton = addRenderableWidget(new AntennaHitbox(leftPos + 14, topPos + 128,
                    imageWidth - 28, 22, "Assign network", () -> send("save")));
            networkButton.active = false;
            saveButton.active = false;
            PacketDistributor.sendToServer(new AntennaConfigActionPacket(
                    "request", menu.getBlockPos(), "", "", 0L));
            refreshConfig();
            return;
        }
        networkButton = addRenderableWidget(new AntennaHitbox(leftPos + 14, topPos + 99,
                imageWidth - 28, 20, "Network: Loading...", this::cycleNetwork));
        peerNetworkButton = addRenderableWidget(new AntennaHitbox(leftPos + 14, topPos + 123,
                imageWidth - 28, 20, "Partner: None", this::cyclePeerNetwork));
        feeInput = addRenderableWidget(new EditBox(font, leftPos + 18, topPos + 156,
                82, 18, Component.literal("Roaming fee")));
        feeInput.setMaxLength(18);
        feeInput.setFilter(value -> value.matches("\\d*"));
        feeInput.setValue("5");
        feeInput.setBordered(false);
        feeInput.setTextColor(AdminColors.TEXT_PRIMARY);
        feeInput.setTextColorUneditable(AdminColors.TEXT_SECONDARY);
        saveButton = addRenderableWidget(new AntennaHitbox(leftPos + 14, topPos + 199,
                112, 20, "Save / Approve", () -> send("save")));
        withdrawButton = addRenderableWidget(new AntennaHitbox(leftPos + 132, topPos + 199,
                78, 20, "Withdraw", () -> send("withdraw")));
        PacketDistributor.sendToServer(new AntennaConfigActionPacket(
                "request", menu.getBlockPos(), "", "", 0L));
        refreshConfig();
    }

    private void refreshConfig() {
        AntennaConfigSyncPacket config = ClientAntennaConfigCache.snapshot();
        if (config == null || !config.position().equals(menu.getBlockPos())
                || cacheRevision == ClientAntennaConfigCache.revision()) {
            return;
        }
        cacheRevision = ClientAntennaConfigCache.revision();
        configReceived = true;
        selectedNetworkId = config.networkId();
        peerNetworkId = testTower ? "" : config.peerNetworkId();
        editable = config.editable();
        agreementMatches = config.agreementMatches();
        if (feeInput != null) {
            feeInput.setValue(Long.toString(config.feePerMinute()));
        }
        updateControls(config);
    }

    private void updateControls(AntennaConfigSyncPacket config) {
        if (networkButton != null) {
            networkButton.setMessage(Component.literal((testTower ? "Choose network: " : "Network: ") + optionName(
                    config.ownedNetworks(), selectedNetworkId, "None available")));
            networkButton.active = editable && !config.ownedNetworks().isEmpty();
        }
        if (peerNetworkButton != null) {
            peerNetworkButton.setMessage(Component.literal("Partner: " + optionName(
                    config.allNetworks(), peerNetworkId, "None / no agreement")));
            peerNetworkButton.active = editable && !config.allNetworks().isEmpty();
        }
        if (feeInput != null) {
            feeInput.setEditable(editable);
        }
        if (saveButton != null) {
            saveButton.active = editable && !config.ownedNetworks().isEmpty();
        }
        if (withdrawButton != null) {
            withdrawButton.active = editable && !peerNetworkId.isBlank();
        }
    }

    private static String optionName(List<AntennaConfigSyncPacket.NetworkOption> options,
                                    String id, String fallback) {
        return options.stream().filter(option -> option.id().equals(id))
                .map(AntennaConfigSyncPacket.NetworkOption::name).findFirst().orElse(fallback);
    }

    private void cycleNetwork() {
        AntennaConfigSyncPacket config = ClientAntennaConfigCache.snapshot();
        if (config == null || config.ownedNetworks().isEmpty()) {
            return;
        }
        List<AntennaConfigSyncPacket.NetworkOption> options = config.ownedNetworks();
        int current = options.stream().map(AntennaConfigSyncPacket.NetworkOption::id)
                .toList().indexOf(selectedNetworkId);
        selectedNetworkId = options.get(current < 0 || current + 1 >= options.size() ? 0 : current + 1).id();
        peerNetworkId = "";
        agreementMatches = false;
        updateControls(config);
        requestConfig();
    }

    private void cyclePeerNetwork() {
        AntennaConfigSyncPacket config = ClientAntennaConfigCache.snapshot();
        if (config == null) {
            return;
        }
        List<AntennaConfigSyncPacket.NetworkOption> options = config.allNetworks().stream()
                .filter(option -> !option.id().equals(selectedNetworkId)).toList();
        if (options.isEmpty()) {
            peerNetworkId = "";
        } else {
            int current = options.stream().map(AntennaConfigSyncPacket.NetworkOption::id)
                    .toList().indexOf(peerNetworkId);
            peerNetworkId = options.get(current < 0 || current + 1 >= options.size()
                    ? 0 : current + 1).id();
        }
        agreementMatches = false;
        updateControls(config);
        requestConfig();
    }

    private void requestConfig() {
        PacketDistributor.sendToServer(new AntennaConfigActionPacket(
                "request", menu.getBlockPos(), selectedNetworkId, peerNetworkId, 0L));
    }

    private void send(String action) {
        if (!editable || selectedNetworkId.isBlank()) {
            return;
        }
        long fee;
        try {
            fee = feeInput == null || feeInput.getValue().isBlank() ? 0L : Long.parseLong(feeInput.getValue());
        } catch (NumberFormatException exception) {
            return;
        }
        PacketDistributor.sendToServer(new AntennaConfigActionPacket(
                action, menu.getBlockPos(), selectedNetworkId, testTower ? "" : peerNetworkId, fee));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        AdminPrimitives.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        AdminPrimitives.drawHeader(graphics, leftPos, topPos, imageWidth, 32);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 3, AdminColors.ACCENT);
        AdminPrimitives.drawContentArea(graphics, leftPos + 1, topPos + 32, imageWidth - 2, 194);
        AdminPrimitives.drawCard(graphics, leftPos + 8, topPos + 39, imageWidth - 16, 54);
        if (!testTower) {
            AdminPrimitives.fillRoundedRect(graphics, leftPos + 14, topPos + 152, 86, 18, 3,
                    AdminColors.BG_INPUT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int energy = menu.getDataValue(0) | menu.getDataValue(1) << 15;
        int capacity = menu.getDataValue(2) | menu.getDataValue(3) << 15;
        int draw = menu.getDataValue(4) | menu.getDataValue(5) << 15;
        int stateIndex = Math.clamp(menu.getDataValue(6), 0, AntennaPowerState.values().length - 1);
        int tierIndex = Math.clamp(menu.getDataValue(7), 0, AntennaTier.values().length - 1);
        int primary = AdminColors.TEXT_PRIMARY;
        int secondary = AdminColors.TEXT_SECONDARY;
        graphics.drawString(font, testTower ? "TEST TOWER" : "ANTENNA CONTROL", 14, 12, primary, false);
        if (testTower) {
            String power = AntennaPowerState.values()[stateIndex] == AntennaPowerState.UNPOWERED
                    ? "Offline" : "Broadcasting";
            String powerSource = AntennaPowerState.values()[stateIndex] == AntennaPowerState.FE_DISABLED
                    ? "Power bypass" : energy + " / " + capacity + " FE";
            graphics.drawString(font, fit(power + "  |  " + powerSource, imageWidth - 28), 14, 48,
                    AntennaPowerState.values()[stateIndex] == AntennaPowerState.UNPOWERED
                            ? AdminColors.ERROR : AdminColors.SUCCESS, false);
            graphics.drawString(font, "Range " + menu.getDataValue(10) + " blocks  |  Users "
                    + menu.getDataValue(8), 14, 68, secondary, false);
            graphics.drawString(font, "Assign a network for normal service", 14, 157, primary, false);
            AntennaConfigSyncPacket config = ClientAntennaConfigCache.snapshot();
            String hint = !configReceived ? "Loading network settings..."
                    : !editable ? "Only the owner or an operator can assign."
                    : config == null || config.ownedNetworks().isEmpty()
                            ? "Create a network first; use a Network Server."
                            : "Select a network above and assign it.";
            graphics.drawString(font, fit(hint, imageWidth - 28), 14, 174, secondary, false);
            graphics.drawString(font, fit("Signal test: /mcn signaltest <player> true",
                    imageWidth - 28), 14, 192, secondary, false);
            return;
        }
        graphics.drawString(font, fit(energy + " / " + capacity + " FE", 170), 14, 47, primary, false);
        graphics.drawString(font, "Draw " + draw + " FE/s", 180, 47, secondary, false);
        int fillWidth = capacity <= 0 ? 0 : (int) Math.clamp((long) energy * 272L / capacity, 0L, 272L);
        AdminPrimitives.fillRoundedRect(graphics, 14, 61, 272, 4, 2, AdminColors.BG_INPUT);
        if (fillWidth > 0) {
            AdminPrimitives.fillRoundedRect(graphics, 14, 61, fillWidth, 4, 2, AdminColors.ACCENT);
        }
        graphics.drawString(font, fit(AntennaPowerState.values()[stateIndex].name()
                        + "  |  Tier " + AntennaTier.values()[tierIndex].name()
                        + "  |  Range " + menu.getDataValue(10), 272),
                14, 70, primary, false);
        graphics.drawString(font, "Users " + menu.getDataValue(8)
                        + "  |  Chunk " + (menu.getDataValue(9) != 0 ? "loaded" : "inactive"),
                14, 83, secondary, false);
        graphics.drawString(font, "Fee / minute", 110, 156, secondary, false);
        String status = agreementMatches
                ? "Both carriers approved the same fee."
                : peerNetworkId.isBlank() ? "Select a partner to propose an agreement."
                : "Approval is pending from the other carrier.";
        graphics.drawString(font, fit(status, imageWidth - 28), 14, 176,
                agreementMatches ? AdminColors.SUCCESS : AdminColors.WARN, false);
        if (!editable) {
            graphics.drawString(font, fit("Only the antenna owner can change settings.",
                    imageWidth - 28), 14, 188, AdminColors.ERROR, false);
        }
    }

    private String fit(String text, int width) {
        return font.plainSubstrByWidth(text, width);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshConfig();
        super.render(graphics, mouseX, mouseY, partialTick);
        if (testTower) {
            drawControl(graphics, networkButton, leftPos + 14, topPos + 101,
                    imageWidth - 28, 22, networkButton.getMessage().getString());
            drawControl(graphics, saveButton, leftPos + 14, topPos + 128,
                    imageWidth - 28, 22, "Assign network");
            renderClippedTooltip(graphics, networkButton, mouseX, mouseY);
            return;
        }
        drawControl(graphics, networkButton, leftPos + 14, topPos + 99,
                imageWidth - 28, 20, networkButton.getMessage().getString());
        drawControl(graphics, peerNetworkButton, leftPos + 14, topPos + 123,
                imageWidth - 28, 20, peerNetworkButton.getMessage().getString());
        drawControl(graphics, saveButton, leftPos + 14, topPos + 199,
                112, 20, "Save / Approve");
        drawControl(graphics, withdrawButton, leftPos + 132, topPos + 199,
                78, 20, "Withdraw");
        renderClippedTooltip(graphics, networkButton, mouseX, mouseY);
        renderClippedTooltip(graphics, peerNetworkButton, mouseX, mouseY);
    }

    private void drawControl(GuiGraphics graphics, Button button, int x, int y,
                             int width, int height, String label) {
        int mouseX = (int) (this.minecraft.mouseHandler.xpos()
                * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getWidth());
        int mouseY = (int) (this.minecraft.mouseHandler.ypos()
                * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getHeight());
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        AdminPrimitives.drawButton(graphics, font, x, y, width, height, fitButtonLabel(label, width),
                AdminPrimitives.ButtonVariant.SECONDARY, hovered, !button.active);
    }

    private String fitButtonLabel(String text, int width) {
        int maxWidth = Math.max(0, (width - 14) * font.lineHeight / AdminTheme.TEXT_BODY);
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        if (font.width(suffix) > maxWidth) return font.plainSubstrByWidth(suffix, maxWidth);
        return font.plainSubstrByWidth(text, maxWidth - font.width(suffix)) + suffix;
    }

    private void renderClippedTooltip(GuiGraphics graphics, Button button, int mouseX, int mouseY) {
        String label = button.getMessage().getString();
        if (button.isMouseOver(mouseX, mouseY) && !fitButtonLabel(label, button.getWidth()).equals(label)) {
            graphics.renderTooltip(font, Component.literal(label), mouseX, mouseY);
        }
    }

    private static final class AntennaHitbox extends Button {
        private AntennaHitbox(int x, int y, int width, int height, String label, Runnable action) {
            super(x, y, width, height, Component.literal(label), button -> action.run(), DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MenuKeyHandler.isInventoryKey(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.blockentity.AntennaPowerState;
import com.example.mobilecellnetwork.blockentity.AntennaTier;
import com.example.mobilecellnetwork.network.AntennaConfigActionPacket;
import com.example.mobilecellnetwork.network.AntennaConfigSyncPacket;
import com.example.mobilecellnetwork.network.ClientAntennaConfigCache;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
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
    private long cacheRevision = -1L;

    public AntennaScreen(AntennaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 250;
        imageHeight = 218;
    }

    @Override
    protected void init() {
        super.init();
        networkButton = addRenderableWidget(new AntennaHitbox(leftPos + 8, topPos + 72,
                imageWidth - 16, 18, "Network: Loading...", this::cycleNetwork));
        peerNetworkButton = addRenderableWidget(new AntennaHitbox(leftPos + 8, topPos + 94,
                imageWidth - 16, 18, "Partner: None", this::cyclePeerNetwork));
        feeInput = addRenderableWidget(new EditBox(font, leftPos + 8, topPos + 135,
                82, 18, Component.literal("Roaming fee")));
        feeInput.setMaxLength(18);
        feeInput.setFilter(value -> value.matches("\\d*"));
        feeInput.setValue("5");
        feeInput.setBordered(false);
        feeInput.setTextColor(PhoneColors.textPrimary(ClientPhoneCache.phoneStyle()));
        feeInput.setTextColorUneditable(PhoneColors.textSecondary(ClientPhoneCache.phoneStyle()));
        saveButton = addRenderableWidget(new AntennaHitbox(leftPos + 8, topPos + 177,
                112, 20, "Save / Approve", () -> send("save")));
        withdrawButton = addRenderableWidget(new AntennaHitbox(leftPos + 126, topPos + 177,
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
        selectedNetworkId = config.networkId();
        peerNetworkId = config.peerNetworkId();
        editable = config.editable();
        agreementMatches = config.agreementMatches();
        if (feeInput != null) {
            feeInput.setValue(Long.toString(config.feePerMinute()));
        }
        updateControls(config);
    }

    private void updateControls(AntennaConfigSyncPacket config) {
        if (networkButton != null) {
            networkButton.setMessage(Component.literal("Network: " + optionName(
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
            fee = feeInput.getValue().isBlank() ? 0L : Long.parseLong(feeInput.getValue());
        } catch (NumberFormatException exception) {
            return;
        }
        PacketDistributor.sendToServer(new AntennaConfigActionPacket(
                action, menu.getBlockPos(), selectedNetworkId, peerNetworkId, fee));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        PhoneRenderer.drawCard(graphics, style, leftPos, topPos, imageWidth, imageHeight);
        PhoneRenderer.drawCard(graphics, style, leftPos + 7, topPos + 7, imageWidth - 14, 59);
        PhoneRenderer.drawCard(graphics, style, leftPos + 7, topPos + 66, imageWidth - 14, 49);
        PhoneTextField.renderBackground(graphics, style, leftPos + 8, topPos + 135, 82, 18);
        PhoneRenderer.drawCard(graphics, style, leftPos + 7, topPos + 151, imageWidth - 14, 21);
        PhoneRenderer.drawCard(graphics, style, leftPos + 7, topPos + 174, imageWidth - 14, 26);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int energy = menu.getDataValue(0) | menu.getDataValue(1) << 15;
        int capacity = menu.getDataValue(2) | menu.getDataValue(3) << 15;
        int draw = menu.getDataValue(4) | menu.getDataValue(5) << 15;
        int stateIndex = Math.clamp(menu.getDataValue(6), 0, AntennaPowerState.values().length - 1);
        int tierIndex = Math.clamp(menu.getDataValue(7), 0, AntennaTier.values().length - 1);
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        int primary = PhoneColors.textPrimary(style);
        int secondary = PhoneColors.textSecondary(style);
        graphics.drawString(font, title, 14, 12, primary, false);
        graphics.drawString(font, "ENERGY", 14, 29, secondary, false);
        graphics.drawString(font, energy + " / " + capacity + " FE", 14, 42, primary, false);
        graphics.drawString(font, "Draw " + draw + " FE/s", 146, 42, secondary, false);
        int fillWidth = capacity <= 0 ? 0 : (int) Math.clamp((long) energy * 200L / capacity, 0L, 200L);
        PhoneRenderer.fillRoundedRect(graphics, style, 14, 56, 200, 4, 2,
                PhoneColors.bgTertiary(style));
        if (fillWidth > 0) {
            PhoneRenderer.fillRoundedRect(graphics, style, 14, 56, fillWidth, 4, 2,
                    PhoneColors.accent(style));
        }
        graphics.drawString(font, AntennaPowerState.values()[stateIndex].name()
                        + "  |  Tier " + AntennaTier.values()[tierIndex].name()
                        + "  |  Range " + menu.getDataValue(10),
                14, 78, primary, false);
        graphics.drawString(font, "Users " + menu.getDataValue(8)
                        + "  |  Chunk " + (menu.getDataValue(9) != 0 ? "loaded" : "inactive"),
                14, 98, secondary, false);
        graphics.drawString(font, "ROAMING AGREEMENT", 14, 119, secondary, false);
        graphics.drawString(font, "Fee / minute", 99, 140, secondary, false);
        String status = agreementMatches
                ? "Both carriers approved the same fee."
                : peerNetworkId.isBlank() ? "Select a partner to propose an agreement."
                : "Approval is pending from the other carrier.";
        graphics.drawString(font, fit(status, imageWidth - 28), 14, 158,
                agreementMatches ? PhoneColors.success(style) : PhoneColors.warn(style), false);
        if (!editable) {
            graphics.drawString(font, fit("Only the antenna owner can change settings.",
                    imageWidth - 28), 14, 174, PhoneColors.error(style), false);
        }
    }

    private String fit(String text, int width) {
        return font.plainSubstrByWidth(text, width);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshConfig();
        super.render(graphics, mouseX, mouseY, partialTick);
        drawControl(graphics, networkButton, leftPos + 8, topPos + 72,
                imageWidth - 16, 18, networkButton.getMessage().getString());
        drawControl(graphics, peerNetworkButton, leftPos + 8, topPos + 94,
                imageWidth - 16, 18, peerNetworkButton.getMessage().getString());
        drawControl(graphics, saveButton, leftPos + 8, topPos + 177,
                112, 20, "Save / Approve");
        drawControl(graphics, withdrawButton, leftPos + 126, topPos + 177,
                78, 20, "Withdraw");
    }

    private void drawControl(GuiGraphics graphics, Button button, int x, int y,
                             int width, int height, String label) {
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        int mouseX = (int) (this.minecraft.mouseHandler.xpos()
                * this.minecraft.getWindow().getGuiScaledWidth() / this.minecraft.getWindow().getWidth());
        int mouseY = (int) (this.minecraft.mouseHandler.ypos()
                * this.minecraft.getWindow().getGuiScaledHeight() / this.minecraft.getWindow().getHeight());
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        int background = !button.active ? PhoneColors.bgTertiary(style)
                : hovered ? PhoneColors.accent(style) : PhoneColors.bgSecondary(style);
        PhoneRenderer.fillRoundedRect(graphics, style, x, y, width, height, 7, background);
        graphics.drawCenteredString(font, fit(label, width - 12), x + width / 2,
                y + (height - font.lineHeight) / 2,
                PhoneColors.textPrimary(style));
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

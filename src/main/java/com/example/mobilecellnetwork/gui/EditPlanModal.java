package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.plans.PlanRegistry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EditPlanModal extends Screen {
    private static final List<SignalTier> TIERS = List.of(
            SignalTier.E, SignalTier.E_PLUS, SignalTier.TWO_G, SignalTier.H, SignalTier.H_PLUS,
            SignalTier.THREE_G, SignalTier.LTE, SignalTier.FOUR_G, SignalTier.FOUR_G_PLUS,
            SignalTier.FIVE_G, SignalTier.FIVE_G_PLUS);
    private static final List<PlanRegistry.DataSpeed> SPEEDS = List.of(
            PlanRegistry.DataSpeed.LOW, PlanRegistry.DataSpeed.MEDIUM,
            PlanRegistry.DataSpeed.HIGH, PlanRegistry.DataSpeed.FULL);
    private static final int SECTION_STEP = 24;
    private static final int FIELD_STEP = 44;
    private final Screen parent;
    private final List<String> existingIds;
    private final List<NetworkOption> networks;
    private final Consumer<List<String>> onConfirm;
    private final java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, EditBox> inputs = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Integer> fieldOffsets = new java.util.LinkedHashMap<>();
    private final Set<String> allowedNetworks = new LinkedHashSet<>();
    private boolean creating;
    private boolean allowRoaming;
    private boolean autoRenew;
    private boolean isDefault;
    private int speedIndex;
    private int tierIndex;
    private int scroll;
    private String carrierNetworkId;
    private String errorField = "";
    private String errorMessage = "";
    private int modalX, modalY, modalW, modalH, contentTop, contentBottom, contentHeight;
    private Button okButton, cancelButton;
    private final List<SectionLine> sections = new ArrayList<>();

    public EditPlanModal(Screen parent, PlanData initial, List<String> existingIds,
                         List<NetworkOption> networks, Consumer<List<String>> onConfirm) {
        super(Component.literal(initial.id().isBlank() ? "New Plan" : "Edit Plan"));
        this.parent = parent;
        this.existingIds = List.copyOf(existingIds);
        this.networks = List.copyOf(networks);
        this.onConfirm = onConfirm;
        creating = initial.id().isBlank();
        values.put("id", initial.id());
        values.put("name", initial.name());
        values.put("description", initial.description());
        values.put("price", initial.price());
        values.put("cycle", initial.cycle());
        values.put("minutes", initial.minutes());
        values.put("texts", initial.texts());
        values.put("data", initial.data());
        values.put("feeMinute", initial.feePerMinute());
        values.put("feeMb", initial.feePerMb());
        allowRoaming = initial.allowRoaming();
        autoRenew = initial.autoRenew();
        isDefault = initial.isDefault();
        carrierNetworkId = initial.carrierNetworkId();
        speedIndex = Math.max(0, SPEEDS.indexOf(parseSpeed(initial.speed())));
        tierIndex = Math.max(0, TIERS.indexOf(parseTier(initial.tier())));
        for (String id : initial.allowedNetworks().split(",")) {
            if (!id.isBlank()) {
                allowedNetworks.add(id.trim());
            }
        }
    }

    @Override
    protected void init() {
        modalW = Math.min(300, width - 20);
        modalH = Math.min(220, height - 20);
        modalX = (width - modalW) / 2;
        modalY = (height - modalH) / 2;
        contentTop = modalY + 40;
        contentBottom = modalY + modalH - 28;
        int half = (modalW - 36) / 2;
        int leftX = modalX + 12;
        int rightX = leftX + half + 12;
        inputs.clear();
        fieldOffsets.clear();
        sections.clear();
        int y = 0;

        y = section("Identity", y);
        field("id", "Plan ID", leftX, y, half, false, !creating);
        field("name", "Display Name", rightX, y, half, false, false);
        y += FIELD_STEP;
        field("description", "Description", leftX, y, modalW - 24, false, false);
        y += FIELD_STEP;

        y = section("Pricing", y);
        field("price", "Price", leftX, y, half, false, false);
        field("cycle", "Billing cycle days", rightX, y, half, false, false);
        y += FIELD_STEP;

        y = section("Allowances (-1 = unlimited)", y);
        field("minutes", "Call minutes", leftX, y, half, true, false);
        field("texts", "Text messages", rightX, y, half, true, false);
        y += FIELD_STEP;
        field("data", "Data MB", leftX, y, modalW - 24, true, false);
        y += FIELD_STEP;

        y = section("Signal & Data", y);
        buttonRow("Data speed: " + SPEEDS.get(speedIndex).name(), leftX, y, half,
                () -> speedIndex = (speedIndex + 1) % SPEEDS.size());
        buttonRow("Max tier: " + tierDisplayName(TIERS.get(tierIndex)), rightX, y, half,
                () -> tierIndex = (tierIndex + 1) % TIERS.size());
        y += FIELD_STEP;

        y = section("Roaming", y);
        buttonRow("Allow roaming: " + yesNo(allowRoaming), leftX, y, modalW - 24,
                () -> allowRoaming = !allowRoaming);
        y += FIELD_STEP;
        field("feeMinute", "Fee per minute", leftX, y, half, false, false);
        field("feeMb", "Fee per MB", rightX, y, half, false, false);
        y += FIELD_STEP;

        y = section("Flags", y);
        buttonRow("Auto-renew: " + yesNo(autoRenew), leftX, y, half,
                () -> autoRenew = !autoRenew);
        buttonRow("Default plan: " + yesNo(isDefault), rightX, y, half,
                () -> isDefault = !isDefault);
        y += FIELD_STEP;

        y = section("Carrier and access", y);
        buttonRow("Plan carrier: " + carrierNetworkName(), leftX, y, modalW - 24, () -> {
            captureInputs();
            if (networks.isEmpty()) {
                carrierNetworkId = "";
            } else {
                int current = networks.stream().map(NetworkOption::id).toList().indexOf(carrierNetworkId);
                carrierNetworkId = current < 0 ? networks.get(0).id()
                        : current + 1 >= networks.size() ? "" : networks.get(current + 1).id();
            }
        });
        y += FIELD_STEP;
        y = section("Allowed networks (empty = all)", y);
        for (NetworkOption network : networks) {
            int rowY = contentTop + y - scroll;
            int currentY = y;
            if (visible(rowY, 18)) {
                addRenderableWidget(Button.builder(Component.literal(
                                (allowedNetworks.contains(network.id()) ? "[x] " : "[ ] ") + network.name()),
                                button -> {
                                    captureInputs();
                                    if (!allowedNetworks.add(network.id())) {
                                        allowedNetworks.remove(network.id());
                                    }
                                    recreateWidgets();
                                })
                        .bounds(leftX, rowY, modalW - 24, 18).build());
            }
            y = currentY + 20;
        }
        contentHeight = y;
        int footerY = modalY + modalH - 23;
        okButton = addRenderableWidget(Button.builder(Component.literal("OK"), button -> submit())
                .bounds(modalX + modalW - 112, footerY, 46, 18).build());
        cancelButton = addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> returnToParent())
                .bounds(modalX + modalW - 62, footerY, 54, 18).build());
    }

    private int section(String title, int y) {
        sections.add(new SectionLine(title, y, modalX + 12, modalW - 24, true));
        return y + SECTION_STEP;
    }

    private void field(String key, String label, int x, int y, int fieldWidth,
                       boolean signed, boolean readOnly) {
        int absoluteY = contentTop + y - scroll;
        fieldOffsets.put(key, y);
        if (visible(absoluteY + 11, 18)) {
            EditBox input = new EditBox(font, x, absoluteY + 11, fieldWidth, 18, Component.literal(label));
            input.setMaxLength(128);
            input.setValue(values.getOrDefault(key, ""));
            input.setHint(Component.literal(label));
            input.setEditable(!readOnly);
            if (key.equals("price") || key.equals("cycle") || key.startsWith("fee")) {
                input.setFilter(text -> text.matches("\\d*"));
            } else if (signed) {
                input.setFilter(text -> text.matches("-?\\d*"));
            }
            inputs.put(key, input);
            addRenderableWidget(input);
        }
        sections.add(new SectionLine(label, y, x, fieldWidth, false));
    }

    private void buttonRow(String label, int x, int y, int buttonWidth, Runnable action) {
        int absoluteY = contentTop + y - scroll + 1;
        if (visible(absoluteY, 18)) {
            String displayLabel = fitText(label, buttonWidth - 8);
            Button button = Button.builder(Component.literal(label), widget -> {
                        captureInputs();
                        action.run();
                        recreateWidgets();
                    }).bounds(x, absoluteY, buttonWidth, 18).build();
            button.setMessage(Component.literal(displayLabel));
            addRenderableWidget(button);
        }
    }

    private String fitText(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        return font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width(suffix))) + suffix;
    }

    private boolean visible(int y, int height) {
        return y >= contentTop && y + height <= contentBottom;
    }

    private void recreateWidgets() {
        captureInputs();
        clearWidgets();
        init();
    }

    private void captureInputs() {
        inputs.forEach((key, input) -> values.put(key, input.getValue()));
    }

    private void submit() {
        captureInputs();
        errorField = "";
        errorMessage = "";
        if (values.get("id").isBlank()) {
            invalid("id", "Plan ID is required.");
        } else if (creating && existingIds.contains(values.get("id"))) {
            invalid("id", "Plan ID already exists.");
        } else if (!values.get("id").matches("[a-z0-9_.-]+")) {
            invalid("id", "Use lowercase letters, numbers, dots, underscores, or hyphens.");
        } else if (values.get("name").isBlank()) {
            invalid("name", "Display Name is required.");
        } else if (values.get("name").contains("|") || values.get("description").contains("|")) {
            invalid(values.get("name").contains("|") ? "name" : "description",
                    "The | character is not allowed.");
        } else if (!validLong("price", 0, Long.MAX_VALUE)) {
            invalid("price", "Price must be 0 or greater.");
        } else if (!validLong("cycle", 1, Integer.MAX_VALUE)) {
            invalid("cycle", "Cycle must be at least 1 day.");
        } else if (!validLong("minutes", -1, Integer.MAX_VALUE)) {
            invalid("minutes", "Enter -1 or a non-negative number.");
        } else if (!validLong("texts", -1, Integer.MAX_VALUE)) {
            invalid("texts", "Enter -1 or a non-negative number.");
        } else if (!validLong("data", -1, Long.MAX_VALUE)) {
            invalid("data", "Enter -1 or a non-negative number.");
        } else if (!validLong("feeMinute", 0, Long.MAX_VALUE)) {
            invalid("feeMinute", "Fee must be non-negative.");
        } else if (!validLong("feeMb", 0, Long.MAX_VALUE)) {
            invalid("feeMb", "Fee must be non-negative.");
        } else {
            onConfirm.accept(List.of(values.get("id"), values.get("name").trim(), values.get("description"),
                    values.get("price"), values.get("cycle"), values.get("minutes"), values.get("texts"),
                    values.get("data"), SPEEDS.get(speedIndex).name(), TIERS.get(tierIndex).name(),
                    Boolean.toString(allowRoaming), values.get("feeMinute"), values.get("feeMb"),
                    Boolean.toString(autoRenew), Boolean.toString(isDefault), String.join(",", allowedNetworks),
                    carrierNetworkId));
            returnToParent();
            return;
        }
        scrollToError();
        recreateWidgets();
    }

    private boolean validLong(String key, long min, long max) {
        try {
            long value = Long.parseLong(values.get(key));
            return value >= min && value <= max;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void invalid(String field, String message) {
        errorField = field;
        errorMessage = message;
    }

    private void scrollToError() {
        int maxScroll = Math.max(0, contentHeight - (contentBottom - contentTop));
        scroll = Math.clamp(fieldOffsets.getOrDefault(errorField, 0) - 8, 0, maxScroll);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        captureInputs();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF1E2A38);
        graphics.fill(modalX + 2, modalY + 2, modalX + modalW - 2, modalY + modalH - 2, 0xFF0F1620);
        graphics.renderOutline(modalX, modalY, modalW, modalH, 0xFF3A4A5A);
        graphics.fill(modalX, modalY, modalX + modalW, modalY + 2, PhoneColors.ACCENT_TEXT);
        graphics.drawString(font, title, modalX + 12, modalY + 10, PhoneColors.PRIMARY_TEXT, true);
        graphics.drawString(font, "Scroll to view all fields", modalX + 12, modalY + 20,
                PhoneColors.SECONDARY_TEXT, true);
        graphics.enableScissor(modalX + 8, contentTop, modalX + modalW - 8, contentBottom);
        for (SectionLine line : sections) {
            int y = contentTop + line.y() - scroll;
            if (y >= contentTop - 10 && y <= contentBottom) {
                boolean fieldError = !line.heading() && line.title().equals(errorFieldLabel(errorField));
                int color = fieldError ? PhoneColors.ERROR_TEXT
                        : line.heading() ? PhoneColors.ACCENT_TEXT : PhoneColors.SECONDARY_TEXT;
                graphics.drawString(font, fitText(line.title(), line.width()), line.x(), y, color, true);
                if (line.heading()) {
                    graphics.fill(modalX + 12, y + 9, modalX + modalW - 12, y + 10, 0xFF354252);
                }
            }
        }
        graphics.disableScissor();
        for (var child : children()) {
            if (child instanceof Renderable renderable) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        EditBox errorInput = inputs.get(errorField);
        if (errorInput != null && !errorMessage.isEmpty()) {
            int x = errorInput.getX(), y = errorInput.getY(), w = errorInput.getWidth(), h = errorInput.getHeight();
            graphics.fill(x - 1, y - 1, x + w + 1, y, PhoneColors.ERROR_TEXT);
            graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, PhoneColors.ERROR_TEXT);
            graphics.fill(x - 1, y, x, y + h, PhoneColors.ERROR_TEXT);
            graphics.fill(x + w, y, x + w + 1, y + h, PhoneColors.ERROR_TEXT);
            graphics.drawString(font, fitText(errorMessage, modalW - 24), modalX + 12,
                    y + h + 2, PhoneColors.ERROR_TEXT, true);
        }
    }

    private String errorFieldLabel(String key) {
        return switch (key) {
            case "id" -> "Plan ID";
            case "name" -> "Display Name";
            case "description" -> "Description";
            case "price" -> "Price";
            case "cycle" -> "Billing cycle days";
            case "minutes" -> "Call minutes";
            case "texts" -> "Text messages";
            case "data" -> "Data MB";
            case "feeMinute" -> "Fee per minute";
            case "feeMb" -> "Fee per MB";
            default -> "";
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, contentHeight - (contentBottom - contentTop));
        int next = Math.clamp(scroll - (int) Math.signum(scrollY) * 24, 0, maxScroll);
        if (next != scroll) {
            scroll = next;
            recreateWidgets();
        }
        return true;
    }

    public static String tierDisplayName(SignalTier tier) {
        return tier.displayName();
    }

    private static String yesNo(boolean value) { return value ? "Yes" : "No"; }

    private String carrierNetworkName() {
        return networks.stream().filter(network -> network.id().equals(carrierNetworkId))
                .map(NetworkOption::name).findFirst().orElse("Not assigned");
    }

    private static PlanRegistry.DataSpeed parseSpeed(String value) {
        try { return PlanRegistry.DataSpeed.valueOf(value); }
        catch (IllegalArgumentException exception) { return PlanRegistry.DataSpeed.MEDIUM; }
    }

    private static SignalTier parseTier(String value) {
        try { return SignalTier.valueOf(value); }
        catch (IllegalArgumentException exception) { return SignalTier.FIVE_G_PLUS; }
    }

    private void returnToParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() { returnToParent(); }

    @Override
    public boolean isPauseScreen() { return false; }

    public record PlanData(String id, String name, String description, String price, String cycle,
                           String minutes, String texts, String data, String speed, String tier,
                           boolean allowRoaming, String feePerMinute, String feePerMb,
                           boolean autoRenew, boolean isDefault, String allowedNetworks,
                           String carrierNetworkId) {
        public static PlanData empty() {
            return new PlanData("", "", "", "0", "30", "-1", "-1", "-1",
                    "MEDIUM", "FIVE_G_PLUS", false, "5", "1", false, false, "", "");
        }
    }

    public record NetworkOption(String id, String name) { }
    private record SectionLine(String title, int y, int x, int width, boolean heading) { }
}

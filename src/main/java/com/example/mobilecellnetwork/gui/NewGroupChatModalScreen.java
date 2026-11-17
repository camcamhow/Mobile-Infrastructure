package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.ClientPhoneCache;
import com.example.mobilecellnetwork.network.CreateGroupChatPacket;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NewGroupChatModalScreen extends Screen implements PhoneNavigableScreen {
    private final PhoneMenu menu;
    private final Set<String> selected = new LinkedHashSet<>();
    private EditBox groupName;
    private int scroll;
    private String error = "";

    public NewGroupChatModalScreen(PhoneMenu menu) {
        super(Component.literal("New Group"));
        this.menu = menu;
    }

    @Override
    public PhoneMenu phoneMenu() { return menu; }

    @Override
    protected void init() {
        int x = modalX();
        int y = PhoneFrame.getScreenY(this) + 104;
        groupName = PhoneTextField.create(font, x + 8, y + 40, 168, 22,
                "Group name", 32, ClientPhoneCache.phoneStyle());
        addRenderableWidget(groupName);
    }

    private Map<String, String> members() {
        Map<String, String> result = new LinkedHashMap<>();
        ClientPhoneCache.contacts().forEach((name, number) -> result.putIfAbsent(number, name));
        ClientPhoneCache.onlinePlayers().stream().filter(player -> player.number().matches("[0-9]{1,9}"))
                .forEach(player -> result.putIfAbsent(player.number(), player.name()));
        return result;
    }

    private void create() {
        if (groupName.getValue().isBlank() || selected.size() < 2) {
            error = "Enter a name and select at least two members.";
            return;
        }
        String name = groupName.getValue().strip();
        PacketDistributor.sendToServer(new CreateGroupChatPacket(name, List.copyOf(selected)));
        minecraft.setScreen(new SmsScreen(menu, "", name));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PhoneFrame.render(graphics, this);
        int x = modalX();
        int y = PhoneFrame.getScreenY(this) + 104;
        PhoneStyle style = ClientPhoneCache.phoneStyle();
        PhoneRenderer.drawCard(graphics, style, x, y, 192, 238);
        PhoneRenderer.drawHeader(graphics, style, x, y + 8, "New Group", font, false);
        PhoneTextField.renderBackground(graphics, style, x + 8, y + 40, 168, 22);
        graphics.drawString(font, "Select at least 2 members", x + 8, y + 69,
                PhoneColors.textSecondary(style), true);
        Map<String, String> candidates = members();
        List<Map.Entry<String, String>> entries = List.copyOf(candidates.entrySet());
        int first = Math.clamp(scroll, 0, Math.max(0, entries.size() - 6));
        for (int index = first; index < Math.min(entries.size(), first + 6); index++) {
            Map.Entry<String, String> entry = entries.get(index);
            int rowY = y + 88 + (index - first) * 21;
            PhoneRenderer.drawCard(graphics, style, x + 8, rowY, 168, 19);
            boolean checked = selected.contains(entry.getKey());
            graphics.drawString(font, (checked ? "[x] " : "[ ] ")
                            + font.plainSubstrByWidth(entry.getValue() + "  " + entry.getKey(), 145),
                    x + 12, rowY + 5, PhoneColors.textPrimary(style), true);
        }
        super.render(graphics, PhoneFrame.displayMouseX(this, mouseX),
                PhoneFrame.displayMouseY(this, mouseY), partialTick);
        PhoneRenderer.drawPrimaryButton(graphics, style, x + 8, y + 218, 78, 20, "Create", font);
        PhoneRenderer.drawSecondaryButton(graphics, style, x + 98, y + 218, 78, 20, "Cancel", font);
        if (!error.isEmpty()) {
            graphics.drawString(font, error, x + 8, y + 202,
                    PhoneColors.error(style), true);
        }
        PhoneFrame.renderStatusBar(graphics, this);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double xMouse = PhoneFrame.layoutMouseX(this, mouseX);
        double yMouse = PhoneFrame.layoutMouseY(this, mouseY);
        int x = modalX();
        int y = PhoneFrame.getScreenY(this) + 104;
        if (button == 0 && xMouse >= x + 8 && xMouse < x + 176
                && yMouse >= y + 88 && yMouse < y + 214) {
            int row = (int) ((yMouse - (y + 88)) / 21) + scroll;
            List<String> numbers = List.copyOf(members().keySet());
            if (row < numbers.size()) {
                String number = numbers.get(row);
                if (!selected.add(number)) selected.remove(number);
                return true;
            }
        }
        if (button == 0 && yMouse >= y + 218 && yMouse < y + 238) {
            if (xMouse >= x + 8 && xMouse < x + 86) create();
            else if (xMouse >= x + 98 && xMouse < x + 176) cancel();
            return true;
        }
        return super.mouseClicked(xMouse, yMouse, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            scroll = Math.clamp(scroll + (scrollY < 0 ? 1 : -1),
                    0, Math.max(0, members().size() - 6));
            return true;
        }
        return super.mouseScrolled(PhoneFrame.layoutMouseX(this, mouseX),
                PhoneFrame.layoutMouseY(this, mouseY), scrollX, scrollY);
    }

    private void cancel() { minecraft.setScreen(new SmsScreen(menu)); }

    private int modalX() {
        return PhoneFrame.getScreenX(this) + (PhoneFrame.getScreenW() - 192) / 2;
    }

    @Override
    public void navigateBack() { cancel(); }

    @Override
    public void navigateHome() { PhoneNavigation.openHome(this); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }
}

package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.DismissLowPowerPromptPacket;
import com.example.mobilecellnetwork.network.SetLowPowerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class LowPowerPromptScreen extends Screen {
    private final String title;
    private final String message;

    public LowPowerPromptScreen(String title, String message) {
        super(Component.literal(title));
        this.title = title;
        this.message = message;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Enable"), button -> {
                    PacketDistributor.sendToServer(new SetLowPowerPacket(true));
                    closePrompt();
                })
                .bounds(width / 2 - 72, height / 2 + 20, 68, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Dismiss"), button -> {
                    PacketDistributor.sendToServer(new DismissLowPowerPromptPacket());
                    closePrompt();
                })
                .bounds(width / 2 + 4, height / 2 + 20, 68, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x88000000);
        graphics.fill(width / 2 - 100, height / 2 - 36, width / 2 + 100, height / 2 + 54, 0xFF151D27);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 22, 0xFFFFFFFF);
        graphics.drawCenteredString(font, message, width / 2, height / 2 - 2, 0xFFD8E2EF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void closePrompt() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new DismissLowPowerPromptPacket());
        closePrompt();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

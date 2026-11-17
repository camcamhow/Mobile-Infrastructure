package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.network.OpenHeldPhonePacket;
import com.example.mobilecellnetwork.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class HeldPhoneView {
    private static final KeyMapping OPEN = new KeyMapping("key.mobilecellnetwork.held_phone",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.categories.mobilecellnetwork");
    private static boolean enabled;
    private static KeyMapping[] movementKeys;
    private static IKeyConflictContext[] originalContexts;
    private static IKeyConflictContext[] phoneContexts;
    private static boolean[] pressed;
    private static Screen lookScreen;
    private static double cursorX;
    private static double cursorY;

    private HeldPhoneView() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean active() {
        return enabled && Minecraft.getInstance().screen instanceof PhoneNavigableScreen;
    }

    public static void onRenderHand(RenderHandEvent event) {
        if (Minecraft.getInstance().screen instanceof PhoneNavigableScreen
                && com.example.mobilecellnetwork.item.CellphoneItem.isPhone(event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (active() && OPEN.matches(event.getKeyCode(), event.getScanCode())
                && !(event.getScreen().getFocused() instanceof EditBox)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.closeContainer();
            }
            minecraft.setScreen(null);
            enabled = false;
            event.setCanceled(true);
        }
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = event.getScreen();
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || minecraft.player == null
                || !minecraft.isWindowActive() || minecraft.getOverlay() != null
                || !(active() || screen instanceof CameraScreen)
                || PhoneFrame.containsPhone(screen, event.getMouseX(), event.getMouseY())) {
            return;
        }
        lookScreen = screen;
        cursorX = minecraft.mouseHandler.xpos();
        cursorY = minecraft.mouseHandler.ypos();
        minecraft.mouseHandler.setIgnoreFirstMove();
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        event.setCanceled(true);
    }

    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (lookScreen != event.getScreen() || event.getMouseButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || minecraft.player == null || !minecraft.isWindowActive()) {
            return;
        }
        double sensitivity = Math.pow(minecraft.options.sensitivity().get() * 0.6D + 0.2D, 3.0D) * 8.0D;
        double dx = event.getDragX() * minecraft.getWindow().getScreenWidth() / event.getScreen().width;
        double dy = event.getDragY() * minecraft.getWindow().getScreenHeight() / event.getScreen().height;
        minecraft.player.turn(dx * sensitivity, dy * sensitivity * (minecraft.options.invertYMouse().get() ? -1 : 1));
        event.setCanceled(true);
    }

    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (lookScreen == event.getScreen() && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            releaseLook();
            event.setCanceled(true);
        }
    }

    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (lookScreen == event.getScreen()) {
            releaseLook();
        }
    }

    private static void releaseLook() {
        if (lookScreen == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        lookScreen = null;
        long window = minecraft.getWindow().getWindow();
        minecraft.mouseHandler.setIgnoreFirstMove();
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        GLFW.glfwSetCursorPos(window, cursorX, cursorY);
    }

    private static boolean movementAllowed(Minecraft minecraft) {
        return (active() || minecraft.screen instanceof CameraScreen)
                && !(minecraft.screen.getFocused() instanceof EditBox)
                && minecraft.isWindowActive();
    }

    private record PhoneMovementContext(IKeyConflictContext original) implements IKeyConflictContext {
        @Override
        public boolean isActive() {
            return original.isActive() || movementAllowed(Minecraft.getInstance());
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return original.conflicts(other instanceof PhoneMovementContext context ? context.original : other);
        }
    }

    private static void restoreMovementKeys() {
        if (movementKeys == null) {
            return;
        }
        for (int i = 0; i < movementKeys.length; i++) {
            movementKeys[i].setDown(false);
            if (movementKeys[i].getKeyConflictContext() == phoneContexts[i]) {
                movementKeys[i].setKeyConflictContext(originalContexts[i]);
            }
        }
        movementKeys = null;
        originalContexts = null;
        phoneContexts = null;
        pressed = null;
    }

    public static void tick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (lookScreen != null && (minecraft.screen != lookScreen || !minecraft.isWindowActive()
                || minecraft.getOverlay() != null || minecraft.player == null)) {
            releaseLook();
        }
        if (minecraft.player == null || minecraft.level == null) {
            enabled = false;
            restoreMovementKeys();
            return;
        }
        if (minecraft.screen == null) {
            enabled = false;
            while (OPEN.consumeClick()) {
                if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(minecraft.player.getMainHandItem())
                        || com.example.mobilecellnetwork.item.CellphoneItem.isPhone(minecraft.player.getOffhandItem())) {
                    PacketDistributor.sendToServer(new OpenHeldPhonePacket());
                } else {
                    minecraft.player.displayClientMessage(Component.translatable("message.mobilecellnetwork.hold_phone"), true);
                }
            }
        }
        if (!active() && !(minecraft.screen instanceof CameraScreen)) {
            restoreMovementKeys();
            return;
        }
        if (movementKeys == null) {
            movementKeys = new KeyMapping[] {minecraft.options.keyUp, minecraft.options.keyDown,
                    minecraft.options.keyLeft, minecraft.options.keyRight, minecraft.options.keyJump,
                    minecraft.options.keyShift, minecraft.options.keySprint};
            originalContexts = new IKeyConflictContext[movementKeys.length];
            phoneContexts = new IKeyConflictContext[movementKeys.length];
            pressed = new boolean[movementKeys.length];
            for (int i = 0; i < movementKeys.length; i++) {
                originalContexts[i] = movementKeys[i].getKeyConflictContext();
                phoneContexts[i] = new PhoneMovementContext(originalContexts[i]);
                movementKeys[i].setKeyConflictContext(phoneContexts[i]);
            }
        }
        boolean move = movementAllowed(minecraft);
        long window = minecraft.getWindow().getWindow();
        for (int i = 0; i < movementKeys.length; i++) {
            KeyMapping key = movementKeys[i];
            var input = key.getKey();
            boolean down = move && input.getType() == InputConstants.Type.KEYSYM
                    && InputConstants.isKeyDown(window, input.getValue());
            boolean toggle = (key == minecraft.options.keyShift && minecraft.options.toggleCrouch().get())
                    || (key == minecraft.options.keySprint && minecraft.options.toggleSprint().get());
            if (!toggle || down != pressed[i]) {
                key.setDown(down);
            }
            pressed[i] = down;
        }
    }
}

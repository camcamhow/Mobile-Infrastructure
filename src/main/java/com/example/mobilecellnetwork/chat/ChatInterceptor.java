package com.example.mobilecellnetwork.chat;

import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.slf4j.Logger;

public final class ChatInterceptor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static PermissionNode<Boolean> bypassPermission;
    public static final PermissionNode<Boolean> ADMIN_PERMISSION = new PermissionNode<>(
            "mobilecellnetwork", "admin", PermissionTypes.BOOLEAN,
            (player, playerId, context) -> player != null && player.hasPermissions(2));
    public static final PermissionNode<Boolean> BYPASS_PLANS_PERMISSION = new PermissionNode<>(
            "mobilecellnetwork", "bypassplans", PermissionTypes.BOOLEAN,
            (player, playerId, context) -> player != null && player.hasPermissions(2));

    private ChatInterceptor() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ChatInterceptor::onServerChat);
        NeoForge.EVENT_BUS.addListener(CommandInterceptor::onCommand);
        NeoForge.EVENT_BUS.addListener(ChatInterceptor::registerPermissions);
    }

    public static void registerPermissions(PermissionGatherEvent.Nodes event) {
        String configuredNode = MobileCellNetworkConfig.CHAT_LOCK_BYPASS_PERMISSION.get().trim();
        if (configuredNode.isEmpty()) {
            throw new IllegalStateException("chatLockBypassPermission must not be empty.");
        }
        int separator = configuredNode.indexOf('.');
        if (separator < 1 || separator == configuredNode.length() - 1) {
            throw new IllegalStateException(
                    "chatLockBypassPermission must use the namespace.permission format.");
        }
        bypassPermission = new PermissionNode<Boolean>(
                configuredNode.substring(0, separator),
                configuredNode.substring(separator + 1),
                PermissionTypes.BOOLEAN,
                (player, playerId, context) -> player != null && player.hasPermissions(2));
        event.addNodes(bypassPermission);
        event.addNodes(ADMIN_PERMISSION, BYPASS_PLANS_PERMISSION);
    }

    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!MobileCellNetworkConfig.CHAT_LOCK_ENABLED.get() || hasBypassPermission(player)) {
            return;
        }

        event.setCanceled(true);
        sendReminder(player);
        LOGGER.debug("Blocked vanilla chat message from {}: {}", player.getGameProfile().getName(), event.getRawText());
        if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(player.getMainHandItem())
                || com.example.mobilecellnetwork.item.CellphoneItem.isPhone(player.getOffhandItem())) {
            PhoneData phoneData = player.getData(ModCapabilities.PHONE_DATA);
            phoneData.getNotifications().add(new PhoneData.Notification(
                    "SMS", "New message reminder: use SMS.", System.currentTimeMillis()));
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                    new com.example.mobilecellnetwork.network.PhoneNotificationPacket(
                            "SMS", "Mobile Cell Network", "New message reminder: use SMS.", false));
        }
    }

    static boolean hasBypassPermission(ServerPlayer player) {
        if (bypassPermission == null) {
            throw new IllegalStateException("Chat bypass permission has not been registered.");
        }
        return PermissionAPI.getPermission(player, bypassPermission);
    }

    static void sendReminder(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.mobilecellnetwork.chat_reminder"), true);
    }
}

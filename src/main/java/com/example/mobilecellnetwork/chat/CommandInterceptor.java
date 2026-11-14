package com.example.mobilecellnetwork.chat;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.mojang.brigadier.tree.CommandNode;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.CommandEvent;

public final class CommandInterceptor {
    private CommandInterceptor() {
    }

    public static void onCommand(CommandEvent event) {
        if (!MobileCellNetworkConfig.CHAT_LOCK_ENABLED.get()
                || !(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)
                || ChatInterceptor.hasBypassPermission(player)) {
            return;
        }

        Set<String> commandNames = new HashSet<>();
        String commandLine = event.getParseResults().getReader().getString().stripLeading();
        int firstWhitespace = commandLine.indexOf(' ');
        commandNames.add(normalize(firstWhitespace < 0 ? commandLine : commandLine.substring(0, firstWhitespace)));
        for (var parsedNode : event.getParseResults().getContext().getNodes()) {
            CommandNode<CommandSourceStack> node = parsedNode.getNode();
            Set<CommandNode<CommandSourceStack>> visited = new HashSet<>();
            while (node != null && visited.add(node)) {
                commandNames.add(normalize(node.getName()));
                node = node.getRedirect();
            }
        }

        Set<String> whitelist = normalizedCommands(MobileCellNetworkConfig.WHITELISTED_COMMANDS.get());
        if (commandNames.stream().anyMatch(whitelist::contains)) {
            return;
        }

        Set<String> blocked = normalizedCommands(MobileCellNetworkConfig.BLOCKED_COMMANDS.get());
        if (commandNames.stream().anyMatch(blocked::contains)) {
            event.setCanceled(true);
            ChatInterceptor.sendReminder(player);
        }
    }

    private static Set<String> normalizedCommands(Iterable<? extends String> commands) {
        return java.util.stream.StreamSupport.stream(commands.spliterator(), false)
                .map(CommandInterceptor::normalize)
                .collect(Collectors.toSet());
    }

    private static String normalize(String command) {
        String normalized = command.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }
}

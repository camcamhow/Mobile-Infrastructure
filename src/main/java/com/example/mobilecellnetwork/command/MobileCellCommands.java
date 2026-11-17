package com.example.mobilecellnetwork.command;

import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.network.CoverageEngine;
import com.example.mobilecellnetwork.network.LowPowerStatePacket;
import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.AntennaPowerState;
import com.example.mobilecellnetwork.blockentity.AntennaRoamingPolicy;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.example.mobilecellnetwork.registry.ModDataComponents;
import com.example.mobilecellnetwork.registry.ModItems;
import com.example.mobilecellnetwork.util.CurrencyUtil;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class MobileCellCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MobileCellCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mobilecellnumber")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("view")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> view(context,
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("number", StringArgumentType.word())
                                        .executes(context -> set(context,
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "number")))))));
        var mcn = Commands.literal("mcn").requires(source -> source.hasPermission(2));
        mcn.then(Commands.literal("powertest")
                .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> {
                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                    NetworkSavedData.get(context.getSource().getServer().overworld()).setPowerTestEnabled(enabled);
                    context.getSource().sendSuccess(() -> Component.literal(enabled
                            ? "Infrastructure power test enabled for this world."
                            : "Infrastructure power test disabled. Infrastructure requires FE unless requireEnergy is disabled in config."),
                            true);
                    return 1;
                })));
        mcn.then(Commands.literal("signal")
                .then(Commands.literal("here").executes(MobileCellCommands::showSignalHere)));
        mcn.then(Commands.literal("signaltest")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setSignalTest(context,
                                        EntityArgument.getPlayer(context, "player"),
                                        BoolArgumentType.getBool(context, "enabled"))))));
        mcn.then(Commands.literal("chatlock")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> setChatLock(context, BoolArgumentType.getBool(context, "enabled")))));
        mcn.then(Commands.literal("reload").executes(MobileCellCommands::reloadModResources));
        var planCommands = Commands.literal("plan");
        planCommands.then(Commands.literal("list").executes(MobileCellCommands::listPlans));
        planCommands.then(Commands.literal("assign")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("planId", StringArgumentType.word())
                                .executes(context -> assignPlan(context,
                                        EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "planId"))))));
        planCommands.then(Commands.literal("reload").executes(MobileCellCommands::reloadPlans));
        planCommands.then(Commands.literal("create")
                .then(Commands.argument("planId", StringArgumentType.word())
                        .executes(context -> planStub(context, "create"))));
        planCommands.then(Commands.literal("delete")
                .then(Commands.argument("planId", StringArgumentType.word())
                        .executes(context -> planStub(context, "delete"))));
        mcn.then(planCommands);
        var networkCommands = Commands.literal("network");
        networkCommands.then(Commands.literal("list").executes(MobileCellCommands::listNetworks));
        networkCommands.then(Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> createNetwork(context,
                                StringArgumentType.getString(context, "name")))));
        networkCommands.then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> deleteNetwork(context,
                                StringArgumentType.getString(context, "name"), ""))
                        .then(Commands.argument("confirmation", StringArgumentType.word())
                                .executes(context -> deleteNetwork(context,
                                        StringArgumentType.getString(context, "name"),
                                        StringArgumentType.getString(context, "confirmation"))))));
        networkCommands.then(Commands.literal("allow")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> setNetworkAllowed(context,
                                StringArgumentType.getString(context, "name"), true))));
        networkCommands.then(Commands.literal("disallow")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> setNetworkAllowed(context,
                                StringArgumentType.getString(context, "name"), false))));
        networkCommands.then(Commands.literal("owner")
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("owner", StringArgumentType.word())
                                .executes(context -> setNetworkOwner(context,
                                        StringArgumentType.getString(context, "name"),
                                        StringArgumentType.getString(context, "owner"))))));
        networkCommands.then(Commands.literal("roaming")
                .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("policy", StringArgumentType.word())
                                .executes(context -> setNetworkRoamingPolicy(context,
                                        StringArgumentType.getString(context, "name"),
                                        StringArgumentType.getString(context, "policy"))))));
        networkCommands.then(Commands.literal("revenue")
                .then(Commands.literal("reset")
                        .then(Commands.argument("networkId", StringArgumentType.word())
                                .executes(context -> resetNetworkRevenue(context,
                                        StringArgumentType.getString(context, "networkId")))))
                .then(Commands.argument("networkId", StringArgumentType.word())
                        .executes(context -> showNetworkRevenue(context,
                                StringArgumentType.getString(context, "networkId")))));
        networkCommands.then(Commands.literal("password")
                .then(Commands.literal("reset")
                        .then(Commands.argument("networkId", StringArgumentType.word())
                                .executes(context -> resetNetworkPassword(context,
                                        StringArgumentType.getString(context, "networkId"), null))
                                .then(Commands.argument("newPassword", StringArgumentType.greedyString())
                                        .executes(context -> resetNetworkPassword(context,
                                                StringArgumentType.getString(context, "networkId"),
                                                StringArgumentType.getString(context, "newPassword")))))));
        networkCommands.then(Commands.literal("assign-server")
                .then(Commands.argument("networkId", StringArgumentType.word())
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> assignServerBlock(context,
                                        StringArgumentType.getString(context, "networkId"),
                                        BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
        mcn.then(networkCommands);
        var antennaCommands = Commands.literal("antenna");
        antennaCommands.then(Commands.literal("list").executes(MobileCellCommands::listAntennas));
        for (String action : new String[]{"enable", "disable"}) {
            antennaCommands.then(Commands.literal(action)
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .executes(context -> setAntennaEnabled(context,
                                    BlockPosArgument.getLoadedBlockPos(context, "pos"), action.equals("enable")))));
        }
        antennaCommands.then(Commands.literal("state")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> antennaState(context,
                                BlockPosArgument.getLoadedBlockPos(context, "pos")))));
        antennaCommands.then(Commands.literal("owner")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("owner", StringArgumentType.word())
                                .executes(context -> setAntennaOwner(context,
                                        BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                        StringArgumentType.getString(context, "owner"))))));
        var roamingCommand = Commands.literal("roaming");
        var roamingPosition = Commands.argument("pos", BlockPosArgument.blockPos());
        var roamingPolicy = Commands.argument("policy", StringArgumentType.word())
                .executes(context -> setAntennaRoamingPolicy(context,
                        BlockPosArgument.getLoadedBlockPos(context, "pos"),
                        StringArgumentType.getString(context, "policy"), ""))
                .then(Commands.argument("entries", StringArgumentType.greedyString())
                        .executes(context -> setAntennaRoamingPolicy(context,
                                BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                StringArgumentType.getString(context, "policy"),
                                StringArgumentType.getString(context, "entries"))));
        roamingPosition.then(roamingPolicy);
        roamingCommand.then(roamingPosition);
        antennaCommands.then(roamingCommand);
        antennaCommands.then(Commands.literal("forceclaim")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("networkId", StringArgumentType.word())
                                .executes(context -> forceClaimAntenna(context,
                                        BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                        StringArgumentType.getString(context, "networkId"))))));
        for (String action : new String[]{"share", "unshare"}) {
            antennaCommands.then(Commands.literal(action)
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .then(Commands.argument("networkId", StringArgumentType.word())
                                    .executes(context -> setAntennaShare(context,
                                            BlockPosArgument.getLoadedBlockPos(context, "pos"),
                                            StringArgumentType.getString(context, "networkId"),
                                            action.equals("share"))))));
        }
        mcn.then(antennaCommands);
        var carrierRoamingCommands = Commands.literal("roaming");
        carrierRoamingCommands.then(Commands.literal("list").executes(MobileCellCommands::listRoamingAgreements));
        var proposalFrom = Commands.argument("fromNet", StringArgumentType.word());
        var proposalTo = Commands.argument("toNet", StringArgumentType.word());
        var proposalMinute = Commands.argument("pm", LongArgumentType.longArg(0));
        var proposalText = Commands.argument("pt", LongArgumentType.longArg(0));
        proposalText.then(Commands.argument("pMB", LongArgumentType.longArg(0))
                .executes(context -> sendRoamingProposal(context,
                        StringArgumentType.getString(context, "fromNet"),
                        StringArgumentType.getString(context, "toNet"),
                        LongArgumentType.getLong(context, "pm"),
                        LongArgumentType.getLong(context, "pt"),
                        LongArgumentType.getLong(context, "pMB"))));
        proposalMinute.then(proposalText);
        proposalTo.then(proposalMinute);
        proposalFrom.then(proposalTo);
        carrierRoamingCommands.then(Commands.literal("proposal").then(proposalFrom));

        var agreementA = Commands.argument("netA", StringArgumentType.word());
        var agreementB = Commands.argument("netB", StringArgumentType.word());
        var agreementMinute = Commands.argument("pm", LongArgumentType.longArg(0));
        var agreementText = Commands.argument("pt", LongArgumentType.longArg(0));
        agreementText.then(Commands.argument("pMB", LongArgumentType.longArg(0))
                .executes(context -> forceRoamingAgreement(context,
                        StringArgumentType.getString(context, "netA"),
                        StringArgumentType.getString(context, "netB"),
                        LongArgumentType.getLong(context, "pm"),
                        LongArgumentType.getLong(context, "pt"),
                        LongArgumentType.getLong(context, "pMB"))));
        agreementMinute.then(agreementText);
        agreementB.then(agreementMinute);
        agreementA.then(agreementB);
        carrierRoamingCommands.then(Commands.literal("force-agreement").then(agreementA));
        carrierRoamingCommands.then(Commands.literal("clear")
                .then(Commands.argument("netA", StringArgumentType.word())
                        .then(Commands.argument("netB", StringArgumentType.word())
                                .executes(context -> clearRoamingAgreement(context,
                                        StringArgumentType.getString(context, "netA"),
                                        StringArgumentType.getString(context, "netB"))))));
        mcn.then(carrierRoamingCommands);
        var emergencyCommands = Commands.literal("emergency");
        emergencyCommands.then(Commands.literal("list").executes(MobileCellCommands::listEmergencyMessages));
        emergencyCommands.then(Commands.literal("clear").executes(MobileCellCommands::archiveEmergencyMessages));
        mcn.then(emergencyCommands);
        var billingCommands = Commands.literal("billing");
        billingCommands.then(Commands.literal("force")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> forceBilling(context, EntityArgument.getPlayer(context, "player")))));
        billingCommands.then(Commands.literal("history")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> billingHistory(context,
                                EntityArgument.getPlayer(context, "player")))));
        mcn.then(billingCommands);
        var playerCommands = Commands.literal("player");
        playerCommands.then(Commands.literal("list").executes(MobileCellCommands::listPlayers));
        playerCommands.then(Commands.literal("number")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("number", StringArgumentType.word())
                                .executes(context -> setPlayerNumber(context,
                                        EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "number"))))));
        playerCommands.then(Commands.literal("plan")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("planId", StringArgumentType.word())
                                .executes(context -> assignPlan(context,
                                        EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "planId"))))));
        playerCommands.then(Commands.literal("lowpower")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setPlayerLowPower(
                                        context, EntityArgument.getPlayer(context, "player"),
                                        BoolArgumentType.getBool(context, "enabled"))))));
        playerCommands.then(Commands.literal("charge")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount",
                                com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                .executes(context -> chargePlayer(
                                        context, EntityArgument.getPlayer(context, "player"),
                                        IntegerArgumentType.getInteger(context, "amount"))))));
        var balanceCommand = Commands.literal("balance")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(context -> adjustBalance(context,
                                                EntityArgument.getPlayer(context, "player"), "add",
                                                LongArgumentType.getLong(context, "amount")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(context -> adjustBalance(context,
                                                EntityArgument.getPlayer(context, "player"), "remove",
                                                LongArgumentType.getLong(context, "amount")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(context -> adjustBalance(context,
                                                EntityArgument.getPlayer(context, "player"), "set",
                                                LongArgumentType.getLong(context, "amount"))))));
        playerCommands.then(balanceCommand);
        mcn.then(playerCommands);
        event.getDispatcher().register(mcn);
    }

    private static int resetNetworkPassword(CommandContext<CommandSourceStack> context,
                                            String networkName, String newPassword) {
        NetworkSavedData saved = NetworkSavedData.get(context.getSource().getServer().overworld());
        String networkId = saved.findNetworkId(networkName);
        if (networkId == null) {
            context.getSource().sendFailure(Component.literal("Unknown network: " + networkName));
            return 0;
        }
        var settings = saved.getNetworkSettings(networkId);
        if (newPassword == null) {
            settings.clearPassword();
            context.getSource().sendSuccess(() -> Component.literal(
                    "Cleared the password for network " + networkName + "."), true);
            logAdmin(context, "network password clear", networkId);
            saved.setDirty();
            return 1;
        }
        int minimum = com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                .MINIMUM_NETWORK_PASSWORD_LENGTH.get();
        if (newPassword.length() < minimum || newPassword.length() > 128) {
            context.getSource().sendFailure(Component.literal(
                    "Network password must be between " + minimum + " and 128 characters."));
            return 0;
        }
        String salt = com.example.mobilecellnetwork.network.PasswordUtil.generateSalt();
        settings.setPasswordSalt(salt);
        settings.setPasswordHash(com.example.mobilecellnetwork.network.PasswordUtil.hash(newPassword, salt));
        saved.setDirty();
        context.getSource().sendSuccess(() -> Component.literal(
                "Reset the password for network " + networkName + "."), true);
        logAdmin(context, "network password reset", networkId);
        return 1;
    }

    private static int assignServerBlock(CommandContext<CommandSourceStack> context,
                                         String networkName, BlockPos position) {
        NetworkSavedData saved = NetworkSavedData.get(context.getSource().getServer().overworld());
        String networkId = saved.findNetworkId(networkName);
        if (networkId == null) {
            context.getSource().sendFailure(Component.literal("Unknown network: " + networkName));
            return 0;
        }
        var level = context.getSource().getLevel();
        if (!level.hasChunkAt(position)
                || !(level.getBlockEntity(position) instanceof com.example.mobilecellnetwork.blockentity.ServerBlockEntity serverBlock)) {
            context.getSource().sendFailure(Component.literal("There is no server block at that position."));
            return 0;
        }
        var settings = saved.getNetworkSettings(networkId);
        if (settings.hasServerBlock()
                && (!settings.getServerBlockDimension().equals(level.dimension().location().toString())
                || settings.getServerBlockPos() != position.asLong())) {
            net.minecraft.resources.ResourceLocation oldDimension =
                    net.minecraft.resources.ResourceLocation.tryParse(settings.getServerBlockDimension());
            if (oldDimension != null) {
                var oldLevel = context.getSource().getServer().getLevel(
                        net.minecraft.resources.ResourceKey.create(
                                net.minecraft.core.registries.Registries.DIMENSION, oldDimension));
                BlockPos oldPosition = BlockPos.of(settings.getServerBlockPos());
                if (oldLevel != null && oldLevel.hasChunkAt(oldPosition)
                        && oldLevel.getBlockEntity(oldPosition)
                        instanceof com.example.mobilecellnetwork.blockentity.ServerBlockEntity oldServer) {
                    oldServer.removeManagedNetwork(networkId);
                }
            }
        }
        settings.setServerBlock(level.dimension().location().toString(), position.asLong());
        settings.setServerOnline(true);
        serverBlock.addManagedNetwork(networkId);
        saved.setDirty();
        context.getSource().sendSuccess(() -> Component.literal(
                "Assigned server block at " + position.toShortString() + " to network " + networkName + "."), true);
        logAdmin(context, "network assign-server", networkId + " @ " + position.toShortString());
        return 1;
    }

    private static int forceClaimAntenna(CommandContext<CommandSourceStack> context,
                                         BlockPos position, String networkName) {
        NetworkSavedData saved = NetworkSavedData.get(context.getSource().getServer().overworld());
        String networkId = saved.findNetworkId(networkName);
        if (networkId == null || !(context.getSource().getLevel().getBlockEntity(position)
                instanceof AntennaBlockEntity antenna)) {
            context.getSource().sendFailure(Component.literal("Unknown network or no antenna at that position."));
            return 0;
        }
        antenna.setNetworkId(networkId);
        if (antenna.getOwnerUUID() == null && context.getSource().getEntity() instanceof ServerPlayer actor) {
            antenna.setOwner(actor.getUUID(), actor.getGameProfile().getName());
        }
        saved.registerAntenna(context.getSource().getLevel().dimension().location() + "@" + position.asLong(),
                networkId);
        context.getSource().sendSuccess(() -> Component.literal(
                "Force-claimed antenna at " + position.toShortString() + " for network " + networkName + "."), true);
        logAdmin(context, "antenna forceclaim", networkId + " @ " + position.toShortString());
        return 1;
    }

    private static int setAntennaShare(CommandContext<CommandSourceStack> context,
                                       BlockPos position, String networkName, boolean add) {
        NetworkSavedData saved = NetworkSavedData.get(context.getSource().getServer().overworld());
        String networkId = saved.findNetworkId(networkName);
        if (networkId == null || !(context.getSource().getLevel().getBlockEntity(position)
                instanceof AntennaBlockEntity antenna)
                || (!add && !antenna.getSharedWithNetworkIds().contains(networkId))
                || (add && networkId.equals(antenna.getNetworkId()))) {
            context.getSource().sendFailure(Component.literal("Unknown network or invalid antenna share."));
            return 0;
        }
        if (add) {
            antenna.addShare(networkId);
        } else {
            antenna.removeShare(networkId);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                (add ? "Shared antenna with " : "Removed antenna share for ") + networkName + "."), true);
        logAdmin(context, add ? "antenna share" : "antenna unshare",
                networkId + " @ " + position.toShortString());
        return 1;
    }

    private static int listRoamingAgreements(CommandContext<CommandSourceStack> context) {
        NetworkSavedData saved = NetworkSavedData.get(context.getSource().getServer().overworld());
        List<com.example.mobilecellnetwork.network.RoamingManager.AgreementEntry> agreements =
                com.example.mobilecellnetwork.network.RoamingManager.activeAgreements(saved);
        if (agreements.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No active roaming agreements."), false);
            return 0;
        }
        for (var entry : agreements) {
            String first = java.util.Optional.ofNullable(saved.getNetwork(entry.networkA()))
                    .map(NetworkSavedData.NetworkRecord::name).orElse(entry.networkA());
            String second = java.util.Optional.ofNullable(saved.getNetwork(entry.networkB()))
                    .map(NetworkSavedData.NetworkRecord::name).orElse(entry.networkB());
            var agreement = entry.agreement();
            context.getSource().sendSuccess(() -> Component.literal(first + " <-> " + second + ": "
                    + CurrencyUtil.format(agreement.ratePerMinute()) + "/min, "
                    + CurrencyUtil.format(agreement.ratePerText()) + "/text, "
                    + CurrencyUtil.format(agreement.ratePerMb()) + "/MB"), false);
        }
        logAdmin(context, "roaming list", Integer.toString(agreements.size()));
        return agreements.size();
    }

    private static int sendRoamingProposal(CommandContext<CommandSourceStack> context,
                                           String fromNetwork, String toNetwork,
                                           long perMinute, long perText, long perMb) {
        NetworkSavedData saved = NetworkSavedData.get(context.getSource().getServer().overworld());
        boolean sent = com.example.mobilecellnetwork.network.RoamingManager.forcePropose(
                context.getSource().getServer(), saved, saved.findNetworkId(fromNetwork),
                saved.findNetworkId(toNetwork), perMinute, perText, perMb);
        if (!sent) {
            context.getSource().sendFailure(Component.literal("Could not send roaming proposal."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Roaming proposal sent from " + fromNetwork + " to " + toNetwork + "."), true);
        logAdmin(context, "roaming proposal", fromNetwork + " -> " + toNetwork);
        return 1;
    }

    private static int forceRoamingAgreement(CommandContext<CommandSourceStack> context,
                                             String networkA, String networkB,
                                             long perMinute, long perText, long perMb) {
        NetworkSavedData saved = NetworkSavedData.get(context.getSource().getServer().overworld());
        boolean created = com.example.mobilecellnetwork.network.RoamingManager.forceAgreement(
                context.getSource().getServer(), saved, saved.findNetworkId(networkA),
                saved.findNetworkId(networkB), perMinute, perText, perMb);
        if (!created) {
            context.getSource().sendFailure(Component.literal("Could not create roaming agreement."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Active roaming agreement created between " + networkA + " and " + networkB + "."), true);
        logAdmin(context, "roaming force-agreement", networkA + " <-> " + networkB);
        return 1;
    }

    private static int clearRoamingAgreement(CommandContext<CommandSourceStack> context,
                                             String networkA, String networkB) {
        NetworkSavedData saved = NetworkSavedData.get(context.getSource().getServer().overworld());
        boolean removed = com.example.mobilecellnetwork.network.RoamingManager.revokeAgreement(
                context.getSource().getServer(), saved, saved.findNetworkId(networkA),
                saved.findNetworkId(networkB));
        if (!removed) {
            context.getSource().sendFailure(Component.literal("No matching roaming agreement was found."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Roaming agreement cleared between " + networkA + " and " + networkB + "."), true);
        logAdmin(context, "roaming clear", networkA + " <-> " + networkB);
        return 1;
    }

    private static int listEmergencyMessages(CommandContext<CommandSourceStack> context) {
        List<NetworkSavedData.EmergencyMessage> messages =
                NetworkSavedData.get(context.getSource().getServer().overworld()).getAllEmergencyMessages();
        if (messages.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No emergency messages recorded."), false);
            return 0;
        }
        messages.forEach(message -> context.getSource().sendSuccess(() -> Component.literal(
                message.senderName() + " (" + message.senderNumber() + ") at " + message.timestamp()
                        + ": " + message.body()), false));
        logAdmin(context, "emergency list", Integer.toString(messages.size()));
        return messages.size();
    }

    private static int archiveEmergencyMessages(CommandContext<CommandSourceStack> context) {
        int archived = NetworkSavedData.get(context.getSource().getServer().overworld())
                .archiveEmergencyMessages();
        context.getSource().sendSuccess(() -> Component.literal(
                "Archived " + archived + " emergency message(s)."), true);
        logAdmin(context, "emergency clear", Integer.toString(archived));
        return archived;
    }

    private static int listNetworks(CommandContext<CommandSourceStack> context) {
        var networks = NetworkSavedData.get(context.getSource().getServer().overworld()).getNetworks();
        if (networks.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No networks registered."), false);
            return 0;
        }
        networks.forEach((id, network) -> context.getSource().sendSuccess(() -> Component.literal(
                network.name() + " [" + id + "] allowed=" + network.allowed()
                        + " antennas=" + network.towers().size()), false));
        logAdmin(context, "network list", Integer.toString(networks.size()));
        return networks.size();
    }

    private static int showNetworkRevenue(CommandContext<CommandSourceStack> context, String networkId) {
        NetworkSavedData saved = NetworkSavedData.get(context.getSource().getServer().overworld());
        String id = saved.getNetwork(networkId) == null ? saved.findNetworkId(networkId) : networkId;
        NetworkSavedData.NetworkRecord network = id == null ? null : saved.getNetwork(id);
        if (network == null) {
            context.getSource().sendFailure(Component.literal("Unknown network ID: " + networkId));
            return 0;
        }
        long balance = saved.getRevenueBalance(id);
        context.getSource().sendSuccess(() -> Component.literal(network.name() + " revenue: "
                + CurrencyUtil.format(balance)), false);
        logAdmin(context, "network revenue", id + " = " + balance);
        return 1;
    }

    private static int resetNetworkRevenue(CommandContext<CommandSourceStack> context, String networkId) {
        NetworkSavedData saved = NetworkSavedData.get(context.getSource().getServer().overworld());
        NetworkSavedData.NetworkRecord network = saved.getNetwork(networkId);
        if (network == null) {
            context.getSource().sendFailure(Component.literal("Unknown network ID: " + networkId));
            return 0;
        }
        saved.resetNetworkRevenueBalance(networkId);
        context.getSource().sendSuccess(() -> Component.literal("Reset revenue for " + network.name() + "."),
                true);
        logAdmin(context, "network revenue reset", networkId);
        return 1;
    }

    private static int createNetwork(CommandContext<CommandSourceStack> context, String name) {
        if (name.isBlank() || name.length() > 64) {
            context.getSource().sendFailure(Component.literal("Network name must be 1-64 characters."));
            return 0;
        }
        NetworkSavedData data = NetworkSavedData.get(context.getSource().getServer().overworld());
        if (data.findNetworkId(name) != null) {
            context.getSource().sendFailure(Component.literal("A network with that name already exists."));
            return 0;
        }
        String id = java.util.UUID.randomUUID().toString();
        var creator = context.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                ? serverPlayer : null;
        data.putNetwork(id, new NetworkSavedData.NetworkRecord(name, true, java.util.List.of(),
                creator == null ? null : creator.getUUID(),
                creator == null ? "Server" : creator.getGameProfile().getName(),
                com.example.mobilecellnetwork.config.MobileCellNetworkConfig.DEFAULT_NETWORK_ROAMING_POLICY.get()));
        context.getSource().sendSuccess(() -> Component.literal("Created network " + name + " [" + id + "]."), true);
        logAdmin(context, "network create", name + " [" + id + "]");
        return 1;
    }

    private static int deleteNetwork(CommandContext<CommandSourceStack> context, String name, String confirmation) {
        NetworkSavedData data = NetworkSavedData.get(context.getSource().getServer().overworld());
        String id = data.findNetworkId(name);
        if (id == null) {
            context.getSource().sendFailure(Component.literal("Unknown network: " + name));
            return 0;
        }
        if (!"confirm".equals(confirmation)) {
            context.getSource().sendFailure(Component.literal(
                    "Type /mcn network delete \"" + name + "\" confirm to delete"));
            return 0;
        }
        var affected = com.example.mobilecellnetwork.blockentity.AntennaBlockEntity
                .loadedAntennas(context.getSource().getServer()).stream()
                .filter(antenna -> antenna.getNetworkId().equals(id)).toList();
        long registered = data.getAntennaNetworks().values().stream().filter(id::equals).count();
        if (registered > 0) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "Warning: " + registered + " registered antenna(s) will be orphaned."), false);
            affected.forEach(antenna -> antenna.setNetworkId("default"));
        }
        if (!data.deleteNetwork(id)) {
            context.getSource().sendFailure(Component.literal("Network deletion failed."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Deleted network " + name + "."), true);
        logAdmin(context, "network delete", name + " [" + id + "], orphaned antennas=" + registered);
        return 1;
    }

    private static int setNetworkAllowed(CommandContext<CommandSourceStack> context, String name, boolean allowed) {
        NetworkSavedData data = NetworkSavedData.get(context.getSource().getServer().overworld());
        String id = data.findNetworkId(name);
        if (id == null || !data.setNetworkAllowed(id, allowed)) {
            context.getSource().sendFailure(Component.literal("Unknown network: " + name));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Network " + name + " " + (allowed ? "allowed." : "disallowed.")), true);
        logAdmin(context, allowed ? "network allow" : "network disallow", name);
        return 1;
    }

    private static int setNetworkOwner(CommandContext<CommandSourceStack> context, String name, String ownerInput) {
        NetworkSavedData data = NetworkSavedData.get(context.getSource().getServer().overworld());
        String id = data.findNetworkId(name);
        if (id == null) {
            context.getSource().sendFailure(Component.literal("Unknown network: " + name));
            return 0;
        }
        OwnerSelection owner = resolveOwner(context, ownerInput);
        if (owner == null) {
            return 0;
        }
        if (!data.setNetworkOwner(id, owner.uuid(), owner.name())) {
            context.getSource().sendFailure(Component.literal("Could not update network owner."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Set owner of network " + name + " to " + owner.name() + "."), true);
        logAdmin(context, "network owner", name + " -> " + owner.name());
        return 1;
    }

    private static int setNetworkRoamingPolicy(
            CommandContext<CommandSourceStack> context, String name, String policyInput) {
        NetworkSavedData data = NetworkSavedData.get(context.getSource().getServer().overworld());
        String id = data.findNetworkId(name);
        if (id == null) {
            context.getSource().sendFailure(Component.literal("Unknown network: " + name));
            return 0;
        }
        AntennaRoamingPolicy policy = parseRoamingPolicy(context, policyInput);
        if (policy == null) {
            return 0;
        }
        if (!data.setNetworkDefaultRoamingPolicy(id, policy)) {
            context.getSource().sendFailure(Component.literal("Could not update network roaming policy."));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Set default roaming policy for network " + name + " to " + policy + "."), true);
        logAdmin(context, "network roaming", name + " -> " + policy);
        return 1;
    }

    private static int setAntennaOwner(
            CommandContext<CommandSourceStack> context, BlockPos position, String ownerInput) {
        if (!(context.getSource().getLevel().getBlockEntity(position) instanceof AntennaBlockEntity antenna)) {
            context.getSource().sendFailure(Component.literal("No antenna at " + position.toShortString()));
            return 0;
        }
        OwnerSelection owner = resolveOwner(context, ownerInput);
        if (owner == null) {
            return 0;
        }
        antenna.setOwner(owner.uuid(), owner.name());
        context.getSource().sendSuccess(() -> Component.literal(
                "Set owner of antenna " + position.toShortString() + " to " + owner.name() + "."), true);
        logAdmin(context, "antenna owner", position.toShortString() + " -> " + owner.name());
        return 1;
    }

    private static int setAntennaRoamingPolicy(
            CommandContext<CommandSourceStack> context, BlockPos position, String policyInput, String entriesInput) {
        if (!(context.getSource().getLevel().getBlockEntity(position) instanceof AntennaBlockEntity antenna)) {
            context.getSource().sendFailure(Component.literal("No antenna at " + position.toShortString()));
            return 0;
        }
        AntennaRoamingPolicy policy = parseRoamingPolicy(context, policyInput);
        if (policy == null) {
            return 0;
        }
        java.util.Set<java.util.UUID> playerWhitelist = new java.util.LinkedHashSet<>();
        java.util.Set<String> networkWhitelist = new java.util.LinkedHashSet<>();
        if (!entriesInput.isBlank()) {
            if (policy != AntennaRoamingPolicy.WHITELIST_PLAYERS
                    && policy != AntennaRoamingPolicy.WHITELIST_NETWORKS) {
                context.getSource().sendFailure(Component.literal(
                        "Whitelist entries are only valid with WHITELIST_PLAYERS or WHITELIST_NETWORKS."));
                return 0;
            }
            String[] entries = entriesInput.trim().split("[,\\s]+");
            if (policy == AntennaRoamingPolicy.WHITELIST_PLAYERS) {
                for (String entry : entries) {
                    OwnerSelection player = resolveOwner(context, entry);
                    if (player == null) {
                        return 0;
                    }
                    if (player.uuid() == null) {
                        context.getSource().sendFailure(Component.literal(
                                "Server cannot be used as a roaming whitelist player."));
                        return 0;
                    }
                    playerWhitelist.add(player.uuid());
                }
            } else {
                NetworkSavedData data = NetworkSavedData.get(context.getSource().getServer().overworld());
                for (String entry : entries) {
                    String networkId = data.findNetworkId(entry);
                    if (networkId == null) {
                        context.getSource().sendFailure(Component.literal("Unknown network: " + entry));
                        return 0;
                    }
                    networkWhitelist.add(networkId);
                }
            }
        }
        antenna.setRoamingPolicy(policy);
        if (policy == AntennaRoamingPolicy.WHITELIST_PLAYERS) {
            antenna.setRoamingWhitelist(playerWhitelist);
        } else if (policy == AntennaRoamingPolicy.WHITELIST_NETWORKS) {
            antenna.setRoamingNetworkWhitelist(networkWhitelist);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Set roaming policy for antenna " + position.toShortString() + " to " + policy
                        + (entriesInput.isBlank() ? "." : " with "
                                + (policy == AntennaRoamingPolicy.WHITELIST_PLAYERS
                                        ? playerWhitelist.size() : networkWhitelist.size())
                                + " whitelist entries.")), true);
        logAdmin(context, "antenna roaming", position.toShortString() + " -> " + policy
                + (entriesInput.isBlank() ? "" : " entries=" + entriesInput));
        return 1;
    }

    private static AntennaRoamingPolicy parseRoamingPolicy(
            CommandContext<CommandSourceStack> context, String policyInput) {
        try {
            return AntennaRoamingPolicy.valueOf(policyInput.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            context.getSource().sendFailure(Component.literal(
                    "Unknown roaming policy. Use " + java.util.Arrays.toString(AntennaRoamingPolicy.values()) + "."));
            return null;
        }
    }

    private static OwnerSelection resolveOwner(CommandContext<CommandSourceStack> context, String input) {
        if ("server".equalsIgnoreCase(input)) {
            return new OwnerSelection(null, "Server");
        }
        var server = context.getSource().getServer();
        ServerPlayer online = server.getPlayerList().getPlayerByName(input);
        if (online != null) {
            return new OwnerSelection(online.getUUID(), online.getGameProfile().getName());
        }
        try {
            java.util.UUID uuid = java.util.UUID.fromString(input);
            var profile = server.getProfileCache().get(uuid);
            return profile.map(gameProfile -> new OwnerSelection(gameProfile.getId(), gameProfile.getName()))
                    .orElseGet(() -> new OwnerSelection(uuid, uuid.toString()));
        } catch (IllegalArgumentException ignored) {
            var profile = server.getProfileCache().get(input);
            if (profile.isPresent()) {
                var gameProfile = profile.get();
                return new OwnerSelection(gameProfile.getId(), gameProfile.getName());
            }
        }
        context.getSource().sendFailure(Component.literal("Unknown player or UUID: " + input));
        return null;
    }

    private record OwnerSelection(java.util.UUID uuid, String name) {
    }

    private static int listAntennas(CommandContext<CommandSourceStack> context) {
        var antennas = AntennaBlockEntity.loadedAntennas(context.getSource().getServer());
        if (antennas.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No loaded antennas."), false);
            return 0;
        }
        for (AntennaBlockEntity antenna : antennas) {
            context.getSource().sendSuccess(() -> Component.literal(
                    antenna.getLevel().dimension().location() + " " + antenna.getBlockPos().toShortString()
                            + " network=" + antenna.getNetworkId() + " enabled=" + antenna.isEnabled()
                            + " state=" + antenna.getPowerState()), false);
        }
        logAdmin(context, "antenna list", Integer.toString(antennas.size()));
        return antennas.size();
    }

    private static int setAntennaEnabled(CommandContext<CommandSourceStack> context, BlockPos position,
                                         boolean enabled) {
        if (!(context.getSource().getLevel().getBlockEntity(position) instanceof AntennaBlockEntity antenna)) {
            context.getSource().sendFailure(Component.literal("No antenna at " + position.toShortString()));
            return 0;
        }
        antenna.setEnabled(enabled);
        context.getSource().sendSuccess(() -> Component.literal(
                "Antenna " + position.toShortString() + (enabled ? " enabled." : " disabled.")), true);
        logAdmin(context, enabled ? "antenna enable" : "antenna disable", position.toShortString());
        return 1;
    }

    private static int antennaState(CommandContext<CommandSourceStack> context, BlockPos position) {
        if (!(context.getSource().getLevel().getBlockEntity(position) instanceof AntennaBlockEntity antenna)) {
            context.getSource().sendFailure(Component.literal("No antenna at " + position.toShortString()));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "FE " + antenna.getEnergyStored() + "/" + antenna.getMaxEnergyStored()
                        + " | state " + antenna.getPowerState() + " | users " + antenna.getUserSet().size()
                        + " | enabled " + antenna.isEnabled()), false);
        logAdmin(context, "antenna state", position.toShortString());
        return 1;
    }

    private static int listPlayers(CommandContext<CommandSourceStack> context) {
        var players = context.getSource().getServer().getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            context.getSource().sendSuccess(() -> Component.literal(
                    player.getGameProfile().getName() + " | "
                            + player.getData(ModCapabilities.PHONE_DATA).getPhoneNumber() + " | plan "
                            + player.getData(ModCapabilities.PLAN_USAGE).getActivePlanId()), false);
        }
        logAdmin(context, "player list", Integer.toString(players.size()));
        return players.size();
    }

    private static int setPlayerNumber(CommandContext<CommandSourceStack> context, ServerPlayer player,
                                       String number) {
        NetworkSavedData data = NetworkSavedData.get(context.getSource().getServer().overworld());
        if (!number.matches("[0-9]{1,9}") || !data.registerNumber(player.getUUID(), number)) {
            context.getSource().sendFailure(Component.literal("Invalid number or number already in use."));
            return 0;
        }
        player.getData(ModCapabilities.PHONE_DATA).setPhoneNumber(number);
        com.example.mobilecellnetwork.network.NetworkHandler.syncPhoneData(player);
        context.getSource().sendSuccess(() -> Component.literal(
                "Set " + player.getGameProfile().getName() + "'s number to " + number + "."), true);
        logAdmin(context, "player number", player.getGameProfile().getName() + " -> " + number);
        return 1;
    }

    private static void logAdmin(CommandContext<CommandSourceStack> context, String action, String target) {
        String actor = context.getSource().getEntity() instanceof ServerPlayer player
                ? player.getGameProfile().getName() : "Server";
        LOGGER.info("Admin action by {}: {} target={}", actor, action, target);
    }

    private static int setChatLock(CommandContext<CommandSourceStack> context, boolean enabled) {
        com.example.mobilecellnetwork.config.MobileCellNetworkConfig.CHAT_LOCK_ENABLED.set(enabled);
        context.getSource().sendSuccess(
                () -> Component.literal("Mobile Cell Network chat lock " + (enabled ? "enabled." : "disabled.")),
                true);
        logAdmin(context, "chatlock", Boolean.toString(enabled));
        return 1;
    }

    private static int listPlans(CommandContext<CommandSourceStack> context) {
        var plans = com.example.mobilecellnetwork.plans.PlanRegistry.getAllPlans();
        if (plans.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No subscription plans are loaded."));
            return 0;
        }
        for (var plan : plans) {
            context.getSource().sendSuccess(() -> Component.literal(plan.id() + " - " + plan.displayName()
                    + " (" + CurrencyUtil.format(plan.price()) + ", "
                    + plan.maxSignalTier().displayName() + ")"), false);
        }
        logAdmin(context, "plan list", Integer.toString(plans.size()));
        return plans.size();
    }

    private static int assignPlan(
            CommandContext<CommandSourceStack> context, ServerPlayer player, String planId) {
        var plan = com.example.mobilecellnetwork.plans.PlanRegistry.findPlan(planId);
        if (plan == null) {
            context.getSource().sendFailure(Component.literal("Unknown plan: " + planId));
            return 0;
        }
        long day = Math.floorDiv(player.serverLevel().getDayTime(), 24_000L);
        var usage = player.getData(ModCapabilities.PLAN_USAGE);
        var previousPlan = usage.getActivePlanId().isBlank() ? null
                : com.example.mobilecellnetwork.plans.PlanRegistry.findPlan(usage.getActivePlanId());
        if (previousPlan == null || !previousPlan.id().equals(plan.id())) {
            com.example.mobilecellnetwork.billing.BillingManager.resetPlanRevenueCycles(player, previousPlan, plan);
        }
        if (com.example.mobilecellnetwork.plans.PlanRegistry.isDowngrade(
                previousPlan == null ? com.example.mobilecellnetwork.plans.PlanRegistry.getDefaultPlan()
                        : previousPlan, plan)) {
            com.example.mobilecellnetwork.voice.CallManager.hangUp(player.getUUID());
        }
        usage.setActivePlanId(plan.id());
        usage.resetDailyCounters(day);
        usage.setCycleStartDay(day);
        usage.setCycleActive(true);
        player.getData(ModCapabilities.PHONE_DATA).setLastKnownPlanId(plan.id());
        NetworkSavedData.get(player.serverLevel().getServer().overworld())
                .setPlanAssignment(player.getUUID(), plan.id());
        com.example.mobilecellnetwork.network.NetworkHandler.syncPhoneData(player);
        com.example.mobilecellnetwork.network.NetworkHandler.syncPlanData(player);
        context.getSource().sendSuccess(() -> Component.literal(
                "Assigned plan " + plan.id() + " to " + player.getGameProfile().getName() + "."), true);
        logAdmin(context, "player plan", player.getGameProfile().getName() + " -> " + plan.id());
        return 1;
    }

    private static int reloadPlans(CommandContext<CommandSourceStack> context) {
        context.getSource().getServer()
                .reloadResources(context.getSource().getServer().getPackRepository().getSelectedIds());
        context.getSource().sendSuccess(() -> Component.literal("Reloading Mobile Cell Network plans."), true);
        logAdmin(context, "plan reload", "datapacks");
        return 1;
    }

    private static int reloadModResources(CommandContext<CommandSourceStack> context) {
        context.getSource().getServer()
                .reloadResources(context.getSource().getServer().getPackRepository().getSelectedIds());
        context.getSource().sendSuccess(() -> Component.literal("Reloading server resources."), true);
        logAdmin(context, "reload", "server resources");
        return 1;
    }

    private static int planStub(CommandContext<CommandSourceStack> context, String operation) {
        context.getSource().sendFailure(Component.literal(
                "Plan " + operation + " is a stub. Add or remove a plan JSON data-pack resource, then run /mcn plan reload."));
        return 0;
    }

    private static int forceBilling(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        com.example.mobilecellnetwork.billing.BillingManager.processPlayer(player, true);
        context.getSource().sendSuccess(() -> Component.literal(
                "Forced billing check for " + player.getGameProfile().getName() + "."), true);
        logAdmin(context, "billing force", player.getGameProfile().getName());
        return 1;
    }

    private static int billingHistory(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        com.example.mobilecellnetwork.billing.BillingManager.sendHistory(player, context.getSource());
        logAdmin(context, "billing history", player.getGameProfile().getName());
        return 1;
    }

    private static int setPlayerLowPower(
            CommandContext<CommandSourceStack> context, ServerPlayer player, boolean enabled) {
        PhoneData phoneData = player.getData(ModCapabilities.PHONE_DATA);
        if (enabled && !com.example.mobilecellnetwork.config.MobileCellNetworkConfig.LOW_POWER_ENABLED.get()) {
            context.getSource().sendFailure(Component.literal("Low Power Mode is disabled on this server."));
            return 0;
        }
        if (enabled && phoneData.isDead()) {
            context.getSource().sendFailure(Component.literal("Cannot enable: phone is dead"));
            return 0;
        }
        phoneData.setLowPowerMode(enabled);
        if (enabled && com.example.mobilecellnetwork.config.MobileCellNetworkConfig.LOW_POWER_DROP_ACTIVE_CALL.get()) {
            com.example.mobilecellnetwork.voice.CallManager.endForLowPower(player);
        }
        PacketDistributor.sendToPlayer(player, new LowPowerStatePacket(enabled));
        context.getSource().sendSuccess(() -> Component.literal(
                "Low Power Mode " + (enabled ? "enabled" : "disabled") + " for "
                        + player.getGameProfile().getName() + "."), true);
        logAdmin(context, "player lowpower", player.getGameProfile().getName() + " -> " + enabled);
        return 1;
    }

    private static int chargePlayer(
            CommandContext<CommandSourceStack> context, ServerPlayer player, int amount) {
        int charge = Math.clamp(amount,
                0, com.example.mobilecellnetwork.config.MobileCellNetworkConfig.PHONE_MAX_CHARGE.get());
        PhoneData phoneData = player.getData(ModCapabilities.PHONE_DATA);
        phoneData.setCharge(charge);
        ItemStack cellphone = findCellphone(player);
        if (!cellphone.isEmpty()) {
            cellphone.set(ModDataComponents.PHONE_CHARGE.get(), charge);
        }
        com.example.mobilecellnetwork.network.NetworkHandler.syncPhoneData(player);
        context.getSource().sendSuccess(() -> Component.literal(
                "Set " + player.getGameProfile().getName() + "'s phone charge to " + charge + "."), true);
        logAdmin(context, "player charge", player.getGameProfile().getName() + " -> " + charge);
        return 1;
    }

    private static int adjustBalance(
            CommandContext<CommandSourceStack> context, ServerPlayer player, String operation, long amount) {
        var account = player.getData(ModCapabilities.BANK_ACCOUNT);
        long previous = account.getBalance();
        if ("remove".equals(operation) && amount > previous) {
            context.getSource().sendFailure(Component.literal(
                    player.getGameProfile().getName() + " has only " + previous + " units."));
            return 0;
        }
        long updated;
        try {
            updated = switch (operation) {
                case "add" -> Math.addExact(previous, amount);
                case "remove" -> previous - amount;
                case "set" -> amount;
                default -> throw new IllegalArgumentException("Unknown balance operation: " + operation);
            };
        } catch (ArithmeticException exception) {
            context.getSource().sendFailure(Component.literal("The resulting balance is too large."));
            return 0;
        }

        long difference = updated - previous;
        account.setBalance(updated);
        if (difference != 0) {
            account.getTransactionHistory().add(new com.example.mobilecellnetwork.capabilities.BankAccount.Transaction(
                    difference, "Admin balance " + operation, System.currentTimeMillis()));
        }
        com.example.mobilecellnetwork.network.NetworkHandler.syncBankAccount(player);
        context.getSource().sendSuccess(() -> Component.literal(
                "Set " + player.getGameProfile().getName() + "'s balance to " + updated + " units."),
                true);
        logAdmin(context, "player balance " + operation,
                player.getGameProfile().getName() + " " + previous + " -> " + updated);
        return 1;
    }

    private static ItemStack findCellphone(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack)) {
                return stack;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int setSignalTest(CommandContext<CommandSourceStack> context, ServerPlayer player, boolean enabled) {
        NetworkSavedData.get(player.server.overworld()).setSignalTestEnabled(player.getUUID(), enabled);
        context.getSource().sendSuccess(() -> Component.literal("Signal testing "
                + (enabled ? "enabled for " : "disabled for ") + player.getGameProfile().getName()
                + ". Calls, messages and data still require a plan."), true);
        logAdmin(context, "signal test", player.getGameProfile().getName() + ": " + enabled);
        return 1;
    }

    private static int showSignalHere(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        boolean test = NetworkSavedData.get(player.server.overworld()).isSignalTestEnabled(player.getUUID());
        AntennaBlockEntity antenna = test ? CoverageEngine.getStrongestTestAntennaFor(player)
                : CoverageEngine.getStrongestAntennaFor(player);
        CoverageEngine.SignalResult result = test ? CoverageEngine.updateTestSignal(player, antenna)
                : CoverageEngine.updateSignal(player, antenna);
        if (antenna == null) {
            context.getSource().sendSuccess(() -> Component.literal("No antenna provides signal here."), false);
            logAdmin(context, "signal here", "no service");
            return 1;
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "Antenna: " + Component.translatable(
                        antenna.getBlockState().getBlock().getDescriptionId()).getString()
                        + " | Distance: " + format(result.distance())
                        + " | Effective range: " + format(result.effectiveRange())
                        + " | Base strength: " + format(result.baseStrength())
                        + " | LOS attenuation: " + format(result.losAttenuation())
                        + " | Final strength: " + format(result.finalStrength())
                        + " | Tier: " + result.tier().displayName()), false);
        logAdmin(context, "signal here", result.tier().displayName());
        return 1;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static int view(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String number = player.getData(ModCapabilities.PHONE_DATA).getPhoneNumber();
        if (number.isBlank()) {
            context.getSource().sendFailure(Component.literal("That player does not have a phone number."));
            return 0;
        }
        context.getSource().sendSuccess(
                () -> Component.literal(player.getGameProfile().getName() + "'s mobile number is " + number + "."),
                false);
        logAdmin(context, "player number view", player.getGameProfile().getName());
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context, ServerPlayer player, String number) {
        if (!number.matches("[0-9]{1,9}")) {
            context.getSource().sendFailure(Component.literal("Phone numbers must contain 1 to 9 digits."));
            return 0;
        }

        NetworkSavedData savedData = NetworkSavedData.get(player.serverLevel().getServer().overworld());
        if (!savedData.registerNumber(player.getUUID(), number)) {
            context.getSource().sendFailure(Component.literal("That phone number is already assigned."));
            return 0;
        }
        player.getData(ModCapabilities.PHONE_DATA).setPhoneNumber(number);
        com.example.mobilecellnetwork.network.NetworkHandler.syncPhoneData(player);
        context.getSource().sendSuccess(
                () -> Component.literal("Set " + player.getGameProfile().getName() + "'s mobile number to "
                        + number + "."),
                true);
        logAdmin(context, "player number", player.getGameProfile().getName() + " -> " + number);
        return 1;
    }
}

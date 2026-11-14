package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.blockentity.AntennaBlockEntity;
import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.data.NetworkSavedData;
import com.example.mobilecellnetwork.gui.ServerMenu;
import com.example.mobilecellnetwork.plans.PlanRegistry;
import com.example.mobilecellnetwork.capabilities.ModCapabilities;
import com.example.mobilecellnetwork.capabilities.PlanUsage;
import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

public final class ServerAdminHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ServerAdminHandler() {
    }

    public static void request(net.minecraft.world.entity.player.Player sender, ServerAdminRequestPacket packet) {
        if (!(sender instanceof ServerPlayer player) || !validMenu(player)
                || !List.of("Networks", "Roaming", "Antennas", "Players", "Plans", "Billing")
                .contains(packet.tab())) {
            return;
        }
        if (!player.hasPermissions(2) && !"Networks".equals(packet.tab()) && !"Plans".equals(packet.tab())) {
            if (!"Roaming".equals(packet.tab())) {
                if ("Plans".equals(packet.tab())) {
                    player.sendSystemMessage(Component.literal("Administrator permission is required to manage plans."));
                }
                return;
            }
        }
        List<String> rows = switch (packet.tab()) {
            case "Networks" -> networkRows(player, true);
            case "Roaming" -> roamingRows(player, true);
            case "Antennas" -> antennaRows(player);
            case "Players" -> playerRows(player);
            case "Plans" -> planRows(player);
            case "Billing" -> billingRows(player);
            default -> List.of();
        };
        PacketDistributor.sendToPlayer(player, new ServerAdminDataPacket(packet.tab(), rows));
    }

    public static void action(net.minecraft.world.entity.player.Player sender, ServerAdminActionPacket packet) {
        if (!(sender instanceof ServerPlayer player)) {
            return;
        }
        if (!validMenu(player) || packet.tab().length() > 16 || packet.action().length() > 16
                || packet.target().length() > 128 || packet.value().length() > 1200) {
            if ("Plans".equals(packet.tab()) && ("create".equals(packet.action()) || "upsert".equals(packet.action()))) {
                sendPlanEditorResult(player, false);
            }
            return;
        }
        if (!player.hasPermissions(2)) {
            if ("Plans".equals(packet.tab())) {
                boolean success = planAction(player, packet);
                if ("create".equals(packet.action()) || "upsert".equals(packet.action())) {
                    sendPlanEditorResult(player, success);
                }
                request(player, new ServerAdminRequestPacket("Plans"));
            } else if ("Networks".equals(packet.tab())
                    && ("alert".equals(packet.action()) || "setPassword".equals(packet.action())
                    || "setup".equals(packet.action()) || "restore".equals(packet.action()))) {
                networkAction(player, packet);
                request(player, new ServerAdminRequestPacket("Networks"));
            } else if ("Roaming".equals(packet.tab())) {
                roamingAction(player, packet);
                request(player, new ServerAdminRequestPacket("Roaming"));
            }
            return;
        }
        if ("setPassword".equals(packet.action()) || "claim".equals(packet.action())
                || "setup".equals(packet.action())) {
            LOGGER.info("Admin credential action by {}: {} target={}",
                    player.getGameProfile().getName(), packet.tab() + "/" + packet.action(), packet.target());
        } else if ("alert".equals(packet.action()) || "emergency".equals(packet.action())) {
            LOGGER.info("Admin alert action by {}: {} target={}",
                    player.getGameProfile().getName(), packet.tab() + "/" + packet.action(), packet.target());
        } else {
            LOGGER.info("Admin action by {}: {} target={} value={}",
                    player.getGameProfile().getName(), packet.tab() + "/" + packet.action(),
                    packet.target(), packet.value());
        }
        switch (packet.tab()) {
            case "Networks" -> networkAction(player, packet);
            case "Roaming" -> roamingAction(player, packet);
            case "Antennas" -> antennaAction(player, packet);
            case "Players" -> playerAction(player, packet);
            case "Plans" -> {
                boolean success = planAction(player, packet);
                if ("create".equals(packet.action()) || "upsert".equals(packet.action())) {
                    sendPlanEditorResult(player, success);
                }
            }
            case "Billing" -> billingAction(player, packet);
            default -> { }
        }

        request(player, new ServerAdminRequestPacket(packet.tab()));
    }

    private static void sendPlanEditorResult(ServerPlayer player, boolean success) {
        PacketDistributor.sendToPlayer(player, new PhoneNotificationPacket("SYSTEM", "Plan editor",
                success ? "Plan saved." : "Plan not saved. Check chat for details.", false));
    }

    public static boolean createNetwork(net.minecraft.world.entity.player.Player sender, String name, String password) {
        if (!(sender instanceof ServerPlayer player)) {
            return false;
        }
        if (!validMenu(player)) {
            sendNetworkCreationResult(player, false, "The server menu is no longer available.");
            return false;
        }

        String normalizedName = name == null ? "" : name.trim();
        int minimumPasswordLength = MobileCellNetworkConfig.MINIMUM_NETWORK_PASSWORD_LENGTH.get();
        if (normalizedName.isEmpty() || normalizedName.length() > 64 || normalizedName.contains("|")) {
            sendNetworkCreationResult(player, false,
                    "Network name must be 1-64 characters and cannot contain '|'.");
            return false;
        }
        if (password == null || password.length() < minimumPasswordLength || password.length() > 128) {
            sendNetworkCreationResult(player, false,
                    "Network password must be between " + minimumPasswordLength + " and 128 characters.");
            return false;
        }

        NetworkSavedData data = NetworkSavedData.get(player.server.overworld());
        if (data.findNetworkId(normalizedName) != null) {
            sendNetworkCreationResult(player, false, "A network with that name already exists.");
            return false;
        }

        String id = UUID.randomUUID().toString();
        data.putNetwork(id, new NetworkSavedData.NetworkRecord(normalizedName, true, List.of(),
                player.getUUID(), player.getGameProfile().getName(),
                MobileCellNetworkConfig.DEFAULT_NETWORK_ROAMING_POLICY.get()));
        var settings = data.getNetworkSettings(id);
        settings.setPasswordHash(PasswordUtil.hash(password, settings.getPasswordSalt()));
        ServerMenu menu = (ServerMenu) player.containerMenu;
        if (!(player.serverLevel().getBlockEntity(menu.position())
                instanceof com.example.mobilecellnetwork.blockentity.ServerBlockEntity serverBlock)) {
            data.deleteNetwork(id);
            sendNetworkCreationResult(player, false, "The server block is no longer available.");
            return false;
        }
        settings.setServerBlock(player.serverLevel().dimension().location().toString(), menu.position().asLong());
        settings.setServerOnline(true);
        serverBlock.addManagedNetwork(id);
        data.setDirty();

        sendNetworkCreationResult(player, true, "Network '" + normalizedName + "' created.");
        request(player, new ServerAdminRequestPacket("Networks"));
        player.server.getPlayerList().getPlayers().forEach(NetworkHandler::syncPlanData);
        return true;
    }

    private static void sendNetworkCreationResult(ServerPlayer player, boolean success, String message) {
        PacketDistributor.sendToPlayer(player, new NetworkCreationResultPacket(success, message));
    }

    private static boolean validMenu(ServerPlayer player) {
        return player.containerMenu instanceof ServerMenu menu
                && menu.stillValid(player);
    }

    private static List<String> networkRows(ServerPlayer player, boolean ownerOnly) {
        NetworkSavedData saved = NetworkSavedData.get(player.serverLevel().getServer().overworld());
        ServerMenu menu = (ServerMenu) player.containerMenu;
        boolean canRestore = player.serverLevel().getBlockEntity(menu.position())
                instanceof com.example.mobilecellnetwork.blockentity.ServerBlockEntity serverBlock
                && serverBlock.getManagedNetworks().isEmpty();
        return saved.getNetworks().entrySet().stream()
                .filter(entry -> !ownerOnly || player.getUUID().equals(entry.getValue().ownerUUID()))
                .filter(entry -> !saved.getNetworkSettings(entry.getKey()).isServerRemoved() || canRestore)
                .map(entry -> {
                    var settings = saved.getNetworkSettings(entry.getKey());
                    String serverState = !settings.hasServerBlock() && canRestore ? "Restore"
                            : !settings.hasServerBlock() ? "No server"
                            : CoverageEngine.isServerOnline(player.server, settings) ? "Online" : "Offline";
                    return String.join("|", "network", entry.getKey(), clean(entry.getValue().name()),
                        Integer.toString(entry.getValue().towers().size()),
                        entry.getValue().allowed() ? "allowed" : "not allowed",
                        clean(entry.getValue().ownerName()), entry.getValue().defaultRoamingPolicy().name(),
                        serverState, Boolean.toString(!settings.getPasswordHash().isBlank()),
                        settings.getServerBlockDimension(), Long.toString(settings.getServerBlockPos()),
                        Long.toString(saved.getRevenueBalance(entry.getKey())),
                        Boolean.toString(settings.isAllowRoaming()));
                })
                .limit(200).toList();
    }

    private static List<String> roamingRows(ServerPlayer player, boolean ownerOnly) {
        NetworkSavedData saved = NetworkSavedData.get(player.server.overworld());
        List<String> rows = new ArrayList<>();
        java.util.Set<String> proposalIds = new java.util.HashSet<>();
        saved.getNetworks().entrySet().forEach(entry -> {
            if (ownerOnly && !player.getUUID().equals(entry.getValue().ownerUUID())) {
                return;
            }
            var settings = saved.getNetworkSettings(entry.getKey());
            if (settings.isServerRemoved()) {
                return;
            }
            String agreements = settings.getAgreements().keySet().stream()
                    .sorted().map(id -> id + "=" + java.util.Optional.ofNullable(saved.getNetwork(id))
                            .map(NetworkSavedData.NetworkRecord::name).map(ServerAdminHandler::clean).orElse(id))
                    .collect(java.util.stream.Collectors.joining(";"));
            String agreementIds = settings.getAgreements().keySet().stream().sorted()
                    .collect(java.util.stream.Collectors.joining(","));
            String networkOptions = saved.getNetworks().entrySet().stream()
                    .map(option -> option.getKey() + "=" + clean(option.getValue().name()))
                    .collect(java.util.stream.Collectors.joining(";"));
            rows.add(String.join("|", "roaming", entry.getKey(), clean(entry.getValue().name()),
                    Boolean.toString(settings.isAllowRoaming()),
                    Long.toString(settings.getDefaultRatePerMinute()),
                    Long.toString(settings.getDefaultRatePerText()),
                    Long.toString(settings.getDefaultRatePerMb()),
                    Boolean.toString(settings.isAutoSendRoamingRequests()),
                    Long.toString(saved.getRevenueThisCycle(entry.getKey())), agreements,
                    networkOptions, agreementIds,
                    Long.toString(saved.getCycleStartDay(entry.getKey()))));
            settings.getIncomingProposals().values().forEach(proposal ->
                    {
                        if (proposalIds.add(proposal.proposalId())) {
                            rows.add(roamingProposalRow(saved, proposal, "INCOMING"));
                        }
                    });
            settings.getAgreements().forEach((otherId, agreement) -> {
                if (ownerOnly || entry.getKey().compareTo(otherId) < 0) {
                    NetworkSavedData.NetworkRecord other = saved.getNetwork(otherId);
                    rows.add(String.join("|", "agreement", entry.getKey(),
                            clean(entry.getValue().name()), clean(other == null ? otherId : other.name()),
                            Long.toString(agreement.ratePerMinute()), Long.toString(agreement.ratePerText()),
                            Long.toString(agreement.ratePerMb()), Long.toString(agreement.agreedAtEpochMillis()),
                            otherId));
                }
            });
        });
        return rows.stream().limit(200).toList();
    }

    private static String roamingProposalRow(NetworkSavedData saved,
                                             com.example.mobilecellnetwork.data.RoamingProposal proposal,
                                             String direction) {
        String from = java.util.Optional.ofNullable(saved.getNetwork(proposal.fromNetworkId()))
                .map(NetworkSavedData.NetworkRecord::name).orElse(proposal.fromNetworkId());
        String to = java.util.Optional.ofNullable(saved.getNetwork(proposal.toNetworkId()))
                .map(NetworkSavedData.NetworkRecord::name).orElse(proposal.toNetworkId());
        return String.join("|", "proposal", proposal.proposalId(), clean(from), clean(to),
                Long.toString(proposal.ratePerMinute()), Long.toString(proposal.ratePerText()),
                Long.toString(proposal.ratePerMb()), proposal.state().name(), clean(proposal.lastMessage()),
                proposal.fromNetworkId(), proposal.toNetworkId(), direction);
    }

    private static List<String> antennaRows(ServerPlayer player) {
        NetworkSavedData saved = NetworkSavedData.get(player.server.overworld());
        Map<String, AntennaBlockEntity> loadedByKey = new java.util.HashMap<>();
        AntennaBlockEntity.loadedAntennas(player.server).forEach(antenna -> loadedByKey.put(
                antenna.getLevel().dimension().location() + "@" + antenna.getBlockPos().asLong(), antenna));
        return saved.getAntennaNetworks().entrySet().stream().map(entry -> {
            AntennaBlockEntity antenna = loadedByKey.get(entry.getKey());
            int delimiter = entry.getKey().lastIndexOf('@');
            String dimension = delimiter < 0 ? "minecraft:overworld" : entry.getKey().substring(0, delimiter);
            net.minecraft.core.BlockPos position;
            try {
                position = antenna == null
                        ? net.minecraft.core.BlockPos.of(Long.parseLong(entry.getKey().substring(delimiter + 1)))
                        : antenna.getBlockPos();
            } catch (NumberFormatException exception) {
                position = net.minecraft.core.BlockPos.ZERO;
            }
            String networkId = antenna == null ? entry.getValue() : antenna.getNetworkId();
            String networkName = "default".equals(networkId) ? "Unassigned"
                    : java.util.Optional.ofNullable(saved.getNetwork(networkId))
                    .map(NetworkSavedData.NetworkRecord::name).orElse("Unassigned");
            String tier = antenna == null ? "Antenna" : antenna.getTier().name();
            String enabled = antenna != null && antenna.isEnabled() ? "true" : "false";
            String power = antenna == null ? "Unloaded" : antenna.getPowerState().name();
            String energy = antenna == null ? "0" : Integer.toString(antenna.getEnergyStored());
            String capacity = antenna == null ? "0" : Integer.toString(antenna.getMaxEnergyStored());
            String users = antenna == null ? "0" : Integer.toString(antenna.getUserSet().size());
            String loaded = antenna == null ? "Unloaded" : "Loaded";
            String networkOptions = saved.getNetworks().entrySet().stream()
                    .map(network -> network.getKey() + "=" + clean(network.getValue().name()))
                    .collect(java.util.stream.Collectors.joining(";"));
            return String.join("|", "antenna", dimension, Integer.toString(position.getX()),
                    Integer.toString(position.getY()), Integer.toString(position.getZ()), tier,
                    clean(networkName), networkId, enabled, power, energy, capacity, users, loaded,
                    clean(antenna == null ? "Unknown" : antenna.getOwnerName()),
                    antenna == null ? "INHERIT" : antenna.getRoamingPolicy().name(),
                    antenna == null ? "" : antenna.getRoamingWhitelist().stream()
                            .map(java.util.UUID::toString).collect(java.util.stream.Collectors.joining(",")),
                    antenna == null ? "" : String.join(",", antenna.getRoamingNetworkWhitelist()),
                    networkOptions,
                    antenna == null ? "" : antenna.getSharedWithNetworkIds().stream()
                            .sorted().collect(java.util.stream.Collectors.joining(",")));
        }).limit(200).toList();
    }

    private static List<String> playerRows(ServerPlayer requestingPlayer) {
        return requestingPlayer.server.getPlayerList().getPlayers().stream()
                .map(player -> {
                    String planId = player.getData(ModCapabilities.PLAN_USAGE).getActivePlanId();
                    PlanRegistry.Plan plan = PlanRegistry.findPlan(planId);
                    return String.join("|", "player", clean(player.getGameProfile().getName()),
                            player.getData(ModCapabilities.PHONE_DATA).getPhoneNumber(), planId,
                            clean(plan == null ? planId : plan.displayName()));
                })
                .limit(200).toList();
    }

    private static List<String> planRows(ServerPlayer player) {
        NetworkSavedData saved = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() == null
                ? null : NetworkSavedData.get(net.neoforged.neoforge.server.ServerLifecycleHooks
                .getCurrentServer().overworld());
        List<String> rows = new ArrayList<>(PlanRegistry.getAllPlans().stream()
                .filter(plan -> ownsNetwork(player, saved, plan.carrierNetworkId()))
                .map(plan -> String.join("|", "plan", plan.id(), clean(plan.displayName()),
                        Long.toString(plan.price()), plan.maxSignalTier().name(),
                        Integer.toString(plan.billingCycleDays()), Integer.toString(plan.callMinutes()),
                        Integer.toString(plan.textMessages()), Long.toString(plan.dataAllowanceMb()),
                        Boolean.toString(plan.allowRoaming()), Long.toString(plan.roamingFeePerMinute()),
                        Long.toString(plan.roamingFeePerMb()), clean(plan.description()), plan.dataSpeed().name(),
                        Boolean.toString(plan.autoRenew()), Boolean.toString(plan.isDefault()),
                        String.join(",", plan.allowedNetworks()), "", plan.carrierNetworkId()))
                .toList());
        if (saved != null) {
            saved.getNetworks().forEach((id, network) -> {
                if (ownsNetwork(player, saved, id)) {
                    rows.add(String.join("|", "plan_network", id, clean(network.name())));
                }
            });
        }
        if (rows.size() > 200) {
            List<String> limited = new ArrayList<>(rows.subList(0, 199));
            limited.add("plan_warning|Only the first 199 plans and networks are shown.");
            return limited;
        }
        return rows;
    }

    private static List<String> billingRows(ServerPlayer player) {
        List<String> rows = new ArrayList<>();
        player.server.getPlayerList().getPlayers().forEach(target ->
                target.getData(ModCapabilities.BANK_ACCOUNT).getTransactionHistory().stream()
                        .filter(transaction -> transaction.reason().startsWith("Plan "))
                        .sorted(java.util.Comparator.comparingLong(
                                com.example.mobilecellnetwork.capabilities.BankAccount.Transaction::timestamp)
                                .reversed())
                        .limit(10)
                        .forEach(transaction -> rows.add(String.join("|", "billing",
                                clean(target.getGameProfile().getName()), clean(transaction.reason()),
                                Long.toString(transaction.amount()), Long.toString(transaction.timestamp())))));
        return rows.stream().limit(200).toList();
    }

    private static String clean(String value) {
        return value.replace('|', '/');
    }

    private static boolean ownsNetwork(ServerPlayer player, NetworkSavedData saved, String networkId) {
        return saved != null && networkId != null && saved.getNetwork(networkId) != null
                && player.getUUID().equals(saved.getNetwork(networkId).ownerUUID());
    }

    private static boolean networkAction(ServerPlayer player, ServerAdminActionPacket packet) {
        NetworkSavedData data = NetworkSavedData.get(player.serverLevel().getServer().overworld());
        if (!"create".equals(packet.action())) {
            String id = data.findNetworkId(packet.target());
            if (!ownsNetwork(player, data, id)) {
                message(player, "You can only manage networks you own.");
                return false;
            }
            if ("restore".equals(packet.action())) {
                ServerMenu menu = (ServerMenu) player.containerMenu;
                if (!ServerBlockPlaceHandler.restore(player, menu.position(), id)) {
                    message(player, "Unable to restore that network to this server.");
                    return false;
                }
                message(player, "Network restored to this server.");
                return true;
            }
        }
        if ("alert".equals(packet.action())) {
            networkAlert(player, packet);
            return true;
        }
        if ("setup".equals(packet.action())) {
            String networkId = data.findNetworkId(packet.target());
            NetworkSavedData.NetworkRecord network = networkId == null ? null : data.getNetwork(networkId);
            String[] fields = packet.value().split("\\|", 2);
            int minimum = MobileCellNetworkConfig.MINIMUM_NETWORK_PASSWORD_LENGTH.get();
            if (network == null || (!player.hasPermissions(2)
                    && !player.getUUID().equals(network.ownerUUID()))) {
                message(player, "You may only set up a network you own.");
                return false;
            }
            if (fields.length != 2 || fields[0].isBlank() || fields[0].length() > 64
                    || fields[0].contains("|") || fields[1].length() < minimum || fields[1].length() > 128) {
                message(player, "Enter a network name and a password between " + minimum + " and 128 characters.");
                return false;
            }
            if (!data.renameNetwork(networkId, fields[0])) {
                message(player, "That network name is invalid or already in use.");
                return false;
            }
            var settings = data.getNetworkSettings(networkId);
            String salt = PasswordUtil.generateSalt();
            settings.setPasswordSalt(salt);
            settings.setPasswordHash(PasswordUtil.hash(fields[1], salt));
            data.setDirty();
            message(player, "Network name and password saved.");
            return true;
        }
        if ("allow".equals(packet.action()) || "disallow".equals(packet.action())) {
            String networkId = data.findNetworkId(packet.target());
            if (networkId == null) {
                message(player, "Unknown network.");
                return false;
            }
            return data.setNetworkAllowed(networkId, "allow".equals(packet.action()));
        }
        if ("owner".equals(packet.action()) || "roaming".equals(packet.action())) {
            String networkId = data.findNetworkId(packet.target());
            if (networkId == null) {
                message(player, "Unknown network.");
                return false;
            }
            if ("owner".equals(packet.action())) {
                Owner owner = resolveOwner(player, packet.value());
                return owner != null && data.setNetworkOwner(networkId, owner.uuid(), owner.name());
            }
            try {
                String policyName = packet.value().split("\\|", -1)[0];
                return data.setNetworkDefaultRoamingPolicy(networkId,
                        com.example.mobilecellnetwork.blockentity.AntennaRoamingPolicy
                                .valueOf(policyName.trim().toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                message(player, "Unknown roaming policy.");
                return false;
            }
        }
        if ("setPassword".equals(packet.action())) {
            String networkId = data.findNetworkId(packet.target());
            if (networkId == null) {
                message(player, "Unknown network.");
                return false;
            }
            if (!ServerBlockPasswordHandler.changePassword(player, networkId, packet.value())) {
                message(player, "Password must be at least "
                        + MobileCellNetworkConfig.MINIMUM_NETWORK_PASSWORD_LENGTH.get()
                        + " and at most 128 characters; only the network owner or an operator may change it.");
                return false;
            }
            message(player, "Network password updated.");
            return true;
        }
        if ("create".equals(packet.action())) {
            String name = packet.target().trim();
            if (name.isEmpty() || name.length() > 64 || data.findNetworkId(name) != null) {
                message(player, "Invalid or duplicate network name.");
                return false;
            }
            data.putNetwork(UUID.randomUUID().toString(),
                    new NetworkSavedData.NetworkRecord(name, true, List.of(), player.getUUID(),
                            player.getGameProfile().getName(),
                            com.example.mobilecellnetwork.config.MobileCellNetworkConfig
                                    .DEFAULT_NETWORK_ROAMING_POLICY.get()));
            return true;
        }
        if ("delete".equals(packet.action())) {
            String id = data.findNetworkId(packet.target());
            if (id == null) {
                message(player, "Unknown network.");
                return false;
            }
            if (!"delete".equalsIgnoreCase(packet.value())) {
                message(player, "Type delete in the confirmation field to remove this network.");
                return false;
            }
            List<AntennaBlockEntity> orphaned = AntennaBlockEntity.loadedAntennas(player.server)
                    .stream().filter(antenna -> antenna.getNetworkId().equals(id)).toList();
            orphaned.forEach(antenna -> antenna.setNetworkId("default"));
            long registered = data.getAntennaNetworks().values().stream().filter(id::equals).count();
            if (registered > 0) {
                message(player, "Warning: orphaned " + registered + " registered antenna(s).");
            }
            return data.deleteNetwork(id);
        }
        return false;
    }

    private static boolean roamingAction(ServerPlayer player, ServerAdminActionPacket packet) {
        NetworkSavedData saved = NetworkSavedData.get(player.server.overworld());
        String networkId = saved.findNetworkId(packet.target());
        if (networkId == null) {
            message(player, "Unknown network.");
            return false;
        }
        NetworkSavedData.NetworkRecord network = saved.getNetwork(networkId);
        if (network == null || !player.getUUID().equals(network.ownerUUID())) {
            message(player, "You can only manage roaming settings for networks you own.");
            return false;
        }
        if ("resetRevenue".equals(packet.action())) {
            if (!player.hasPermissions(2)) {
                message(player, "Only an operator can reset roaming revenue.");
                return false;
            }
            if (!"DELETE".equalsIgnoreCase(packet.value().trim())) {
                message(player, "Type DELETE to confirm resetting this network's roaming revenue.");
                return false;
            }
            saved.resetRevenueBalance(networkId);
            message(player, "Roaming revenue balance reset.");
            return true;
        }
        if ("propose".equals(packet.action())) {
            var settings = saved.getNetworkSettings(networkId);
            if (!RoamingManager.propose(player, networkId, packet.value(),
                    settings.getDefaultRatePerMinute(), settings.getDefaultRatePerText(),
                    settings.getDefaultRatePerMb())) {
                message(player, "Could not send roaming proposal.");
                return false;
            }
            message(player, "Roaming proposal sent.");
            return true;
        }
        if (!"saveSettings".equals(packet.action())) {
            return false;
        }
        String[] fields = packet.value().split("\\|", -1);
        if (fields.length != 5
                || !(fields[0].equals("allow") || fields[0].equals("disallow"))
                || !(fields[4].equals("auto") || fields[4].equals("manual"))) {
            message(player, "Roaming settings are incomplete or invalid.");
            return false;
        }
        try {
            long minuteRate = Long.parseLong(fields[1]);
            long textRate = Long.parseLong(fields[2]);
            long mbRate = Long.parseLong(fields[3]);
            if (minuteRate < 0L || textRate < 0L || mbRate < 0L) {
                message(player, "Roaming rates must be non-negative.");
                return false;
            }
            var settings = saved.getNetworkSettings(networkId);
            settings.setAllowRoaming(fields[0].equals("allow"));
            settings.setDefaultRates(minuteRate, textRate, mbRate);
            settings.setAutoSendRoamingRequests(fields[4].equals("auto"));
            saved.setDirty();
            message(player, "Roaming settings saved.");
            return true;
        } catch (NumberFormatException exception) {
            message(player, "Enter whole-number roaming rates.");
            return false;
        }
    }

    private static void networkAlert(ServerPlayer player, ServerAdminActionPacket packet) {
        NetworkSavedData data = NetworkSavedData.get(player.serverLevel().getServer().overworld());
        NetworkSavedData.NetworkRecord network = data.getNetwork(packet.target());
        if (network == null) {
            message(player, "Unknown network.");
            return;
        }
        if (!player.hasPermissions(2) && !player.getUUID().equals(network.ownerUUID())) {
            message(player, "You can only send alerts for networks you own.");
            return;
        }
        String[] fields = packet.value().split("\\n", 2);
        if (fields.length != 2) {
            message(player, "Choose a recipient and enter an alert message.");
            return;
        }
        String recipient = fields[0].trim();
        String body = fields[1].trim();
        if (body.isEmpty() || body.length() > MobileCellNetworkConfig.SMS_MAX_LENGTH.get()) {
            message(player, "Alert message must be 1-"
                    + MobileCellNetworkConfig.SMS_MAX_LENGTH.get() + " characters.");
            return;
        }
        java.util.Set<UUID> recipients = new java.util.LinkedHashSet<>();
        if ("all".equalsIgnoreCase(recipient)) {
            data.getIssuedNumbers().keySet().stream()
                    .filter(id -> isNetworkSubscriber(player, data, id, packet.target()))
                    .forEach(recipients::add);
        } else {
            UUID recipientId = data.getUUIDForNumber(recipient);
            if (recipientId == null) {
                ServerPlayer onlineRecipient = player.server.getPlayerList().getPlayerByName(recipient);
                if (onlineRecipient != null) {
                    recipientId = onlineRecipient.getUUID();
                } else {
                    recipientId = player.server.getProfileCache().get(recipient)
                            .map(com.mojang.authlib.GameProfile::getId).orElse(null);
                }
            }
            if (recipientId == null || data.getNumberFor(recipientId) == null
                    || !isNetworkSubscriber(player, data, recipientId, packet.target())) {
                message(player, "Recipient is not a subscriber to this network.");
                return;
            }
            recipients.add(recipientId);
        }
        if (recipients.isEmpty()) {
            message(player, "No phone users are subscribed to this network.");
            return;
        }
        int delivered = NetworkHandler.deliverPrivilegedAlert(
                player, data, recipients, network.name(), body);
        message(player, "Network alert sent to " + delivered + " phone user(s).");
    }

    private static boolean isNetworkSubscriber(ServerPlayer source, NetworkSavedData data,
                                               UUID playerId, String networkId) {
        ServerPlayer online = source.server.getPlayerList().getPlayer(playerId);
        String planId = online == null ? data.getPlanAssignment(playerId)
                : online.getData(ModCapabilities.PLAN_USAGE).getActivePlanId();
        PlanRegistry.Plan plan = planId == null || planId.isBlank()
                ? PlanRegistry.getDefaultPlan() : PlanRegistry.getPlan(planId);
        return plan.allowedNetworks().isEmpty() || plan.allowedNetworks().contains(networkId);
    }

    private static boolean antennaAction(ServerPlayer player, ServerAdminActionPacket packet) {
        if ("assign".equals(packet.action()) || "owner".equals(packet.action())
                || "roaming".equals(packet.action())) {
            message(player, "Configure antenna networks and roaming agreements at the antenna itself.");
            return false;
        }
        String[] target = packet.target().split("\\|", 2);
        net.minecraft.server.level.ServerLevel antennaLevel = player.serverLevel();
        String positionText = packet.target();
        if (target.length == 2) {
            net.minecraft.resources.ResourceLocation dimension = net.minecraft.resources.ResourceLocation.tryParse(target[0]);
            if (dimension == null) {
                message(player, "Invalid antenna dimension.");
                return false;
            }
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimensionKey =
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                            dimension);
            antennaLevel = player.server.getLevel(dimensionKey);
            positionText = target[1];
        }
        BlockPos position = parsePosition(positionText);
        if (antennaLevel == null || position == null || !antennaLevel.hasChunkAt(position)
                || !(antennaLevel.getBlockEntity(position) instanceof AntennaBlockEntity antenna)) {
            message(player, "No loaded antenna at that position.");
            return false;
        }
        if ("claim".equals(packet.action())) {
            String[] credentials = packet.value().split("\\|", 2);
            if (credentials.length != 2 || credentials[0].isBlank()) {
                message(player, "Choose a network and enter its password.");
                return false;
            }
            AntennaClaimHandler.ClaimResult result = AntennaClaimHandler.claim(
                    player, position, credentials[0], credentials[1]);
            if (result != AntennaClaimHandler.ClaimResult.SUCCESS) {
                message(player, switch (result) {
                    case WRONG_PASSWORD -> "Incorrect network password.";
                    case NETWORK_NOT_FOUND -> "Unknown network.";
                    case ANTENNA_ALREADY_CLAIMED -> "Antenna is already claimed by another network.";
                    case ANTENNA_NOT_FOUND -> "Antenna not found.";
                    case SUCCESS -> "Antenna claimed.";
                });
                return false;
            }
            message(player, "Antenna claimed.");
            return true;
        }
        if ("unclaim".equals(packet.action())) {
            AntennaClaimHandler.UnclaimResult result = AntennaClaimHandler.unclaim(player, position);
            if (result != AntennaClaimHandler.UnclaimResult.SUCCESS) {
                message(player, result == AntennaClaimHandler.UnclaimResult.NOT_OWNER
                        ? "Only the network owner may unclaim this antenna." : "Antenna not found.");
                return false;
            }
            message(player, "Antenna unclaimed.");
            return true;
        }
        if ("share".equals(packet.action())) {
            boolean shared = AntennaShareHandler.addShare(player, position, packet.value());
            if (!shared) {
                message(player, "Unable to share antenna with that network.");
                return false;
            }
            message(player, "Antenna shared with network.");
            return true;
        }
        if ("unshare".equals(packet.action())) {
            boolean unshared = AntennaShareHandler.removeShare(player, position, packet.value());
            if (!unshared) {
                message(player, "Unable to remove antenna network share.");
                return false;
            }
            message(player, "Antenna network share removed.");
            return true;
        }
        switch (packet.action()) {
            case "enable" -> antenna.setEnabled(true);
            case "disable" -> antenna.setEnabled(false);
            case "delete" -> {
                if (!"DELETE".equalsIgnoreCase(packet.value().trim())) {
                    message(player, "Type DELETE to confirm removing this antenna.");
                    return false;
                }
                NetworkSavedData.get(player.serverLevel().getServer().overworld()).unregisterAntenna(
                        antennaLevel.dimension().location() + "@" + position.asLong());
                antennaLevel.destroyBlock(position, false);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private static Owner resolveOwner(ServerPlayer requester, String value) {
        if ("server".equalsIgnoreCase(value.trim())) {
            return new Owner(null, "Server");
        }
        ServerPlayer owner = requester.server.getPlayerList().getPlayers().stream()
                .filter(candidate -> candidate.getGameProfile().getName().equalsIgnoreCase(value.trim()))
                .findFirst().orElse(null);
        if (owner == null) {
            message(requester, "Owner must be an online player name or Server.");
            return null;
        }
        return new Owner(owner.getUUID(), owner.getGameProfile().getName());
    }

    private record Owner(java.util.UUID uuid, String name) { }

    private static BlockPos parsePosition(String input) {
        String[] coordinates = input.trim().split("\\s+");
        if (coordinates.length != 3) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(coordinates[0]),
                    Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean playerAction(ServerPlayer player, ServerAdminActionPacket packet) {
        ServerPlayer target = player.server.getPlayerList().getPlayerByName(packet.target());
        if (target == null) {
            message(player, "Player is not online.");
            return false;
        }
        if ("number".equals(packet.action())) {
            NetworkSavedData data = NetworkSavedData.get(player.serverLevel().getServer().overworld());
            if (!packet.value().matches("[0-9]{1,9}") || !data.registerNumber(target.getUUID(), packet.value())) {
                message(player, "Invalid number or number already in use.");
                return false;
            }
            target.getData(ModCapabilities.PHONE_DATA).setPhoneNumber(packet.value());
            NetworkHandler.syncPhoneData(target);
            return true;
        }
        if ("plan".equals(packet.action())) {
            return assignPlan(player, target, packet.value());
        }
        return false;
    }

    private static boolean assignPlan(ServerPlayer actor, ServerPlayer target, String planId) {
        PlanRegistry.Plan plan = PlanRegistry.findPlan(planId);
        if (plan == null) {
            message(actor, "Unknown plan.");
            return false;
        }
        var usage = target.getData(ModCapabilities.PLAN_USAGE);
        PlanRegistry.Plan previous = usage.getActivePlanId().isBlank()
                ? null : PlanRegistry.findPlan(usage.getActivePlanId());
        if (previous == null || !previous.id().equals(plan.id())) {
            com.example.mobilecellnetwork.billing.BillingManager.resetPlanRevenueCycles(target, previous, plan);
        }
        if (PlanRegistry.isDowngrade(previous == null ? PlanRegistry.getDefaultPlan() : previous, plan)) {
            com.example.mobilecellnetwork.voice.CallManager.hangUp(target.getUUID());
        }
        long day = Math.floorDiv(target.serverLevel().getDayTime(), 24_000L);
        usage.setActivePlanId(plan.id());
        usage.resetDailyCounters(day);
        usage.setCycleStartDay(day);
        usage.setCycleActive(true);
        target.getData(ModCapabilities.PHONE_DATA).setLastKnownPlanId(plan.id());
        NetworkSavedData.get(target.serverLevel().getServer().overworld())
                .setPlanAssignment(target.getUUID(), plan.id());
        NetworkHandler.syncPhoneData(target);
        NetworkHandler.syncPlanData(target);
        return true;
    }

    private static boolean planAction(ServerPlayer player, ServerAdminActionPacket packet) {
        NetworkSavedData savedData = NetworkSavedData.get(player.server.overworld());
        PlanRegistry.Plan existing = PlanRegistry.findPlan(packet.target());
        if (existing != null && !ownsNetwork(player, savedData, existing.carrierNetworkId())) {
            message(player, "You can only manage plans belonging to networks you own.");
            return false;
        }
        if ("delete".equals(packet.action())) {
            if (!"DELETE".equalsIgnoreCase(packet.value().trim())) {
                player.sendSystemMessage(Component.literal("Type DELETE in the confirmation dialog to remove this plan."));
                return false;
            }
            PlanRegistry.Plan fallback = PlanRegistry.getDefaultPlan();
            List<ServerPlayer> assigned = player.server.getPlayerList().getPlayers().stream()
                    .filter(target -> target.getData(ModCapabilities.PLAN_USAGE).getActivePlanId()
                            .equals(packet.target()))
                    .toList();
            boolean removed = PlanRegistry.deleteAdminPlan(packet.target());
            if (!removed) {
                player.sendSystemMessage(Component.literal("Cannot delete unknown or default plan."));
                return false;
            }
            assigned.forEach(target -> com.example.mobilecellnetwork.voice.CallManager.hangUp(target.getUUID()));
            NetworkSavedData saved = NetworkSavedData.get(player.serverLevel().getServer().overworld());
            saved.deletePlan(packet.target());
            saved.getPlanAssignments().forEach((playerId, assignedPlan) -> {
                if (assignedPlan.equals(packet.target())) {
                    saved.setPlanAssignment(playerId, fallback.id());
                }
            });
            for (ServerPlayer target : assigned) {
                assignPlan(player, target, fallback.id());
            }
            player.server.getPlayerList().getPlayers().forEach(NetworkHandler::syncPlanData);
            player.sendSystemMessage(Component.literal("Plan deleted: " + packet.target()));
            return true;
        }
        if (!"upsert".equals(packet.action()) && !"create".equals(packet.action())) {
            return false;
        }
        String[] fields = packet.value().split("\\|", -1);
        if (fields.length != 16 || fields[0].isBlank()) {
            player.sendSystemMessage(Component.literal("Plan editor fields are incomplete."));
            return false;
        }
        try {
            if (!packet.target().matches("[a-z0-9_.-]+")) {
                throw new IllegalArgumentException("Invalid plan ID.");
            }
            PlanRegistry.Plan previous = PlanRegistry.findPlan(packet.target());
            if ("create".equals(packet.action()) && previous != null) {
                player.sendSystemMessage(Component.literal("Plan ID already exists."));
                return false;
            }
            if ("upsert".equals(packet.action()) && previous == null) {
                player.sendSystemMessage(Component.literal("The plan no longer exists. Refresh and try again."));
                return false;
            }
            long price = Long.parseLong(fields[2]);
            int cycleDays = Integer.parseInt(fields[3]);
            int callMinutes = Integer.parseInt(fields[4]);
            int texts = Integer.parseInt(fields[5]);
            long dataMb = Long.parseLong(fields[6]);
            PlanRegistry.DataSpeed speed = PlanRegistry.DataSpeed.valueOf(
                    fields[7].trim().toUpperCase(java.util.Locale.ROOT));
            SignalTier tier = SignalTier.valueOf(fields[8].trim().toUpperCase(java.util.Locale.ROOT));
            boolean allowRoaming = parseBoolean(fields[9]);
            long roamingFeePerMinute = Long.parseLong(fields[10]);
            long roamingFeePerMb = Long.parseLong(fields[11]);
            boolean autoRenew = parseBoolean(fields[12]);
            boolean isDefault = parseBoolean(fields[13]);
            List<String> allowedNetworks = fields[14].isBlank() ? List.of()
                    : java.util.Arrays.stream(fields[14].split(",")).map(String::trim)
                    .filter(value -> !value.isEmpty()).distinct().toList();
            var saved = NetworkSavedData.get(player.serverLevel().getServer().overworld());
            String carrierNetworkId = fields[15].trim();
            if (carrierNetworkId.isBlank()) {
                player.sendSystemMessage(Component.literal("Select a carrier network for this plan."));
                return false;
            }
            if (!ownsNetwork(player, saved, carrierNetworkId)) {
                message(player, "Select a carrier network you own.");
                return false;
            }
            if (isDefault != (previous != null && previous.isDefault())) {
                message(player, "The server-wide default plan cannot be changed here.");
                return false;
            }
            if (allowedNetworks.stream().anyMatch(id -> saved.getNetwork(id) == null)
                    || (!carrierNetworkId.isEmpty() && saved.getNetwork(carrierNetworkId) == null)) {
                player.sendSystemMessage(Component.literal("The carrier or an allowed network no longer exists."));
                return false;
            }
            if (fields[0].isBlank() || price < 0 || cycleDays < 1 || callMinutes < -1 || texts < -1
                    || dataMb < -1 || roamingFeePerMinute < 0 || roamingFeePerMb < 0) {
                throw new IllegalArgumentException("Plan values are outside their allowed range.");
            }
            PlanRegistry.Plan plan = new PlanRegistry.Plan(packet.target(), fields[0].trim(),
                    fields[1], price, cycleDays, dataMb, speed,
                    tier, callMinutes, texts, allowedNetworks, autoRenew, isDefault,
                    allowRoaming, roamingFeePerMinute, roamingFeePerMb, carrierNetworkId,
                    previous == null ? PlanRegistry.RoamingMode.CHARGED : previous.roamingMode(),
                    previous != null && previous.allowOverage(),
                    previous == null ? 0L : previous.overageRatePerMinute(),
                    previous == null ? 0L : previous.overageRatePerText(),
                    previous == null ? 0L : previous.overageRatePerMb());
            if (!PlanRegistry.putAdminPlan(plan)) {
                player.sendSystemMessage(Component.literal("The plan could not be saved."));
                return false;
            }
            if (previous != null && PlanRegistry.isDowngrade(previous, plan)) {
                player.server.getPlayerList().getPlayers().stream()
                        .filter(target -> target.getData(ModCapabilities.PLAN_USAGE).getActivePlanId()
                                .equals(plan.id()))
                        .forEach(target -> com.example.mobilecellnetwork.voice.CallManager.hangUp(target.getUUID()));
            }
            Map<String, String> attributes = Map.ofEntries(
                    Map.entry("description", plan.description()),
                    Map.entry("price", Long.toString(plan.price())),
                    Map.entry("billingCycleDays", Integer.toString(plan.billingCycleDays())),
                    Map.entry("dataAllowanceMb", Long.toString(plan.dataAllowanceMb())),
                    Map.entry("dataSpeed", plan.dataSpeed().name()),
                    Map.entry("maxSignalTier", plan.maxSignalTier().name()),
                    Map.entry("callMinutes", Integer.toString(plan.callMinutes())),
                    Map.entry("textMessages", Integer.toString(plan.textMessages())),
                    Map.entry("allowedNetworks", String.join(",", plan.allowedNetworks())),
                    Map.entry("autoRenew", Boolean.toString(plan.autoRenew())),
                    Map.entry("isDefault", Boolean.toString(plan.isDefault())),
                    Map.entry("allowRoaming", Boolean.toString(plan.allowRoaming())),
                    Map.entry("roamingFeePerMinute", Long.toString(plan.roamingFeePerMinute())),
                    Map.entry("roamingFeePerMb", Long.toString(plan.roamingFeePerMb())),
                    Map.entry("carrierNetworkId", plan.carrierNetworkId()),
                    Map.entry("roamingMode", plan.roamingMode().name()),
                    Map.entry("allowOverage", Boolean.toString(plan.allowOverage())),
                    Map.entry("overageRatePerMinute", Long.toString(plan.overageRatePerMinute())),
                    Map.entry("overageRatePerText", Long.toString(plan.overageRatePerText())),
                    Map.entry("overageRatePerMb", Long.toString(plan.overageRatePerMb())));
            NetworkSavedData.get(player.serverLevel().getServer().overworld())
                    .putPlan(plan.id(), new NetworkSavedData.PlanRecord(plan.displayName(), attributes));
            player.server.getPlayerList().getPlayers().forEach(NetworkHandler::syncPlanData);
            player.sendSystemMessage(Component.literal("Plan saved: " + plan.displayName()));
            return true;
        } catch (IllegalArgumentException exception) {
            player.sendSystemMessage(Component.literal("Invalid plan values: " + exception.getMessage()));
            return false;
        }
    }

    private static boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Expected true or false.");
    }

    private static boolean billingAction(ServerPlayer player, ServerAdminActionPacket packet) {
        if ("emergency".equals(packet.action())) {
            String body = packet.value().trim();
            int maxLength = MobileCellNetworkConfig.EMERGENCY_MESSAGE_MAX_LENGTH.get();
            if (body.isEmpty() || body.length() > maxLength) {
                message(player, "Emergency message must be 1-" + maxLength + " characters.");
                return false;
            }
            NetworkSavedData data = NetworkSavedData.get(player.serverLevel().getServer().overworld());
            BlockPos location = player.blockPosition();
            data.addEmergencyMessage(new NetworkSavedData.EmergencyMessage(
                    UUID.randomUUID(), player.getUUID(), player.getGameProfile().getName(), "DISPATCH",
                    body, player.serverLevel().dimension().location().toString(),
                    location.getX(), location.getY(), location.getZ(), System.currentTimeMillis()));
            player.server.getPlayerList().getPlayers().forEach(NetworkHandler::sendEmergencyDispatchData);
            int delivered = NetworkHandler.deliverPrivilegedAlert(player, data,
                    data.getIssuedNumbers().keySet(), "EMERGENCY", body);
            message(player, "Emergency alert sent to " + delivered + " phone user(s).");
            return true;
        }
        ServerPlayer target = player.server.getPlayerList().getPlayerByName(packet.target());
        if (target == null) {
            message(player, "Player is not online.");
            return false;
        }
        if ("force".equals(packet.action())) {
            return com.example.mobilecellnetwork.billing.BillingManager.processPlayer(target, true);
        }
        if ("history".equals(packet.action())) {
            message(player, "Billing transactions for " + target.getGameProfile().getName()
                    + " are listed in the Billing tab.");
            return true;
        }
        return false;
    }

    private static void message(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }
}

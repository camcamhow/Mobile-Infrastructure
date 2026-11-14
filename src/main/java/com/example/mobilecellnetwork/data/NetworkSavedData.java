package com.example.mobilecellnetwork.data;

import com.example.mobilecellnetwork.MobileCellNetwork;
import com.example.mobilecellnetwork.blockentity.AntennaRoamingPolicy;
import com.example.mobilecellnetwork.capabilities.BankAccount;
import com.example.mobilecellnetwork.capabilities.PhoneData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class NetworkSavedData extends SavedData {
    private static final String DATA_NAME = MobileCellNetwork.MOD_ID + "_network_data";
    public static final Factory<NetworkSavedData> FACTORY =
            new Factory<>(NetworkSavedData::new, NetworkSavedData::load, DataFixTypes.LEVEL);

    private final Map<UUID, String> issuedNumbers = new LinkedHashMap<>();
    private final Map<String, String> antennaNetworks = new LinkedHashMap<>();
    private final Map<String, NetworkRecord> networks = new LinkedHashMap<>();
    private final Map<String, NetworkSettings> networkSettings = new LinkedHashMap<>();
    private final Map<String, Long> revenueBalances = new LinkedHashMap<>();
    private final Map<String, Long> revenueThisCycle = new LinkedHashMap<>();
    private final Map<String, Long> revenueCycleStartDays = new LinkedHashMap<>();
    private final Map<String, List<BankAccount.Transaction>> networkRevenueTransactions = new LinkedHashMap<>();
    private final Map<String, PlanRecord> plans = new LinkedHashMap<>();
    private final Map<UUID, MapPointOfInterest> publicMapPoints = new LinkedHashMap<>();
    private final Map<UUID, String> planAssignments = new LinkedHashMap<>();
    private final List<SocialPost> socialPosts = new ArrayList<>();
    private final List<EmergencyMessage> emergencyMessages = new ArrayList<>();
    private final List<EmergencyMessage> archivedEmergencyMessages = new ArrayList<>();
    private final List<PendingCharge> pendingBillingCharges = new ArrayList<>();
    private final Map<UUID, List<PhoneData.Message>> pendingSms = new LinkedHashMap<>();
    private final Map<UUID, List<PendingCall>> pendingCalls = new LinkedHashMap<>();
    private final Map<String, Map<String, Long>> roamingAgreements = new LinkedHashMap<>();
    private final Map<UUID, List<PendingRoamingPayout>> pendingRoamingPayouts = new LinkedHashMap<>();
    private final java.util.Set<UUID> pendingBillingChecks = new java.util.LinkedHashSet<>();

    public static NetworkSavedData get(net.minecraft.server.level.ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void addEmergencyMessage(EmergencyMessage message) {
        emergencyMessages.add(message);
        while (emergencyMessages.size() > EmergencyMessages.MAX_STORED_MESSAGES) {
            emergencyMessages.remove(0);
        }
        setDirty();
    }

    public List<EmergencyMessage> getEmergencyMessages() {
        return emergencyMessages.stream()
                .sorted(java.util.Comparator.comparingLong(EmergencyMessage::timestamp).reversed())
                .toList();
    }

    public List<EmergencyMessage> getAllEmergencyMessages() {
        return java.util.stream.Stream.concat(emergencyMessages.stream(), archivedEmergencyMessages.stream())
                .sorted(java.util.Comparator.comparingLong(EmergencyMessage::timestamp).reversed())
                .toList();
    }

    public int archiveEmergencyMessages() {
        int count = emergencyMessages.size();
        if (count == 0) {
            return 0;
        }
        archivedEmergencyMessages.addAll(emergencyMessages);
        emergencyMessages.clear();
        while (archivedEmergencyMessages.size() > EmergencyMessages.MAX_STORED_MESSAGES) {
            archivedEmergencyMessages.remove(0);
        }
        setDirty();
        return count;
    }

    public String getNumberFor(UUID playerId) {
        return issuedNumbers.get(playerId);
    }

    public Map<UUID, String> getIssuedNumbers() {
        return Map.copyOf(issuedNumbers);
    }

    public UUID getUUIDForNumber(String number) {
        for (Map.Entry<UUID, String> entry : issuedNumbers.entrySet()) {
            if (entry.getValue().equals(number)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean isNumberTaken(String number) {
        return getUUIDForNumber(number) != null;
    }

    public boolean registerNumber(UUID playerId, String number) {
        if (number == null || !number.matches("[0-9]{1,9}")) {
            return false;
        }
        UUID currentOwner = getUUIDForNumber(number);
        if (currentOwner != null && !currentOwner.equals(playerId)) {
            return false;
        }
        String previousNumber = issuedNumbers.put(playerId, number);
        if (!number.equals(previousNumber)) {
            setDirty();
        }
        return true;
    }

    public void registerAntenna(String positionKey, String networkId) {
        String previousNetwork = antennaNetworks.put(positionKey, networkId);
        if (networkId.equals(previousNetwork)) {
            return;
        }
        if (previousNetwork != null) {
            removeTower(previousNetwork, positionKey);
        }
        NetworkRecord network = networks.get(networkId);
        if (network != null && !network.towers().contains(positionKey)) {
            List<String> towers = new ArrayList<>(network.towers());
            towers.add(positionKey);
            networks.put(networkId, network.withTowers(towers));
        }
        setDirty();
    }

    public void registerAntenna(BlockPos position, com.example.mobilecellnetwork.blockentity.AntennaBlockEntity antenna) {
        if (antenna.getLevel() == null) {
            return;
        }
        String key = antenna.getLevel().dimension().location() + "@" + position.asLong();
        registerAntenna(key, antenna.getNetworkId());
    }

    public void unregisterAntenna(net.minecraft.resources.ResourceLocation dimension, BlockPos position) {
        unregisterAntenna(dimension + "@" + position.asLong());
    }

    private void removeTower(String networkId, String positionKey) {
        NetworkRecord network = networks.get(networkId);
        if (network == null || !network.towers().contains(positionKey)) {
            return;
        }
        List<String> towers = new ArrayList<>(network.towers());
        towers.remove(positionKey);
        networks.put(networkId, network.withTowers(towers));
    }

    public Map<String, String> getAntennaNetworks() {
        return Map.copyOf(antennaNetworks);
    }

    public void unregisterAntenna(String positionKey) {
        String networkId = antennaNetworks.remove(positionKey);
        if (networkId != null) {
            removeTower(networkId, positionKey);
            setDirty();
        }
    }

    public void putNetwork(String id, NetworkRecord network) {
        networks.put(id, network);
        setDirty();
    }

    public boolean renameNetwork(String id, String name) {
        NetworkRecord network = networks.get(id);
        String updated = name == null ? "" : name.trim();
        if (network == null || updated.isEmpty() || updated.length() > 64 || updated.contains("|")
                || networks.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(id)
                && entry.getValue().name().equalsIgnoreCase(updated))) {
            return false;
        }
        networks.put(id, network.withName(updated));
        setDirty();
        return true;
    }

    public Map<String, NetworkRecord> getNetworks() {
        return Map.copyOf(networks);
    }

    public NetworkSettings getNetworkSettings(String networkId) {
        if (!networks.containsKey(networkId)) {
            return null;
        }
        return networkSettings.computeIfAbsent(networkId, ignored -> {
            setDirty();
            // Networks that predate server blocks stay online until a server block is removed.
            NetworkSettings created = new NetworkSettings();
            created.setServerOnline(true);
            return created;
        });
    }

    public NetworkRecord getNetwork(String id) {
        return networks.get(id);
    }

    public long getRevenueBalance(String networkId) {
        return revenueBalances.getOrDefault(networkId, 0L);
    }

    public long getNetworkRevenueBalance(String networkId) {
        return getRevenueBalance(networkId);
    }

    public long getRoamingRevenueThisCycle(String networkId) {
        return getRevenueThisCycle(networkId);
    }

    public long getRevenueThisCycle(String networkId) {
        return revenueThisCycle.getOrDefault(networkId, 0L);
    }

    public long getCycleStartDay(String networkId) {
        return revenueCycleStartDays.getOrDefault(networkId, 0L);
    }

    public Map<String, Long> getRevenueBalances() {
        return Map.copyOf(revenueBalances);
    }

    public void addRoamingRevenue(String networkId, long amount) {
        if (amount <= 0L || !networks.containsKey(networkId)) {
            return;
        }
        addRevenue(networkId, amount);
        addRevenueThisCycle(networkId, amount);
    }

    public void addRevenue(String networkId, long amount) {
        if (amount <= 0L || !networks.containsKey(networkId)) {
            return;
        }
        long current = revenueBalances.getOrDefault(networkId, 0L);
        revenueBalances.put(networkId, current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount);
        setDirty();
    }

    public void creditRevenue(String networkId, long amount, String reason) {
        if (amount <= 0L || !networks.containsKey(networkId)) {
            return;
        }
        addRevenue(networkId, amount);
        networkRevenueTransactions.computeIfAbsent(networkId, ignored -> new ArrayList<>())
                .add(new BankAccount.Transaction(amount, reason, System.currentTimeMillis()));
        setDirty();
    }

    public void addRevenueThisCycle(String networkId, long amount) {
        if (amount <= 0L || !networks.containsKey(networkId)) {
            return;
        }
        long current = revenueThisCycle.getOrDefault(networkId, 0L);
        revenueThisCycle.put(networkId,
                current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount);
        setDirty();
    }

    public void resetRevenueThisCycle(String networkId, long currentDay) {
        if (!networks.containsKey(networkId)) {
            return;
        }
        revenueThisCycle.put(networkId, 0L);
        revenueCycleStartDays.put(networkId, Math.max(0L, currentDay));
        setDirty();
    }

    public List<BankAccount.Transaction> getNetworkRevenueTransactions(String networkId) {
        return List.copyOf(networkRevenueTransactions.getOrDefault(networkId, List.of()));
    }

    public void resetRevenueBalance(String networkId) {
        if (revenueBalances.remove(networkId) != null) {
            setDirty();
        }
    }

    public void resetNetworkRevenueBalance(String networkId) {
        resetRevenueBalance(networkId);
    }

    public UUID getNetworkOwnerUUID(String id) {
        NetworkRecord network = networks.get(id);
        return network == null ? null : network.ownerUUID();
    }

    public Long getRoamingAgreementFee(String hostNetworkId, String visitingNetworkId) {
        return roamingAgreements.getOrDefault(hostNetworkId, Map.of()).get(visitingNetworkId);
    }

    public Map<String, Long> getRoamingAgreements(String networkId) {
        return Map.copyOf(roamingAgreements.getOrDefault(networkId, Map.of()));
    }

    public boolean setRoamingAgreement(String networkId, String otherNetworkId, long feePerMinute) {
        if (networks.get(networkId) == null || networks.get(otherNetworkId) == null
                || networkId.equals(otherNetworkId) || feePerMinute < 0L) {
            return false;
        }
        roamingAgreements.computeIfAbsent(networkId, ignored -> new LinkedHashMap<>())
                .put(otherNetworkId, feePerMinute);
        setDirty();
        return true;
    }

    public void removeRoamingAgreement(String networkId, String otherNetworkId) {
        Map<String, Long> agreements = roamingAgreements.get(networkId);
        if (agreements != null && agreements.remove(otherNetworkId) != null) {
            if (agreements.isEmpty()) {
                roamingAgreements.remove(networkId);
            }
            setDirty();
        }
    }

    public void addPendingRoamingPayout(UUID recipient, long amount, String reason) {
        if (recipient == null || amount <= 0L) {
            return;
        }
        pendingRoamingPayouts.computeIfAbsent(recipient, ignored -> new ArrayList<>())
                .add(new PendingRoamingPayout(amount, reason));
        setDirty();
    }

    public List<PendingRoamingPayout> takePendingRoamingPayouts(UUID recipient) {
        List<PendingRoamingPayout> payouts = pendingRoamingPayouts.remove(recipient);
        if (payouts == null) {
            return List.of();
        }
        setDirty();
        return List.copyOf(payouts);
    }

    public String getNetworkOwnerName(String id) {
        NetworkRecord network = networks.get(id);
        return network == null ? "Server" : network.ownerName();
    }

    public AntennaRoamingPolicy getNetworkDefaultRoamingPolicy(String id) {
        NetworkRecord network = networks.get(id);
        return network == null ? AntennaRoamingPolicy.DENY_ALL : network.defaultRoamingPolicy();
    }

    public boolean setNetworkOwner(String id, UUID ownerUUID, String ownerName) {
        NetworkRecord network = networks.get(id);
        if (network == null) {
            return false;
        }
        networks.put(id, network.withOwner(ownerUUID, ownerName));
        setDirty();
        return true;
    }

    public boolean setNetworkDefaultRoamingPolicy(String id, AntennaRoamingPolicy policy) {
        NetworkRecord network = networks.get(id);
        if (network == null) {
            return false;
        }
        networks.put(id, network.withDefaultRoamingPolicy(policy));
        setDirty();
        return true;
    }

    public boolean setNetworkAllowed(String id, boolean allowed) {
        NetworkRecord network = networks.get(id);
        if (network == null) {
            return false;
        }
        networks.put(id, network.withAllowed(allowed));
        setDirty();
        return true;
    }

    public boolean deleteNetwork(String id) {
        NetworkRecord removed = networks.remove(id);
        if (removed == null) {
            return false;
        }
        networkSettings.remove(id);
        revenueBalances.remove(id);
        networkSettings.values().forEach(settings -> {
            settings.getAgreements().remove(id);
            settings.getOutgoingProposals().remove(id);
            settings.getIncomingProposals().remove(id);
        });
        roamingAgreements.remove(id);
        roamingAgreements.values().forEach(agreements -> agreements.remove(id));
        roamingAgreements.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        java.util.Set<String> removedTowerKeys = new java.util.LinkedHashSet<>(removed.towers());
        antennaNetworks.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(id)) {
                removedTowerKeys.add(entry.getKey());
                return true;
            }
            return false;
        });
        for (Map.Entry<String, NetworkRecord> entry : new ArrayList<>(networks.entrySet())) {
            List<String> towers = entry.getValue().towers().stream()
                    .filter(positionKey -> !removedTowerKeys.contains(positionKey)
                            || !antennaNetworks.containsKey(positionKey))
                    .toList();
            if (towers.size() != entry.getValue().towers().size()) {
                networks.put(entry.getKey(), entry.getValue().withTowers(towers));
            }
        }
        planAssignments.values().removeIf(id::equals);
        setDirty();
        return true;
    }

    public List<MapPointOfInterest> getPublicMapPoints() {
        return List.copyOf(publicMapPoints.values());
    }

    public boolean addPublicMapPoint(MapPointOfInterest point) {
        if (point.visibility() != MapPointOfInterest.Visibility.PUBLIC
                || publicMapPoints.size() >= 512 || publicMapPoints.containsKey(point.id())) {
            return false;
        }
        publicMapPoints.put(point.id(), point);
        setDirty();
        return true;
    }

    public boolean removePublicMapPoint(UUID pointId, UUID ownerId) {
        MapPointOfInterest point = publicMapPoints.get(pointId);
        if (point == null || !java.util.Objects.equals(point.ownerUUID(), ownerId)) {
            return false;
        }
        publicMapPoints.remove(pointId);
        setDirty();
        return true;
    }

    public String findNetworkId(String nameOrId) {
        if (networks.containsKey(nameOrId)) {
            return nameOrId;
        }
        return networks.entrySet().stream()
                .filter(entry -> entry.getValue().name().equalsIgnoreCase(nameOrId))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    public void putPlan(String id, PlanRecord plan) {
        plans.put(id, plan);
        setDirty();
    }

    public void deletePlan(String id) {
        plans.put(id, new PlanRecord("", Map.of("deleted", "true")));
        setDirty();
    }

    public Map<String, PlanRecord> getPlans() {
        return Map.copyOf(plans);
    }

    public void clearPlanAssignment(UUID playerId) {
        if (planAssignments.remove(playerId) != null) {
            setDirty();
        }
    }

    public void setPlanAssignment(UUID playerId, String planId) {
        planAssignments.put(playerId, planId);
        setDirty();
    }

    public Map<UUID, String> getPlanAssignments() {
        return Map.copyOf(planAssignments);
    }

    public String getPlanAssignment(UUID playerId) {
        return planAssignments.get(playerId);
    }

    public void addSocialPost(SocialPost post) {
        socialPosts.add(post);
        while (socialPosts.size() > 500) {
            socialPosts.remove(0);
        }
        setDirty();
    }

    public List<SocialPost> getSocialPosts() {
        return List.copyOf(socialPosts);
    }

    public SocialPost getSocialPost(UUID postId) {
        return socialPosts.stream().filter(post -> post.id().equals(postId)).findFirst().orElse(null);
    }

    public void addPendingBillingCharge(PendingCharge charge) {
        pendingBillingCharges.add(charge);
        setDirty();
    }

    public List<PendingCharge> getPendingBillingCharges() {
        return List.copyOf(pendingBillingCharges);
    }

    public void addPendingSms(UUID recipient, PhoneData.Message message) {
        pendingSms.computeIfAbsent(recipient, ignored -> new ArrayList<>()).add(message);
        setDirty();
    }

    public List<PhoneData.Message> takePendingSms(UUID recipient) {
        List<PhoneData.Message> messages = pendingSms.remove(recipient);
        if (messages == null) {
            return List.of();
        }
        setDirty();
        return List.copyOf(messages);
    }

    public void addPendingCall(UUID recipient, PendingCall call) {
        pendingCalls.computeIfAbsent(recipient, ignored -> new ArrayList<>()).add(call);
        setDirty();
    }

    public List<PendingCall> takePendingCalls(UUID recipient) {
        List<PendingCall> calls = pendingCalls.remove(recipient);
        if (calls == null) {
            return List.of();
        }
        setDirty();
        return List.copyOf(calls);
    }

    public void queueBillingCheck(UUID playerId) {
        if (pendingBillingChecks.add(playerId)) {
            setDirty();
        }
    }

    public boolean takeBillingCheck(UUID playerId) {
        boolean removed = pendingBillingChecks.remove(playerId);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag numberList = new ListTag();
        issuedNumbers.forEach((playerId, number) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", playerId);
            entry.putString("number", number);
            numberList.add(entry);
        });
        tag.put("issuedNumbers", numberList);

        CompoundTag antennaTag = new CompoundTag();
        antennaNetworks.forEach(antennaTag::putString);
        tag.put("antennaNetworks", antennaTag);

        ListTag networkList = new ListTag();
        networks.forEach((id, network) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            entry.putString("name", network.name());
            entry.putBoolean("allowed", network.allowed());
            if (network.ownerUUID() != null) {
                entry.putUUID("ownerUUID", network.ownerUUID());
            }
            entry.putString("ownerName", network.ownerUUID() == null ? "Server" : network.ownerName());
            entry.putString("defaultRoamingPolicy", network.defaultRoamingPolicy().name());
            ListTag towers = new ListTag();
            network.towers().forEach(tower -> towers.add(net.minecraft.nbt.StringTag.valueOf(tower)));
            entry.put("towers", towers);
            networkList.add(entry);
        });
        tag.put("networks", networkList);

        ListTag revenueList = new ListTag();
        revenueBalances.forEach((id, balance) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("network", id);
            entry.putLong("balance", balance);
            revenueList.add(entry);
        });
        tag.put("roamingRevenueBalances", revenueList);

        ListTag revenueCycleList = new ListTag();
        revenueThisCycle.forEach((id, balance) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("network", id);
            entry.putLong("balance", balance);
            entry.putLong("cycleStartDay", revenueCycleStartDays.getOrDefault(id, 0L));
            revenueCycleList.add(entry);
        });
        tag.put("revenueThisCycle", revenueCycleList);

        ListTag revenueTransactions = new ListTag();
        networkRevenueTransactions.forEach((networkId, transactions) -> transactions.forEach(transaction -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("network", networkId);
            entry.putLong("amount", transaction.amount());
            entry.putString("reason", transaction.reason());
            entry.putLong("timestamp", transaction.timestamp());
            entry.putString("details", transaction.details());
            revenueTransactions.add(entry);
        }));
        tag.put("networkRevenueTransactions", revenueTransactions);

        CompoundTag settingsTag = new CompoundTag();
        networkSettings.forEach((id, settings) -> settingsTag.put(id, settings.toTag()));
        tag.put("networkSettings", settingsTag);

        ListTag agreementList = new ListTag();
        roamingAgreements.forEach((networkId, agreements) -> agreements.forEach((otherNetworkId, fee) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("network", networkId);
            entry.putString("otherNetwork", otherNetworkId);
            entry.putLong("feePerMinute", fee);
            agreementList.add(entry);
        }));
        tag.put("roamingAgreements", agreementList);

        ListTag payoutList = new ListTag();
        pendingRoamingPayouts.forEach((recipient, payouts) -> payouts.forEach(payout -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("recipient", recipient);
            entry.putLong("amount", payout.amount());
            entry.putString("reason", payout.reason());
            payoutList.add(entry);
        }));
        tag.put("pendingRoamingPayouts", payoutList);

        ListTag publicPoints = new ListTag();
        publicMapPoints.values().forEach(point -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", point.id());
            entry.putString("name", point.name());
            entry.putString("dimension", point.dimension());
            entry.putInt("x", point.x());
            entry.putInt("y", point.y());
            entry.putInt("z", point.z());
            if (point.ownerUUID() != null) {
                entry.putUUID("owner", point.ownerUUID());
            }
            entry.putString("ownerName", point.ownerName());
            publicPoints.add(entry);
        });
        tag.put("publicMapPoints", publicPoints);

        ListTag planList = new ListTag();
        plans.forEach((id, plan) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id);
            entry.putString("name", plan.name());
            CompoundTag attributes = new CompoundTag();
            plan.attributes().forEach(attributes::putString);
            entry.put("attributes", attributes);
            planList.add(entry);
        });
        tag.put("plans", planList);

        ListTag assignmentList = new ListTag();
        planAssignments.forEach((playerId, planId) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", playerId);
            entry.putString("plan", planId);
            assignmentList.add(entry);
        });
        tag.put("planAssignments", assignmentList);

        ListTag postList = new ListTag();
        for (SocialPost post : socialPosts) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", post.id());
            entry.putUUID("author", post.author());
            entry.putString("authorName", post.authorName());
            entry.putString("text", post.text());
            entry.putString("attachment", post.attachment());
            entry.putByteArray("image", post.image());
            entry.putLong("createdAt", post.createdAt());
            ListTag likes = new ListTag();
            post.likes().forEach(id -> {
                CompoundTag like = new CompoundTag();
                like.putUUID("player", id);
                likes.add(like);
            });
            entry.put("likes", likes);
            ListTag comments = new ListTag();
            post.comments().forEach(comment -> {
                CompoundTag commentTag = new CompoundTag();
                commentTag.putUUID("author", comment.author());
                commentTag.putString("text", comment.text());
                commentTag.putLong("createdAt", comment.createdAt());
                comments.add(commentTag);
            });
            entry.put("comments", comments);
            postList.add(entry);
        }
        tag.put("socialPosts", postList);

        ListTag emergencyList = new ListTag();
        emergencyMessages.forEach(message -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", message.id());
            entry.putUUID("sender", message.senderId());
            entry.putString("senderName", message.senderName());
            entry.putString("senderNumber", message.senderNumber());
            entry.putString("body", message.body());
            entry.putString("dimension", message.dimension());
            entry.putInt("x", message.x());
            entry.putInt("y", message.y());
            entry.putInt("z", message.z());
            entry.putLong("timestamp", message.timestamp());
            emergencyList.add(entry);
        });
        tag.put("emergencyMessages", emergencyList);
        ListTag archivedEmergencyList = new ListTag();
        archivedEmergencyMessages.forEach(message -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", message.id());
            entry.putUUID("sender", message.senderId());
            entry.putString("senderName", message.senderName());
            entry.putString("senderNumber", message.senderNumber());
            entry.putString("body", message.body());
            entry.putString("dimension", message.dimension());
            entry.putInt("x", message.x());
            entry.putInt("y", message.y());
            entry.putInt("z", message.z());
            entry.putLong("timestamp", message.timestamp());
            archivedEmergencyList.add(entry);
        });
        tag.put("archivedEmergencyMessages", archivedEmergencyList);

        ListTag chargeList = new ListTag();
        for (PendingCharge charge : pendingBillingCharges) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", charge.playerId());
            entry.putLong("amount", charge.amount());
            entry.putString("reason", charge.reason());
            chargeList.add(entry);
        }
        tag.put("pendingBillingCharges", chargeList);
        ListTag pendingSmsList = new ListTag();
        pendingSms.forEach((recipient, messages) -> messages.forEach(message -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("recipient", recipient);
            entry.putString("senderNumber", message.senderNumber());
            entry.putString("recipientNumber", message.recipientNumber());
            entry.putString("body", message.body());
            entry.putLong("timestamp", message.timestamp());
            entry.putBoolean("read", message.read());
            pendingSmsList.add(entry);
        }));
        tag.put("pendingSms", pendingSmsList);
        ListTag pendingCallsList = new ListTag();
        pendingCalls.forEach((recipient, calls) -> calls.forEach(call -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("recipient", recipient);
            entry.putString("callerNumber", call.callerNumber());
            entry.putLong("timestamp", call.timestamp());
            entry.putBoolean("missed", call.missed());
            entry.putString("note", call.note());
            pendingCallsList.add(entry);
        }));
        tag.put("pendingCalls", pendingCallsList);
        ListTag billingList = new ListTag();
        pendingBillingChecks.forEach(player -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", player);
            billingList.add(entry);
        });
        tag.put("pendingBillingChecks", billingList);
        return tag;
    }

    private static NetworkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        NetworkSavedData data = new NetworkSavedData();
        for (Tag value : tag.getList("issuedNumbers", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("player")) {
                data.issuedNumbers.put(entry.getUUID("player"), entry.getString("number"));
            }
        }

        CompoundTag antennas = tag.getCompound("antennaNetworks");
        antennas.getAllKeys().forEach(key -> data.antennaNetworks.put(key, antennas.getString(key)));

        for (Tag value : tag.getList("networks", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            List<String> towers = readStrings(entry.getList("towers", Tag.TAG_STRING));
            UUID ownerUUID = entry.hasUUID("ownerUUID") ? entry.getUUID("ownerUUID") : null;
            String ownerName = ownerUUID == null ? "Server"
                    : entry.contains("ownerName", Tag.TAG_STRING) ? entry.getString("ownerName") : "";
            AntennaRoamingPolicy defaultRoamingPolicy;
            try {
                defaultRoamingPolicy = entry.contains("defaultRoamingPolicy", Tag.TAG_STRING)
                        ? AntennaRoamingPolicy.valueOf(entry.getString("defaultRoamingPolicy"))
                        : AntennaRoamingPolicy.DENY_ALL;
            } catch (IllegalArgumentException ignored) {
                defaultRoamingPolicy = AntennaRoamingPolicy.DENY_ALL;
            }
            data.networks.put(entry.getString("id"), new NetworkRecord(entry.getString("name"),
                    entry.getBoolean("allowed"), towers, ownerUUID, ownerName, defaultRoamingPolicy));
        }

        for (Tag value : tag.getList("roamingRevenueBalances", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            String networkId = entry.getString("network");
            long balance = entry.getLong("balance");
            if (data.networks.containsKey(networkId) && balance > 0L) {
                data.revenueBalances.put(networkId, balance);
            }
        }

        for (Tag value : tag.getList("networkRevenueTransactions", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            String networkId = entry.getString("network");
            if (data.networks.containsKey(networkId)) {
                data.networkRevenueTransactions.computeIfAbsent(networkId, ignored -> new ArrayList<>())
                        .add(new BankAccount.Transaction(entry.getLong("amount"), entry.getString("reason"),
                                entry.getLong("timestamp"), entry.getString("details")));
            }
        }

        boolean hasRevenueThisCycle = tag.contains("revenueThisCycle", Tag.TAG_LIST);
        String revenueCycleKey = hasRevenueThisCycle ? "revenueThisCycle" : "roamingRevenueThisCycle";
        for (Tag value : tag.getList(revenueCycleKey, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            String networkId = entry.getString("network");
            long balance = entry.getLong("balance");
            if (data.networks.containsKey(networkId) && balance >= 0L) {
                data.revenueThisCycle.put(networkId, balance);
                long startDay = entry.contains("cycleStartDay", Tag.TAG_LONG)
                        ? entry.getLong("cycleStartDay") : 0L;
                data.revenueCycleStartDays.put(networkId, Math.max(0L, startDay));
            }
        }
        if (!hasRevenueThisCycle && !tag.contains("roamingRevenueThisCycle", Tag.TAG_LIST)) {
            data.revenueThisCycle.putAll(data.revenueBalances);
        }

        CompoundTag settingsTag = tag.getCompound("networkSettings");
        for (String key : settingsTag.getAllKeys()) {
            if (data.networks.containsKey(key)) {
                data.networkSettings.put(key, NetworkSettings.fromTag(settingsTag.getCompound(key)));
            }
        }

        for (Tag value : tag.getList("roamingAgreements", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            String networkId = entry.getString("network");
            String otherNetworkId = entry.getString("otherNetwork");
            long fee = entry.getLong("feePerMinute");
            if (data.networks.containsKey(networkId) && data.networks.containsKey(otherNetworkId)
                    && !networkId.equals(otherNetworkId) && fee >= 0L) {
                data.roamingAgreements.computeIfAbsent(networkId, ignored -> new LinkedHashMap<>())
                        .put(otherNetworkId, fee);
            }
        }

        for (Tag value : tag.getList("pendingRoamingPayouts", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("recipient") && entry.getLong("amount") > 0L) {
                data.pendingRoamingPayouts.computeIfAbsent(entry.getUUID("recipient"),
                                ignored -> new ArrayList<>())
                        .add(new PendingRoamingPayout(entry.getLong("amount"), entry.getString("reason")));
            }
        }

        for (Tag value : tag.getList("publicMapPoints", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (!entry.hasUUID("id")) {
                continue;
            }
            MapPointOfInterest point = new MapPointOfInterest(entry.getUUID("id"), entry.getString("name"),
                    entry.getString("dimension"), entry.getInt("x"), entry.getInt("y"), entry.getInt("z"),
                    entry.hasUUID("owner") ? entry.getUUID("owner") : null, entry.getString("ownerName"),
                    MapPointOfInterest.Visibility.PUBLIC);
            data.publicMapPoints.put(point.id(), point);
        }

        for (Tag value : tag.getList("plans", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            Map<String, String> attributes = new HashMap<>();
            CompoundTag attributeTag = entry.getCompound("attributes");
            attributeTag.getAllKeys().forEach(key -> attributes.put(key, attributeTag.getString(key)));
            data.plans.put(entry.getString("id"), new PlanRecord(entry.getString("name"), attributes));
        }

        for (Tag value : tag.getList("planAssignments", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("player")) {
                data.planAssignments.put(entry.getUUID("player"), entry.getString("plan"));
            }
        }

        for (Tag value : tag.getList("socialPosts", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("author")) {
                List<UUID> likes = new ArrayList<>();
                for (Tag likeValue : entry.getList("likes", Tag.TAG_COMPOUND)) {
                    CompoundTag like = (CompoundTag) likeValue;
                    if (like.hasUUID("player")) {
                        likes.add(like.getUUID("player"));
                    }
                }
                List<SocialComment> comments = new ArrayList<>();
                for (Tag commentValue : entry.getList("comments", Tag.TAG_COMPOUND)) {
                    CompoundTag comment = (CompoundTag) commentValue;
                    if (comment.hasUUID("author")) {
                        comments.add(new SocialComment(comment.getUUID("author"), comment.getString("text"),
                                comment.getLong("createdAt")));
                    }
                }
                data.socialPosts.add(new SocialPost(entry.hasUUID("id") ? entry.getUUID("id") : UUID.randomUUID(),
                        entry.getUUID("author"), entry.getString("authorName"), entry.getString("text"), entry.getString("attachment"),
                        entry.getLong("createdAt"), likes, comments, entry.getByteArray("image")));
            }
        }

        for (Tag value : tag.getList("emergencyMessages", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (!entry.hasUUID("sender")) {
                continue;
            }
            data.emergencyMessages.add(new EmergencyMessage(
                    entry.hasUUID("id") ? entry.getUUID("id") : UUID.randomUUID(),
                    entry.getUUID("sender"), entry.getString("senderName"), entry.getString("senderNumber"),
                    entry.getString("body"), entry.getString("dimension"), entry.getInt("x"),
                    entry.getInt("y"), entry.getInt("z"), entry.getLong("timestamp")));
        }
        if (data.emergencyMessages.size() > EmergencyMessages.MAX_STORED_MESSAGES) {
            data.emergencyMessages.subList(0,
                    data.emergencyMessages.size() - EmergencyMessages.MAX_STORED_MESSAGES).clear();
        }
        for (Tag value : tag.getList("archivedEmergencyMessages", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("sender")) {
                data.archivedEmergencyMessages.add(new EmergencyMessage(
                        entry.hasUUID("id") ? entry.getUUID("id") : UUID.randomUUID(),
                        entry.getUUID("sender"), entry.getString("senderName"), entry.getString("senderNumber"),
                        entry.getString("body"), entry.getString("dimension"), entry.getInt("x"),
                        entry.getInt("y"), entry.getInt("z"), entry.getLong("timestamp")));
            }
        }
        if (data.archivedEmergencyMessages.size() > EmergencyMessages.MAX_STORED_MESSAGES) {
            data.archivedEmergencyMessages.subList(0,
                    data.archivedEmergencyMessages.size() - EmergencyMessages.MAX_STORED_MESSAGES).clear();
        }

        for (Tag value : tag.getList("pendingBillingCharges", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            if (entry.hasUUID("player")) {
                data.pendingBillingCharges.add(new PendingCharge(entry.getUUID("player"),
                        entry.getLong("amount"), entry.getString("reason")));
            }
        }
        for (Tag pendingValue : tag.getList("pendingSms", Tag.TAG_COMPOUND)) {
            CompoundTag pendingEntry = (CompoundTag) pendingValue;
            if (pendingEntry.hasUUID("recipient")) {
                PhoneData.Message message = new PhoneData.Message(pendingEntry.getString("senderNumber"),
                        pendingEntry.getString("recipientNumber"), pendingEntry.getString("body"),
                        pendingEntry.getLong("timestamp"), pendingEntry.getBoolean("read"));
                data.pendingSms.computeIfAbsent(pendingEntry.getUUID("recipient"), ignored -> new ArrayList<>())
                        .add(message);
            }
        }
        for (Tag callValue : tag.getList("pendingCalls", Tag.TAG_COMPOUND)) {
            CompoundTag callEntry = (CompoundTag) callValue;
            if (callEntry.hasUUID("recipient")) {
                data.pendingCalls.computeIfAbsent(callEntry.getUUID("recipient"), ignored -> new ArrayList<>())
                        .add(new PendingCall(callEntry.getString("callerNumber"),
                                callEntry.getLong("timestamp"), callEntry.getBoolean("missed"),
                                callEntry.getString("note")));
            }
            for (Tag billingValue : tag.getList("pendingBillingChecks", Tag.TAG_COMPOUND)) {
                CompoundTag billingEntry = (CompoundTag) billingValue;
                if (billingEntry.hasUUID("player")) {
                    data.pendingBillingChecks.add(billingEntry.getUUID("player"));
                }
            }
        }
        return data;
    }

    private static List<String> readStrings(ListTag list) {
        List<String> strings = new ArrayList<>(list.size());
        for (Tag value : list) {
            strings.add(value.getAsString());
        }
        return strings;
    }

    public record NetworkRecord(String name, boolean allowed, List<String> towers, UUID ownerUUID,
                                String ownerName, AntennaRoamingPolicy defaultRoamingPolicy) {
        public NetworkRecord {
            towers = List.copyOf(towers);
            ownerName = ownerUUID == null ? "Server" : ownerName == null ? "" : ownerName;
            defaultRoamingPolicy = defaultRoamingPolicy == null
                    ? AntennaRoamingPolicy.DENY_ALL : defaultRoamingPolicy;
        }

        public NetworkRecord(String name, boolean allowed, List<String> towers) {
            this(name, allowed, towers, null, "Server", AntennaRoamingPolicy.DENY_ALL);
        }

        public NetworkRecord withTowers(List<String> updatedTowers) {
            return new NetworkRecord(name, allowed, updatedTowers, ownerUUID, ownerName, defaultRoamingPolicy);
        }

        public NetworkRecord withName(String updatedName) {
            return new NetworkRecord(updatedName, allowed, towers, ownerUUID, ownerName, defaultRoamingPolicy);
        }

        public NetworkRecord withAllowed(boolean updatedAllowed) {
            return new NetworkRecord(name, updatedAllowed, towers, ownerUUID, ownerName, defaultRoamingPolicy);
        }

        public NetworkRecord withOwner(UUID updatedOwnerUUID, String updatedOwnerName) {
            return new NetworkRecord(name, allowed, towers, updatedOwnerUUID, updatedOwnerName,
                    defaultRoamingPolicy);
        }

        public NetworkRecord withDefaultRoamingPolicy(AntennaRoamingPolicy policy) {
            return new NetworkRecord(name, allowed, towers, ownerUUID, ownerName, policy);
        }
    }

    public record PlanRecord(String name, Map<String, String> attributes) {
        public PlanRecord {
            attributes = Map.copyOf(attributes);
        }
    }

    public void toggleSocialLike(UUID postId, UUID playerId) {
        for (int i = 0; i < socialPosts.size(); i++) {
            SocialPost post = socialPosts.get(i);
            if (post.id().equals(postId)) {
                List<UUID> likes = new ArrayList<>(post.likes());
                if (!likes.remove(playerId)) {
                    likes.add(playerId);
                }
                socialPosts.set(i, post.withLikes(likes));
                setDirty();
                return;
            }
        }
    }

    public void addSocialComment(UUID postId, SocialComment comment) {
        for (int i = 0; i < socialPosts.size(); i++) {
            SocialPost post = socialPosts.get(i);
            if (post.id().equals(postId)) {
                List<SocialComment> comments = new ArrayList<>(post.comments());
                comments.add(comment);
                socialPosts.set(i, post.withComments(comments));
                setDirty();
                return;
            }
        }
    }

    public record SocialComment(UUID author, String text, long createdAt) {
    }

    public record EmergencyMessage(UUID id, UUID senderId, String senderName, String senderNumber,
                                   String body, String dimension, int x, int y, int z, long timestamp) {
        public EmergencyMessage {
            id = id == null ? UUID.randomUUID() : id;
            senderName = senderName == null ? "" : senderName;
            senderNumber = senderNumber == null ? "" : senderNumber;
            body = body == null ? "" : body;
            dimension = dimension == null ? "" : dimension;
        }
    }

    public record SocialPost(UUID id, UUID author, String authorName, String text, String attachment, long createdAt,
                             List<UUID> likes, List<SocialComment> comments, byte[] image) {
        public SocialPost {
            likes = List.copyOf(likes);
            comments = List.copyOf(comments);
            image = image == null ? new byte[0] : image.clone();
        }

        public SocialPost(UUID author, String text, long createdAt) {
            this(UUID.randomUUID(), author, "", text, "", createdAt, List.of(), List.of(), new byte[0]);
        }

        public SocialPost(UUID id, UUID author, String text, String attachment, long createdAt,
                          List<UUID> likes, List<SocialComment> comments, byte[] image) {
            this(id, author, "", text, attachment, createdAt, likes, comments, image);
        }

        @Override
        public byte[] image() {
            return image.clone();
        }

        public boolean hasImage() {
            return image.length > 0;
        }

        public SocialPost withLikes(List<UUID> updated) {
            return new SocialPost(id, author, authorName, text, attachment, createdAt, updated, comments, image);
        }

        public SocialPost withComments(List<SocialComment> updated) {
            return new SocialPost(id, author, authorName, text, attachment, createdAt, likes, updated, image);
        }
    }

    public record PendingCharge(UUID playerId, long amount, String reason) {
    }

    public record PendingCall(String callerNumber, long timestamp, boolean missed, String note) {
        public PendingCall(String callerNumber, long timestamp, boolean missed) {
            this(callerNumber, timestamp, missed, "");
        }

    }

    public record PendingRoamingPayout(long amount, String reason) {
    }
}

package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.plans.PlanRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlanSyncPacket(List<PlanEntry> plans, String activePlanId, long cycleStartDay,
                             int minutesUsed, int textsUsed, long dataUsedMb, double partialDataUsedMb,
                             List<NetworkEntry> networks)
        implements CustomPacketPayload {
    public static final Type<PlanSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "plan_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlanSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(Math.min(packet.plans().size(), 256));
                packet.plans().stream().limit(256).forEach(plan -> {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, plan.id());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, plan.displayName());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, plan.description());
                    buffer.writeLong(plan.price());
                    buffer.writeVarInt(plan.billingCycleDays());
                    buffer.writeLong(plan.dataAllowanceMb());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, plan.dataSpeed());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, plan.maxSignalTier());
                    buffer.writeVarInt(plan.callMinutes());
                    buffer.writeVarInt(plan.textMessages());
                    buffer.writeVarInt(Math.min(plan.allowedNetworks().size(), 64));
                    plan.allowedNetworks().stream().limit(64).forEach(value -> ByteBufCodecs.STRING_UTF8.encode(buffer, value));
                    ByteBufCodecs.STRING_UTF8.encode(buffer, plan.carrierNetworkId());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, plan.carrierNetworkName());
                    buffer.writeBoolean(plan.autoRenew());
                    buffer.writeBoolean(plan.isDefault());
                    buffer.writeBoolean(plan.allowRoaming());
                    buffer.writeLong(plan.roamingFeePerMinute());
                    buffer.writeLong(plan.roamingFeePerMb());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, plan.roamingMode());
                    buffer.writeBoolean(plan.allowOverage());
                    buffer.writeLong(plan.overageRatePerMinute());
                    buffer.writeLong(plan.overageRatePerText());
                    buffer.writeLong(plan.overageRatePerMb());
                });
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.activePlanId());
                buffer.writeLong(packet.cycleStartDay());
                buffer.writeVarInt(packet.minutesUsed());
                buffer.writeVarInt(packet.textsUsed());
                buffer.writeLong(packet.dataUsedMb());
                buffer.writeDouble(packet.partialDataUsedMb());
                buffer.writeVarInt(Math.min(packet.networks().size(), 256));
                packet.networks().stream().limit(256).forEach(network -> {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, network.id());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, network.name());
                    ByteBufCodecs.STRING_UTF8.encode(buffer, network.ownerName());
                    buffer.writeVarInt(network.antennaCount());
                    buffer.writeBoolean(network.allowRoaming());
                });
            }, buffer -> {
                int count = Math.clamp(buffer.readVarInt(), 0, 256);
                List<PlanEntry> plans = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    String id = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    String name = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    String description = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    long price = buffer.readLong();
                    int cycleDays = buffer.readVarInt();
                    long dataAllowance = buffer.readLong();
                    String speed = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    String tier = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    int minutes = buffer.readVarInt();
                    int texts = buffer.readVarInt();
                    int networkCount = Math.clamp(buffer.readVarInt(), 0, 64);
                    List<String> networks = new ArrayList<>(networkCount);
                    for (int n = 0; n < networkCount; n++) {
                        networks.add(ByteBufCodecs.STRING_UTF8.decode(buffer));
                    }
                    String carrierNetworkId = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    String carrierNetworkName = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    boolean autoRenew = buffer.readBoolean();
                    boolean isDefault = buffer.readBoolean();
                    boolean allowRoaming = buffer.readBoolean();
                    long roamingFeePerMinute = buffer.readLong();
                    long roamingFeePerMb = buffer.readLong();
                    String roamingMode = ByteBufCodecs.STRING_UTF8.decode(buffer);
                    boolean allowOverage = buffer.readBoolean();
                    long overageMinute = buffer.readLong();
                    long overageText = buffer.readLong();
                    long overageMb = buffer.readLong();
                    plans.add(new PlanEntry(id, name, description, price, cycleDays, dataAllowance,
                            speed, tier, minutes, texts, networks, carrierNetworkId, carrierNetworkName,
                            autoRenew, isDefault, allowRoaming,
                            roamingFeePerMinute, roamingFeePerMb, roamingMode, allowOverage,
                            overageMinute, overageText, overageMb));
                }
                String activePlanId = ByteBufCodecs.STRING_UTF8.decode(buffer);
                long cycleStartDay = buffer.readLong();
                int minutesUsed = buffer.readVarInt();
                int textsUsed = buffer.readVarInt();
                long dataUsedMb = buffer.readLong();
                double partialDataUsedMb = buffer.readDouble();
                int networkCount = Math.clamp(buffer.readVarInt(), 0, 256);
                List<NetworkEntry> networks = new ArrayList<>(networkCount);
                for (int i = 0; i < networkCount; i++) {
                    networks.add(new NetworkEntry(ByteBufCodecs.STRING_UTF8.decode(buffer),
                            ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.STRING_UTF8.decode(buffer),
                            buffer.readVarInt(), buffer.readBoolean()));
                }
                return new PlanSyncPacket(plans, activePlanId, cycleStartDay, minutesUsed, textsUsed,
                        dataUsedMb, partialDataUsedMb, networks);
            });

    public static PlanSyncPacket forPlayer(List<PlanRegistry.Plan> plans, java.util.Map<String, String> networkNames,
                                           String activePlanId,
                                           long cycleStartDay, int minutesUsed, int textsUsed,
                                           long dataUsedMb, double partialDataUsedMb,
                                           List<NetworkEntry> networks) {
        List<PlanEntry> entries = plans.stream().map(plan -> new PlanEntry(
                plan.id(), plan.displayName(), plan.description(), plan.price(), plan.billingCycleDays(),
                plan.dataAllowanceMb(), plan.dataSpeed().name(), plan.maxSignalTier().name(),
                plan.callMinutes(), plan.textMessages(), plan.allowedNetworks(), plan.carrierNetworkId(),
                networkNames.getOrDefault(plan.carrierNetworkId(), ""),
                plan.autoRenew(), plan.isDefault(), plan.allowRoaming(),
                plan.roamingFeePerMinute(), plan.roamingFeePerMb(), plan.roamingMode().name(),
                plan.allowOverage(), plan.overageRatePerMinute(), plan.overageRatePerText(),
                plan.overageRatePerMb())).toList();
        return new PlanSyncPacket(entries, activePlanId, cycleStartDay, minutesUsed, textsUsed,
                dataUsedMb, partialDataUsedMb, List.copyOf(networks));
    }

    public record PlanEntry(String id, String displayName, String description, long price, int billingCycleDays,
                            long dataAllowanceMb, String dataSpeed, String maxSignalTier, int callMinutes,
                            int textMessages, List<String> allowedNetworks, String carrierNetworkId,
                            String carrierNetworkName,
                            boolean autoRenew, boolean isDefault,
                            boolean allowRoaming, long roamingFeePerMinute, long roamingFeePerMb,
                            String roamingMode, boolean allowOverage, long overageRatePerMinute,
                            long overageRatePerText, long overageRatePerMb) {
        public PlanEntry {
            roamingMode = roamingMode == null ? "CHARGED" : roamingMode;
            allowedNetworks = List.copyOf(allowedNetworks);
            carrierNetworkId = carrierNetworkId == null ? "" : carrierNetworkId;
            carrierNetworkName = carrierNetworkName == null ? "" : carrierNetworkName;
        }
    }

    public record NetworkEntry(String id, String name, String ownerName, int antennaCount, boolean allowRoaming) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

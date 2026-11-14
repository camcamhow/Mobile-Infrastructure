package com.example.mobilecellnetwork.network.packet;

import com.example.mobilecellnetwork.data.RoamingAgreement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RoamingAgreementSyncPacket(String networkId, List<RoamingAgreement> agreements)
        implements CustomPacketPayload {
    public static final Type<RoamingAgreementSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "roaming_agreement_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RoamingAgreementSyncPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.networkId());
                buffer.writeVarInt(Math.min(256, packet.agreements().size()));
                packet.agreements().stream().limit(256).forEach(agreement -> {
                    ByteBufCodecs.STRING_UTF8.encode(buffer, agreement.otherNetworkId());
                    buffer.writeLong(agreement.ratePerMinute());
                    buffer.writeLong(agreement.ratePerText());
                    buffer.writeLong(agreement.ratePerMb());
                    buffer.writeLong(agreement.agreedAtEpochMillis());
                });
            }, buffer -> {
                String networkId = ByteBufCodecs.STRING_UTF8.decode(buffer);
                int count = Math.clamp(buffer.readVarInt(), 0, 256);
                List<RoamingAgreement> agreements = new ArrayList<>(count);
                for (int index = 0; index < count; index++) {
                    agreements.add(new RoamingAgreement(ByteBufCodecs.STRING_UTF8.decode(buffer),
                            buffer.readLong(), buffer.readLong(), buffer.readLong(), buffer.readLong()));
                }
                return new RoamingAgreementSyncPacket(networkId, agreements);
            });

    public RoamingAgreementSyncPacket {
        agreements = List.copyOf(agreements);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

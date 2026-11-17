package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SignalUpdatePacket(
        SignalTier signalTier,
        String activeNetworkName,
        String activeNetworkId,
        int antennaCountInRange,
        int signalBars,
        double finalStrength,
        boolean hasDebugInfo,
        double distance,
        double effectiveRange,
        double baseStrength,
        double losAttenuation,
        int sampleCount,
        boolean roaming,
        long roamingFeesThisSession
) implements CustomPacketPayload {
    public static final Type<SignalUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "signal_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SignalUpdatePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.signalTier.name());
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.activeNetworkName);
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.activeNetworkId);
                ByteBufCodecs.VAR_INT.encode(buffer, packet.antennaCountInRange);
                ByteBufCodecs.VAR_INT.encode(buffer, packet.signalBars);
                ByteBufCodecs.DOUBLE.encode(buffer, packet.finalStrength);
                ByteBufCodecs.BOOL.encode(buffer, packet.hasDebugInfo);
                ByteBufCodecs.DOUBLE.encode(buffer, packet.distance);
                ByteBufCodecs.DOUBLE.encode(buffer, packet.effectiveRange);
                ByteBufCodecs.DOUBLE.encode(buffer, packet.baseStrength);
                ByteBufCodecs.DOUBLE.encode(buffer, packet.losAttenuation);
                ByteBufCodecs.VAR_INT.encode(buffer, packet.sampleCount);
                ByteBufCodecs.BOOL.encode(buffer, packet.roaming);
                buffer.writeLong(packet.roamingFeesThisSession);
            },
            buffer -> new SignalUpdatePacket(
                    SignalTier.valueOf(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    buffer.readLong())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

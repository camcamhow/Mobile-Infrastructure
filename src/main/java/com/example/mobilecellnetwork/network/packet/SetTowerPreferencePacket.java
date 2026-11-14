package com.example.mobilecellnetwork.network.packet;

import com.example.mobilecellnetwork.data.TowerPreferenceMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetTowerPreferencePacket(TowerPreferenceMode mode, BlockPos pinnedTower)
        implements CustomPacketPayload {
    public static final Type<SetTowerPreferencePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "set_tower_preference"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetTowerPreferencePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, packet.mode().name());
                buffer.writeBoolean(packet.pinnedTower() != null);
                if (packet.pinnedTower() != null) {
                    buffer.writeBlockPos(packet.pinnedTower());
                }
            }, buffer -> new SetTowerPreferencePacket(
                    TowerPreferenceMode.valueOf(ByteBufCodecs.STRING_UTF8.decode(buffer)),
                    buffer.readBoolean() ? buffer.readBlockPos() : null));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

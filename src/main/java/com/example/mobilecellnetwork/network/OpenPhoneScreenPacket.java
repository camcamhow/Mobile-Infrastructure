package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenPhoneScreenPacket(int menuId, boolean heldView) implements CustomPacketPayload {
    public static final Type<OpenPhoneScreenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "open_phone_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPhoneScreenPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenPhoneScreenPacket::menuId,
                    ByteBufCodecs.BOOL, OpenPhoneScreenPacket::heldView, OpenPhoneScreenPacket::new);

    public static void handle(OpenPhoneScreenPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> com.example.mobilecellnetwork.client.ClientPayloadHandlers.openPhone(packet));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

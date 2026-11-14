package com.example.mobilecellnetwork.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ContactsActionPacket(String action, String previousName, String name, String number)
        implements CustomPacketPayload {
    public static final Type<ContactsActionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "contacts_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContactsActionPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ContactsActionPacket::action,
                    ByteBufCodecs.STRING_UTF8, ContactsActionPacket::previousName,
                    ByteBufCodecs.STRING_UTF8, ContactsActionPacket::name,
                    ByteBufCodecs.STRING_UTF8, ContactsActionPacket::number, ContactsActionPacket::new);

    public ContactsActionPacket(String action, String name, String number) {
        this(action, name, name, number);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}

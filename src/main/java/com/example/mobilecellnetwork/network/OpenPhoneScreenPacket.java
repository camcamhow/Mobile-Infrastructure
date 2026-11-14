package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.gui.PhoneMenu;
import com.example.mobilecellnetwork.gui.PhoneScreen;
import com.example.mobilecellnetwork.gui.LockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenPhoneScreenPacket(int menuId) implements CustomPacketPayload {
    public static final Type<OpenPhoneScreenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "open_phone_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPhoneScreenPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenPhoneScreenPacket::menuId, OpenPhoneScreenPacket::new);

    public static void handle(OpenPhoneScreenPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> {
            var minecraft = Minecraft.getInstance();
            if (minecraft.player != null
                    && minecraft.player.containerMenu.containerId == packet.menuId()
                    && minecraft.player.containerMenu instanceof PhoneMenu menu) {
                NetworkHandler.clearClientPhoneData();
                PhoneDeviceStatePacket state = PhoneDeviceClientCache.state();
                if (state != null && (state.lockScreenEnabled() || state.pinSet() || state.lockedAll())) {
                    minecraft.setScreen(new LockScreen(menu));
                } else {
                    PhoneDeviceClientCache.setUnlocked(true);
                    PhoneScreen.openPhone(menu, minecraft.player.getInventory());
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.example.mobilecellnetwork.mixin;

import com.example.mobilecellnetwork.gui.LowPowerPromptScreen;
import com.example.mobilecellnetwork.gui.ConfirmActionModal;
import com.example.mobilecellnetwork.gui.MapPointCreateModal;
import com.example.mobilecellnetwork.gui.MapPointShareModal;
import com.example.mobilecellnetwork.gui.PhoneNavigableScreen;
import com.example.mobilecellnetwork.gui.PhonePinEntryScreen;
import com.example.mobilecellnetwork.gui.PhoneColors;
import com.example.mobilecellnetwork.gui.PhoneFrame;
import com.example.mobilecellnetwork.gui.PhoneStyle;
import com.example.mobilecellnetwork.network.ClientPhoneCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public abstract class PhoneFontMixin {
    @ModifyVariable(method = "getFontSet", at = @At("HEAD"), argsOnly = true)
    private ResourceLocation mobilecellnetwork$usePhoneFont(ResourceLocation font) {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft == null ? null : minecraft.screen;
        if (screen != null && font.equals(Style.DEFAULT_FONT)
                && (screen instanceof PhoneNavigableScreen || screen instanceof PhonePinEntryScreen
                || screen instanceof MapPointCreateModal || screen instanceof MapPointShareModal
                || screen instanceof LowPowerPromptScreen
                || screen instanceof ConfirmActionModal modal && modal.isPhoneModal())) {
            return ResourceLocation.fromNamespaceAndPath("mobilecellnetwork",
                    ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI ? "android_bold"
                            : PhoneColors.dark(ClientPhoneCache.phoneStyle()) ? "snapple" : "snapple_light");
        }
        return font;
    }

    @ModifyVariable(method = {
            "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",
            "drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I"
    }, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean mobilecellnetwork$lightPhoneShadow(boolean shadow) {
        return PhoneFrame.isDrawingPhone()
                && (ClientPhoneCache.phoneStyle() == PhoneStyle.SAMSUNG_ONE_UI
                || !PhoneColors.dark(ClientPhoneCache.phoneStyle())) ? false : shadow;
    }
}

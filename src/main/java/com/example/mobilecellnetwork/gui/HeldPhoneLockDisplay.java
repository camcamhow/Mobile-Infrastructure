package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import com.example.mobilecellnetwork.network.ClientSignalCache;
import com.example.mobilecellnetwork.network.SignalTier;
import com.example.mobilecellnetwork.registry.ModDataComponents;
import com.example.mobilecellnetwork.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class HeldPhoneLockDisplay {
    private static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "snapple");
    private static final ResourceLocation ANDROID_FONT = ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "android_bold");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, MMM d");

    private HeldPhoneLockDisplay() {
    }

    public static void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                              MultiBufferSource buffers) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!context.firstPerson() || !com.example.mobilecellnetwork.item.CellphoneItem.isPhone(stack)
                || minecraft.player == null || minecraft.screen != null) {
            return;
        }
        int charge = stack.getOrDefault(ModDataComponents.PHONE_CHARGE.get(), 0);
        if (charge <= 0) {
            return;
        }
        int maximum = MobileCellNetworkConfig.PHONE_MAX_CHARGE.get();
        int percent = maximum > 0 ? Math.clamp((int) ((long) charge * 100 / maximum), 0, 100) : 0;
        boolean hasService = ClientSignalCache.tier() != SignalTier.NO_SERVICE
                && !ClientSignalCache.networkName().isBlank();
        LocalDateTime now = LocalDateTime.now();
        Font font = minecraft.font;

        pose.pushPose();
        boolean android = stack.is(ModItems.ANDROID_PHONE.get());
        double screenDepth = android ? 0.467D : 0.474D;
        pose.translate(0.5D, 0.91D, screenDepth);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(0.0038F, -0.0038F, 0.0038F);
        ResourceLocation displayFont = android ? ANDROID_FONT : FONT;
        draw(font, buffers, pose, now.format(TIME), -49, 5, 0.8F, displayFont);
        if (hasService) {
            if (ClientSignalCache.roaming()) {
                draw(font, buffers, pose, "R", 19, 6, 0.6F, displayFont);
            }
            int bars = Math.clamp(ClientSignalCache.bars(), 0, 4);
            for (int i = 0; i < 4; i++) {
                float height = 2.0F + i * 1.2F;
                rectangle(buffers, pose, 24 + i * 2.8F, 12 - height,
                        26 + i * 2.8F, 12, i < bars ? 0xFFFFFFFF : 0x88FFFFFF);
            }
        } else {
            draw(font, buffers, pose, "SOS", 36 - font.width(label("SOS", displayFont)) * 0.65F, 6, 0.65F, displayFont);
        }
        rectangle(buffers, pose, 39, 6, 49, 7, 0xFFFFFFFF);
        rectangle(buffers, pose, 39, 11, 49, 12, 0xFFFFFFFF);
        rectangle(buffers, pose, 39, 7, 40, 11, 0xFFFFFFFF);
        rectangle(buffers, pose, 48, 7, 49, 11, 0xFFFFFFFF);
        rectangle(buffers, pose, 49, 7.5F, 50, 10.5F, 0xFFFFFFFF);
        if (percent > 0) {
            rectangle(buffers, pose, 41, 8, 41 + percent * 6.0F / 100, 10,
                    percent <= 20 ? 0xFFFF453A : 0xFFFFFFFF);
        }
        centered(font, buffers, pose, now.format(DATE), 36, 1.0F, displayFont);
        centered(font, buffers, pose, now.format(TIME), 52, 2.2F, displayFont);
        pose.popPose();
    }

    private static Component label(String text, ResourceLocation font) {
        return Component.literal(text).withStyle(style -> style.withFont(font));
    }

    private static void centered(Font font, MultiBufferSource buffers, PoseStack pose,
                                 String text, float y, float scale, ResourceLocation displayFont) {
        draw(font, buffers, pose, text, -font.width(label(text, displayFont)) * scale / 2.0F, y, scale, displayFont);
    }

    private static void draw(Font font, MultiBufferSource buffers, PoseStack pose,
                             String text, float x, float y, float scale, ResourceLocation displayFont) {
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        font.drawInBatch(label(text, displayFont), 0, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    private static void rectangle(MultiBufferSource buffers, PoseStack pose,
                                  float left, float top, float right, float bottom, int color) {
        VertexConsumer vertices = buffers.getBuffer(RenderType.debugQuads());
        int red = color >> 16 & 255;
        int green = color >> 8 & 255;
        int blue = color & 255;
        int alpha = color >>> 24;
        var matrix = pose.last().pose();
        vertices.addVertex(matrix, left, bottom, 0.1F).setColor(red, green, blue, alpha);
        vertices.addVertex(matrix, right, bottom, 0.1F).setColor(red, green, blue, alpha);
        vertices.addVertex(matrix, right, top, 0.1F).setColor(red, green, blue, alpha);
        vertices.addVertex(matrix, left, top, 0.1F).setColor(red, green, blue, alpha);
    }
}

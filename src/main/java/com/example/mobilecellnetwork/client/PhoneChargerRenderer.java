package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class PhoneChargerRenderer implements BlockEntityRenderer<PhoneChargerBlockEntity> {
    private final ItemRenderer itemRenderer;
    private final EntityRenderDispatcher entityRenderer;
    private final Font font;

    public PhoneChargerRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
        entityRenderer = context.getEntityRenderer();
        font = context.getFont();
    }

    @Override
    public void render(PhoneChargerBlockEntity charger, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        PowerIndicatorRenderer.charger(charger.isDisplayPowered(), pose, buffers, light, overlay);
        ItemStack phone = charger.getDisplayPhone();
        if (phone.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5D, 0.17D, 0.5D);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(0.65F, 0.65F, 0.65F);
        itemRenderer.renderStatic(phone, ItemDisplayContext.NONE, light, overlay, pose, buffers,
                charger.getLevel(), 0);
        pose.popPose();
        int percent = charger.getDisplayChargePercent();
        if (percent < 0) return;
        String label = "Charge: " + percent + "%";
        pose.pushPose();
        pose.translate(0.5D, 0.75D, 0.5D);
        pose.mulPose(entityRenderer.cameraOrientation());
        pose.scale(0.018F, -0.018F, 0.018F);
        float textX = -font.width(label) / 2.0F;
        font.drawInBatch(label, textX, 0, 0x20FFFFFF, false,
                pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0x40000000, light);
        font.drawInBatch(label, textX, 0, 0xFFFFFFFF, false,
                pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
        pose.popPose();
    }
}

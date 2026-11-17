package com.example.mobilecellnetwork.client;

import com.example.mobilecellnetwork.infrastructure.MeterReading;
import com.example.mobilecellnetwork.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.event.RenderHandEvent;

public final class HeldTesterRenderer {
    private static final ResourceLocation LCD =
            ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");

    private HeldTesterRenderer() {}

    public static void onRenderHand(RenderHandEvent event) {
        if (!event.getItemStack().is(ModItems.TECHNICIAN_TESTER.get())) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        event.setCanceled(true);
        boolean right = (event.getHand() == InteractionHand.MAIN_HAND)
                == (minecraft.player.getMainArm() == HumanoidArm.RIGHT);
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(right ? 0.4 : -0.4, -0.22 - event.getEquipProgress() * 0.6
                + Math.sin(event.getSwingProgress() * Math.PI) * 0.03, -1.05);
        pose.scale(0.9F, 0.9F, 0.9F);
        minecraft.getItemRenderer().renderStatic(event.getItemStack(), ItemDisplayContext.NONE,
                event.getPackedLight(), OverlayTexture.NO_OVERLAY, pose, event.getMultiBufferSource(),
                minecraft.level, minecraft.player.getId());

        var data = event.getItemStack().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        MeterReading reading = MeterReading.load(data.getCompound(MeterReading.DATA_KEY));
        var lines = reading.lines(minecraft.level.getGameTime());
        var lcd = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(LCD));
        TestAntennaRenderer.box(lcd, pose, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                4.4F / 16 - 0.5F, 8.6F / 16 - 0.5F, 10.755F / 16 - 0.5F,
                11.6F / 16 - 0.5F, 13.4F / 16 - 0.5F, 10.76F / 16 - 0.5F, 0xFFB9C9A0);
        pose.translate(4.6F / 16 - 0.5F, 13.1F / 16 - 0.5F, 10.78F / 16 - 0.5F);
        pose.scale(0.004F, -0.004F, 0.004F);
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            pose.pushPose();
            float fit = Math.min(1, 105F / Math.max(1, minecraft.font.width(line)));
            pose.translate(0, i * 10, 0);
            pose.scale(fit, fit, 1);
            minecraft.font.drawInBatch(line, 0, 0, 0xFF152A20, false, pose.last().pose(),
                    event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
        renderChargeGauge(() -> event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(LCD)),
                pose, reading.energy(), reading.capacity());
        pose.popPose();
    }

    private static void renderChargeGauge(java.util.function.Supplier<VertexConsumer> buffer, PoseStack pose,
                                          int energy, int capacity) {
        if (capacity <= 0) return;
        // Font rendering can flush the LCD batch; obtain a live consumer after drawing text.
        VertexConsumer out = buffer.get();
        int activeSegments = Math.clamp((int) Math.ceil(10.0 * Math.max(0, energy) / capacity), 0, 10);
        int color = activeSegments <= 2 ? 0xFFFFB45C : 0xFF9DCE8D;
        for (int segment = 0; segment < 10; segment++) {
            float x = 13 + segment * 8;
            TestAntennaRenderer.box(out, pose, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    x, 62, 0.5F, x + 6, 66, 1.5F, 0xFF344334);
            if (segment < activeSegments) {
                TestAntennaRenderer.box(out, pose, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        x + 1, 63, 1.5F, x + 5, 65, 2.5F, color);
            }
        }
    }
}

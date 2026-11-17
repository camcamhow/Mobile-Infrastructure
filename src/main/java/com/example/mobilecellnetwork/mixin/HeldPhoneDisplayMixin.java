package com.example.mobilecellnetwork.mixin;

import com.example.mobilecellnetwork.gui.HeldPhoneLockDisplay;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class HeldPhoneDisplayMixin {
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void mobilecellnetwork$renderHeldLockScreen(ItemStack stack, ItemDisplayContext context,
                                                         boolean leftHand, PoseStack pose,
                                                         MultiBufferSource buffers, int light, int overlay,
                                                         BakedModel model, CallbackInfo callback) {
        HeldPhoneLockDisplay.render(stack, context, pose, buffers);
    }
}

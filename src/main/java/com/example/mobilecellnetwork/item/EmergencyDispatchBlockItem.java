package com.example.mobilecellnetwork.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class EmergencyDispatchBlockItem extends BlockItem {
    public EmergencyDispatchBlockItem(net.minecraft.world.level.block.Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || !player.hasPermissions(2)) {
            if (player != null && !context.getLevel().isClientSide) {
                player.displayClientMessage(Component.literal(
                        "You need administrator permission to place the Emergency Dispatch."), true);
            }
            return InteractionResult.FAIL;
        }
        return super.useOn(context);
    }
}

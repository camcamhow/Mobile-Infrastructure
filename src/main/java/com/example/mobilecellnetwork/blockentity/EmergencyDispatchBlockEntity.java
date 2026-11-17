package com.example.mobilecellnetwork.blockentity;

import com.example.mobilecellnetwork.gui.EmergencyDispatchMenu;
import com.example.mobilecellnetwork.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EmergencyDispatchBlockEntity extends BlockEntity {
    public EmergencyDispatchBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.EMERGENCY_DISPATCH.get(), position, state);
    }

    public void openDispatchMenu(Player player) {
        if (!(player instanceof ServerPlayer admin) || !admin.hasPermissions(2)) {
            player.displayClientMessage(Component.literal(
                    "You need administrator permission to use the Emergency Dispatch."), true);
            return;
        }
        admin.openMenu(new SimpleMenuProvider(
                (id, inventory, user) -> new EmergencyDispatchMenu(id, inventory, worldPosition),
                Component.literal("Emergency Dispatch")),
                buffer -> buffer.writeBlockPos(worldPosition));
    }
}

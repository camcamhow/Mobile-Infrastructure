package com.example.mobilecellnetwork.item;

import java.util.List;
import com.example.mobilecellnetwork.gui.PhoneMenu;
import com.example.mobilecellnetwork.network.OpenPhoneScreenPacket;
import com.example.mobilecellnetwork.network.PhoneStyleSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;

public class CellphoneItem extends Item {
    private final com.example.mobilecellnetwork.gui.PhoneStyle fixedStyle;

    public CellphoneItem(Properties properties) {
        this(properties, null);
    }

    public CellphoneItem(Properties properties, com.example.mobilecellnetwork.gui.PhoneStyle fixedStyle) {
        super(properties);
        this.fixedStyle = fixedStyle;
    }

    public static boolean isPhone(ItemStack stack) {
        return stack.getItem() instanceof CellphoneItem;
    }

    public com.example.mobilecellnetwork.gui.PhoneStyle fixedStyle() {
        return fixedStyle;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.getData(com.example.mobilecellnetwork.capabilities.ModCapabilities.PHONE_DATA) == null) {
                return InteractionResultHolder.fail(stack);
            }
            openPhone(serverPlayer, stack, false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void openPhone(ServerPlayer serverPlayer, ItemStack stack, boolean heldView) {
        if (!isPhone(stack)) {
            return;
        }
        if (serverPlayer.getData(com.example.mobilecellnetwork.capabilities.ModCapabilities.PHONE_DATA) == null) {
            serverPlayer.displayClientMessage(Component.literal("Phone data is unavailable."), true);
            return;
        }
        java.util.UUID phoneId = com.example.mobilecellnetwork.network.PhoneDeviceService
                .prepareAndOpen(serverPlayer, stack);
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> {
                    com.example.mobilecellnetwork.network.PhoneDeviceService.bindMenu(
                            serverPlayer, phoneId, containerId);
                    PhoneMenu menu = new PhoneMenu(containerId, inventory);
                    menu.setDeviceId(phoneId);
                    return menu;
                },
                Component.translatable("gui.mobilecellnetwork.phone"));
        java.util.OptionalInt openedMenu = serverPlayer.openMenu(provider);
        if (openedMenu.isPresent()) {
            int menuId = openedMenu.getAsInt();
            com.example.mobilecellnetwork.network.PhoneDeviceService.sendState(serverPlayer, true);
            PacketDistributor.sendToPlayer(serverPlayer, new PhoneStyleSyncPacket(
                    serverPlayer.getData(com.example.mobilecellnetwork.capabilities.ModCapabilities.PHONE_DATA)
                            .getPhoneStyle()));
            PacketDistributor.sendToPlayer(serverPlayer, new OpenPhoneScreenPacket(menuId, heldView));
        } else {
            com.example.mobilecellnetwork.network.PhoneDeviceService.closeActiveSession(serverPlayer);
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.mobilecellnetwork.cellphone"));
    }
}

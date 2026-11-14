package com.example.mobilecellnetwork.registry;

import com.example.mobilecellnetwork.MobileCellNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MobileCellNetwork.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MOBILE_CELL_NETWORK =
            CREATIVE_MODE_TABS.register("mobile_cell_network", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mobilecellnetwork"))
                    .icon(() -> ModItems.CELLPHONE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COPPER_ANTENNA.get());
                        output.accept(ModItems.IRON_ANTENNA.get());
                        output.accept(ModItems.DIAMOND_ANTENNA.get());
                        output.accept(ModItems.SERVER_BLOCK.get());
                        output.accept(ModItems.ATM_BLOCK.get());
                        output.accept(ModItems.SHOP_TERMINAL_BLOCK.get());
                        output.accept(ModItems.PHONE_CHARGER.get());
                        output.accept(ModItems.EMERGENCY_DISPATCH.get());
                        output.accept(ModItems.CELLPHONE.get());
                        output.accept(ModItems.BANK_CARD.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}

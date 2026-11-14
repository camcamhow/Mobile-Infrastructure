package com.example.mobilecellnetwork.registry;

import com.example.mobilecellnetwork.MobileCellNetwork;
import com.example.mobilecellnetwork.item.EmergencyDispatchBlockItem;
import com.example.mobilecellnetwork.item.BankCardItem;
import com.example.mobilecellnetwork.item.CellphoneItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MobileCellNetwork.MOD_ID);

    public static final DeferredHolder<Item, BlockItem> COPPER_ANTENNA = ITEMS.register(
            "copper_antenna", () -> new BlockItem(ModBlocks.COPPER_ANTENNA.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> IRON_ANTENNA = ITEMS.register(
            "iron_antenna", () -> new BlockItem(ModBlocks.IRON_ANTENNA.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> DIAMOND_ANTENNA = ITEMS.register(
            "diamond_antenna", () -> new BlockItem(ModBlocks.DIAMOND_ANTENNA.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> TEST_ANTENNA = ITEMS.register(
            "test_antenna", () -> new BlockItem(ModBlocks.TEST_ANTENNA.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> SERVER_BLOCK = ITEMS.register(
            "server_block", () -> new BlockItem(ModBlocks.SERVER_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> SIGNAL_BOOSTER = ITEMS.register(
            "signal_booster", () -> new BlockItem(ModBlocks.SIGNAL_BOOSTER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> ATM_BLOCK = ITEMS.register(
            "atm_block", () -> new BlockItem(ModBlocks.ATM_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> SHOP_TERMINAL_BLOCK = ITEMS.register(
            "shop_terminal_block", () -> new BlockItem(ModBlocks.SHOP_TERMINAL_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> PHONE_CHARGER = ITEMS.register(
            "phone_charger", () -> new BlockItem(ModBlocks.PHONE_CHARGER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, EmergencyDispatchBlockItem> EMERGENCY_DISPATCH = ITEMS.register(
            "emergency_dispatch", () -> new EmergencyDispatchBlockItem(
                    ModBlocks.EMERGENCY_DISPATCH.get(), new Item.Properties()));

    public static final DeferredHolder<Item, CellphoneItem> CELLPHONE = ITEMS.register(
            "cellphone", () -> new CellphoneItem(phoneProperties(),
                    com.example.mobilecellnetwork.gui.PhoneStyle.IOS));
    public static final DeferredHolder<Item, CellphoneItem> ANDROID_PHONE = ITEMS.register(
            "android_phone", () -> new CellphoneItem(phoneProperties(),
                    com.example.mobilecellnetwork.gui.PhoneStyle.SAMSUNG_ONE_UI));
    public static final DeferredHolder<Item, CellphoneItem> SNAPPLE_PHONE = ITEMS.register(
            "snapple_phone", () -> new CellphoneItem(phoneProperties(),
                    com.example.mobilecellnetwork.gui.PhoneStyle.IOS));

    private static Item.Properties phoneProperties() {
        return new Item.Properties()
                    .stacksTo(1)
                    .durability(250)
                    .component(ModDataComponents.PHONE_CHARGE.get(), 10_000)
                    .component(ModDataComponents.PHONE_LOCKS.get(), java.util.List.of())
                    .component(ModDataComponents.PHONE_PIN.get(), "");
    }
    public static final DeferredHolder<Item, BankCardItem> BANK_CARD = ITEMS.register(
            "bank_card", () -> new BankCardItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}

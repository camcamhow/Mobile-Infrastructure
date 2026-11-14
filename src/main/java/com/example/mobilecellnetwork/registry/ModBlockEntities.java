package com.example.mobilecellnetwork.registry;

import com.example.mobilecellnetwork.MobileCellNetwork;
import com.example.mobilecellnetwork.blockentity.EmergencyDispatchBlockEntity;
import com.example.mobilecellnetwork.blockentity.AtmBlockEntity;
import com.example.mobilecellnetwork.blockentity.CopperAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.DiamondAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.IronAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity;
import com.example.mobilecellnetwork.blockentity.ServerBlockEntity;
import com.example.mobilecellnetwork.blockentity.ShopTerminalBlockEntity;
import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MobileCellNetwork.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperAntennaBlockEntity>> COPPER_ANTENNA =
            BLOCK_ENTITY_TYPES.register("copper_antenna", () -> BlockEntityType.Builder.of(
                    CopperAntennaBlockEntity::new, ModBlocks.COPPER_ANTENNA.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronAntennaBlockEntity>> IRON_ANTENNA =
            BLOCK_ENTITY_TYPES.register("iron_antenna", () -> BlockEntityType.Builder.of(
                    IronAntennaBlockEntity::new, ModBlocks.IRON_ANTENNA.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DiamondAntennaBlockEntity>> DIAMOND_ANTENNA =
            BLOCK_ENTITY_TYPES.register("diamond_antenna", () -> BlockEntityType.Builder.of(
                    DiamondAntennaBlockEntity::new, ModBlocks.DIAMOND_ANTENNA.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestAntennaBlockEntity>> TEST_ANTENNA =
            BLOCK_ENTITY_TYPES.register("test_antenna", () -> BlockEntityType.Builder.of(
                    TestAntennaBlockEntity::new, ModBlocks.TEST_ANTENNA.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerBlockEntity>> SERVER =
            BLOCK_ENTITY_TYPES.register("server_block", () -> BlockEntityType.Builder.of(
                    ServerBlockEntity::new, ModBlocks.SERVER_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SignalBoosterBlockEntity>> SIGNAL_BOOSTER =
            BLOCK_ENTITY_TYPES.register("signal_booster", () -> BlockEntityType.Builder.of(
                    SignalBoosterBlockEntity::new, ModBlocks.SIGNAL_BOOSTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AtmBlockEntity>> ATM =
            BLOCK_ENTITY_TYPES.register("atm_block", () -> BlockEntityType.Builder.of(
                    AtmBlockEntity::new, ModBlocks.ATM_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShopTerminalBlockEntity>> SHOP_TERMINAL =
            BLOCK_ENTITY_TYPES.register("shop_terminal_block", () -> BlockEntityType.Builder.of(
                    ShopTerminalBlockEntity::new, ModBlocks.SHOP_TERMINAL_BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhoneChargerBlockEntity>> PHONE_CHARGER =
            BLOCK_ENTITY_TYPES.register("phone_charger", () -> BlockEntityType.Builder.of(
                    PhoneChargerBlockEntity::new, ModBlocks.PHONE_CHARGER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmergencyDispatchBlockEntity>>
            EMERGENCY_DISPATCH = BLOCK_ENTITY_TYPES.register("emergency_dispatch", () ->
            BlockEntityType.Builder.of(EmergencyDispatchBlockEntity::new, ModBlocks.EMERGENCY_DISPATCH.get())
                    .build(null));

    private ModBlockEntities() {
    }
}

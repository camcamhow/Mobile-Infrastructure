package com.example.mobilecellnetwork.registry;

import com.example.mobilecellnetwork.MobileCellNetwork;
import com.example.mobilecellnetwork.block.EmergencyDispatchBlock;
import com.example.mobilecellnetwork.block.NetworkBlock;
import com.example.mobilecellnetwork.block.TestTowerBlock;
import com.example.mobilecellnetwork.block.TestTowerSectionBlock;
import com.example.mobilecellnetwork.block.PhoneChargerBlock;
import com.example.mobilecellnetwork.block.SignalBoosterBlock;
import com.example.mobilecellnetwork.blockentity.AtmBlockEntity;
import com.example.mobilecellnetwork.blockentity.CopperAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.DiamondAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.IronAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.ServerBlockEntity;
import com.example.mobilecellnetwork.blockentity.ShopTerminalBlockEntity;
import com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MobileCellNetwork.MOD_ID);
    public static final DeferredHolder<Block, NetworkBlock> POWER_TAP = BLOCKS.register("power_tap",
            () -> new com.example.mobilecellnetwork.block.PowerTapBlock(
                    BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredHolder<Block, NetworkBlock> COPPER_ANTENNA = BLOCKS.register(
            "copper_antenna",
            () -> new NetworkBlock(antennaProperties(2.0F), CopperAntennaBlockEntity::new)
    );
    public static final DeferredHolder<Block, NetworkBlock> IRON_ANTENNA = BLOCKS.register(
            "iron_antenna",
            () -> new NetworkBlock(antennaProperties(3.0F), IronAntennaBlockEntity::new)
    );
    public static final DeferredHolder<Block, NetworkBlock> DIAMOND_ANTENNA = BLOCKS.register(
            "diamond_antenna",
            () -> new NetworkBlock(antennaProperties(4.0F), DiamondAntennaBlockEntity::new)
    );
    public static final DeferredHolder<Block, NetworkBlock> TEST_ANTENNA = BLOCKS.register(
            "test_antenna",
            () -> new TestTowerBlock(antennaProperties(3.0F).noOcclusion())
    );
    public static final DeferredHolder<Block, NetworkBlock> TOWER_TIER_1 = BLOCKS.register("tower_tier_1",
            () -> new TestTowerBlock(antennaProperties(3).noOcclusion(),
                    com.example.mobilecellnetwork.block.TowerVariant.TIER_1));
    public static final DeferredHolder<Block, NetworkBlock> MONOPOLE_TOWER = BLOCKS.register("monopole_tower",
            () -> new TestTowerBlock(antennaProperties(3).noOcclusion(),
                    com.example.mobilecellnetwork.block.TowerVariant.MONOPOLE));
    public static final DeferredHolder<Block, TestTowerSectionBlock> MONOPOLE_TOWER_SECTION =
            BLOCKS.register("monopole_tower_section",
                    () -> towerSection(com.example.mobilecellnetwork.block.TowerVariant.MONOPOLE));
    public static final DeferredHolder<Block, NetworkBlock> TOWER_TIER_2 = BLOCKS.register("tower_tier_2",
            () -> new TestTowerBlock(antennaProperties(3).noOcclusion(),
                    com.example.mobilecellnetwork.block.TowerVariant.TIER_2));
    public static final DeferredHolder<Block, TestTowerSectionBlock> TOWER_TIER_1_SECTION =
            BLOCKS.register("tower_tier_1_section", () -> towerSection(com.example.mobilecellnetwork.block.TowerVariant.TIER_1));
    public static final DeferredHolder<Block, TestTowerSectionBlock> TOWER_TIER_2_SECTION =
            BLOCKS.register("tower_tier_2_section", () -> towerSection(com.example.mobilecellnetwork.block.TowerVariant.TIER_2));
    public static final DeferredHolder<Block, TestTowerSectionBlock> TEST_TOWER_SECTION = BLOCKS.register(
            "test_tower_section",
            () -> new TestTowerSectionBlock(antennaProperties(3.0F).noOcclusion().noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK))
    );
    public static final DeferredHolder<Block, NetworkBlock> SERVER_BLOCK = BLOCKS.register(
            "server_block",
            () -> new NetworkBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL).noOcclusion(),
                    ServerBlockEntity::new)
    );
    public static final DeferredHolder<Block, NetworkBlock> SIGNAL_BOOSTER = BLOCKS.register(
            "signal_booster", () -> new SignalBoosterBlock(
                    BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredHolder<Block, NetworkBlock> ATM_BLOCK = BLOCKS.register(
            "atm_block",
            () -> new NetworkBlock(BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL),
                    AtmBlockEntity::new)
    );
    public static final DeferredHolder<Block, NetworkBlock> SHOP_TERMINAL_BLOCK = BLOCKS.register(
            "shop_terminal_block",
            () -> new NetworkBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL),
                    ShopTerminalBlockEntity::new)
    );
    public static final DeferredHolder<Block, NetworkBlock> PHONE_CHARGER = BLOCKS.register(
            "phone_charger",
            () -> new PhoneChargerBlock(BlockBehaviour.Properties.of().strength(3.0F)
                    .sound(SoundType.METAL).noOcclusion())
    );
    public static final DeferredHolder<Block, EmergencyDispatchBlock> EMERGENCY_DISPATCH = BLOCKS.register(
            "emergency_dispatch",
            () -> new EmergencyDispatchBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL))
    );

    private ModBlocks() {
    }

    private static TestTowerSectionBlock towerSection(com.example.mobilecellnetwork.block.TowerVariant variant) {
        return new TestTowerSectionBlock(antennaProperties(3).noOcclusion().noLootTable()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK), variant);
    }

    private static BlockBehaviour.Properties antennaProperties(float strength) {
        return BlockBehaviour.Properties.of().strength(strength).sound(SoundType.COPPER);
    }
}

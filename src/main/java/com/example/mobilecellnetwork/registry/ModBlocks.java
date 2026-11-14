package com.example.mobilecellnetwork.registry;

import com.example.mobilecellnetwork.MobileCellNetwork;
import com.example.mobilecellnetwork.block.EmergencyDispatchBlock;
import com.example.mobilecellnetwork.block.NetworkBlock;
import com.example.mobilecellnetwork.blockentity.AtmBlockEntity;
import com.example.mobilecellnetwork.blockentity.CopperAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.DiamondAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.IronAntennaBlockEntity;
import com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity;
import com.example.mobilecellnetwork.blockentity.ServerBlockEntity;
import com.example.mobilecellnetwork.blockentity.ShopTerminalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MobileCellNetwork.MOD_ID);

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
    public static final DeferredHolder<Block, NetworkBlock> SERVER_BLOCK = BLOCKS.register(
            "server_block",
            () -> new NetworkBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL),
                    ServerBlockEntity::new)
    );
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
            () -> new NetworkBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL),
                    PhoneChargerBlockEntity::new)
    );
    public static final DeferredHolder<Block, EmergencyDispatchBlock> EMERGENCY_DISPATCH = BLOCKS.register(
            "emergency_dispatch",
            () -> new EmergencyDispatchBlock(BlockBehaviour.Properties.of().strength(4.0F).sound(SoundType.METAL))
    );

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties antennaProperties(float strength) {
        return BlockBehaviour.Properties.of().strength(strength).sound(SoundType.COPPER);
    }
}

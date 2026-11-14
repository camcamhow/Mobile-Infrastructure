package com.example.mobilecellnetwork.network;

import com.example.mobilecellnetwork.config.MobileCellNetworkConfig;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class SignalAttenuation {
    private static final TagKey<Block> BLOCKING = tag("blocking");
    private static final TagKey<Block> HIGH = tag("high");
    private static final TagKey<Block> MEDIUM = tag("medium");
    private static final TagKey<Block> LOW = tag("low");
    private static final TagKey<Block> NONE = tag("none");
    private static final ConcurrentMap<BlockState, Double> CACHE = new ConcurrentHashMap<>();

    private SignalAttenuation() {
    }

    public static double getAttenuation(BlockState state) {
        return CACHE.computeIfAbsent(state, SignalAttenuation::lookup);
    }

    private static double lookup(BlockState state) {
        if (state.isAir()) return 0.0D;
        if (state.is(BlockTags.LEAVES)) return MobileCellNetworkConfig.ATTENUATION_LOW.get();
        if (state.is(BLOCKING)) return MobileCellNetworkConfig.ATTENUATION_BLOCKING.get();
        if (state.is(HIGH)) return MobileCellNetworkConfig.ATTENUATION_HIGH.get();
        if (state.is(MEDIUM)) return MobileCellNetworkConfig.ATTENUATION_MEDIUM.get();
        if (state.is(LOW)) return MobileCellNetworkConfig.ATTENUATION_LOW.get();
        if (state.is(NONE)) return MobileCellNetworkConfig.ATTENUATION_NONE.get();
        return MobileCellNetworkConfig.UNKNOWN_BLOCK_ATTENUATION.get();
    }

    private static TagKey<Block> tag(String name) {
        return TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath("mobilecellnetwork", "signal_attenuation/" + name));
    }
}

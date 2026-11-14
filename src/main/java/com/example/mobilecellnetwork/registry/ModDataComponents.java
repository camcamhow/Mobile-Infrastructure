package com.example.mobilecellnetwork.registry;

import com.example.mobilecellnetwork.MobileCellNetwork;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MobileCellNetwork.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PHONE_CHARGE =
            DATA_COMPONENTS.registerComponentType(
                    "phone_charge",
                    builder -> builder
                            .persistent(Codec.intRange(0, 10_000))
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
            );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> PHONE_ID =
            DATA_COMPONENTS.registerComponentType("phone_id", builder -> builder
                    .persistent(Codec.STRING.xmap(UUID::fromString, UUID::toString))
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> PHONE_LOCKS =
            DATA_COMPONENTS.registerComponentType("phone_locks", builder -> builder
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.fromCodec(Codec.STRING.listOf())));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> PHONE_PIN =
            DATA_COMPONENTS.registerComponentType("phone_pin", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private ModDataComponents() {
    }
}

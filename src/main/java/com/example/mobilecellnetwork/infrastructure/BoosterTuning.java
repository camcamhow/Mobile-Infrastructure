package com.example.mobilecellnetwork.infrastructure;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import net.minecraft.nbt.CompoundTag;

public record BoosterTuning(String networkId) {
    public BoosterTuning {
        Objects.requireNonNull(networkId);
    }

    public boolean automatic() {
        return networkId.isEmpty();
    }

    public boolean matches(String candidateNetwork) {
        return automatic() || networkId.equals(candidateNetwork);
    }

    public <T> T strongest(List<T> candidates, Function<T, String> network, ToDoubleFunction<T> strength) {
        return candidates.stream().filter(candidate -> matches(network.apply(candidate)))
                .filter(candidate -> strength.applyAsDouble(candidate) > 0)
                .max(Comparator.comparingDouble(strength)).orElse(null);
    }

    public void save(CompoundTag tag) {
        tag.putString("boosterNetwork", networkId);
    }

    public static BoosterTuning load(CompoundTag tag) {
        return new BoosterTuning(tag.getString("boosterNetwork"));
    }
}

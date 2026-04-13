package com.sypztep.client.provider;

import com.sypztep.Temporature;
import com.sypztep.common.data.BiomeTemperatureData;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.sypztep.system.temperature.TemperatureHelper.cToMc;
import static com.sypztep.system.temperature.TemperatureHelper.fToMc;

public class TemporatureBiomeTemperatureProvider extends FabricDynamicRegistryProvider {

    private HolderLookup.RegistryLookup<Biome> biomeLookup;

    public TemporatureBiomeTemperatureProvider(FabricPackOutput out, CompletableFuture<HolderLookup.Provider> registries) {
        super(out, registries);
    }

    @Override
    protected void configure(HolderLookup.@NonNull Provider registries, @NonNull Entries entries) {
        biomeLookup = registries.lookupOrThrow(Registries.BIOME);

        add(entries, "nether", cToMc(55), cToMc(65),
                Biomes.NETHER_WASTES, Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST,
                Biomes.SOUL_SAND_VALLEY, Biomes.BASALT_DELTAS);

        add(entries, "end", cToMc(-5), cToMc(5),
                Biomes.THE_END, Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS,
                Biomes.END_BARRENS, Biomes.SMALL_END_ISLANDS);

        add(entries, "void", cToMc(-30), cToMc(-10),
                Biomes.THE_VOID);

        add(entries, "desert", fToMc(48), fToMc(115),
                Biomes.DESERT);

        add(entries, "badlands", fToMc(84), fToMc(120),
                Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);

        add(entries, "savanna", fToMc(48), fToMc(84),
                Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);

        add(entries, "jungle", fToMc(76), fToMc(89),
                Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE);

        add(entries, "plains", fToMc(32), fToMc(72),
                Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);

        add(entries, "forest", fToMc(25), fToMc(68),
                Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
                Biomes.DARK_FOREST, Biomes.PALE_GARDEN);

        add(entries, "swamp", fToMc(48), fToMc(72),
                Biomes.SWAMP, Biomes.MANGROVE_SWAMP);

        add(entries, "cherry", cToMc(-2), cToMc(18),
                Biomes.CHERRY_GROVE);

        add(entries, "mushroom", cToMc(5), cToMc(20),
                Biomes.MUSHROOM_FIELDS);

        add(entries, "taiga", fToMc(12), fToMc(48),
                Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA,
                Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS);

        add(entries, "snowy_taiga", fToMc(0), fToMc(32),
                Biomes.SNOWY_TAIGA);

        add(entries, "snowy", fToMc(8), fToMc(30),
                Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_BEACH, Biomes.SNOWY_SLOPES,
                Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.GROVE);

        add(entries, "frozen_ocean", fToMc(12), fToMc(31),
                Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.FROZEN_RIVER);

        add(entries, "cold_ocean", fToMc(25), fToMc(42),
                Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN);

        add(entries, "ocean", fToMc(40), fToMc(60),
                Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.RIVER);

        add(entries, "warm_ocean", fToMc(67), fToMc(76),
                Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN);

        add(entries, "beach", fToMc(40), fToMc(70),
                Biomes.BEACH, Biomes.STONY_SHORE);

        add(entries, "stony_peaks", cToMc(0), cToMc(20),
                Biomes.STONY_PEAKS);

        add(entries, "dripstone", cToMc(5), cToMc(15),
                Biomes.DRIPSTONE_CAVES);

        add(entries, "lush_caves", cToMc(8), cToMc(18),
                Biomes.LUSH_CAVES);

        add(entries, "deep_dark", cToMc(-4), cToMc(8),
                Biomes.DEEP_DARK);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private void add(Entries entries, String id, double low, double high, ResourceKey<Biome>... keys) {
        Holder<Biome>[] holders = new Holder[keys.length];
        for (int i = 0; i < keys.length; i++) holders[i] = biomeLookup.getOrThrow(keys[i]);
        HolderSet<Biome> set = HolderSet.direct(holders);
        entries.add(BiomeTemperatureData.key(Temporature.id(id)),
                new BiomeTemperatureData(set, low, high, Optional.empty()));
    }
    @SafeVarargs
    @SuppressWarnings("unchecked")
    private void add(Entries entries, String id, double low, double high, double waterTemp, ResourceKey<Biome>... keys) {
        Holder<Biome>[] holders = new Holder[keys.length];
        for (int i = 0; i < keys.length; i++) holders[i] = biomeLookup.getOrThrow(keys[i]);
        HolderSet<Biome> set = HolderSet.direct(holders);
        entries.add(BiomeTemperatureData.key(Temporature.id(id)),
                new BiomeTemperatureData(set, low, high, Optional.of(waterTemp)));
    }

    @Override
    public @NotNull String getName() {
        return Temporature.MODID + " Biome Temperature Data";
    }
}

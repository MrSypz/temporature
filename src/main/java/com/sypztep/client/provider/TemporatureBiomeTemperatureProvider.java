package com.sypztep.client.provider;

import com.sypztep.Temporature;
import com.sypztep.common.data.BiomeTemperatureData;
import com.sypztep.system.temperature.TemperatureHelper;
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

public class TemporatureBiomeTemperatureProvider extends FabricDynamicRegistryProvider {

    private HolderLookup.RegistryLookup<Biome> biomeLookup;

    public TemporatureBiomeTemperatureProvider(FabricPackOutput out, CompletableFuture<HolderLookup.Provider> registries) {
        super(out, registries);
    }

    @Override
    protected void configure(HolderLookup.@NonNull Provider registries, @NonNull Entries entries) {
        biomeLookup = registries.lookupOrThrow(Registries.BIOME);

        // All lowTemp/highTemp/waterTemp values declared in Celsius and converted to MC at datagen time.
        // JSON stores MC doubles; runtime layers work in MC exclusively.

        add(entries, "nether", TemperatureHelper.cToMc(55.0), TemperatureHelper.cToMc(65.0), null,
                Biomes.NETHER_WASTES, Biomes.CRIMSON_FOREST, Biomes.WARPED_FOREST,
                Biomes.SOUL_SAND_VALLEY, Biomes.BASALT_DELTAS);

        add(entries, "end", TemperatureHelper.cToMc(-5.0), TemperatureHelper.cToMc(5.0), TemperatureHelper.cToMc(-15.0),
                Biomes.THE_END, Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS,
                Biomes.END_BARRENS, Biomes.SMALL_END_ISLANDS);

        add(entries, "void", TemperatureHelper.cToMc(-30.0), TemperatureHelper.cToMc(-10.0), TemperatureHelper.cToMc(-31.0),
                Biomes.THE_VOID);

        add(entries, "desert", TemperatureHelper.cToMc(20.0), TemperatureHelper.cToMc(42.0), TemperatureHelper.cToMc(18.0),
                Biomes.DESERT, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);

        add(entries, "savanna", TemperatureHelper.cToMc(15.0), TemperatureHelper.cToMc(32.0), TemperatureHelper.cToMc(20.0),
                Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);

        add(entries, "jungle", TemperatureHelper.cToMc(15.0), TemperatureHelper.cToMc(32.0), TemperatureHelper.cToMc(22.0),
                Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE);

        add(entries, "plains", TemperatureHelper.cToMc(-5.0), TemperatureHelper.cToMc(18.0), TemperatureHelper.cToMc(12.0),
                Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW);

        add(entries, "forest", TemperatureHelper.cToMc(-6.0), TemperatureHelper.cToMc(15.0), TemperatureHelper.cToMc(10.0),
                Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST,
                Biomes.DARK_FOREST, Biomes.PALE_GARDEN);

        add(entries, "swamp", TemperatureHelper.cToMc(0.0), TemperatureHelper.cToMc(22.0), TemperatureHelper.cToMc(16.0),
                Biomes.SWAMP, Biomes.MANGROVE_SWAMP);

        add(entries, "taiga", TemperatureHelper.cToMc(-15.0), TemperatureHelper.cToMc(8.0), TemperatureHelper.cToMc(4.0),
                Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA,
                Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS);

        add(entries, "snowy_taiga", TemperatureHelper.cToMc(-28.0), TemperatureHelper.cToMc(-8.0), TemperatureHelper.cToMc(-2.0),
                Biomes.SNOWY_TAIGA);

        add(entries, "snowy", TemperatureHelper.cToMc(-35.0), TemperatureHelper.cToMc(-15.0), TemperatureHelper.cToMc(-5.0),
                Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.SNOWY_BEACH, Biomes.SNOWY_SLOPES,
                Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.GROVE);

        add(entries, "frozen_ocean", TemperatureHelper.cToMc(-22.0), TemperatureHelper.cToMc(-12.0), TemperatureHelper.cToMc(-5.0),
                Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.FROZEN_RIVER);

        add(entries, "cold_ocean", TemperatureHelper.cToMc(-5.0), TemperatureHelper.cToMc(10.0), TemperatureHelper.cToMc(5.0),
                Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN);

        add(entries, "ocean", TemperatureHelper.cToMc(2.0), TemperatureHelper.cToMc(18.0), TemperatureHelper.cToMc(14.0),
                Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.RIVER);

        add(entries, "warm_ocean", TemperatureHelper.cToMc(18.0), TemperatureHelper.cToMc(30.0), TemperatureHelper.cToMc(24.0),
                Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN);

        add(entries, "beach", TemperatureHelper.cToMc(5.0), TemperatureHelper.cToMc(25.0), TemperatureHelper.cToMc(18.0),
                Biomes.BEACH, Biomes.STONY_SHORE);

        add(entries, "mushroom", TemperatureHelper.cToMc(5.0), TemperatureHelper.cToMc(20.0), TemperatureHelper.cToMc(14.0),
                Biomes.MUSHROOM_FIELDS);

        add(entries, "cherry", TemperatureHelper.cToMc(-2.0), TemperatureHelper.cToMc(18.0), TemperatureHelper.cToMc(12.0),
                Biomes.CHERRY_GROVE);

        add(entries, "stony_peaks", TemperatureHelper.cToMc(0.0), TemperatureHelper.cToMc(20.0), TemperatureHelper.cToMc(8.0),
                Biomes.STONY_PEAKS);

        add(entries, "dripstone", TemperatureHelper.cToMc(5.0), TemperatureHelper.cToMc(15.0), TemperatureHelper.cToMc(10.0),
                Biomes.DRIPSTONE_CAVES);

        add(entries, "lush_caves", TemperatureHelper.cToMc(8.0), TemperatureHelper.cToMc(18.0), TemperatureHelper.cToMc(14.0),
                Biomes.LUSH_CAVES);

        add(entries, "deep_dark", TemperatureHelper.cToMc(0.0), TemperatureHelper.cToMc(8.0), TemperatureHelper.cToMc(6.0),
                Biomes.DEEP_DARK);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    private void add(Entries entries, String id, double low, double high, Double waterTemp, ResourceKey<Biome>... keys) {
        Holder<Biome>[] holders = new Holder[keys.length];
        for (int i = 0; i < keys.length; i++) holders[i] = biomeLookup.getOrThrow(keys[i]);
        HolderSet<Biome> set = HolderSet.direct(holders);
        entries.add(BiomeTemperatureData.key(Temporature.id(id)),
                new BiomeTemperatureData(set, low, high, Optional.ofNullable(waterTemp)));
    }

    @Override
    public @NotNull String getName() {
        return Temporature.MODID + " Biome Temperature Data";
    }
}

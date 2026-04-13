package com.sypztep.temporature.client.provider;

import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.common.data.BlockTemperatureData;
import com.sypztep.temporature.system.temperature.TemperatureHelper;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TemporatureBlockTemperatureProvider extends FabricDynamicRegistryProvider {

    private HolderLookup.RegistryLookup<Block> blockLookup;

    public TemporatureBlockTemperatureProvider(FabricPackOutput out, CompletableFuture<HolderLookup.Provider> registries) {
        super(out, registries);
    }

    @Override
    protected void configure(HolderLookup.@NonNull Provider registries, @NonNull Entries entries) {
        blockLookup = registries.lookupOrThrow(Registries.BLOCK);

        Map<String, String> lit = Map.of("lit", "true");

        // Values chosen to match Cold Sweat's defaults (converted °F → °C).
        // Format: (id, tempC, range, maxEffectC, fade, logarithmic, predicates, blocks...)

        // ── Heat sources ──
        add(entries, "lava", TemperatureHelper.cToMc(11.1), 7, TemperatureHelper.cToMc(111.0), true, true, Map.of(), Blocks.LAVA);
        add(entries, "fire", TemperatureHelper.cToMc(8.3), 7, TemperatureHelper.cToMc(25.0), true, true, Map.of(), Blocks.FIRE);
        add(entries, "soul_fire", TemperatureHelper.cToMc(11.1), 7, TemperatureHelper.cToMc(30.0), true, true, Map.of(), Blocks.SOUL_FIRE);
        add(entries, "campfire", TemperatureHelper.cToMc(8.3), 7, TemperatureHelper.cToMc(25.0), true, true, lit, Blocks.CAMPFIRE);
        add(entries, "soul_campfire", TemperatureHelper.cToMc(11.1), 7, TemperatureHelper.cToMc(30.0), true, true, lit, Blocks.SOUL_CAMPFIRE);
        add(entries, "magma_block", TemperatureHelper.cToMc(6.7), 3, TemperatureHelper.cToMc(26.7), true, false, Map.of(), Blocks.MAGMA_BLOCK);
        add(entries, "furnace", TemperatureHelper.cToMc(6.7), 4, TemperatureHelper.cToMc(20.0), true, false, lit, Blocks.FURNACE);
        add(entries, "blast_furnace", TemperatureHelper.cToMc(8.3), 4, TemperatureHelper.cToMc(25.0), true, false, lit, Blocks.BLAST_FURNACE);
        add(entries, "smoker", TemperatureHelper.cToMc(5.6), 4, TemperatureHelper.cToMc(16.7), true, false, lit, Blocks.SMOKER);
        add(entries, "lantern", TemperatureHelper.cToMc(2.8), 4, TemperatureHelper.cToMc(11.1), true, false, Map.of(), Blocks.LANTERN, Blocks.SOUL_LANTERN);
        add(entries, "torch", TemperatureHelper.cToMc(1.7), 3, TemperatureHelper.cToMc(8.3), true, false, Map.of(), Blocks.TORCH, Blocks.WALL_TORCH, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH);
        add(entries, "glowstone", TemperatureHelper.cToMc(1.1), 3, TemperatureHelper.cToMc(5.6), true, false, Map.of(), Blocks.GLOWSTONE, Blocks.SHROOMLIGHT);
        add(entries, "redstone_lamp_lit", TemperatureHelper.cToMc(1.1), 3, TemperatureHelper.cToMc(5.6), true, false, lit, Blocks.REDSTONE_LAMP);

        // ── Cold sources ──
        add(entries, "blue_ice", TemperatureHelper.cToMc(-8.9), 4, TemperatureHelper.cToMc(-35.6), true, true, Map.of(), Blocks.BLUE_ICE);
        add(entries, "packed_ice", TemperatureHelper.cToMc(-6.7), 4, TemperatureHelper.cToMc(-26.7), true, true, Map.of(), Blocks.PACKED_ICE);
        add(entries, "ice", TemperatureHelper.cToMc(-3.3), 4, TemperatureHelper.cToMc(-13.3), true, false, Map.of(), Blocks.ICE, Blocks.FROSTED_ICE);
        add(entries, "snow_block", TemperatureHelper.cToMc(-2.2), 3, TemperatureHelper.cToMc(-11.1), true, false, Map.of(), Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW);
        add(entries, "snow_layer", TemperatureHelper.cToMc(-1.1), 2, TemperatureHelper.cToMc(-5.6), true, false, Map.of(), Blocks.SNOW);
    }

    private void add(Entries entries, String id, double temp, int range, double maxEffect, boolean fade, boolean log, Map<String, String> predicates, Block... blocks) {
        @SuppressWarnings("unchecked")
        Holder<Block>[] holders = new Holder[blocks.length];
        for (int i = 0; i < blocks.length; i++)
            holders[i] = blockLookup.getOrThrow(BuiltInRegistries.BLOCK.getResourceKey(blocks[i]).orElseThrow());
        HolderSet<Block> set = HolderSet.direct(holders);
        entries.add(BlockTemperatureData.key(Temporature.id(id)),
                new BlockTemperatureData(set, temp, range, maxEffect, fade, log, Optional.empty(), Optional.empty(), predicates));
    }

    @Override
    public @NotNull String getName() {
        return Temporature.MODID + " Block Temperature Data";
    }
}

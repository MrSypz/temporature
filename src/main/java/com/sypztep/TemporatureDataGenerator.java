package com.sypztep;

import com.sypztep.client.provider.*;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageType;

public class TemporatureDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

		pack.addProvider(TemporatureLanguageProvider::new);
		pack.addProvider(TemporatureDamageTypeProvider::new);
		pack.addProvider(TemporatureDamageTypeTagProvider::new);

		pack.addProvider(TemporatureBiomeTemperatureProvider::new);
		pack.addProvider(TemporatureDimensionTemperatureProvider::new);
		pack.addProvider(TemporatureStructureTemperatureProvider::new);
		pack.addProvider(TemporatureBlockTemperatureProvider::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Temporature.BIOME_TEMPERATURES, _ -> {});
		registryBuilder.add(Temporature.DIMENSION_TEMPERATURES, _ -> {});
		registryBuilder.add(Temporature.STRUCTURE_TEMPERATURES, _ -> {});
		registryBuilder.add(Temporature.BLOCK_TEMPERATURES, _ -> {});
		registryBuilder.add(Registries.DAMAGE_TYPE, context -> {
			context.register(Temporature.HEATSTROKE, new DamageType("heatstroke", 0.0f));
			context.register(Temporature.HYPOTHERMIA, new DamageType("hypothermia", 0.0f));
		});
	}
}


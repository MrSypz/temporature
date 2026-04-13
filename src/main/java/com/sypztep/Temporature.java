package com.sypztep;

import com.sypztep.common.data.BiomeTemperatureData;
import com.sypztep.common.data.BlockTemperatureData;
import com.sypztep.common.data.DimensionTemperatureData;
import com.sypztep.common.data.StructureTemperatureData;
import com.sypztep.common.network.ConfigSyncPayloadS2C;
import com.sypztep.config.TemporatureServerConfig;
import com.sypztep.plateau.common.api.PlateauDamageTypes;
import com.sypztep.system.temperature.TemperatureLayerRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Temporature implements ModInitializer {
	public static final String MODID = "temporature";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	public static final ResourceKey<DamageType> HYPOTHERMIA = PlateauDamageTypes.createKey(id("hypothermia"));
	public static final ResourceKey<DamageType> HEATSTROKE = PlateauDamageTypes.createKey(id("heatstroke"));

	public static final ResourceKey<Registry<BiomeTemperatureData>> BIOME_TEMPERATURES =
			ResourceKey.createRegistryKey(id("biome_temperature"));

	public static final ResourceKey<Registry<DimensionTemperatureData>> DIMENSION_TEMPERATURES =
			ResourceKey.createRegistryKey(id("dimension_temperature"));

	public static final ResourceKey<Registry<StructureTemperatureData>> STRUCTURE_TEMPERATURES =
			ResourceKey.createRegistryKey(id("structure_temperature"));

	public static final ResourceKey<Registry<BlockTemperatureData>> BLOCK_TEMPERATURES =
			ResourceKey.createRegistryKey(id("block_temperature"));
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MODID, path);
	}
	@Override
	public void onInitialize() {
		TemporatureServerConfig.HANDLER.load();

		DynamicRegistries.registerSynced(BIOME_TEMPERATURES, BiomeTemperatureData.CODEC);
		DynamicRegistries.registerSynced(DIMENSION_TEMPERATURES, DimensionTemperatureData.CODEC);
		DynamicRegistries.registerSynced(STRUCTURE_TEMPERATURES, StructureTemperatureData.CODEC);
		DynamicRegistries.registerSynced(BLOCK_TEMPERATURES, BlockTemperatureData.CODEC);

		TemperatureLayerRegistry.init();

		PayloadTypeRegistry.clientboundPlay().register(ConfigSyncPayloadS2C.ID, ConfigSyncPayloadS2C.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, _, _) ->
				ConfigSyncPayloadS2C.send(handler.getPlayer()));
	}
}
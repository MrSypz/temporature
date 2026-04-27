package com.sypztep.temporature;

import com.sypztep.temporature.api.TemporatureApi;
import com.sypztep.temporature.common.PlayerTemperatureComponent;
import com.sypztep.temporature.common.data.BiomeTemperatureData;
import com.sypztep.temporature.common.data.BlockTemperatureData;
import com.sypztep.temporature.common.data.DimensionTemperatureData;
import com.sypztep.temporature.common.data.StructureTemperatureData;
import com.sypztep.temporature.common.network.ConfigSyncManager;
import com.sypztep.temporature.common.network.ConfigSyncRegistry;
import com.sypztep.temporature.common.network.payload.SyncAckC2S;
import com.sypztep.temporature.common.network.payload.SyncDataS2C;
import com.sypztep.temporature.common.network.payload.SyncHelloS2C;
import com.sypztep.temporature.common.network.payload.SyncResponseC2S;
import com.sypztep.temporature.config.ConfigSyncUtil;
import com.sypztep.temporature.config.TemporatureServerConfig;
import com.sypztep.plateau.common.api.PlateauDamageTypes;
import com.sypztep.temporature.system.temperature.TemperatureLayerRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Temporature implements ModInitializer {
	public static final String MODID  = "temporature";
	public static final Logger LOGGER = LoggerFactory.getLogger("Temporature");

	public static final ResourceKey<DamageType> HYPOTHERMIA = PlateauDamageTypes.createKey(id("hypothermia"));
	public static final ResourceKey<DamageType> HEATSTROKE  = PlateauDamageTypes.createKey(id("heatstroke"));

	public static final ResourceKey<Registry<BiomeTemperatureData>>     BIOME_TEMPERATURES     = ResourceKey.createRegistryKey(id("biome_temperature"));
	public static final ResourceKey<Registry<DimensionTemperatureData>> DIMENSION_TEMPERATURES = ResourceKey.createRegistryKey(id("dimension_temperature"));
	public static final ResourceKey<Registry<StructureTemperatureData>> STRUCTURE_TEMPERATURES = ResourceKey.createRegistryKey(id("structure_temperature"));
	public static final ResourceKey<Registry<BlockTemperatureData>>     BLOCK_TEMPERATURES     = ResourceKey.createRegistryKey(id("block_temperature"));

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MODID, path);
	}

	@Override
	public void onInitialize() {
		TemporatureServerConfig.HANDLER.load();

		DynamicRegistries.registerSynced(BIOME_TEMPERATURES,     BiomeTemperatureData.CODEC);
		DynamicRegistries.registerSynced(DIMENSION_TEMPERATURES, DimensionTemperatureData.CODEC);
		DynamicRegistries.registerSynced(STRUCTURE_TEMPERATURES, StructureTemperatureData.CODEC);
		DynamicRegistries.registerSynced(BLOCK_TEMPERATURES,     BlockTemperatureData.CODEC);

		TemperatureLayerRegistry.init();

		// ── Register this mod's config for universal sync ─────────────────────
		//
		// The applier also sets the "synced from server" flag so the client config
		// is restored to the local file on disconnect (see TemporatureClient).
		ConfigSyncRegistry.register(
				MODID,
				TemporatureServerConfig::getInstance,
				TemporatureServerConfig.class,
				cfg -> {
					TemporatureServerConfig.applyFrom(cfg);
					TemporatureServerConfig.setSyncedFromServer(true);
				},
				ConfigSyncUtil::syncHashCode
		);

		TemporatureApi.registerRateModifier((player, changeBy, worldTemp, _) -> {
			TemporatureServerConfig cfg = TemporatureServerConfig.getInstance();
			if (!cfg.enableAdaptation) return changeBy;

			PlayerTemperatureComponent comp = TemporatureApi.getComponent(player);
			float adaptShift = comp.getAdaptedBiomeTemp() - PlayerTemperatureComponent.NEUTRAL_TEMP;
			if (adaptShift == 0) return changeBy;

			double worldShift = worldTemp - PlayerTemperatureComponent.NEUTRAL_TEMP;
			if (Math.signum(adaptShift) == Math.signum(worldShift)) {
				double strength = Math.min(Math.abs(adaptShift) / cfg.maxAdaptShift, 1.0) * cfg.adaptStrength;
				changeBy *= (1.0 - strength);
			}
			return changeBy;
		});

		registerPayloads();
		registerEvents();
	}

	private static void registerPayloads() {
		PayloadTypeRegistry.clientboundPlay().register(SyncHelloS2C.ID, SyncHelloS2C.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncDataS2C.ID,  SyncDataS2C.CODEC);

		PayloadTypeRegistry.serverboundPlay().register(SyncResponseC2S.ID, SyncResponseC2S.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SyncAckC2S.ID,      SyncAckC2S.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SyncResponseC2S.ID,
				(pkt, ctx) -> ConfigSyncManager.onSyncResponse(ctx.player(), pkt));
		ServerPlayNetworking.registerGlobalReceiver(SyncAckC2S.ID,
				(pkt, ctx) -> ConfigSyncManager.onSyncAck(ctx.player(), pkt));
	}

	private static void registerEvents() {
		ServerPlayConnectionEvents.JOIN.register((handler, _, server) -> {
			if (server.isDedicatedServer())
				ConfigSyncManager.onPlayerJoin(handler.getPlayer(), server);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, _) ->
				ConfigSyncManager.onPlayerDisconnect(handler.getPlayer()));
	}
}
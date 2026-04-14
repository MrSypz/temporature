package com.sypztep.temporature.client.provider;

import com.sypztep.temporature.Temporature;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class TemporatureLanguageProvider extends FabricLanguageProvider {
    public TemporatureLanguageProvider(FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, "en_us", registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.@NonNull Provider wrapperLookup, @NonNull TranslationBuilder translate) {
        generateConfig(translate);

        translate.add("death.attack.hypothermia", "%1$s froze to death");
        translate.add("death.attack.hypothermia.player", "%1$s froze to death whilst fighting %2$s");
        translate.add("death.attack.heatstroke", "%1$s died of heatstroke");
        translate.add("death.attack.heatstroke.player", "%1$s died of heatstroke whilst fighting %2$s");

        translate.add("key.temporature.metabolism", "Metabolism");
        translate.add("key.category.temporature.survival", "Survival");

        translate.add("screen.temporature.config_sync.title", "Server Config Sync");
        translate.add("screen.temporature.config_sync.description", "The server wants to override your local Temporature config. Review the changes below:");
        translate.add("screen.temporature.config_sync.hash", "Hash: %s");
        translate.add("screen.temporature.config_sync.changes", "Changes (%s)");
        translate.add("screen.temporature.config_sync.no_changes", "No differences — configs already match.");
        translate.add("screen.temporature.config_sync.accept", "Accept");
        translate.add("screen.temporature.config_sync.deny", "Deny");
        translate.add("screen.temporature.config_sync.denied", "You denied the server config sync.");
    }

    private void generateConfig(TranslationBuilder translate) {
        String cfg = "config." + Temporature.MODID + ".";

        // Main title
        translate.add(cfg + "title", "Temporature Configuration");
        translate.add(cfg + "server_locked", "This setting is controlled by the server and cannot be changed while connected");

        // ── Temperature Category ──
        translate.add(cfg + "category.temperature", "Temperature");
        translate.add(cfg + "category.temperature.tooltip", "Body temperature, weather exposure, and thermal damage");

        // Core Temperature group
        translate.add(cfg + "temperature.core", "Core Temperature");
        translate.add(cfg + "temperature.core.description", "Controls how body temperature accumulates from the environment and when damage is dealt");
        translate.add(cfg + "temperature.enable", "Enable Temperature System");
        translate.add(cfg + "temperature.enable.description", "Master toggle for the temperature system including body/world temperature and thermal damage");
        translate.add(cfg + "temperature.chase_rate", "Body Chase Rate");
        translate.add(cfg + "temperature.chase_rate.description", "Multiplier on how fast body temperature changes when outside the habitable band");
        translate.add(cfg + "temperature.min_habitable", "Min Habitable Temperature");
        translate.add(cfg + "temperature.min_habitable.description", "Lower bound of the safe temperature band in MC units (1 MC = 25°C). Below this, body temperature starts dropping");
        translate.add(cfg + "temperature.max_habitable", "Max Habitable Temperature");
        translate.add(cfg + "temperature.max_habitable.description", "Upper bound of the safe temperature band in MC units. Above this, body temperature starts rising");
        translate.add(cfg + "temperature.damage_interval", "Damage Interval (ticks)");
        translate.add(cfg + "temperature.damage_interval.description", "Base ticks between temperature damage pulses at the Freezing/Burning threshold");
        translate.add(cfg + "temperature.base_damage", "Base Damage");
        translate.add(cfg + "temperature.base_damage.description", "Amount of damage dealt per temperature damage pulse");
        translate.add(cfg + "temperature.block_scan_radius", "Block Scan Radius");
        translate.add(cfg + "temperature.block_scan_radius.description", "Cubic radius around the player scanned for heat/cold emitting blocks (campfires, ice, lava, etc.)");

        // ── Wetness group ──
        translate.add(cfg + "wetness.group", "Wetness");
        translate.add(cfg + "wetness.group.description", "Controls how water exposure affects temperature. Being wet accumulates a water temperature value that lingers after leaving the water");
        translate.add(cfg + "wetness.water_soak_speed", "Water Soak Speed");
        translate.add(cfg + "wetness.water_soak_speed.description", "How fast wetness and water temperature accumulate while submerged per tick");
        translate.add(cfg + "wetness.rain_soak_speed", "Rain Soak Speed");
        translate.add(cfg + "wetness.rain_soak_speed.description", "How fast wetness and water temperature accumulate while standing in rain per tick");
        translate.add(cfg + "wetness.max_rain_wetness", "Max Rain Wetness");
        translate.add(cfg + "wetness.max_rain_wetness.description", "Maximum wetness level reachable from rain alone (0.0 to 1.0). Rain cannot soak beyond this cap");
        translate.add(cfg + "wetness.dry_rate", "Dry Rate");
        translate.add(cfg + "wetness.dry_rate.description", "Base rate at which wetness decreases per tick when not in water or rain");
        translate.add(cfg + "wetness.hot_dry_bonus", "Hot Dry Bonus");
        translate.add(cfg + "wetness.hot_dry_bonus.description", "Extra drying speed per tick when body temperature is above the warm threshold, scaled by how hot you are");
        translate.add(cfg + "wetness.cold_dry_multiplier", "Cold Dry Multiplier");
        translate.add(cfg + "wetness.cold_dry_multiplier.description", "Multiplier on drying speed when body temperature is below the hypothermia threshold. Lower values = slower drying in cold");
        translate.add(cfg + "wetness.default_water_temp", "Default Water Temperature");
        translate.add(cfg + "wetness.default_water_temp.description", "Fallback water temperature offset in MC units, used when a biome does not define its own waterTemp. Negative values cool the player (default: -23.3\u00B0C = -0.93 MC)");

        // ── Adaptation group ──
        translate.add(cfg + "adaptation.group", "Biome Adaptation");
        translate.add(cfg + "adaptation.group.description", "Players gradually acclimatize to biomes they spend time in, shifting their habitable band. Desert dwellers tolerate heat better but become more vulnerable to cold, and vice versa");
        translate.add(cfg + "adaptation.enable", "Enable Adaptation");
        translate.add(cfg + "adaptation.enable.description", "Toggle the biome adaptation system. When enabled, players slowly adapt to their current biome's base temperature over in-game days");
        translate.add(cfg + "adaptation.rate", "Adaptation Rate");
        translate.add(cfg + "adaptation.rate.description", "EMA rate per tick at which adapted biome temperature drifts toward the current biome. Default 0.00017 (~5 game days to full adapt). Doubled after 1 game day of sustained exposure");
        translate.add(cfg + "adaptation.max_shift", "Max Adaptation Shift");
        translate.add(cfg + "adaptation.max_shift.description", "Maximum distance from the neutral temperate band that adaptation can shift the habitable zone, in MC units (default 0.4 = ~10\u00B0C)");
        translate.add(cfg + "adaptation.strength", "Adaptation Strength");
        translate.add(cfg + "adaptation.strength.description", "Maximum rate reduction applied when fully adapted and in matching temperature stress direction (default 0.4 = 40% slower accumulation)");
        translate.add(cfg + "adaptation.threshold_extreme", "Biome Extreme Bias");
        translate.add(cfg + "adaptation.threshold_extreme.description", "Weights the biome low/high temps that adaptation tracks. 0.5 = true midpoint (stable average). Higher values (e.g. 0.8) bias toward the extreme of the biome — so desert pulls you harder toward hot, and tundra harder toward cold. Adjust if you want more intense acclimatization to biome peaks");

        // ── Client Category ──
        translate.add(cfg + "category.client", "Client");
        translate.add(cfg + "category.client.tooltip", "Display and HUD settings (client-side only)");
        translate.add(cfg + "client.display", "Display");
        translate.add(cfg + "client.display.description", "Controls for HUD elements and visual preferences");
        translate.add(cfg + "client.show_world_gauge", "Show World Gauge");
        translate.add(cfg + "client.show_world_gauge.description", "Toggle the world temperature gauge on the HUD");
        translate.add(cfg + "client.temperature_unit", "Temperature Unit");
        translate.add(cfg + "client.temperature_unit.description", "Unit used for temperature display on the HUD");
        translate.add(cfg + "client.unit.celsius", "Celsius (°C)");
        translate.add(cfg + "client.unit.fahrenheit", "Fahrenheit (°F)");
    }
}

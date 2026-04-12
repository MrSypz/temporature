package com.sypztep.client.provider;

import com.sypztep.Temporature;
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
    }

    private void generateConfig(TranslationBuilder translate) {
        String cfg = "config." + Temporature.MODID + ".";

        // Main title
        translate.add(cfg + "title", "Temporature Configuration");

        // =====================================
        // TEMPERATURE CATEGORY
        // =====================================
        translate.add(cfg + "category.temperature", "Temperature");
        translate.add(cfg + "category.temperature.tooltip", "Body temperature, weather exposure, and thermal damage");
        translate.add(cfg + "temperature.rate", "Temperature System");
        translate.add(cfg + "temperature.rate.description", "Enable/Disable if you want to playing with temperature system, which includes body temperature, weather exposure, and thermal damage. Body temperature is affected by world temperature, and heat/cold sources in the environment. If body temperature exceeds heat threshold or drops below cold threshold, player takes periodic damage until temperature returns to safe range.");
        translate.add(cfg + "temperature.min_habitable", "Min Habitable Temperature");
        translate.add(cfg + "temperature.min_habitable.description", "minimum temperature at which player can survive without taking damage from cold");
        translate.add(cfg + "temperature.max_habitable", "Max Habitable Temperature");
        translate.add(cfg + "temperature.max_habitable.description", "maximum temperature at which player can survive without taking damage from heat");
        translate.add(cfg + "temperature.core", "Core Temperature");
        translate.add(cfg + "temperature.core.description", "Core temperature system configuration");
        translate.add(cfg + "temperature.enable", "Enable Temperature System");
        translate.add(cfg + "temperature.enable.description", "Master toggle for body/world temperature, armor insulation, and thermal damage");
        translate.add(cfg + "temperature.chase_rate", "Body Chase Rate");
        translate.add(cfg + "temperature.chase_rate.description", "How fast body temperature chases world temperature per second");
        translate.add(cfg + "temperature.cold_threshold", "Cold Damage Threshold");
        translate.add(cfg + "temperature.cold_threshold.description", "Body temperature below this value deals hypothermia damage");
        translate.add(cfg + "temperature.heat_threshold", "Heat Damage Threshold");
        translate.add(cfg + "temperature.heat_threshold.description", "Body temperature above this value deals heatstroke damage");
        translate.add(cfg + "temperature.damage_interval", "Damage Interval (ticks)");
        translate.add(cfg + "temperature.damage_interval.description", "Ticks between temperature damage pulses");
        translate.add(cfg + "temperature.base_damage", "Base Damage");
        translate.add(cfg + "temperature.base_damage.description", "Base HP damage per temperature pulse; scales with severity beyond threshold");
        translate.add(cfg + "temperature.block_scan_radius", "Block Scan Radius");
        translate.add(cfg + "temperature.block_scan_radius.description", "Cubic radius around the player scanned for heat/cold sources");
        translate.add(cfg + "temperature.hot_hydration_mul", "Hot Hydration Drain Multiplier");
        translate.add(cfg + "temperature.hot_hydration_mul.description", "Extra hydration drain scaling when the body is hot");
        translate.add(cfg + "temperature.cold_energy_mul", "Cold Energy Drain Multiplier");
        translate.add(cfg + "temperature.cold_energy_mul.description", "Extra basal energy drain scaling when the body is cold (shivering)");
    }
}

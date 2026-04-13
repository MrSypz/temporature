package com.sypztep.config;

import com.sypztep.client.TemperatureUnit;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TemporatureConfigScreen {
    public static Screen buildScreen(Screen screen) {
        boolean synced = TemporatureServerConfig.isSyncedFromServer();
        boolean tempEnabled = !synced && TemporatureServerConfig.getInstance().enableTemperatureSystem;

        // =========================================================
        // CORE TEMPERATURE OPTIONS
        // =========================================================
        Option<Boolean> tempEnableOpt = Option.<Boolean>createBuilder()
                .available(!synced)
                .name(Component.translatable("config.temporature.temperature.enable"))
                .description(OptionDescription.of(Component.translatable(synced
                        ? "config.temporature.server_locked"
                        : "config.temporature.temperature.enable.description")))
                .binding(true, () -> TemporatureServerConfig.getInstance().enableTemperatureSystem,
                        newVal -> TemporatureServerConfig.getInstance().enableTemperatureSystem = newVal)
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<Float> tempRateOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.temperature.chase_rate"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.chase_rate.description")))
                .binding(1.0f, () -> TemporatureServerConfig.getInstance().tempRate,
                        newVal -> TemporatureServerConfig.getInstance().tempRate = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.1f, 5.0f).step(0.1f))
                .build();

        Option<Double> tempMinHabitableOpt = Option.<Double>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.temperature.min_habitable"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.min_habitable.description")))
                .binding(0.4, () -> TemporatureServerConfig.getInstance().minHabitableTemp,
                        newVal -> TemporatureServerConfig.getInstance().minHabitableTemp = newVal)
                .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(-2.0, 2.0).step(0.05))
                .build();

        Option<Double> tempMaxHabitableOpt = Option.<Double>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.temperature.max_habitable"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.max_habitable.description")))
                .binding(1.5, () -> TemporatureServerConfig.getInstance().maxHabitableTemp,
                        newVal -> TemporatureServerConfig.getInstance().maxHabitableTemp = newVal)
                .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(-2.0, 4.0).step(0.05))
                .build();

        Option<Integer> tempDamageIntervalOpt = Option.<Integer>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.temperature.damage_interval"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.damage_interval.description")))
                .binding(40, () -> TemporatureServerConfig.getInstance().tempDamageInterval,
                        newVal -> TemporatureServerConfig.getInstance().tempDamageInterval = newVal)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(20, 200).step(20))
                .build();

        Option<Float> tempBaseDamageOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.temperature.base_damage"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.base_damage.description")))
                .binding(0.02f, () -> TemporatureServerConfig.getInstance().tempBaseDamage,
                        newVal -> TemporatureServerConfig.getInstance().tempBaseDamage = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.01f, 0.2f).step(0.01f))
                .build();

        Option<Integer> tempBlockScanRadiusOpt = Option.<Integer>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.temperature.block_scan_radius"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.block_scan_radius.description")))
                .binding(7, () -> TemporatureServerConfig.getInstance().blockScanRadius,
                        newVal -> TemporatureServerConfig.getInstance().blockScanRadius = newVal)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(3, 12).step(1))
                .build();

        // =========================================================
        // WETNESS OPTIONS
        // =========================================================
        Option<Float> waterSoakSpeedOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.wetness.water_soak_speed"))
                .description(OptionDescription.of(Component.translatable("config.temporature.wetness.water_soak_speed.description")))
                .binding(0.02f, () -> TemporatureServerConfig.getInstance().waterSoakSpeed,
                        newVal -> TemporatureServerConfig.getInstance().waterSoakSpeed = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.005f, 0.1f).step(0.005f))
                .build();

        Option<Float> rainSoakSpeedOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.wetness.rain_soak_speed"))
                .description(OptionDescription.of(Component.translatable("config.temporature.wetness.rain_soak_speed.description")))
                .binding(0.005f, () -> TemporatureServerConfig.getInstance().rainSoakSpeed,
                        newVal -> TemporatureServerConfig.getInstance().rainSoakSpeed = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.001f, 0.05f).step(0.001f))
                .build();

        Option<Float> maxRainWetnessOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.wetness.max_rain_wetness"))
                .description(OptionDescription.of(Component.translatable("config.temporature.wetness.max_rain_wetness.description")))
                .binding(1.0f, () -> TemporatureServerConfig.getInstance().maxRainWetness,
                        newVal -> TemporatureServerConfig.getInstance().maxRainWetness = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.1f, 1.0f).step(0.1f))
                .build();

        Option<Float> dryRateOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.wetness.dry_rate"))
                .description(OptionDescription.of(Component.translatable("config.temporature.wetness.dry_rate.description")))
                .binding(0.0008f, () -> TemporatureServerConfig.getInstance().dryRate,
                        newVal -> TemporatureServerConfig.getInstance().dryRate = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0002f, 0.005f).step(0.0002f))
                .build();

        Option<Float> hotDryBonusOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.wetness.hot_dry_bonus"))
                .description(OptionDescription.of(Component.translatable("config.temporature.wetness.hot_dry_bonus.description")))
                .binding(0.0008f, () -> TemporatureServerConfig.getInstance().hotDryBonus,
                        newVal -> TemporatureServerConfig.getInstance().hotDryBonus = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 0.005f).step(0.0002f))
                .build();

        Option<Float> coldDryMultiplierOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.wetness.cold_dry_multiplier"))
                .description(OptionDescription.of(Component.translatable("config.temporature.wetness.cold_dry_multiplier.description")))
                .binding(0.3f, () -> TemporatureServerConfig.getInstance().coldDryMultiplier,
                        newVal -> TemporatureServerConfig.getInstance().coldDryMultiplier = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.1f, 1.0f).step(0.1f))
                .build();

        Option<Double> defaultWaterTempOpt = Option.<Double>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.wetness.default_water_temp"))
                .description(OptionDescription.of(Component.translatable("config.temporature.wetness.default_water_temp.description")))
                .binding(-0.93, () -> TemporatureServerConfig.getInstance().defaultWaterTemp,
                        newVal -> TemporatureServerConfig.getInstance().defaultWaterTemp = newVal)
                .controller(opt -> DoubleSliderControllerBuilder.create(opt).range(-1.0, 1.0).step(0.02))
                .build();

        // =========================================================
        // CLIENT OPTIONS
        // =========================================================
        Option<Boolean> showWorldGaugeOpt = Option.<Boolean>createBuilder()
                .name(Component.translatable("config.temporature.client.show_world_gauge"))
                .description(OptionDescription.of(Component.translatable("config.temporature.client.show_world_gauge.description")))
                .binding(true, () -> TemporatureClientConfig.getInstance().showWorldGauge,
                        newVal -> TemporatureClientConfig.getInstance().showWorldGauge = newVal)
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<TemperatureUnit> temperatureUnitOpt = Option.<TemperatureUnit>createBuilder()
                .name(Component.translatable("config.temporature.client.temperature_unit"))
                .description(OptionDescription.of(Component.translatable("config.temporature.client.temperature_unit.description")))
                .binding(TemperatureUnit.CELSIUS, () -> TemporatureClientConfig.getInstance().temperatureUnit,
                        newVal -> TemporatureClientConfig.getInstance().temperatureUnit = newVal)
                .controller(opt -> EnumControllerBuilder.create(opt)
                        .enumClass(TemperatureUnit.class)
                        .formatValue(unit -> Component.translatable("config.temporature.client.unit." + unit.name().toLowerCase())))
                .build();

        // =========================================================
        // ENABLE TOGGLE LISTENER
        // =========================================================
        tempEnableOpt.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE || event == OptionEventListener.Event.INITIAL) {
                boolean enable = option.pendingValue();

                tempRateOpt.setAvailable(enable);
                tempMinHabitableOpt.setAvailable(enable);
                tempMaxHabitableOpt.setAvailable(enable);
                tempDamageIntervalOpt.setAvailable(enable);
                tempBaseDamageOpt.setAvailable(enable);
                tempBlockScanRadiusOpt.setAvailable(enable);

                waterSoakSpeedOpt.setAvailable(enable);
                rainSoakSpeedOpt.setAvailable(enable);
                maxRainWetnessOpt.setAvailable(enable);
                dryRateOpt.setAvailable(enable);
                hotDryBonusOpt.setAvailable(enable);
                coldDryMultiplierOpt.setAvailable(enable);
                defaultWaterTempOpt.setAvailable(enable);
            }
        });

        // =========================================================
        // BUILD SCREEN
        // =========================================================
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.temporature.title"))

                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.temporature.category.temperature"))
                        .tooltip(Component.translatable("config.temporature.category.temperature.tooltip"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("config.temporature.temperature.core"))
                                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.core.description")))
                                .option(tempEnableOpt)
                                .option(tempRateOpt)
                                .option(tempMinHabitableOpt)
                                .option(tempMaxHabitableOpt)
                                .option(tempDamageIntervalOpt)
                                .option(tempBaseDamageOpt)
                                .option(tempBlockScanRadiusOpt)
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("config.temporature.wetness.group"))
                                .description(OptionDescription.of(Component.translatable("config.temporature.wetness.group.description")))
                                .option(waterSoakSpeedOpt)
                                .option(rainSoakSpeedOpt)
                                .option(maxRainWetnessOpt)
                                .option(dryRateOpt)
                                .option(hotDryBonusOpt)
                                .option(coldDryMultiplierOpt)
                                .option(defaultWaterTempOpt)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.temporature.category.client"))
                        .tooltip(Component.translatable("config.temporature.category.client.tooltip"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("config.temporature.client.display"))
                                .description(OptionDescription.of(Component.translatable("config.temporature.client.display.description")))
                                .option(showWorldGaugeOpt)
                                .option(temperatureUnitOpt)
                                .build())
                        .build())
                .save(() -> {
                    TemporatureServerConfig.HANDLER.save();
                    TemporatureClientConfig.HANDLER.save();
                })
                .build()
                .generateScreen(screen);
    }
}

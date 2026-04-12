package com.sypztep;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.*;

public final class TemporatureConfigScreen {
    public static Screen buildScreen(Screen screen) {
        // =========================================================
        // TEMPERATURE
        // =========================================================
        boolean tempEnabled = TemporatureServerConfig.getInstance().enableTemperatureSystem;

        Option<Boolean> tempEnableOpt = Option.<Boolean>createBuilder()
                .name(Component.translatable("config.temporature.temperature.enable"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.enable.description")))
                .binding(true, () -> TemporatureServerConfig.getInstance().enableTemperatureSystem,
                        newVal -> TemporatureServerConfig.getInstance().enableTemperatureSystem = newVal)
                .controller(TickBoxControllerBuilder::create)
                .build();

        Option<Float> tempRateOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.temperature.rate"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.rate.description")))
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
                .binding(2.0f, () -> TemporatureServerConfig.getInstance().tempBaseDamage,
                        newVal -> TemporatureServerConfig.getInstance().tempBaseDamage = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 5.0f).step(0.5f))
                .build();

        Option<Integer> tempBlockScanRadiusOpt = Option.<Integer>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.temperature.block_scan_radius"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.block_scan_radius.description")))
                .binding(7, () -> TemporatureServerConfig.getInstance().blockScanRadius,
                        newVal -> TemporatureServerConfig.getInstance().blockScanRadius = newVal)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(3, 12).step(1))
                .build();

        Option<Float> tempHotHydrationMulOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.temperature.hot_hydration_mul"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.hot_hydration_mul.description")))
                .binding(1.0f, () -> TemporatureServerConfig.getInstance().hotHydrationDrainMul,
                        newVal -> TemporatureServerConfig.getInstance().hotHydrationDrainMul = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 3f).step(0.1f))
                .build();

        Option<Float> tempColdEnergyMulOpt = Option.<Float>createBuilder()
                .available(tempEnabled)
                .name(Component.translatable("config.temporature.temperature.cold_energy_mul"))
                .description(OptionDescription.of(Component.translatable("config.temporature.temperature.cold_energy_mul.description")))
                .binding(1.0f, () -> TemporatureServerConfig.getInstance().coldEnergyDrainMul,
                        newVal -> TemporatureServerConfig.getInstance().coldEnergyDrainMul = newVal)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 3f).step(0.1f))
                .build();

        tempEnableOpt.addEventListener((option, event) -> {
            if (event == OptionEventListener.Event.STATE_CHANGE || event == OptionEventListener.Event.INITIAL) {
                boolean enable = option.pendingValue();

                tempRateOpt.setAvailable(enable);
                tempMinHabitableOpt.setAvailable(enable);
                tempMaxHabitableOpt.setAvailable(enable);
                tempDamageIntervalOpt.setAvailable(enable);
                tempBaseDamageOpt.setAvailable(enable);
                tempBlockScanRadiusOpt.setAvailable(enable);
                tempHotHydrationMulOpt.setAvailable(enable);
                tempColdEnergyMulOpt.setAvailable(enable);
            }
        });

        // =========================================================
        // BUILD SCREEN
        // =========================================================
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.temporature.title"))

                // =====================================
                // TEMPERATURE CATEGORY
                // =====================================
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
                                .option(tempHotHydrationMulOpt)
                                .option(tempColdEnergyMulOpt)
                                .build())
                        .build())
                .save(TemporatureServerConfig.HANDLER::save)
                .build()
                .generateScreen(screen);
    }
}
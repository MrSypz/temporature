package com.sypztep.temporature.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.sypztep.temporature.common.TemperatureEntityComponents;
import com.sypztep.temporature.system.temperature.TemperatureHelper;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.HeartType.class)
public class GuiMixin {
    @ModifyExpressionValue(
            method = "forPlayer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isFullyFrozen()Z"))
    private static boolean shouldShowFrozenHeart(boolean original, @Local(argsOnly = true) Player player) {
        return original || (TemperatureEntityComponents.PLAYER_TEMPERATURE.get(player).getBodyTemp() <= TemperatureHelper.FREEZING_DEV);
    }
}

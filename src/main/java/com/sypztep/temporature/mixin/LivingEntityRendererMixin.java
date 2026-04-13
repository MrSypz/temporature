package com.sypztep.temporature.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sypztep.temporature.common.TemperatureEntityComponents;
import com.sypztep.temporature.system.temperature.TemperatureHelper;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @WrapOperation(
            method = "extractRenderState",
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;isFullyFrozen()Z")
    )
    private boolean handleFrozenState(LivingEntity entity, Operation<Boolean> original) {
        if (entity instanceof Player player)
            return TemperatureEntityComponents.PLAYER_TEMPERATURE
                    .get(player).
                    getBodyTemp() <= TemperatureHelper.FREEZING_DEV;
        return original.call(entity);
    }
}

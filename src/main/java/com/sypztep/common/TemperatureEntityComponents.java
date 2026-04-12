package com.sypztep.common;

import com.sypztep.Temporature;
import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

public final class TemperatureEntityComponents implements EntityComponentInitializer {
    public static final ComponentKey<PlayerTemperatureComponent> PLAYER_TEMPERATURE = ComponentRegistry.getOrCreate(Temporature.id("player_temperature"), PlayerTemperatureComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.beginRegistration(Player.class, PLAYER_TEMPERATURE).respawnStrategy(RespawnCopyStrategy.LOSSLESS_ONLY).end(PlayerTemperatureComponent::new);
    }
}

package com.sypztep.temporature.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sypztep.temporature.Temporature;
import com.sypztep.temporature.common.PlayerTemperatureComponent;
import com.sypztep.temporature.common.TemperatureEntityComponents;
import com.sypztep.plateau.client.v1.postprocess.PostEffectManager;
import com.sypztep.plateau.client.v1.postprocess.PostProcessResources;
import com.sypztep.temporature.system.temperature.TemperatureHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.Set;

@Environment(EnvType.CLIENT)
public final class HeatHazeRenderer {
    private static final Identifier POST_CHAIN_ID = Temporature.id("heat_haze");

    private static PostProcessResources resources;
    private static float smoothIntensity = 0f;

    private HeatHazeRenderer() {}

    public static void register() {
        PostEffectManager.register(HeatHazeRenderer::renderIfNeeded, HeatHazeRenderer::close);
    }

    /** Called after GameRenderer.renderLevel() returns — full scene is in mainRenderTarget. */
    public static void renderIfNeeded(Minecraft mc, float partialTick, GraphicsResourceAllocator allocator) {
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null || mc.level == null) return;

        float target = getHeatIntensity(localPlayer);
        smoothIntensity = Mth.lerp(0.04f, smoothIntensity, target);

        if (smoothIntensity < 0.005f) return;

        applyPostEffect(mc, smoothIntensity, allocator);
    }

    public static void close() {
        if (resources != null) { resources.close(); resources = null; }
        smoothIntensity = 0f;
    }

    private static float getHeatIntensity(LocalPlayer localPlayer) {
        PlayerTemperatureComponent temp = TemperatureEntityComponents.PLAYER_TEMPERATURE.get(localPlayer);
        float dev = temp.getBodyTemp() + temp.getBaseOffset();
        float t = (float) ((dev - TemperatureHelper.WARM_DEV) / (TemperatureHelper.BURNING_DEV - TemperatureHelper.WARM_DEV));
        return Mth.clamp(t, 0f, 1f);
    }

    private static void applyPostEffect(Minecraft mc, float intensity, GraphicsResourceAllocator allocator) {
        PostChain postChain = mc.getShaderManager().getPostChain(POST_CHAIN_ID, Set.of(PostChain.MAIN_TARGET_ID));
        if (postChain == null) return;

        if (resources == null) {
            resources = new PostProcessResources("temporature_heat_haze", Temporature.id("heat_haze_dummy"), "HeatHazeConfig", 4);
        }

        resources.patchUniforms(postChain);

        try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder()
                .mapBuffer(resources.getUniformBuffer(), false, true)) {
            Std140Builder.intoBuffer(view.data()).putFloat(intensity);
        }

        executeMainOnly(postChain, mc.getMainRenderTarget(), allocator);
    }

    private static void executeMainOnly(PostChain postChain, RenderTarget mainTarget, GraphicsResourceAllocator allocator) {
        FrameGraphBuilder fgb = new FrameGraphBuilder();
        ResourceHandle<RenderTarget> mainHandle = fgb.importExternal("main", mainTarget);

        PostChain.TargetBundle bundle = new PostChain.TargetBundle() {
            private ResourceHandle<RenderTarget> main = mainHandle;

            @Override
            public void replace(Identifier id, @NonNull ResourceHandle<RenderTarget> handle) {
                if (id.equals(PostChain.MAIN_TARGET_ID)) this.main = handle;
            }

            @Override
            public ResourceHandle<RenderTarget> get(Identifier id) {
                if (id.equals(PostChain.MAIN_TARGET_ID)) return this.main;
                return null;
            }
        };

        postChain.addToFrame(fgb, mainTarget.width, mainTarget.height, bundle);
        fgb.execute(allocator);
    }
}

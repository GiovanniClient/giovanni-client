package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.events.GiovanniWorldRenderEvent;

@Mixin(LevelRenderer.class)
public abstract class GiovanniLevelRendererMixin {
    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addWeatherPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    private void giovanni$renderWorldOverlays(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci, @Local FrameGraphBuilder frame) {
        FramePass pass = frame.addPass("giovanni$world_render");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        pass.executes(() -> new GiovanniWorldRenderEvent(cameraState, new PoseStack(), deltaTracker).render());
    }
}

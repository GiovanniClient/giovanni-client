package rocket.giovanniclient.client.features.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public enum GiovanniRenderLayers {
    ;

    private static final RenderPipeline TRACER_LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.parse("giovanniclient:pipeline/tracer_lines"))
                    .withDepthStencilState(Optional.empty())
                    .build()
    );

    public static final RenderType TRACER_LINES = RenderType.create("giovanniclient:tracer_lines",
            RenderSetup.builder(TRACER_LINES_PIPELINE)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup());

    private static final RenderPipeline RAT_MODEL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.parse("giovanniclient:pipeline/rat_model"))
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .build()
    );

    public static final RenderType RAT_MODEL = RenderType.create("giovanniclient:rat_model",
            RenderSetup.builder(RAT_MODEL_PIPELINE)
                    .withTexture("Sampler0", Identifier.parse("giovanniclient:textures/entity/big_rat.png"))
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup());
}

package rocket.giovanniclient.client.features.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
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
}

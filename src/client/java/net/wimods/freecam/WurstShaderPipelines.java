/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public enum WurstShaderPipelines
{
	;
	
	/**
	 * Similar to the RENDERTYPE_LINES Snippet, but without fog.
	 */
	public static final Snippet FOGLESS_LINES_SNIPPET = RenderPipeline
		.builder(RenderPipelines.MATRICES_FOG_SNIPPET,
			RenderPipelines.GLOBALS_SNIPPET)
		.withVertexShader(Identifier.parse("wi_freecam:core/fogless_lines"))
		.withFragmentShader(Identifier.parse("wi_freecam:core/fogless_lines"))
		.withBlend(BlendFunction.TRANSLUCENT).withCull(false)
		.withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
			Mode.LINES)
		.buildSnippet();
	
	/**
	 * Similar to the LINES ShaderPipeline, but with no fog.
	 */
	public static final RenderPipeline DEPTH_TEST_LINES =
		RenderPipelines.register(RenderPipeline.builder(FOGLESS_LINES_SNIPPET)
			.withLocation(Identifier
				.parse("wi_freecam:pipeline/wi_freecam_depth_test_lines"))
			.build());
	
	/**
	 * Similar to the LINES ShaderPipeline, but with no depth test or fog.
	 */
	public static final RenderPipeline ESP_LINES =
		RenderPipelines.register(RenderPipeline.builder(FOGLESS_LINES_SNIPPET)
			.withLocation(
				Identifier.parse("wi_freecam:pipeline/wi_freecam_esp_lines"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).build());
}

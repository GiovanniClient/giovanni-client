/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam;


import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;

public enum WurstRenderLayers {
    ;

    /**
     * Similar to {@link RenderLayer#getLines()}, but with line width 2.
     */
    public static final RenderLayer LINES = RenderLayer.of("wi_freecam:lines",
            RenderSetup.builder(WurstShaderPipelines.DEPTH_TEST_LINES)
                    .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .build());

    /**
     * Similar to {@link RenderLayer#getLines()}, but with line width 2 and no
     * depth test.
     */
    public static final RenderLayer ESP_LINES =
            RenderLayer.of("wi_freecam:esp_lines",
                    RenderSetup.builder(WurstShaderPipelines.ESP_LINES)
                            .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                            .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                            .build());

    /**
     * Returns either {@link #LINES} or {@link #ESP_LINES} depending on the
     * value of {@code depthTest}.
     */
    public static RenderLayer getLines(boolean depthTest) {
        return depthTest ? LINES : ESP_LINES;
    }
}

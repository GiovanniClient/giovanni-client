/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.wimods.freecam.WiFreecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
	@Inject(method = "submitFeatures", at = @At("TAIL"))
	private void onSubmitFeatures(LevelRenderState levelRenderState,
		SubmitNodeCollector collector, boolean renderOutline, CallbackInfo ci)
	{
		WiFreecam freecam = WiFreecam.INSTANCE;
		if(freecam.isEnabled())
		{
			float tickProgress = Minecraft.getInstance().getDeltaTracker()
				.getGameTimeDeltaPartialTick(false);
			freecam.onRender(collector, tickProgress);
		}
	}
}

/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.wimods.freecam.WiFreecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin
{
	/*
	 * This mixin needs to run after renderScoreboardSidebar()
	 * and before tabList.setVisible()
	 */
	@Inject(at = @At("HEAD"),
		method = "renderPlayerList")
	private void onRenderTabList(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci)
	{
		if(WiFreecam.MC.getDebugHud().shouldShowDebugHud())
			return;
		
		float tickDelta = tickCounter.getTickProgress(true);
		// FreecamHud.onRenderGui(context, tickDelta);
	}
}

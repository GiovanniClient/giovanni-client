/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.wimods.freecam.FreecamHud;
import net.wimods.freecam.WiFreecam;

@Mixin(Gui.class)
public abstract class GuiMixin
{
	/*
	 * This mixin needs to run after renderScoreboardSidebar()
	 * and before tabList.setVisible()
	 */
	@Inject(
		method = "extractTabList(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
		at = @At("HEAD"))
	private void onExtractTabList(GuiGraphicsExtractor context,
		DeltaTracker tickCounter, CallbackInfo ci)
	{
		if(WiFreecam.MC.debugEntries.isOverlayVisible())
			return;
		
		float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);
		FreecamHud.onRenderGui(context, tickDelta);
	}
}

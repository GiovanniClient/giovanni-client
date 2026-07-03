/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Camera;
import net.wimods.freecam.WiFreecam;

/**
 * Last updated for <a href=
 * "https://github.com/CaffeineMC/sodium/tree/bf93ed83b128c7c6222840fbe52164432a80f97c">Sodium
 * mc26.2-0.9.0-fabric</a>.
 */
@Pseudo
@Mixin(
	targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager")
public class RenderSectionManagerMixin
{
	/**
	 * Turns off Sodium's occlusion culling when in Freecam.
	 */
	@Inject(method = "shouldUseOcclusionCulling",
		at = @At("HEAD"),
		cancellable = true,
		require = 0,
		remap = false)
	private void onShouldUseOcclusionCulling(Camera camera, boolean spectator,
		CallbackInfoReturnable<Boolean> cir)
	{
		if(WiFreecam.INSTANCE.isEnabled())
			cir.setReturnValue(false);
	}
}

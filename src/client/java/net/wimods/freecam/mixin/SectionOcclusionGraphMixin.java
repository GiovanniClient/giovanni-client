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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.core.Direction;
import net.wimods.freecam.WiFreecam;

@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin
{
	/**
	 * Turns off the visibility graph when in Freecam without Sodium installed,
	 * making things like caves become visible that would normally be hidden
	 * behind other blocks and thus skipped for better rendering performance.
	 */
	@WrapOperation(method = "runUpdates",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;facesCanSeeEachother(Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;)Z"))
	private boolean wrapFacesCanSeeEachother(SectionMesh mesh, Direction from,
		Direction to, Operation<Boolean> original)
	{
		if(WiFreecam.INSTANCE.isEnabled())
			return true;
		
		return original.call(mesh, from, to);
	}
}

/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import net.minecraft.client.render.ChunkRenderingDataPreparer;
import net.minecraft.client.render.chunk.AbstractChunkRenderData;
import net.minecraft.util.math.Direction;
import net.wimods.freecam.WiFreecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkRenderingDataPreparer.class)
public class ChunkRenderingDataPreparerMixin
{
	/**
	 * Turns off the visibility graph when in Freecam, making things like caves
	 * become visible that would normally be hidden behind other blocks and thus
	 * skipped for better rendering performance.
	 */
	@Redirect(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/chunk/AbstractChunkRenderData;" +
                "isVisibleThrough(Lnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/Direction;)Z"),
		method = "update")
	private boolean onFacesCanSeeEachother(AbstractChunkRenderData mesh, Direction from, Direction to)
	{
		if(WiFreecam.INSTANCE.isEnabled())
			return true;
		
		return mesh.isVisibleThrough(from, to);
	}
}

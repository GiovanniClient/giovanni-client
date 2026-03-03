/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.waypoint.TrackedWaypoint;
import net.wimods.freecam.WiFreecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin implements TrackedWaypoint.YawProvider {
	@Shadow
	private boolean thirdPerson;
	
	@Inject(at = @At("RETURN"),
		method = "update",
		cancellable = false)
	public void onSetup(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
		WiFreecam freecam = WiFreecam.INSTANCE;
		if(!freecam.isEnabled())
			return;

        this.thirdPerson = true;
		setPos(freecam.getCamPos(tickProgress));
		setRotation(freecam.getCamYaw(), freecam.getCamPitch());
	}
	
	@Shadow
	protected abstract void setPos(Vec3d pos);
	
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);
}

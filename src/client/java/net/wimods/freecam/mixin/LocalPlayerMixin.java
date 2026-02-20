/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.wimods.freecam.WiFreecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayerEntity
{
	@Shadow
	public Input input;
	
	@Unique
	private Input realInput;
	
	private LocalPlayerMixin(WiFreecam freecam, ClientWorld world,
		GameProfile profile)
	{
		super(world, profile);
	}
	
	@Inject(at = @At("HEAD"), method = "isSneaking", cancellable = true)
	private void onIsShiftKeyDown(CallbackInfoReturnable<Boolean> cir)
	{
		if(WiFreecam.INSTANCE.isMovingCamera())
			cir.setReturnValue(false);
	}
	
	@Inject(at = @At("HEAD"), method = "tickMovement")
	private void onAiStepHead(CallbackInfo ci)
	{
		if(!WiFreecam.INSTANCE.isMovingCamera())
			return;
		
		realInput = input;
		input.tick();
		input = new Input();
	}
	
	@Inject(at = @At("RETURN"), method = "tickMovement")
	private void onAiStepReturn(CallbackInfo ci)
	{
		if(realInput == null)
			return;
		
		input = realInput;
		realInput = null;
	}
	
	@Override
	public void changeLookDirection(double deltaYaw, double deltaPitch)
	{
		WiFreecam freecam = WiFreecam.INSTANCE;
		if(freecam.isMovingCamera())
		{
			freecam.turn(deltaYaw, deltaPitch);
			return;
		}
		
		super.changeLookDirection(deltaYaw, deltaPitch);
	}
}

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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import net.wimods.freecam.WiFreecam;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin
{
	@Inject(method = "onScroll(JDD)V", at = @At("RETURN"))
	private void onOnScroll(long window, double horizontal, double vertical,
		CallbackInfo ci)
	{
		WiFreecam.INSTANCE.onMouseScroll(vertical);
	}
	
	@WrapWithCondition(method = "onScroll(JDD)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
	private boolean wrapOnScroll(Inventory inventory, int slot)
	{
		return !WiFreecam.INSTANCE.isControllingScrollEvents();
	}
}

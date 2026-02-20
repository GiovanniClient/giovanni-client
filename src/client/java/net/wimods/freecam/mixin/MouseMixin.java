/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Mouse;
import net.minecraft.entity.player.PlayerInventory;
import net.wimods.freecam.WiFreecam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin
{
	@Inject(at = @At("RETURN"), method = "onMouseScroll")
	private void onOnScroll(long window, double horizontal, double vertical,
		CallbackInfo ci)
	{
		WiFreecam.INSTANCE.onMouseScroll(vertical);
	}
	
	@WrapWithCondition(at = @At(value = "INVOKE",
		target = "Lnet/minecraft/entity/player/PlayerInventory;setSelectedSlot(I)V"),
		method = "onMouseScroll")
	private boolean wrapOnScroll(PlayerInventory inventory, int slot)
	{
		return WiFreecam.INSTANCE.isControllingScrollEvents();
	}
}

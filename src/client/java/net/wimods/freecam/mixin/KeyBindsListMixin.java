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
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.wimods.freecam.WiFreecam;

@Mixin(KeyBindsList.class)
public class KeyBindsListMixin
{
	/**
	 * Translates Freecam's keybind names using Freecam's translator instead of
	 * Minecraft's, so that they can be displayed correctly.
	 */
	@WrapOperation(
		method = "<init>(Lnet/minecraft/client/gui/screens/options/controls/KeyBindsScreen;Lnet/minecraft/client/Minecraft;)V",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
			ordinal = 0))
	private MutableComponent translateFreecamKeybinds(String key,
		Operation<MutableComponent> original, @Local KeyMapping keyMapping)
	{
		WiFreecam freecam = WiFreecam.INSTANCE;
		if(freecam.getKeybinds().getAll().contains(keyMapping))
			return Component.literal(freecam.getTranslator().translate(key));
		
		return original.call(key);
	}
}

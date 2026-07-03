/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.wimods.freecam.WiFreecam;

@Mixin(KeyMapping.Category.class)
public class KeyMappingCategoryMixin
{
	@Shadow
	@Final
	private Identifier id;
	
	/**
	 * Translates Freecam's keybind category using Freecam's translator instead
	 * of Minecraft's, so that it can be displayed correctly.
	 */
	@Inject(method = "label()Lnet/minecraft/network/chat/Component;",
		at = @At("HEAD"),
		cancellable = true)
	private void translateFreecamCategory(CallbackInfoReturnable<Component> cir)
	{
		WiFreecam freecam = WiFreecam.INSTANCE;
		if(freecam.getKeybinds().category.id().equals(id))
		{
			String key = id.toLanguageKey("key.category");
			cir.setReturnValue(
				Component.literal(freecam.getTranslator().translate(key)));
		}
	}
}

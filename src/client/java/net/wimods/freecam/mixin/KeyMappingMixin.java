/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import java.util.Map;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.KeyMapping;
import net.wimods.freecam.WiFreecam;
import net.wimods.freecam.mixinterface.IKeyMapping;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements IKeyMapping
{
	@Shadow
	private InputConstants.Key key;
	
	@Override
	@Unique
	@Deprecated // use IKeyMapping.isActuallyDown() instead
	public boolean freecam_isActuallyDown()
	{
		Window window = WiFreecam.MC.getWindow();
		int code = key.getValue();
		
		if(key.getType() == InputConstants.Type.MOUSE)
			return GLFW.glfwGetMouseButton(window.handle(), code) == 1;
		
		return InputConstants.isKeyDown(window, code);
	}
	
	/*
	 * Prevents keybind chat components from resolving Freecam's key mappings.
	 *
	 * <p>
	 * See https://wurst.wiki/sign_translation_vulnerability
	 */
	@WrapOperation(
		method = "createNameSupplier(Ljava/lang/String;)Ljava/util/function/Supplier;",
		at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/KeyMapping;ALL:Ljava/util/Map;"))
	private static Map<String, KeyMapping> excludeModdedKeyMappingsFromALL(
		Operation<Map<String, KeyMapping>> original)
	{
		Map<String, KeyMapping> adjusted = Maps.newHashMap(original.call());
		WiFreecam.INSTANCE.getKeybinds().getAll()
			.forEach(key -> adjusted.remove(key.getName()));
		return adjusted;
	}
}

/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.wimods.freecam.WiFreecam;
import net.wimods.freecam.mixinterface.IKeyMapping;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

import static sb.rocket.giovanniclient.client.bootstrap.ClientKeybinds.ALL_KEYS;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin implements IKeyMapping
{
	@Shadow
    private InputUtil.Key boundKey;
	
	@Override
	@Unique
	@Deprecated // use IKeyMapping.isActuallyDown() instead
	public boolean freecam_isActuallyDown()
	{
		Window window = WiFreecam.MC.getWindow();
		int code = boundKey.getCode();
		
		if(boundKey.getCategory() == InputUtil.Type.MOUSE)
			return GLFW.glfwGetMouseButton(window.getHandle(), code) == 1;
		
		return InputUtil.isKeyPressed(window, code);
	}
	
	/*
	 * Prevents keybind chat components from resolving Freecam's key mappings.
	 *
	 * <p>
	 * See https://wurst.wiki/sign_translation_vulnerability
	 */
	@WrapOperation(at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/KeyBinding;KEYS_BY_ID:Ljava/util/Map;"), method = "getLocalizedName")

    private static Map<String, KeyBinding> excludeModdedKeyMappingsFromKEYS_BY_ID(Operation<Map<String, KeyBinding>> original) {
        Map<String, KeyBinding> adjusted = Maps.newHashMap(original.call());

        // Reference the new location of your keys
        for (KeyBinding key : ALL_KEYS) {
            adjusted.remove(key.getBoundKeyTranslationKey());
        }

        return adjusted;
    }
}

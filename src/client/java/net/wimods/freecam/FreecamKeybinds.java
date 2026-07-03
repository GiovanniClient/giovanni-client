/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class FreecamKeybinds
{
	public final KeyMapping.Category category = KeyMapping.Category
		.register(Identifier.fromNamespaceAndPath("wi_freecam", "wi_freecam"));
	
	private final ArrayList<KeyMapping> all = new ArrayList<>();
	
	public final KeyMapping toggleKey =
		key("key.wi_freecam.toggle", Type.KEYSYM, GLFW.GLFW_KEY_U);
	
	public final KeyMapping openSettingsKey =
		key("key.wi_freecam.open_settings", Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_CONTROL);
	
	public final KeyMapping switchControlKey =
		key("key.wi_freecam.switch_control", Type.KEYSYM,
			InputConstants.UNKNOWN.getValue());
	
	public void onUpdate()
	{
		WiFreecam freecam = WiFreecam.INSTANCE;
		
		while(toggleKey.consumeClick())
			freecam.setEnabled(!freecam.isEnabled());
		
		while(openSettingsKey.consumeClick())
			freecam.getGui().open();
		
		while(switchControlKey.consumeClick())
			freecam.getSettings().applyInputTo.selectNext();
	}
	
	private KeyMapping key(String name, Type type, int keyCode)
	{
		KeyMapping key =
			new KeyMapping(name, type, keyCode, category, all.size());
		all.add(key);
		KeyMappingHelper.registerKeyMapping(key);
		return key;
	}
	
	public List<KeyMapping> getAll()
	{
		return Collections.unmodifiableList(all);
	}
}

/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam;

import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.Mth;
import net.wimods.freecam.settings.CheckboxSetting;
import net.wimods.freecam.settings.ColorSetting;
import net.wimods.freecam.settings.Setting;
import net.wimods.freecam.settings.SliderSetting;
import net.wimods.freecam.settings.SliderSetting.ValueDisplay;

public final class FreecamSettings
{
	private final LinkedHashMap<String, Setting> settings =
		new LinkedHashMap<>();
	
	public final FreecamInputSetting applyInputTo = new FreecamInputSetting();
	
	public final FreecamInteractionSetting interactFrom =
		new FreecamInteractionSetting();
	
	public final SliderSetting horizontalSpeed =
		new SliderSetting("Horizontal speed",
			"description.wurst.setting.freecam.horizontal_speed", 1, 0.05, 10,
			0.05, ValueDisplay.DECIMAL);
	
	public final SliderSetting verticalSpeed = new SliderSetting(
		"Vertical speed", "description.wurst.setting.freecam.vertical_speed", 1,
		0.05, 5, 0.05,
		_ -> ValueDisplay.DECIMAL.getValueString(getActualVerticalSpeed()));
	
	public final CheckboxSetting scrollToChangeSpeed =
		new CheckboxSetting("Scroll to change speed",
			"description.wurst.setting.freecam.scroll_to_change_speed", true);
	
	public final CheckboxSetting renderSpeed =
		new CheckboxSetting("Show speed in HackList",
			"description.wurst.setting.freecam.show_speed_in_hacklist", true);
	
	public final FreecamInitialPosSetting initialPos =
		new FreecamInitialPosSetting();
	
	public final CheckboxSetting tracer = new CheckboxSetting("Tracer",
		"description.wurst.setting.freecam.tracer", false);
	
	public final ColorSetting color =
		new ColorSetting("Tracer color", Color.WHITE);
	
	public final CheckboxSetting hideHand = new CheckboxSetting("Hide hand",
		"description.wurst.setting.freecam.hide_hand", true);
	
	public final CheckboxSetting disableOnDamage =
		new CheckboxSetting("Disable on damage",
			"description.wurst.setting.freecam.disable_on_damage", true);
	
	public final CheckboxSetting reloadChunks =
		new CheckboxSetting("Reload chunks",
			"description.wurst.setting.freecam.reload_chunks", true);
	
	public final CheckboxSetting excludeFromStats = new CheckboxSetting(
		"Exclude me from statistics",
		"description.wurst.setting.freecam.exclude_me_from_statistics", false);
	
	public FreecamSettings()
	{
		add(applyInputTo);
		add(interactFrom);
		add(horizontalSpeed);
		add(verticalSpeed);
		add(scrollToChangeSpeed);
		add(renderSpeed);
		add(initialPos);
		add(tracer);
		add(color);
		add(hideHand);
		add(disableOnDamage);
		add(reloadChunks);
		add(excludeFromStats);
	}
	
	private void add(Setting setting)
	{
		String key = setting.getName().toLowerCase();
		if(settings.containsKey(key))
			throw new IllegalArgumentException("Duplicate setting: " + key);
		
		settings.put(key, setting);
	}
	
	public double getActualVerticalSpeed()
	{
		return Mth.clamp(horizontalSpeed.getValue() * verticalSpeed.getValue(),
			0.05, 10);
	}
	
	public Map<String, Setting> getMap()
	{
		return Collections.unmodifiableMap(settings);
	}
}

/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.wimods.freecam.WiFreecam;
import net.wimods.freecam.clickgui.Component;
import net.wimods.freecam.clickgui.components.CheckboxComponent;
import net.wimods.freecam.util.json.JsonUtils;
import net.wimods.freecam.util.text.WText;

public class CheckboxSetting extends Setting
{
	private boolean checked;
	private final boolean checkedByDefault;
	
	public CheckboxSetting(String name, WText description, boolean checked)
	{
		super(name, description);
		this.checked = checked;
		checkedByDefault = checked;
	}
	
	public CheckboxSetting(String name, String descriptionKey, boolean checked)
	{
		this(name, WText.translated(descriptionKey), checked);
	}
	
	public CheckboxSetting(String name, boolean checked)
	{
		this(name, WText.empty(), checked);
	}
	
	public final boolean isChecked()
	{
		return checked;
	}
	
	public final boolean isCheckedByDefault()
	{
		return checkedByDefault;
	}
	
	public final void setChecked(boolean checked)
	{
		this.checked = checked;
		WiFreecam.INSTANCE.saveSettings();
	}
	
	@Override
	public final Component getComponent()
	{
		return new CheckboxComponent(this);
	}
	
	@Override
	public final void fromJson(JsonElement json)
	{
		if(!JsonUtils.isBoolean(json))
			return;
		
		checked = json.getAsBoolean();
	}
	
	@Override
	public final JsonElement toJson()
	{
		return new JsonPrimitive(checked);
	}
}

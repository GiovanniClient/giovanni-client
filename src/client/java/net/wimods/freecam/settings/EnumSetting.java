/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.settings;

import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.wimods.freecam.WiFreecam;
import net.wimods.freecam.clickgui.Component;
import net.wimods.freecam.clickgui.components.ComboBoxComponent;
import net.wimods.freecam.util.json.JsonUtils;
import net.wimods.freecam.util.text.WText;

public class EnumSetting<T extends Enum<T>> extends Setting
{
	private final T[] values;
	private T selected;
	private final T defaultSelected;
	
	public EnumSetting(String name, WText description, T[] values, T selected)
	{
		super(name, description);
		this.values = Objects.requireNonNull(values);
		this.selected = Objects.requireNonNull(selected);
		defaultSelected = selected;
	}
	
	public EnumSetting(String name, String descriptionKey, T[] values,
		T selected)
	{
		this(name, WText.translated(descriptionKey), values, selected);
	}
	
	public EnumSetting(String name, T[] values, T selected)
	{
		this(name, WText.empty(), values, selected);
	}
	
	public T[] getValues()
	{
		return values;
	}
	
	public T getSelected()
	{
		return selected;
	}
	
	public T getDefaultSelected()
	{
		return defaultSelected;
	}
	
	public void setSelected(T selected)
	{
		this.selected = Objects.requireNonNull(selected);
		WiFreecam.INSTANCE.saveSettings();
	}
	
	public boolean setSelected(String selected)
	{
		for(T value : values)
		{
			if(!value.toString().equalsIgnoreCase(selected))
				continue;
			
			setSelected(value);
			return true;
		}
		
		return false;
	}
	
	public void selectNext()
	{
		int next = selected.ordinal() + 1;
		if(next >= values.length)
			next = 0;
		
		setSelected(values[next]);
	}
	
	public void selectPrev()
	{
		int prev = selected.ordinal() - 1;
		if(prev < 0)
			prev = values.length - 1;
		
		setSelected(values[prev]);
	}
	
	@Override
	public Component getComponent()
	{
		return new ComboBoxComponent<>(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		if(!JsonUtils.isString(json))
			return;
		
		setSelected(json.getAsString());
	}
	
	@Override
	public JsonElement toJson()
	{
		return new JsonPrimitive(selected.toString());
	}
}

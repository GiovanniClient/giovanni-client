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

import net.minecraft.util.Mth;
import net.wimods.freecam.WiFreecam;
import net.wimods.freecam.clickgui.Component;
import net.wimods.freecam.clickgui.components.SliderComponent;
import net.wimods.freecam.util.json.JsonUtils;
import net.wimods.freecam.util.text.WText;

public class SliderSetting extends Setting
{
	private double value;
	private final double defaultValue;
	private final double minimum;
	private final double maximum;
	private final double increment;
	private final ValueDisplay display;
	
	public SliderSetting(String name, WText description, double value,
		double minimum, double maximum, double increment, ValueDisplay display)
	{
		super(name, description);
		this.value = value;
		defaultValue = value;
		
		this.minimum = minimum;
		this.maximum = maximum;
		
		this.increment = increment;
		this.display = display;
	}
	
	public SliderSetting(String name, String descriptionKey, double value,
		double minimum, double maximum, double increment, ValueDisplay display)
	{
		this(name, WText.translated(descriptionKey), value, minimum, maximum,
			increment, display);
	}
	
	public SliderSetting(String name, double value, double minimum,
		double maximum, double increment, ValueDisplay display)
	{
		this(name, WText.empty(), value, minimum, maximum, increment, display);
	}
	
	public final double getValue()
	{
		return Mth.clamp(value, minimum, maximum);
	}
	
	public final float getValueF()
	{
		return (float)getValue();
	}
	
	public final int getValueI()
	{
		return (int)getValue();
	}
	
	public final double getDefaultValue()
	{
		return defaultValue;
	}
	
	public final String getValueString()
	{
		return display.getValueString(getValue());
	}
	
	public final void setValue(double value)
	{
		value = (int)Math.round(value / increment) * increment;
		value = Mth.clamp(value, minimum, maximum);
		
		this.value = value;
		WiFreecam.INSTANCE.saveSettings();
	}
	
	public final void increaseValue()
	{
		setValue(getValue() + increment);
	}
	
	public final void decreaseValue()
	{
		setValue(getValue() - increment);
	}
	
	public final double getMinimum()
	{
		return minimum;
	}
	
	public final double getMaximum()
	{
		return maximum;
	}
	
	public final double getRange()
	{
		return maximum - minimum;
	}
	
	public final double getIncrement()
	{
		return increment;
	}
	
	public final double getPercentage()
	{
		return (getValue() - minimum) / getRange();
	}
	
	public float[] getKnobColor()
	{
		float f = (float)(2 * getPercentage());
		
		float red = Mth.clamp(f, 0, 1);
		float green = Mth.clamp(2 - f, 0, 1);
		float blue = 0;
		
		return new float[]{red, green, blue};
	}
	
	@Override
	public final Component getComponent()
	{
		return new SliderComponent(this);
	}
	
	@Override
	public final void fromJson(JsonElement json)
	{
		if(!JsonUtils.isNumber(json))
			return;
		
		double value = json.getAsDouble();
		if(value > maximum || value < minimum)
			return;
		
		this.value = value;
	}
	
	@Override
	public final JsonElement toJson()
	{
		return new JsonPrimitive(Math.round(value * 1e6) / 1e6);
	}
	
	public static interface ValueDisplay
	{
		public static final ValueDisplay INTEGER = v -> (int)v + "";
		
		public static final ValueDisplay DECIMAL = v -> {
			String s = Math.round(v * 1e6) / 1e6 + "";
			return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
		};
		
		public static final ValueDisplay PERCENTAGE =
			v -> (int)(Math.round(v * 1e8) / 1e6) + "%";
		
		public String getValueString(double value);
		
		public default ValueDisplay withSuffix(String suffix)
		{
			return v -> getValueString(v) + suffix;
		}
	}
}

/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.settings;

import java.awt.Color;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.minecraft.util.Mth;
import net.wimods.freecam.WiFreecam;
import net.wimods.freecam.clickgui.Component;
import net.wimods.freecam.clickgui.components.ColorComponent;
import net.wimods.freecam.util.ColorUtils;
import net.wimods.freecam.util.json.JsonException;
import net.wimods.freecam.util.json.JsonUtils;
import net.wimods.freecam.util.text.WText;

public final class ColorSetting extends Setting
{
	private Color color;
	private final Color defaultColor;
	
	public ColorSetting(String name, WText description, Color color)
	{
		super(name, description);
		this.color = Objects.requireNonNull(color);
		defaultColor = color;
	}
	
	public ColorSetting(String name, String descriptionKey, Color color)
	{
		this(name, WText.translated(descriptionKey), color);
	}
	
	public ColorSetting(String name, Color color)
	{
		this(name, WText.empty(), color);
	}
	
	public Color getColor()
	{
		return color;
	}
	
	public float[] getColorF()
	{
		float red = color.getRed() / 255F;
		float green = color.getGreen() / 255F;
		float blue = color.getBlue() / 255F;
		return new float[]{red, green, blue};
	}
	
	public int getColorI()
	{
		return color.getRGB() | 0xFF000000;
	}
	
	public int getColorI(int alpha)
	{
		return color.getRGB() & 0x00FFFFFF | alpha << 24;
	}
	
	public int getColorI(float alpha)
	{
		return getColorI((int)(Mth.clamp(alpha, 0, 1) * 255));
	}
	
	public int getRed()
	{
		return color.getRed();
	}
	
	public int getGreen()
	{
		return color.getGreen();
	}
	
	public int getBlue()
	{
		return color.getBlue();
	}
	
	public Color getDefaultColor()
	{
		return defaultColor;
	}
	
	public void setColor(Color color)
	{
		this.color = Objects.requireNonNull(color);
		WiFreecam.INSTANCE.saveSettings();
	}
	
	@Override
	public Component getComponent()
	{
		return new ColorComponent(this);
	}
	
	@Override
	public void fromJson(JsonElement json)
	{
		if(!JsonUtils.isString(json))
			return;
		
		try
		{
			setColor(ColorUtils.parseHex(json.getAsString()));
			
		}catch(JsonException e)
		{
			e.printStackTrace();
			setColor(defaultColor);
		}
	}
	
	@Override
	public JsonElement toJson()
	{
		return new JsonPrimitive(ColorUtils.toHex(color));
	}
}

/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.settings;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import com.google.gson.JsonElement;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.wimods.freecam.WiFreecam;
import net.wimods.freecam.clickgui.Component;
import net.wimods.freecam.util.text.WText;

public abstract class Setting
{
	private final String name;
	private final WText description;
	
	public Setting(String name, WText description)
	{
		this.name = Objects.requireNonNull(name);
		this.description = Objects.requireNonNull(description);
	}
	
	public final String getName()
	{
		return name;
	}
	
	public final String getDescription()
	{
		return description.toString();
	}
	
	public final String getWrappedDescription(int width)
	{
		List<FormattedText> lines = WiFreecam.MC.font.getSplitter()
			.splitLines(getDescription(), width, Style.EMPTY);
		
		StringJoiner joiner = new StringJoiner("\n");
		lines.stream().map(FormattedText::getString)
			.forEach(s -> joiner.add(s));
		
		return joiner.toString();
	}
	
	public abstract Component getComponent();
	
	public abstract void fromJson(JsonElement json);
	
	public abstract JsonElement toJson();
}

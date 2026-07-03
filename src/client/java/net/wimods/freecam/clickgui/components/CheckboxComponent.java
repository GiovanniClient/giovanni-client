/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.clickgui.components;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.wimods.freecam.WiFreecam;
import net.wimods.freecam.clickgui.ClickGui;
import net.wimods.freecam.clickgui.ClickGuiIcons;
import net.wimods.freecam.clickgui.Component;
import net.wimods.freecam.settings.CheckboxSetting;
import net.wimods.freecam.util.RenderUtils;

public final class CheckboxComponent extends Component
{
	private static final ClickGui GUI = WiFreecam.INSTANCE.getGui();
	private static final Font TR = MC.font;
	private static final int BOX_SIZE = 11;
	
	private final CheckboxSetting setting;
	
	public CheckboxComponent(CheckboxSetting setting)
	{
		this.setting = setting;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton,
		MouseButtonEvent context)
	{
		switch(mouseButton)
		{
			case GLFW.GLFW_MOUSE_BUTTON_LEFT:
			setting.setChecked(!setting.isChecked());
			break;
			
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
			setting.setChecked(setting.isCheckedByDefault());
			break;
		}
	}
	
	@Override
	public void render(GuiGraphicsExtractor context, int mouseX, int mouseY,
		float partialTicks)
	{
		int x1 = getX();
		int x2 = x1 + getWidth();
		int x3 = x1 + BOX_SIZE;
		int y1 = getY();
		int y2 = y1 + getHeight();
		
		boolean hovering = isHovering(mouseX, mouseY);
		boolean hText = hovering && mouseX >= x3;
		
		if(hText)
			GUI.setTooltip(getTooltip());
		
		// background
		context.fill(x3, y1, x2, y2, getFillColor(false));
		
		// box
		context.fill(x1, y1, x3, y2, getFillColor(hovering));
		int outlineColor = RenderUtils.toIntColor(GUI.getAcColor(), 0.5F);
		RenderUtils.drawBorder2D(context, x1, y1, x3, y2, outlineColor);
		
		context.guiRenderState.up();
		
		// check
		if(setting.isChecked())
			ClickGuiIcons.drawCheck(context, x1, y1, x3, y2, hovering, false);
		
		// text
		String name = setting.getName();
		context.text(TR, name, x3 + 2, y1 + 2, GUI.getTxtColor(), false);
	}
	
	private int getFillColor(boolean hovering)
	{
		float opacity = GUI.getOpacity() * (hovering ? 1.5F : 1);
		return RenderUtils.toIntColor(GUI.getBgColor(), opacity);
	}
	
	private String getTooltip()
	{
		return setting.getWrappedDescription(200);
	}
	
	@Override
	public int getDefaultWidth()
	{
		return BOX_SIZE + TR.width(setting.getName()) + 2;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return BOX_SIZE;
	}
}

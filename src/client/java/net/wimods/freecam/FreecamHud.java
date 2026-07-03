/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam;

import java.util.ArrayList;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.CommonColors;
import net.wimods.freecam.clickgui.screens.ClickGuiScreen;

public final class FreecamHud
{
	private static final ArrayList<WiFreecam> activeHax = new ArrayList<>();
	
	public static void onRenderGui(GuiGraphicsExtractor context,
		float partialTicks)
	{
		WiFreecam freecam = WiFreecam.INSTANCE;
		
		if(freecam.getSettings().renderSpeed.isChecked())
			drawHackList(context, partialTicks);
		
		// pinned windows
		if(!(WiFreecam.MC.screen instanceof ClickGuiScreen))
			freecam.getGui().renderPinnedWindows(context, partialTicks);
	}
	
	private static void drawHackList(GuiGraphicsExtractor context,
		float partialTicks)
	{
		Font font = WiFreecam.MC.font;
		int posX = 2;
		int posY = 2;
		
		for(WiFreecam hack : activeHax)
		{
			context.text(font, hack.getRenderName(), posX + 1, posY + 1,
				CommonColors.BLACK, false);
			context.guiRenderState.up();
			context.text(font, hack.getRenderName(), posX, posY,
				CommonColors.WHITE, false);
			
			posY += 9;
		}
	}
	
	public static void updateState(WiFreecam hack)
	{
		if(hack.isEnabled())
		{
			if(activeHax.contains(hack))
				return;
			
			activeHax.add(hack);
			
		}else
			activeHax.remove(hack);
	}
}

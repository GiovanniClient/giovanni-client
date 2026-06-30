/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixinterface;

import net.minecraft.client.KeyMapping;

public interface IKeyMapping
{
	/**
	 * Returns whether the user is actually pressing this key on their keyboard
	 * or mouse.
	 */
	public default boolean isActuallyDown()
	{
		return freecam_isActuallyDown();
	}
	
	public default void setDown(boolean down)
	{
		asVanilla().setDown(down);
	}
	
	public default KeyMapping asVanilla()
	{
		return (KeyMapping)this;
	}
	
	/**
	 * Returns the given KeyMapping object as an IKeyMapping, allowing you to
	 * access the isActuallyDown() method.
	 */
	public static IKeyMapping get(KeyMapping km)
	{
		return (IKeyMapping)km;
	}
	
	/**
	 * @deprecated Use {@link #isActuallyDown()} instead.
	 */
	@Deprecated
	public boolean freecam_isActuallyDown();
}

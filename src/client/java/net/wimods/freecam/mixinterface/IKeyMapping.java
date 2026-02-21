/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixinterface;

import net.minecraft.client.option.KeyBinding;

public interface IKeyMapping
{
	/**
	 * Returns whether the user is actually pressing this key on their keyboard
	 * or mouse.
	 */
	default boolean isActuallyDown() {
		return freecam_isActuallyDown();
	}
	
	default void setDown(boolean down) {
		asVanilla().setPressed(down);
	}
	
	default KeyBinding asVanilla() {
		return (KeyBinding)this;
	}
	
	/**
	 * Returns the given KeyMapping object as an IKeyMapping, allowing you to
	 * access the isActuallyDown() method.
	 */
	static IKeyMapping get(KeyBinding km) {
		return (IKeyMapping)km;
	}

	/**
	 * @deprecated Use {@link #isActuallyDown()} instead.
	 */
	@Deprecated
    boolean freecam_isActuallyDown();
}

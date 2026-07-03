/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.util.text;

import java.util.Map;
import java.util.Objects;

import net.wimods.freecam.FreecamTranslator;
import net.wimods.freecam.WiFreecam;

public final class WTranslatedTextContent implements WTextContent
{
	private final String key;
	private final Object[] args;
	private String translation;
	private Map<String, String> lastLanguage;
	
	public WTranslatedTextContent(String key, Object... args)
	{
		this.key = Objects.requireNonNull(key);
		this.args = args;
	}
	
	private void update()
	{
		FreecamTranslator translator = WiFreecam.INSTANCE.getTranslator();
		Map<String, String> language =
			translator.getWiFreecamsCurrentLanguage();
		if(language == lastLanguage)
			return;
		
		translation = translator.translate(key, args);
		lastLanguage = language;
	}
	
	@Override
	public String toString()
	{
		update();
		return translation;
	}
}

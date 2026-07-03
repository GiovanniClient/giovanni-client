/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.wimods.freecam.WiFreecam;
import net.wimods.freecam.util.json.JsonException;
import net.wimods.freecam.util.json.JsonUtils;
import net.wimods.freecam.util.json.WsonObject;

public final class SettingsFile
{
	private final Path path;
	private boolean disableSaving;
	
	public SettingsFile(Path path)
	{
		this.path = path;
	}
	
	public void load()
	{
		try
		{
			WsonObject wson = JsonUtils.parseFileToObject(path);
			loadSettings(wson);
			
		}catch(NoSuchFileException e)
		{
			// The file doesn't exist yet. No problem, we'll create it later.
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't load " + path.getFileName());
			e.printStackTrace();
		}
		
		save();
	}
	
	public void loadProfile(Path profilePath) throws IOException, JsonException
	{
		if(!profilePath.getFileName().toString().endsWith(".json"))
			throw new IllegalArgumentException();
		
		WsonObject wson = JsonUtils.parseFileToObject(profilePath);
		loadSettings(wson);
		
		save();
	}
	
	private void loadSettings(WsonObject wson)
	{
		try
		{
			disableSaving = true;
			Map<String, Setting> settings =
				WiFreecam.INSTANCE.getSettings().getMap();
			
			for(Entry<String, JsonElement> e : wson.toJsonObject().entrySet())
			{
				String key = e.getKey().toLowerCase();
				if(!settings.containsKey(key))
					continue;
				
				settings.get(key).fromJson(e.getValue());
			}
			
		}finally
		{
			disableSaving = false;
		}
	}
	
	public void save()
	{
		if(disableSaving)
			return;
		
		JsonObject json = createJson();
		
		try
		{
			JsonUtils.toJson(json, path);
			
		}catch(IOException | JsonException e)
		{
			System.out.println("Couldn't save " + path.getFileName());
			e.printStackTrace();
		}
	}
	
	public void saveProfile(Path profilePath) throws IOException, JsonException
	{
		if(!profilePath.getFileName().toString().endsWith(".json"))
			throw new IllegalArgumentException();
		
		JsonObject json = createJson();
		Files.createDirectories(profilePath.getParent());
		JsonUtils.toJson(json, profilePath);
	}
	
	private JsonObject createJson()
	{
		JsonObject json = new JsonObject();
		Collection<Setting> settings =
			WiFreecam.INSTANCE.getSettings().getMap().values();
		settings.forEach(s -> json.add(s.getName(), s.toJson()));
		return json;
	}
}

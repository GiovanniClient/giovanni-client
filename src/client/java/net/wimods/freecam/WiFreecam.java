/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.Command;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.wimods.freecam.FreecamInputSetting.ApplyInputTo;
import net.wimods.freecam.FreecamInteractionSetting.InteractFrom;
import net.wimods.freecam.clickgui.ClickGui;
import net.wimods.freecam.mixinterface.IKeyMapping;
import net.wimods.freecam.settings.SettingsFile;
import net.wimods.freecam.util.EntityUtils;
import net.wimods.freecam.util.PlausibleAnalytics;
import net.wimods.freecam.util.RenderUtils;

public enum WiFreecam
{
	INSTANCE;
	
	public static final Minecraft MC = Minecraft.getInstance();
	public static final Logger LOGGER = LoggerFactory.getLogger("WI Freecam");
	
	private boolean enabled;
	private Vec3 camPos;
	private Vec3 prevCamPos;
	private float camYaw;
	private float camPitch;
	private float lastHealth;
	
	private FreecamSettings settings;
	private SettingsFile settingsFile;
	private PlausibleAnalytics plausible;
	private ClickGui gui;
	private boolean guiInitialized;
	private FreecamKeybinds keybinds;
	private FreecamTranslator translator;
	
	public void initialize()
	{
		LOGGER.info("Starting WI Freecam...");
		
		Path configDir = createConfigDir();
		
		settings = new FreecamSettings();
		settingsFile = new SettingsFile(configDir.resolve("settings.json"));
		settingsFile.load();
		
		plausible = new PlausibleAnalytics();
		plausible.pageview("/");
		
		Path guiFile = configDir.resolve("windows.json");
		gui = new ClickGui(guiFile);
		
		keybinds = new FreecamKeybinds();
		ClientTickEvents.END_CLIENT_TICK.register(_ -> keybinds.onUpdate());
		
		translator = new FreecamTranslator();
		
		ClientCommandRegistrationCallback.EVENT
			.register((dispatcher, _) -> dispatcher
				.register(ClientCommands.literal("freecam").executes(_ -> {
					MC.schedule(() -> getGui().open());
					return Command.SINGLE_SUCCESS;
				})));
	}
	
	public String getRenderName()
	{
		if(!settings.renderSpeed.isChecked())
			return "Freecam";
		
		return "Freecam [" + settings.horizontalSpeed.getValueString() + ", "
			+ settings.verticalSpeed.getValueString() + "]";
	}
	
	private void onEnable()
	{
		lastHealth = Float.MIN_VALUE;
		LocalPlayer player = MC.player;
		float eyeHeight = player.getEyeHeight(player.getPose());
		Vec3 eyesPos = player.position().add(0, eyeHeight, 0);
		camPos = eyesPos.add(settings.initialPos.getSelected().getOffset());
		prevCamPos = camPos;
		camYaw = player.getYRot();
		camPitch = player.getXRot();
	}
	
	private void onDisable()
	{
		if(settings.reloadChunks.isChecked())
			MC.levelRenderer.allChanged();
	}
	
	public void onUpdate()
	{
		LocalPlayer player = MC.player;
		if(player == null)
		{
			setEnabled(false);
			return;
		}
		
		// Check for damage
		float currentHealth = player.getHealth();
		if(settings.disableOnDamage.isChecked() && currentHealth < lastHealth)
		{
			setEnabled(false);
			return;
		}
		lastHealth = currentHealth;
		
		if(!isMovingCamera() || MC.screen != null)
		{
			prevCamPos = camPos;
			return;
		}
		
		// Get movement vector (x=left, y=forward)
		Vec2 moveVector = player.input.getMoveVector();
		
		// Convert to world coordinates
		double yawRad = MC.gameRenderer.getMainCamera().yRot() * Mth.DEG_TO_RAD;
		double sinYaw = Mth.sin(yawRad);
		double cosYaw = Mth.cos(yawRad);
		double offsetX = moveVector.x * cosYaw - moveVector.y * sinYaw;
		double offsetZ = moveVector.x * sinYaw + moveVector.y * cosYaw;
		
		// Calculate vertical offset
		double offsetY = 0;
		double vSpeed = settings.getActualVerticalSpeed();
		if(IKeyMapping.get(MC.options.keyJump).isActuallyDown())
			offsetY += vSpeed;
		if(IKeyMapping.get(MC.options.keyShift).isActuallyDown())
			offsetY -= vSpeed;
		
		// Apply to camera
		Vec3 offsetVec = new Vec3(offsetX, 0, offsetZ)
			.scale(settings.horizontalSpeed.getValueF()).add(0, offsetY, 0);
		prevCamPos = camPos;
		camPos = camPos.add(offsetVec);
	}
	
	public void onMouseScroll(double amount)
	{
		if(!isControllingScrollEvents())
			return;
		
		if(amount > 0)
			settings.horizontalSpeed.increaseValue();
		else if(amount < 0)
			settings.horizontalSpeed.decreaseValue();
	}
	
	public boolean isControllingScrollEvents()
	{
		return isMovingCamera() && settings.scrollToChangeSpeed.isChecked()
			&& MC.screen == null;
	}
	
	public boolean isMovingCamera()
	{
		return enabled
			&& settings.applyInputTo.getSelected() == ApplyInputTo.CAMERA;
	}
	
	public boolean isClickingFromCamera()
	{
		return enabled
			&& settings.interactFrom.getSelected() == InteractFrom.CAMERA;
	}
	
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		if(!settings.tracer.isChecked())
			return;
		
		int colorI = settings.color.getColorI(0x80);
		
		// Box
		double extraSize = 0.05;
		AABB rawBox = EntityUtils.getLerpedBox(MC.player, partialTicks);
		AABB box = rawBox.move(0, extraSize, 0).inflate(extraSize);
		RenderUtils.drawOutlinedBox(matrixStack, box, colorI, false);
		
		// Line
		RenderUtils.drawTracer(matrixStack, partialTicks, rawBox.getCenter(),
			colorI, false);
	}
	
	public boolean shouldHideHand()
	{
		return enabled && settings.hideHand.isChecked();
	}
	
	public Vec3 getCamPos(float partialTicks)
	{
		return Mth.lerp(partialTicks, prevCamPos, camPos);
	}
	
	public Vec3 getScaledCamDir(double scale)
	{
		return Vec3.directionFromRotation(camPitch, camYaw).scale(scale);
	}
	
	public void turn(double deltaYaw, double deltaPitch)
	{
		// This needs to be consistent with Entity.turn()
		camYaw += (float)(deltaYaw * 0.15);
		camPitch += (float)(deltaPitch * 0.15);
		camPitch = Mth.clamp(camPitch, -90, 90);
	}
	
	public float getCamYaw()
	{
		return camYaw;
	}
	
	public float getCamPitch()
	{
		return camPitch;
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public void setEnabled(boolean enabled)
	{
		if(this.enabled == enabled)
			return;
		
		this.enabled = enabled;
		
		FreecamHud.updateState(this);
		
		if(enabled)
			onEnable();
		else
			onDisable();
	}
	
	private Path createConfigDir()
	{
		try
		{
			Path configDir =
				FabricLoader.getInstance().getConfigDir().resolve("wi_freecam");
			Files.createDirectories(configDir);
			return configDir;
			
		}catch(IOException e)
		{
			throw new RuntimeException(
				"Couldn't create WI Freecam config directory.", e);
		}
	}
	
	public FreecamSettings getSettings()
	{
		return settings;
	}
	
	public void saveSettings()
	{
		settingsFile.save();
	}
	
	public PlausibleAnalytics getPlausible()
	{
		return plausible;
	}
	
	public ClickGui getGui()
	{
		if(!guiInitialized)
		{
			guiInitialized = true;
			gui.init();
		}
		
		return gui;
	}
	
	public FreecamKeybinds getKeybinds()
	{
		return keybinds;
	}
	
	public FreecamTranslator getTranslator()
	{
		return translator;
	}
}

/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.wimods.freecam.mixinterface.IKeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.util.Utils;

public enum WiFreecam {
	INSTANCE;
	
	public static final MinecraftClient MC = MinecraftClient.getInstance();
	public static final Logger LOGGER = LoggerFactory.getLogger("WI Freecam");
	
	public static boolean FREECAM_ENABLED;
	private Vec3d camPos;
	private Vec3d prevCamPos;
	private float camYaw;
	private float camPitch;
	private float lastHealth;
	
	public void initialize() {
		LOGGER.info("Starting WI Freecam...");

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if(FREECAM_ENABLED)
				onUpdate();
		});
	}

	private void onEnable() {
		lastHealth = Float.MIN_VALUE;
        ClientPlayerEntity player = MC.player;
		float eyeHeight = player.getEyeHeight(player.getPose());
		Vec3d eyesPos = player.getEntityPos().add(0, eyeHeight, 0);
		camPos = eyesPos.add(ConfigManager.getConfig().freecamConfig.CAMERA_SPAWN_POSITION.get().getOffset());
		prevCamPos = camPos;
		camYaw = player.getYaw();
		camPitch = player.getPitch();
	}
	
	private void onDisable() {
		MC.worldRenderer.reload();
	}
	
	private void onUpdate() {
        FreecamConfig settingz = ConfigManager.getConfig().freecamConfig;
		ClientPlayerEntity player = MC.player;
		if(player == null) {
			setEnabled(false);
			return;
		}
		
		// Check for damage
		float currentHealth = player.getHealth();
		if(settingz.DISABLE_ON_DAMAGE && currentHealth < lastHealth) {
			setEnabled(false);
			return;
		}
		lastHealth = currentHealth;
		
		if(!isMovingCamera() || MC.currentScreen != null) {
			prevCamPos = camPos;
			return;
		}
		
		// Get movement vector (x=left, y=forward)
		Vec2f moveVector = player.input.getMovementInput();
		
		// Convert to world coordinates
		double yawRad = MC.gameRenderer.getCamera().getYaw() * MathHelper.RADIANS_PER_DEGREE;
		double sinYaw = MathHelper.sin((float) yawRad);
		double cosYaw = MathHelper.cos((float) yawRad);
		double offsetX = moveVector.x * cosYaw - moveVector.y * sinYaw;
		double offsetZ = moveVector.x * sinYaw + moveVector.y * cosYaw;
		
		// Calculate vertical offset
		double offsetY = 0;
		double vSpeed = settingz.getActualVerticalSpeed();
		if(IKeyMapping.get(MC.options.jumpKey).isActuallyDown())
			offsetY += vSpeed;
		if(IKeyMapping.get(MC.options.sneakKey).isActuallyDown())
			offsetY -= vSpeed;

        Vec3d offsetVec = new Vec3d(offsetX, 0, offsetZ)
                .multiply((float) settingz.HORIZONTAL_SPEED / 20.0f).add(0, offsetY, 0);

        prevCamPos = camPos;
		camPos = camPos.add(offsetVec);
	}

    public void onMouseScroll(double amount) {
        FreecamConfig settingz = ConfigManager.getConfig().freecamConfig;

        if(isControllingScrollEvents())
            return;

        int oldHorizontal = settingz.HORIZONTAL_SPEED;
        int oldVertical = settingz.VERTICAL_SPEED;

        if(amount > 0)
            settingz.increaseSpeed();
        else if(amount < 0)
            settingz.decreaseSpeed();

        if (settingz.PRINT_SPEED_TO_CHAT && (settingz.HORIZONTAL_SPEED != oldHorizontal || settingz.VERTICAL_SPEED != oldVertical))
        {
            String message = String.format("§bSpeed: §fH:%.2f §fV:%.2f",
                    settingz.HORIZONTAL_SPEED / 20.0,
                    settingz.VERTICAL_SPEED / 20.0);

            Utils.chat(message);
        }
    }
	
	public boolean isControllingScrollEvents() {
		return !isMovingCamera() || !ConfigManager.getConfig().freecamConfig.SCROLL_TO_CHANGE_SPEED || MC.currentScreen != null;
	}
	
	public boolean isMovingCamera()	{
		return FREECAM_ENABLED && ConfigManager.getConfig().freecamConfig.APPLY_INPUT_TO.get() == FreecamConfig.InputEnum.Camera;
	}
	
	public boolean shouldHideHand()	{
		return FREECAM_ENABLED && ConfigManager.getConfig().freecamConfig.HIDE_HAND;
	}
	
	public Vec3d getCamPos(float partialTicks) {
		return MathHelper.lerp(partialTicks, prevCamPos, camPos);
	}
	
	public void turn(double deltaYaw, double deltaPitch) {
		// This needs to be consistent with Entity.turn()
		camYaw += (float)(deltaYaw * 0.15);
		camPitch += (float)(deltaPitch * 0.15);
		camPitch = MathHelper.clamp(camPitch, -90, 90);
	}
	
	public float getCamYaw() {
		return camYaw;
	}
	
	public float getCamPitch() {
		return camPitch;
	}
	
	public boolean isEnabled() {
		return FREECAM_ENABLED;
	}
	
	public void setEnabled(boolean enabled) {
		if(this.FREECAM_ENABLED == enabled)
			return;
		
		this.FREECAM_ENABLED = enabled;

		if(enabled)
			onEnable();
		else
			onDisable();
	}
}

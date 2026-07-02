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

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.wimods.freecam.mixinterface.IKeyMapping;
import net.wimods.freecam.util.EntityUtils;
import net.wimods.freecam.util.RenderUtils;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.util.Utils;

public enum WiFreecam {
	INSTANCE;
	
	public static final Minecraft MC = Minecraft.getInstance();
	public static final Logger LOGGER = LoggerFactory.getLogger("WI Freecam");

    public static boolean FREECAM_ENABLED;
	private Vec3 camPos;
	private Vec3 prevCamPos;
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

	private void onEnable()
	{
		lastHealth = Float.MIN_VALUE;
		LocalPlayer player = MC.player;
		float eyeHeight = player.getEyeHeight(player.getPose());
		Vec3 eyesPos = player.position().add(0, eyeHeight, 0);
        camPos = eyesPos.add(ConfigManager.getConfig().freecamConfig.CAMERA_SPAWN_POSITION.get().getOffset());
		prevCamPos = camPos;
		camYaw = player.getYRot();
		camPitch = player.getXRot();
	}
	
	private void onDisable()
	{
	    MC.levelRenderer.allChanged();
	}

	public void onUpdate() {
        FreecamConfig settingz = ConfigManager.getConfig().freecamConfig;
		LocalPlayer player = MC.player;
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

		if(!isMovingCamera() || MC.screen != null) {
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
		double vSpeed = settingz.getActualVerticalSpeed();
		if(IKeyMapping.get(MC.options.keyJump).isActuallyDown())
			offsetY += vSpeed;
		if(IKeyMapping.get(MC.options.keyShift).isActuallyDown())
			offsetY -= vSpeed;

		// Apply to camera
		Vec3 offsetVec = new Vec3(offsetX, 0, offsetZ)
			.scale(settingz.HORIZONTAL_SPEED / 200.0).add(0, offsetY, 0);
		prevCamPos = camPos;
		camPos = camPos.add(offsetVec);
	}

    public void onMouseScroll(double amount) {
		FreecamConfig settingz = ConfigManager.getConfig().freecamConfig;

		if (!isControllingScrollEvents())
			return;

		int oldHorizontal = settingz.HORIZONTAL_SPEED;
		int oldVertical = settingz.VERTICAL_SPEED;

		if (amount > 0) {
			settingz.increaseHorizontalSpeed();
			settingz.increaseVerticalSpeed();
		}
		else if (amount < 0) {
			settingz.decereaseHorizontalSpeed();
			settingz.decreaseVerticalSpeed();
		}

        if (settingz.PRINT_SPEED_TO_CHAT && (settingz.HORIZONTAL_SPEED != oldHorizontal || settingz.VERTICAL_SPEED != oldVertical))
        {
            String message = String.format("§bSpeed: §fH:%.2f §fV:%.2f",
                    settingz.HORIZONTAL_SPEED / 200.0,
                    settingz.VERTICAL_SPEED / 200.0);

            Utils.chat(message);
        }
    }

	public boolean isControllingScrollEvents() {
		return isMovingCamera()
				&& ConfigManager.getConfig().freecamConfig.SCROLL_TO_CHANGE_SPEED
				&& MC.screen == null;
	}

    public boolean isMovingCamera()	{
        return FREECAM_ENABLED && ConfigManager.getConfig().freecamConfig.APPLY_INPUT_TO.get() == FreecamConfig.InputEnum.Camera;
    }

    public boolean isClickingFromCamera() {

        return FREECAM_ENABLED && ConfigManager.getConfig().freecamConfig.APPLY_INPUT_TO.get() == FreecamConfig.InputEnum.Camera;
    }
	
	public void onRender(PoseStack matrixStack, float partialTicks) {
        if(!ConfigManager.getConfig().freecamConfig.FREECAM_TRACER) return;

        int colorI = ConfigManager.getConfig().freecamConfig.FREECAM_TRACER_COLOR.getEffectiveColour().getRGB();
		
		// Box
		double extraSize = 0.05;
		AABB rawBox = EntityUtils.getLerpedBox(MC.player, partialTicks);
		AABB box = rawBox.move(0, extraSize, 0).inflate(extraSize);
		RenderUtils.drawOutlinedBox(matrixStack, box, colorI, false);
		
		// Line
		RenderUtils.drawTracer(matrixStack, partialTicks, rawBox.getCenter(),
			colorI, false);
	}

    public boolean shouldHideHand()	{
        return FREECAM_ENABLED && ConfigManager.getConfig().freecamConfig.HIDE_HAND;
    }
	
	public Vec3 getCamPos(float partialTicks) {
		return Mth.lerp(partialTicks, prevCamPos, camPos);
	}
	
	public Vec3 getScaledCamDir(double scale) {
		return Vec3.directionFromRotation(camPitch, camYaw).scale(scale);
	}

	public void turn(double deltaYaw, double deltaPitch) {
		// This needs to be consistent with Entity.turn()
		camYaw += (float)(deltaYaw * 0.15);
		camPitch += (float)(deltaPitch * 0.15);
		camPitch = Mth.clamp(camPitch, -90, 90);
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

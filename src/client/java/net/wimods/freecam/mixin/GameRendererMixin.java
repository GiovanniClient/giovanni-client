/*
 * Copyright (c) 2026-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.freecam.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.GameRenderer;
import net.wimods.freecam.WiFreecam;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
{
    /**
     * Prevents view bobbing when in Freecam.
     */
    @WrapOperation(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
                    ordinal = 0))
    private void onBobView(GameRenderer instance, PoseStack matrices,
                           float tickDelta, Operation<Void> original)
    {
        if(!WiFreecam.INSTANCE.isEnabled())
            original.call(instance, matrices, tickDelta);
    }
}

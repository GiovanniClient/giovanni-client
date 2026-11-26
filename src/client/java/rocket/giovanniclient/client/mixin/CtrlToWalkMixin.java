package rocket.giovanniclient.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.wimods.freecam.mixinterface.IKeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.config.MainConfig;

@Mixin(LocalPlayer.class)
public abstract class CtrlToWalkMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow protected int sprintTriggerTime;

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"))
    private boolean giovanni$walkWhileSprintKeyHeld(LocalPlayer player, Operation<Boolean> original) {
        if (giovanni$isCtrlToWalkActive()) {
            KeyMapping sprintKey = minecraft.options.keySprint;
            if (!sprintKey.isUnbound() && IKeyMapping.get(sprintKey).isActuallyDown()) {
                // This gate also covers vanilla's double-tap sprint.
                sprintTriggerTime = 0;
                if (player.isSprinting()) {
                    player.setSprinting(false);
                }
                return false;
            }
        }
        return original.call(player);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean giovanni$requestAutomaticSprint(boolean original) {
        return original || giovanni$isCtrlToWalkActive();
    }

    @Unique
    private boolean giovanni$isCtrlToWalkActive() {
        MainConfig config = ConfigManager.getConfig();
        return config != null && config.miscConfig != null && config.miscConfig.CTRL_TO_WALK
                && minecraft.gui.screen() == null && minecraft.isWindowActive();
    }
}

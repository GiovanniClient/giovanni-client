package sb.rocket.giovanniclient.client.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.config.ConfigManager;

@Mixin(Entity.class)
public abstract class UndoInvisibilityMixin {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void giovanni$disableInvisibility(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        var rc = ConfigManager.getConfig().rc;

        // Fel → mob
        if (rc.FEL_VISIBLE_TOGGLE && entity instanceof MobEntity) {
            cir.setReturnValue(false);
            return;
        }

        // Shadow Assassin → player
        if (rc.SHADOW_ASSASSIN_VISIBLE_TOGGLE && entity instanceof PlayerEntity) {
            cir.setReturnValue(false);
        }
    }
}

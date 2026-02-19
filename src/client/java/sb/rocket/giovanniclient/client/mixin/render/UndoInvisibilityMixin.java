package sb.rocket.giovanniclient.client.mixin.render;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.util.PlayerLocator;

@Mixin(Entity.class)
public abstract class UndoInvisibilityMixin {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void giovanni$disableInvisibility(CallbackInfoReturnable<Boolean> cir) {

        String currentLocation = PlayerLocator.getPlayerLocation();

        Entity entity = (Entity) (Object) this;
        var rc = ConfigManager.getConfig().rc.renderEntitiesAccordion;

        if (rc.EVERYTHING_VISIBLE_TOGGLE) {
            cir.setReturnValue(false);
            return;
        }

        // Fel → mob
        if (rc.FEL_VISIBLE_TOGGLE && entity instanceof EndermanEntity) {
            if (!currentLocation.contains("Catacombs")) return;
            cir.setReturnValue(false);
            return;
        }

        // Shadow Assassin → player
        if (rc.SHADOW_ASSASSIN_VISIBLE_TOGGLE && entity instanceof PlayerEntity) {
            if (!currentLocation.contains("Catacombs")) return;
            cir.setReturnValue(false);
            return;
        }
    }
}

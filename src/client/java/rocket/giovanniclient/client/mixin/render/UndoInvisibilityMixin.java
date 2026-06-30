package rocket.giovanniclient.client.mixin.render;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.util.PlayerLocator;

@Mixin(Entity.class)
public abstract class UndoInvisibilityMixin {

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void giovanni$disableInvisibility(CallbackInfoReturnable<Boolean> cir) {

        String currentLocation = PlayerLocator.getPlayerLocation();

        Entity entity = (Entity) (Object) this;
        var rc = ConfigManager.getConfig().rc.renderEntitiesAccordion;
        var dc = ConfigManager.getConfig().debugConfig;
        var riftconfig = ConfigManager.getConfig().riftconfig;

        if (dc.EVERYTHING_VISIBLE_TOGGLE && !(entity instanceof ArmorStand)) {
            cir.setReturnValue(false);
            return;
        } else if (dc.SEE_INVISIBLE_ARMOR_STANDS && entity instanceof ArmorStand) {
            cir.setReturnValue(false);
            return;
        }

        // Fel → mob
        if (rc.FEL_VISIBLE_TOGGLE && entity instanceof EnderMan) {
            if (!currentLocation.contains("Catacombs")) return;
            cir.setReturnValue(false);
            return;
        }

        // Shadow Assassin → player
        if (rc.SHADOW_ASSASSIN_VISIBLE_TOGGLE && entity instanceof Player) {
            if (!currentLocation.contains("Catacombs")) return;
            cir.setReturnValue(false);
            return;
        }

        if (riftconfig.INVIS_PLAYERS_IN_TINY_DANCER && entity instanceof Player
                && currentLocation.contains("Mirrorverse")) {
            cir.setReturnValue(true);
        }
    }
}

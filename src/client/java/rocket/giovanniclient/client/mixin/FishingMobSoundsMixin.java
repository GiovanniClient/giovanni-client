package rocket.giovanniclient.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.item.FishingRodItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(SoundEngine.class)
public abstract class FishingMobSoundsMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void giovanni$muteFishingMobSounds(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        var config = ConfigManager.getConfig();
        if (config == null || config.miscConfig == null || !config.miscConfig.MUTE_FISHING_MOB_SOUNDS) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isHolding(stack -> stack.getItem() instanceof FishingRodItem)
                && giovanni$isMobDamageSound(sound.getIdentifier().getPath())) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

    @Unique
    private static boolean giovanni$isMobDamageSound(String path) {
        if (path.equals("item.wolf_armor.damage")) {
            return true;
        }
        if (!path.startsWith("entity.") || path.startsWith("entity.player.")
                || path.equals("entity.ender_eye.death") || path.equals("entity.shulker_bullet.hurt")) {
            return false;
        }

        int action = path.lastIndexOf('.');
        return switch (path) {
            case "entity.creaking.sway", "entity.iron_golem.damage", "entity.generic.big_fall", "entity.generic.small_fall",
                 "entity.hostile.big_fall", "entity.hostile.small_fall" -> true;
            default -> path.endsWith(".hurt") || path.startsWith(".hurt_", action)
                    || path.endsWith(".death") || path.startsWith(".death_", action);
        };
    }
}

package rocket.giovanniclient.client.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import rocket.giovanniclient.client.config.ConfigManager;

/**
 * Lowers SBO's hard AFK-timeout floor from 15 seconds to one second.
 */
@Pseudo
@Mixin(targets = "net.sbo.mod.utils.SboTimerManager$SBOTimer", remap = false)
public class SboTimerMixin {
    @ModifyConstant(
            method = "getInactivityLimitNanos",
            constant = @Constant(longValue = 15L),
            require = 0
    )
    private long giovanni$allowOneSecondInactivityLimit(long originalMinimum) {
        var config = ConfigManager.getConfig();
        return config != null
                && config.fc != null
                && config.fc.SBO_ONE_SECOND_AFK_TIMEOUT
                ? 1L
                : originalMinimum;
    }
}

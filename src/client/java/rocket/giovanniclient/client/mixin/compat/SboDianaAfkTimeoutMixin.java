package rocket.giovanniclient.client.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Lets SBO expose a one-second AFK timeout in its Diana config screen.
 *
 * SBO is deliberately not a compile dependency: this mixin is applied only
 * when its optional target class is present at runtime.
 */
@Pseudo
@Mixin(targets = "net.sbo.mod.settings.categories.Diana", remap = false)
public class SboDianaAfkTimeoutMixin {
    @ModifyArg(
            method = "afkTimeout_delegate$lambda$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lkotlin/ranges/IntRange;<init>(II)V"
            ),
            index = 0,
            require = 0
    )
    private static int giovanni$allowOneSecondAfkTimeout(int originalMinimum) {
        return 1;
    }
}

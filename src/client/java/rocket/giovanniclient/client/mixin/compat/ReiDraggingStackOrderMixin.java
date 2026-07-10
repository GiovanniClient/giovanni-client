package rocket.giovanniclient.client.mixin.compat;

import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.impl.client.gui.dragging.CurrentDraggingStack;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(value = me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl.class, remap = false)
public abstract class ReiDraggingStackOrderMixin {
    @Shadow
    private CurrentDraggingStack draggingStack;

    @Unique
    private boolean giovanni$handledEarlyDraggingStackClick;

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void giovanni$updateDraggingStackBeforeWidgets(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        giovanni$handledEarlyDraggingStackClick = false;

        if (giovanni$isPatchEnabled()
                && REIRuntime.getInstance().isOverlayVisible()
                && draggingStack != null) {
            draggingStack.mouseClicked(event, doubleClick);
            giovanni$handledEarlyDraggingStackClick = true;
        }
    }

    @Redirect(
            method = "mouseClicked",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/shedaniel/rei/impl/client/gui/dragging/CurrentDraggingStack;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"
            )
    )
    private boolean giovanni$skipDuplicateDraggingStackClick(
            CurrentDraggingStack instance,
            MouseButtonEvent event,
            boolean doubleClick
    ) {
        if (giovanni$handledEarlyDraggingStackClick) {
            return false;
        }

        return instance.mouseClicked(event, doubleClick);
    }

    @Unique
    private boolean giovanni$isPatchEnabled() {
        try {
            var config = ConfigManager.getConfig();
            return config == null
                    || config.ibc == null
                    || config.ibc.FIX_REI_DRAGGING;
        } catch (Throwable ignored) {
            return true;
        }
    }
}

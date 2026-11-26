package rocket.giovanniclient.client.mixin.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.bootstrap.ClientKeybinds;
import rocket.giovanniclient.client.features.misc.GuiScaleCycler;

@Mixin(Minecraft.class)
public abstract class GuiScaleKeybindMixin {

    @Inject(method = "handleGlobalKeyPress", at = @At("RETURN"), cancellable = true)
    private void giovanni$cycleScaleOnScreen(InputConstants.Key key, boolean controlDown,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;

        Minecraft client = (Minecraft) (Object) this;
        var screen = client.gui.screen();
        if (screen == null || client.player == null) return;

        var scaleKey = ClientKeybinds.cycleGuiScale();
        if (scaleKey == null || scaleKey.isUnbound() || !scaleKey.matches(key)) return;
        if (screen instanceof KeyBindsScreen controls && controls.selectedKey != null) return;
        if (key.getType() != InputConstants.Type.MOUSE
                && screen.getFocused() instanceof EditBox field && field.canConsumeInput()) return;

        GuiScaleCycler.cycle(client);
        cir.setReturnValue(true);
    }
}

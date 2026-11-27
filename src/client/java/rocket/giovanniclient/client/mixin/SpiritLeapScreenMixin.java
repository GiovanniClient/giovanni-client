package rocket.giovanniclient.client.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rocket.giovanniclient.client.features.dungeons.SpiritLeap;

@Mixin(Gui.class)
public class SpiritLeapScreenMixin {
    @ModifyVariable(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            at = @At("HEAD"), argsOnly = true)
    private Screen giovanni$useSpiritLeapScreen(Screen screen) {
        return SpiritLeap.replaceScreen(screen);
    }
}

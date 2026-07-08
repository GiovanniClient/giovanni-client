package rocket.giovanniclient.client.mixin.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.architectury.impl.fabric.ScreenInputDelegate$DelegateScreen")
public abstract class ArchitecturyDelegateScreenInitMixin extends Screen {

    protected ArchitecturyDelegateScreenInitMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void giovanni$initFabricScreenEvents(Screen parent, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        // Architectury creates this delegate outside Minecraft's normal setScreen path, so Fabric's screen event state needs a manual init.
        this.init(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
    }
}

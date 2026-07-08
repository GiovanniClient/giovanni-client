package rocket.giovanniclient.client.mixin;

import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(JoinMultiplayerScreen.class)
public class SyncProxyConfig {
    /**
     * every time we draw the multiplayer screen, we sync the state of the proxy from main to client
     * might be cursed done this way but hey it works
     */
    @Inject(method = "init", at = @At("HEAD"))
    private void init(CallbackInfo info) {
        ConfigManager.syncProxyState();
    }
}

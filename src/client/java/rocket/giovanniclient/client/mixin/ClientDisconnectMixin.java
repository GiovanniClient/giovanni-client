package rocket.giovanniclient.client.mixin;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.DisconnectionDetails;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.features.misc.AutoReconnect;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientDisconnectMixin {
    @Shadow @Final protected @Nullable ServerData serverData;
    @Shadow @Final protected @Nullable Screen postDisconnectScreen;
    @Shadow protected boolean isTransferring;

    @Inject(method = "createDisconnectScreen", at = @At("RETURN"))
    private void giovanni$scheduleReconnect(DisconnectionDetails details, CallbackInfoReturnable<Screen> cir) {
        if (!isTransferring && serverData != null && !serverData.isRealm()
                && cir.getReturnValue() instanceof DisconnectedScreen screen) {
            AutoReconnect.schedule(screen, postDisconnectScreen, serverData);
        }
    }
}

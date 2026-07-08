package rocket.giovanniclient.giovanniclient.mixin;

import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rocket.giovanniclient.giovanniclient.config.ProxyState;

@Mixin(ClientIntentionPacket.class)
public class ClientIntentionPacketMixin {

    @ModifyArg(
            method = "write",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/FriendlyByteBuf;writeUtf(Ljava/lang/String;)Lnet/minecraft/network/FriendlyByteBuf;"
            ),
            index = 0
    )
    private String modifyHostName(String originalHost) {
        return ProxyState.enabled ? "mc.hypixel.net" : originalHost;
    }
}
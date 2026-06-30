package rocket.giovanniclient.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.util.ScoreboardUtils;
import rocket.giovanniclient.client.util.SlayerUtils;

@Mixin(ClientPacketListener.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void onChatMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        String plainText = ScoreboardUtils.stripMinecraftFormatting(packet.content().getString());
        if (plainText.contains("SLAYER QUEST COMPLETE!")) SlayerUtils.setIsBossAlive(false);
    }
}
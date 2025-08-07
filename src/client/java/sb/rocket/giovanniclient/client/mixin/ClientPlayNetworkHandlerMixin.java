package sb.rocket.giovanniclient.client.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.util.ScoreboardUtils;
import sb.rocket.giovanniclient.client.util.SlayerUtils;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void onChatMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        String plainText = ScoreboardUtils.stripMinecraftFormatting(packet.content().getString());
        if (plainText.contains("SLAYER QUEST COMPLETE!")) SlayerUtils.setIsBossAlive(false);
    }
}
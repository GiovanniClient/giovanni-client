package rocket.giovanniclient.client.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.bootstrap.LocalCommandPolicy;

/** Last line of defence after Fabric has attempted client command execution. */
@Mixin(Connection.class)
public abstract class LocalCommandPacketGuardMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void giovanni$neverSendLocalCommand(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (packet instanceof ServerboundCommandSuggestionPacket suggestions) {
            if (LocalCommandPolicy.isLocal(suggestions.getCommand())) ci.cancel();
            return;
        }
        boolean local = switch (packet) {
            case ServerboundChatCommandPacket command -> LocalCommandPolicy.isLocal(command.command());
            case ServerboundChatCommandSignedPacket command -> LocalCommandPolicy.isLocal(command.command());
            case ServerboundChatPacket chat -> LocalCommandPolicy.isLocalSlashMessage(chat.message());
            default -> false;
        };
        if (!local) return;
        // Cancel before scheduling feedback: UI errors must never allow the packet through.
        ci.cancel();
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.gui.hud.getChat().addClientSystemMessage(Component.literal(
                "§cGiovanni: invalid or unavailable local command. It was not sent to the server."
                        + " §e/gio help §7or §e/gio dex help")));
    }

}

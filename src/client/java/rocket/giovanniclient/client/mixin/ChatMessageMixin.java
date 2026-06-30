package rocket.giovanniclient.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.fun.FunConfig;

@Mixin(ClientPacketListener.class)
public class ChatMessageMixin {
    @Unique
    private final FunConfig fc = ConfigManager.getConfig().fc;

    @ModifyVariable(
            method = "sendChat",
            at = @At("HEAD"),
            argsOnly = true
    )
    private String modifySentChatMessage(String userInput) {
        if (fc.FAKE_IRONMAN_TOGGLE && !userInput.startsWith("/")) {
            return "♲: " + userInput;
        }

        if (fc.TROLL_FEATURES && userInput.equals("Help Wizardman!"))
            return "Help Giovanni!";

        return userInput;
    }
}
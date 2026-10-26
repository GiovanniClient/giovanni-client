package rocket.giovanniclient.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.config.MainConfig;
import rocket.giovanniclient.client.features.misc.ChatHistorySearch;

@Mixin(ChatScreen.class)
public abstract class ChatHistoryMixin {
    @Shadow protected EditBox input;
    @Shadow private int historyPos;
    @Shadow private String historyBuffer;
    @Shadow private CommandSuggestions commandSuggestions;
    @Shadow public abstract void moveInHistory(int direction);

    @Unique
    private final ChatHistorySearch giovanni$historySearch = new ChatHistorySearch();
    @Unique
    private boolean giovanni$changingHistory;

    @Unique
    private boolean giovanni$isHistoryPrefixEnabled() {
        MainConfig config = ConfigManager.getConfig();
        return config != null && config.miscConfig != null && config.miscConfig.CHAT_HISTORY_PREFIX;
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void giovanni$resetHistorySearch(CallbackInfo ci) {
        // init also runs on resize, when vanilla resets its history position.
        giovanni$historySearch.reset();
    }

    @Inject(method = "onEdited", at = @At("HEAD"))
    private void giovanni$restartHistorySearch(String value, CallbackInfo ci) {
        if (!giovanni$changingHistory) {
            giovanni$historySearch.reset();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void giovanni$prioritizeHistory(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!giovanni$isHistoryPrefixEnabled()) return;
        // Command suggestions normally consume the arrows before moveInHistory is called.
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN) {
            moveInHistory(event.key() == GLFW.GLFW_KEY_UP ? -1 : 1);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "moveInHistory", at = @At("HEAD"), cancellable = true)
    private void giovanni$filterHistory(int direction, CallbackInfo ci) {
        if (!giovanni$isHistoryPrefixEnabled()) {
            giovanni$historySearch.reset();
            return;
        }

        var history = Minecraft.getInstance().gui.hud.getChat().getRecentChat();
        if (!giovanni$historySearch.isActive()) {
            historyBuffer = input.getValue();
        }
        var selection = giovanni$historySearch.move(history, input.getValue(), direction);
        historyPos = selection.index();
        if (!selection.value().equals(input.getValue())) {
            giovanni$changingHistory = true;
            try {
                input.setValue(selection.value());
            } finally {
                giovanni$changingHistory = false;
            }
            commandSuggestions.setAllowSuggestions(false);
        }
        ci.cancel();
    }
}

package sb.rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonEditorFlow;
import sb.rocket.giovanniclient.client.util.ScoreboardUtils;

import java.util.List;

public final class ClientCommands {

    private ClientCommands() {}

    public static void register() {
        registerConfigAliases();
        registerSidebar();
        registerInventoryButtonsEditor();
    }

    private static void registerConfigAliases() {
        String[] aliases = {"giovanni", "giovanniclient", "gio", "giocli", "giova", "zoo"};

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String alias : aliases) {
                dispatcher.register(ClientCommandManager.literal(alias).executes(context -> {
                    ConfigManager.openConfigScreenFromCommand();
                    return 1;
                }));
            }
        });
    }

    private static void registerSidebar() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("sidebar").executes(context -> {
                    List<String> lines = ScoreboardUtils.getCleanedSidebarLines();

                    if (lines.isEmpty()) {
                        context.getSource().sendFeedback(Text.literal("No scoreboard sidebar currently displayed or it is empty."));
                        return 0;
                    }

                    context.getSource().sendFeedback(Text.literal("--- Scoreboard Sidebar ---"));
                    for (String line : lines) context.getSource().sendFeedback(Text.literal(line));
                    context.getSource().sendFeedback(Text.literal("--------------------------"));

                    return 1;
                }))
        );
    }

    private static void registerInventoryButtonsEditor() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("gioeditbuttons").executes(ctx -> {
                    InventoryButtonEditorFlow.requestOpenFromCommand(ctx.getSource());
                    return 1;
                }))
        );
    }
}

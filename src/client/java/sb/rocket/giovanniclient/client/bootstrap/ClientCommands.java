package sb.rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import sb.rocket.giovanniclient.client.util.ScoreboardUtils;
import sb.rocket.giovanniclient.client.util.TabListUtils;

import java.util.List;

public final class ClientCommands {

    private ClientCommands() {}

    public static void register() {
        registerConfigAliases();
        registerSidebar();
        registerInventoryButtonsEditor();
        registerTabDump();
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
                dispatcher.register(ClientCommandManager.literal("gioeditbuttons")
                        .executes(context -> {
                            EditModeState.setEditMode(true);
                            MinecraftClient.getInstance().send(() -> {
                                MinecraftClient.getInstance().setScreen(new InventoryScreen(MinecraftClient.getInstance().player));
                            });
                            return 1;
                        })
                ));
    }

    private static void registerTabDump() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("tabdump").executes(ctx -> {
                    List<String> lines = TabListUtils.getCleanedLines(true, true);

                    if (lines.isEmpty()) {
                        ctx.getSource().sendFeedback(Text.literal("No TAB list currently available (not in-world / not connected)."));
                        return 0;
                    }

                    ctx.getSource().sendFeedback(Text.literal("--- TAB (Player List) ---"));
                    for (String line : lines) ctx.getSource().sendFeedback(Text.literal(line));
                    ctx.getSource().sendFeedback(Text.literal("-------------------------"));

                    return 1;
                }))
        );
    }
}

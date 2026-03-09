package rocket.giovanniclient.client.bootstrap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import rocket.giovanniclient.client.util.ScoreboardUtils;
import rocket.giovanniclient.client.util.TabListUtils;

import java.util.List;

import static rocket.giovanniclient.client.GiovanniClientClient.UPDATE_MANAGER;

public final class ClientCommands {

    private ClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerConfigAliases(dispatcher);
            registerSidebar(dispatcher);
            registerInventoryButtonsEditor(dispatcher);
            registerTabDump(dispatcher);
            registerRStestcommand(dispatcher);
            registerUpdateCommands(dispatcher);
        });
    }

    public static void registerSafemode() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerUpdateCommands(dispatcher);
        });
    }

    private static void registerConfigAliases(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        String[] aliases = {"giovanni", "giovanniclient", "gio", "zoo"};

        for (String alias : aliases) {
            dispatcher.register(ClientCommandManager.literal(alias).executes(context -> {
                ConfigManager.openConfigScreenFromCommand();
                return 1;
            }));
        }
    }

    private static void registerSidebar(CommandDispatcher<FabricClientCommandSource> dispatcher) {
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
        }));
    }

    private static void registerInventoryButtonsEditor(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("gioeditbuttons")
                .executes(context -> {
                    EditModeState.setEditMode(true);
                    MinecraftClient.getInstance().send(() -> {
                        assert MinecraftClient.getInstance().player != null;
                        MinecraftClient.getInstance().setScreen(new InventoryScreen(MinecraftClient.getInstance().player));
                    });
                    return 1;
                })
        );
    }

    private static void registerTabDump(CommandDispatcher<FabricClientCommandSource> dispatcher) {
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
        }));
    }

    private static void registerUpdateCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("giovanni-check-update")
                .executes(context -> {
                    UPDATE_MANAGER.handleCheckCommand();
                    return 1;
                })
        );
        dispatcher.register(ClientCommandManager.literal("giovanni-do-update")
                .executes(context -> {
                    UPDATE_MANAGER.handleInstallCommand();
                    return 1;
                })
        );
    }

    private static void registerRStestcommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("ratterscanner")
                .then(ClientCommandManager.argument("sha256", StringArgumentType.string())
                        .executes(context -> {
                            String hash = StringArgumentType.getString(context, "sha256");
                            UPDATE_MANAGER.handleTestCommand(hash);
                            return 1;
                        })
                )
        );
    }
}

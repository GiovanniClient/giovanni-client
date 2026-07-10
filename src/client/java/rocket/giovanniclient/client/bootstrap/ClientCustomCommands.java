package rocket.giovanniclient.client.bootstrap;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueOutput;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import rocket.giovanniclient.client.features.render.PlayerTracer;
import rocket.giovanniclient.client.features.updater.RatterScannerChecker;
import rocket.giovanniclient.client.util.ScoreboardUtils;
import rocket.giovanniclient.client.util.TabListUtils;

import java.util.List;

import static rocket.giovanniclient.client.GiovanniClientClient.UPDATE_MANAGER;

public final class ClientCustomCommands {

    private ClientCustomCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            slash_giovanniclient(dispatcher);
            slash_giovanniUpdates(dispatcher);
            inventorybuttons(dispatcher);
            slash_dumpsidebar(dispatcher);
            slash_dumptab(dispatcher);
            slash_dumpentities(dispatcher);
            slash_tracer(dispatcher);
            slash_ratterscannertestsha256(dispatcher);
        });
    }

    public static void registerSafemode() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            slash_giovanniUpdates(dispatcher);
        });
    }

    private static void slash_giovanniclient(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        String[] aliases = {"giovanni", "giovanniclient", "gio", "zoo"};

        for (String alias : aliases) {
            dispatcher.register(ClientCommands.literal(alias).executes(context -> {
                ConfigManager.openConfigScreenFromCommand();
                return 1;
            }));
        }
    }

    private static void slash_dumpsidebar(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("dumpsidebar").executes(context -> {
            List<String> lines = ScoreboardUtils.getCleanedSidebarLines();

            if (lines.isEmpty()) {
                context.getSource().sendFeedback(Component.literal("No scoreboard sidebar currently displayed or it is empty."));
                return 0;
            }

            context.getSource().sendFeedback(Component.literal("--- Scoreboard Sidebar ---"));
            for (String line : lines) context.getSource().sendFeedback(Component.literal(line));
            context.getSource().sendFeedback(Component.literal("--------------------------"));

            return 1;
        }));
    }

    private static void inventorybuttons(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        String[] aliases = {"gioeditbuttons", "inventorybuttons", "neubuttons", "giobuttons"};

        for (String alias : aliases) {
            dispatcher.register(ClientCommands.literal(alias).executes(context -> {
                Minecraft client = Minecraft.getInstance();

                client.execute(() -> {
                    if (client.player != null) {
                        EditModeState.setEditMode(true);
                        client.setScreen(new InventoryScreen(client.player));
                    }
                });

                return 1;
            }));
        }
    }

    private static void slash_dumptab(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("dumptab").executes(ctx -> {
            if (!ConfigManager.getConfig().debugConfig.DEBUG) return 0;

            List<String> lines = TabListUtils.getCleanedLines(true, true);

            if (lines.isEmpty()) {
                ctx.getSource().sendFeedback(Component.literal("No TAB list currently available (not in-world / not connected)."));
                return 0;
            }

            ctx.getSource().sendFeedback(Component.literal("--- TAB (Player List) ---"));
            for (String line : lines) ctx.getSource().sendFeedback(Component.literal(line));
            ctx.getSource().sendFeedback(Component.literal("-------------------------"));

            return 1;
        }));
    }

    private static void slash_dumpentities(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("dumpentities").executes(context -> {
            Minecraft client = Minecraft.getInstance();

            if (client.level == null || client.player == null) {
                context.getSource().sendFeedback(Component.literal("Not in-world / not connected."));
                return 0;
            }

            double radius = 7.0;
            double radiusSquared = radius * radius;
            int count = 0;

            context.getSource().sendFeedback(Component.literal("--- Entity NBT within 7 blocks ---"));

            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity.distanceToSqr(client.player) > radiusSquared) continue;

                TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
                entity.saveWithoutId(output);

                String name = entity.getDisplayName().getString();
                String type = entity.getType().toString();
                double distance = Math.sqrt(entity.distanceToSqr(client.player));

                context.getSource().sendFeedback(Component.literal(
                        String.format("[%d] %s (%s) - %.2f blocks", ++count, name, type, distance)
                ));
                context.getSource().sendFeedback(Component.literal(output.buildResult().toString()));
            }

            if (count == 0) {
                context.getSource().sendFeedback(Component.literal("No entities found within 7 blocks."));
            } else {
                context.getSource().sendFeedback(Component.literal("Dumped " + count + " entities."));
            }

            context.getSource().sendFeedback(Component.literal("----------------------------------"));

            return count > 0 ? 1 : 0;
        }));
    }

    private static void slash_tracer(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        String[] aliases = {"tracer", "tracker"};

        for (String alias : aliases) {
            dispatcher.register(ClientCommands.literal(alias)
                    .executes(context -> {
                        context.getSource().sendFeedback(Component.literal("§eUsage: /" + alias + " <on|off|name>"));
                        return 0;
                    })
                    .then(ClientCommands.argument("target", StringArgumentType.greedyString())
                            .executes(context -> {
                                String target = StringArgumentType.getString(context, "target").trim();
                                if (target.equalsIgnoreCase("on")) {
                                    PlayerTracer.trackAll();
                                    context.getSource().sendFeedback(Component.literal("§aTracking all players."));
                                    return 1;
                                }

                                if (target.equalsIgnoreCase("off")) {
                                    PlayerTracer.stop();
                                    context.getSource().sendFeedback(Component.literal("§cPlayer tracer disabled."));
                                    return 1;
                                }

                                PlayerTracer.trackName(target);
                                context.getSource().sendFeedback(Component.literal("§aTracking players containing: §f" + target));
                                return 1;
                            }))
            );
        }
    }


    private static void slash_giovanniUpdates(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("giovanni-check-update")
                .executes(context -> {
                    UPDATE_MANAGER.runUpdateFlow();
                    return 1;
                })
        );
    }

    private static void slash_ratterscannertestsha256(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("ratterscannertestsha256")
                .then(ClientCommands.argument("hash", StringArgumentType.string())
                        .executes(ClientCustomCommands::execute)
                )
        );
    }

    private static int execute(CommandContext<FabricClientCommandSource> context) {
        String hash = StringArgumentType.getString(context, "hash");

        // Validate SHA256 format (64 hex characters)
        if (!hash.matches("[a-fA-F0-9]{64}")) {
            context.getSource().sendFeedback(
                    Component.literal("§cInvalid SHA256 hash format. Expected 64 hexadecimal characters.")
            );
            return 0;
        }

        context.getSource().sendFeedback(
                Component.literal("§7Checking hash with RatterScanner...")
        );

        // Perform async check
        RatterScannerChecker.checkHash(hash)
                .thenAccept(status -> {
                    // Run on render thread for UI
                    Minecraft.getInstance().execute(() -> {
                        String message = switch (status) {
                            case VERIFIED_SAFE -> "§a✓ VERIFIED SAFE";
                            case MALICIOUS -> "§c✗ MALICIOUS / UNSAFE";
                            case UNCHECKED -> "§e⚠ UNCHECKED / PENDING";
                            case ERROR -> "§c✗ API ERROR";
                            case OFF -> "§c✗ RAT CHECK TURNED OFF";
                        };

                        context.getSource().sendFeedback(
                                Component.literal("§7Result: " + message)
                        );
                    });
                })
                .exceptionally(ex -> {
                    Minecraft.getInstance().execute(() -> {
                        context.getSource().sendFeedback(
                                Component.literal("§c✗ Exception: " + ex.getMessage())
                        );
                    });
                    return null;
                });

        return 1; // Success
    }

}

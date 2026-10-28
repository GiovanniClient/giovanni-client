package rocket.giovanniclient.client.bootstrap;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import rocket.giovanniclient.client.features.pokedex.DexStore;
import rocket.giovanniclient.client.features.pokedex.DexProfile;
import rocket.giovanniclient.client.features.pokedex.DexScan;
import rocket.giovanniclient.client.features.pokedex.MobPokedex;

import java.io.IOException;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static com.mojang.brigadier.arguments.DoubleArgumentType.*;

final class PokedexCommands {
    private PokedexCommands() {}

    static LiteralArgumentBuilder<FabricClientCommandSource> create(String alias) {
        return ClientCommands.literal(alias).executes(PokedexCommands::help)
                .then(ClientCommands.literal("help").executes(PokedexCommands::help))
                .then(ClientCommands.literal("list").executes(context -> run(context, () -> {
            String island = requireIsland();
            feedback(context, "§ePokédex — " + island + " (" + MobPokedex.entries().size() + " mobs)");
            for (DexStore.Entry entry : MobPokedex.entries()) {
                feedback(context, "§f • " + entry.name() + " §7— " + entry.variants().size() + " variants, "
                        + MobPokedex.mobs(entry.name()).size() + " currently recognised");
            }
            feedback(context, "§7" + MobPokedex.file());
            return 1;
        }))).then(ClientCommands.literal("scan").executes(context -> scan(context, DexScan.DEFAULT_RADIUS))
                .then(ClientCommands.argument("radius", doubleArg(1, DexScan.MAX_RADIUS))
                        .executes(context -> scan(context, getDouble(context, "radius")))))
                .then(ClientCommands.literal("add").executes(context -> usage(context, "/gio dex add <name>"))
                .then(ClientCommands.argument("name", greedyString()).executes(context -> run(context,
                        () -> save(context, getString(context, "name"), MobPokedex.captureNearby())))))
                .then(ClientCommands.literal("capture").executes(PokedexCommands::captureUsage)
                        .then(ClientCommands.argument("name", string())
                                .executes(PokedexCommands::captureUsage)
                                .then(ClientCommands.argument("ids", greedyString()).executes(context -> run(context,
                                        () -> save(context, getString(context, "name"), MobPokedex.captureIds(getString(context, "ids"))))))))
                .then(ClientCommands.literal("variant").executes(PokedexCommands::variantUsage)
                        .then(ClientCommands.argument("name", string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        MobPokedex.entries().stream().map(DexStore.Entry::name).map(name -> escapeIfRequired(name)), builder))
                                .executes(context -> run(context,
                                        () -> save(context, getString(context, "name"), MobPokedex.captureNearby(), true)))
                                .then(ClientCommands.argument("ids", greedyString()).executes(context -> run(context,
                                        () -> save(context, getString(context, "name"), MobPokedex.captureIds(getString(context, "ids")), true))))))
                .then(ClientCommands.literal("info").executes(context -> usage(context, "/gio dex info <name>"))
                        .then(ClientCommands.argument("name", greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        MobPokedex.entries().stream().map(DexStore.Entry::name), builder))
                                .executes(context -> run(context, () -> {
                                    requireIsland();
                                    String name = getString(context, "name");
                                    DexStore.Entry entry = MobPokedex.entries().stream()
                                            .filter(candidate -> candidate.name().equalsIgnoreCase(name.trim())).findFirst()
                                            .orElseThrow(() -> new IllegalArgumentException("Mob not in this island's Pokédex: " + name));
                                    feedback(context, "§e" + entry.name() + " — " + entry.variants().size() + " variants");
                                    for (int i = 0; i < entry.variants().size(); i++) {
                                        feedback(context, "§7Variant " + (i + 1) + ":");
                                        for (var part : entry.variants().get(i).parts()) {
                                            feedback(context, "§f • " + part.role() + " " + part.signature().type()
                                                    + " \"" + part.signature().name() + "\" " + part.signature().equipment()
                                                    + " " + part.signature().appearance());
                                        }
                                    }
                                    return 1;
                                }))))
                .then(ClientCommands.literal("remove").executes(context -> usage(context, "/gio dex remove <name>"))
                        .then(ClientCommands.argument("name", greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        MobPokedex.entries().stream().map(DexStore.Entry::name), builder))
                                .executes(context -> run(context, () -> {
                                    boolean removed = MobPokedex.remove(getString(context, "name"));
                                    feedback(context, removed ? "§aMob removed from this island's Pokédex." : "§eMob not found in this island's Pokédex.");
                                    return removed ? 1 : 0;
                                }))))
                .then(ClientCommands.literal("reload").executes(context -> run(context, () -> {
                    MobPokedex.reload();
                    feedback(context, "§aReloaded Pokédex for " + MobPokedex.island() + ".");
                    return 1;
                })));
    }

    private static int help(CommandContext<FabricClientCommandSource> context) {
        feedback(context, "§ePokédex — /gio dex or /gio pokedex");
        feedback(context, "§e/gio dex scan [radius] §7— save loaded parts within the radius (default 7, maximum 128); * marks the proposed capture.");
        feedback(context, "§e/gio dex add <name> §7— automatically capture the targeted or nearby mob.");
        feedback(context, "§e/gio dex capture \"name\" <ID...> §7— capture only the IDs selected from scan.");
        feedback(context, "§e/gio dex variant \"name\" [ID...] §7— add a variant to an existing mob; omit IDs to select parts automatically.");
        feedback(context, "§e/gio dex list §7— list mobs for the current island.");
        feedback(context, "§e/gio dex info <name> §7— show saved signatures and variants.");
        feedback(context, "§e/gio dex remove <name> §7— remove the mob and all its variants from this island.");
        feedback(context, "§e/gio dex reload §7— reload the current island's file.");
        feedback(context, "§7Repeat add with the same name to add a variant. Then: §e/gio glow <name> §7or §e/gio hide <name>§7.");
        feedback(context, "§7capture and variant with IDs use the latest scan even after movement or despawn. A new scan replaces it; changing island/world clears it.");
        return 1;
    }

    private static int captureUsage(CommandContext<FabricClientCommandSource> context) {
        usage(context, "/gio dex capture \"name\" <IDs from scan...> — example: /gio dex capture Pig 171357");
        feedback(context, "§7For automatic selection use §e/gio dex add Pig§7. capture requires IDs.");
        return 0;
    }

    private static int scan(CommandContext<FabricClientCommandSource> context, double radius) {
        return run(context, () -> {
            DexScan scan = MobPokedex.scan(radius);
            feedback(context, "§eScan saved: " + scan.entities().size() + " parts within " + radius + " blocks — " + scan.island()
                    + " (* = proposed capture):");
            for (DexScan.Part part : scan.entities()) {
                feedback(context, (scan.proposedIds().contains(part.id()) ? "§a* " : "§7  ") + DexScan.describe(part));
            }
            feedback(context, "§7Capture from the saved snapshot: /gio dex capture \"Mob name\" 123 124 125");
            feedback(context, "§7You can use the IDs after movement/despawn. The next scan replaces this snapshot.");
            return 1;
        });
    }

    private static int usage(CommandContext<FabricClientCommandSource> context, String syntax) {
        context.getSource().sendError(Component.literal("Usage: " + syntax));
        return 0;
    }

    private static int variantUsage(CommandContext<FabricClientCommandSource> context) {
        usage(context, "/gio dex variant \"existing name\" [IDs from scan...]");
        feedback(context, "§7Examples: §e/gio dex variant Goblin §7or §e/gio dex variant Goblin 123 124§7. Use quotes for names with spaces.");
        return 0;
    }

    private static int save(CommandContext<FabricClientCommandSource> context, String name, DexScan.Capture capture) throws IOException {
        return save(context, name, capture, false);
    }

    private static int save(CommandContext<FabricClientCommandSource> context, String name, DexScan.Capture capture, boolean variant) throws IOException {
        boolean added = variant ? MobPokedex.addVariant(name, capture) : MobPokedex.add(name, capture);
        int variants = MobPokedex.entries().stream().filter(entry -> DexProfile.key(entry.name()).equals(DexProfile.key(name)))
                .mapToInt(entry -> entry.variants().size()).findFirst().orElse(0);
        feedback(context, added ? "§a" + (variant ? "Added variant to " : "Saved ") + name.trim() + " in " + MobPokedex.island()
                + ": " + capture.entities().size() + " parts, " + variants + " total variants."
                : "§eThis variant is already registered.");
        for (DexScan.Part part : capture.entities()) feedback(context, "§7 • " + DexScan.describe(part));
        if (capture.profile().typeOnly()) {
            feedback(context, "§eType-only signature: matches all identical " + capture.profile().parts().getFirst().signature().type()
                    + " entities in this island.");
        }
        feedback(context, "§7Use /gio glow " + name.trim() + " or /gio hide " + name.trim() + ".");
        return 1;
    }

    private static String requireIsland() {
        String island = MobPokedex.island();
        if (island == null) throw new IllegalStateException("Island unknown. Wait for the SkyBlock TAB Area to appear.");
        if (MobPokedex.error() != null) throw new IllegalStateException(MobPokedex.error() + ". Fix the file and /gio dex reload.");
        return island;
    }

    private static int run(CommandContext<FabricClientCommandSource> context, Action action) {
        try {
            return action.run();
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            context.getSource().sendError(Component.literal("Pokédex: " + exception.getMessage()));
            return 0;
        }
    }

    private static void feedback(CommandContext<FabricClientCommandSource> context, String message) {
        context.getSource().sendFeedback(Component.literal(message));
    }

    @FunctionalInterface
    private interface Action { int run() throws IOException; }
}

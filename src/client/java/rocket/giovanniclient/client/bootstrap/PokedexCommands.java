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
                .then(ClientCommands.literal("add").executes(context -> usage(context, "/gio dex add <nome>"))
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
                .then(ClientCommands.literal("info").executes(context -> usage(context, "/gio dex info <nome>"))
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
                .then(ClientCommands.literal("remove").executes(context -> usage(context, "/gio dex remove <nome>"))
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
        feedback(context, "§ePokédex — /gio dex oppure /gio pokedex");
        feedback(context, "§e/gio dex scan [raggio] §7— salva una copia delle parti caricate entro il raggio (default 7, massimo 128); * indica la cattura proposta.");
        feedback(context, "§e/gio dex add <nome> §7— cattura automaticamente il mob puntato o vicino.");
        feedback(context, "§e/gio dex capture \"nome\" <ID...> §7— cattura solo gli ID scelti da scan.");
        feedback(context, "§e/gio dex variant \"nome\" [ID...] §7— aggiunge una variante a un mob esistente; senza ID sceglie le parti automaticamente.");
        feedback(context, "§e/gio dex list §7— elenco dei mob dell'isola attuale.");
        feedback(context, "§e/gio dex info <nome> §7— mostra firme e varianti salvate.");
        feedback(context, "§e/gio dex remove <nome> §7— elimina il mob e tutte le sue varianti da quest'isola.");
        feedback(context, "§e/gio dex reload §7— rilegge il file dell'isola attuale.");
        feedback(context, "§7Ripeti add con lo stesso nome per aggiungere una variante. Poi: §e/gio glow <nome> §7o §e/gio hide <nome>§7.");
        feedback(context, "§7capture e variant con ID usano l'ultima scan anche dopo movimento o despawn. Una nuova scan sostituisce la precedente; cambio isola/mondo la cancella.");
        return 1;
    }

    private static int captureUsage(CommandContext<FabricClientCommandSource> context) {
        usage(context, "/gio dex capture \"nome\" <ID da scan...> — esempio: /gio dex capture Pig 171357");
        feedback(context, "§7Per la scelta automatica usa §e/gio dex add Pig§7. capture richiede gli ID.");
        return 0;
    }

    private static int scan(CommandContext<FabricClientCommandSource> context, double radius) {
        return run(context, () -> {
            DexScan scan = MobPokedex.scan(radius);
            feedback(context, "§eScan salvata: " + scan.entities().size() + " parti entro " + radius + " blocchi — " + scan.island()
                    + " (* = cattura proposta):");
            for (DexScan.Part part : scan.entities()) {
                feedback(context, (scan.proposedIds().contains(part.id()) ? "§a* " : "§7  ") + DexScan.describe(part));
            }
            feedback(context, "§7Cattura dalla copia salvata: /gio dex capture \"Nome mob\" 123 124 125");
            feedback(context, "§7Puoi usare gli ID anche dopo movimento/despawn. La prossima scan sostituisce questa copia.");
            return 1;
        });
    }

    private static int usage(CommandContext<FabricClientCommandSource> context, String syntax) {
        context.getSource().sendError(Component.literal("Sintassi: " + syntax));
        return 0;
    }

    private static int variantUsage(CommandContext<FabricClientCommandSource> context) {
        usage(context, "/gio dex variant \"nome esistente\" [ID da scan...]");
        feedback(context, "§7Esempi: §e/gio dex variant Goblin §7oppure §e/gio dex variant Goblin 123 124§7. Usa le virgolette per nomi con spazi.");
        return 0;
    }

    private static int save(CommandContext<FabricClientCommandSource> context, String name, DexScan.Capture capture) throws IOException {
        return save(context, name, capture, false);
    }

    private static int save(CommandContext<FabricClientCommandSource> context, String name, DexScan.Capture capture, boolean variant) throws IOException {
        boolean added = variant ? MobPokedex.addVariant(name, capture) : MobPokedex.add(name, capture);
        int variants = MobPokedex.entries().stream().filter(entry -> DexProfile.key(entry.name()).equals(DexProfile.key(name)))
                .mapToInt(entry -> entry.variants().size()).findFirst().orElse(0);
        feedback(context, added ? "§a" + (variant ? "Aggiunta variante a " : "Salvato ") + name.trim() + " in " + MobPokedex.island()
                + ": " + capture.entities().size() + " parti, " + variants + " varianti totali."
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

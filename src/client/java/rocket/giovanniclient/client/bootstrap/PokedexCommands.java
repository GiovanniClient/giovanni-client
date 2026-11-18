package rocket.giovanniclient.client.bootstrap;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import rocket.giovanniclient.client.features.pokedex.DexCatalog;
import rocket.giovanniclient.client.features.pokedex.DexProfile;
import rocket.giovanniclient.client.features.pokedex.DexScan;
import rocket.giovanniclient.client.features.pokedex.DexStore;
import rocket.giovanniclient.client.features.pokedex.MobPokedex;
import rocket.giovanniclient.client.features.render.GlowOverrideManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.StringArgumentType.*;

final class PokedexCommands {
    private static final String CAPTURE_OPTIONS = " [--health] [--size]";

    private PokedexCommands() {}

    static LiteralArgumentBuilder<FabricClientCommandSource> create(String alias) {
        var root = ClientCommands.literal(alias).executes(PokedexCommands::help)
                .then(ClientCommands.literal("help").executes(PokedexCommands::help))
                .then(ClientCommands.literal("scan").executes(context -> scan(context, DexScan.DEFAULT_RADIUS))
                        .then(ClientCommands.argument("radius", doubleArg(1, DexScan.MAX_RADIUS))
                                .executes(context -> scan(context, getDouble(context, "radius")))))
                .then(ClientCommands.literal("reload").executes(context -> run(context, () -> {
                    MobPokedex.reload();
                    boolean error = false;
                    for (DexCatalog catalog : activeCatalogs()) error |= reportCatalogError(context, catalog);
                    if (error) return 0;
                    feedback(context, "§aReloaded active Pokédex catalogs for " + MobPokedex.catalogName(DexCatalog.CURRENT) + ".");
                    return 1;
                })));
        for (DexCatalog catalog : DexCatalog.sharedCatalogs()) {
            root.then(catalogCommands(ClientCommands.literal(catalog.commandName())
                    .executes(context -> sharedHelp(context, catalog))
                    .then(ClientCommands.literal("help").executes(context -> sharedHelp(context, catalog))), catalog));
        }
        return catalogCommands(root, DexCatalog.CURRENT);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> catalogCommands(
            LiteralArgumentBuilder<FabricClientCommandSource> root, DexCatalog target) {
        String prefix = prefix(target);
        return root.then(ClientCommands.literal("list").executes(context -> run(context, () -> list(context, target))))
                .then(ClientCommands.literal("add").executes(context -> usage(context, prefix + " add <name>"))
                        .then(ClientCommands.argument("name", greedyString()).executes(context -> run(context,
                                () -> save(context, getString(context, "name"), MobPokedex.captureNearby(), target, false)))))
                .then(ClientCommands.literal("capture").executes(context -> captureUsage(context, target))
                        .then(ClientCommands.argument("name", string())
                                .executes(context -> captureUsage(context, target))
                                .then(ClientCommands.argument("ids", greedyString()).executes(context -> run(context,
                                        () -> save(context, getString(context, "name"), MobPokedex.captureIds(getString(context, "ids")), target, false))))))
                .then(ClientCommands.literal("variant").executes(context -> variantUsage(context, target))
                        .then(ClientCommands.argument("name", string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        MobPokedex.entries(target).stream().map(DexStore.Entry::name).map(name -> escapeIfRequired(name)), builder))
                                .executes(context -> run(context,
                                        () -> save(context, getString(context, "name"), MobPokedex.captureNearby(), target, true)))
                                .then(ClientCommands.argument("ids", greedyString()).executes(context -> run(context,
                                        () -> save(context, getString(context, "name"), MobPokedex.captureIds(getString(context, "ids")), target, true))))))
                .then(ClientCommands.literal("info").executes(context -> usage(context, prefix + " info <name>"))
                        .then(ClientCommands.argument("name", greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        visibleEntries(target).stream().map(DexStore.Entry::name), builder))
                                .executes(context -> run(context, () -> info(context, getString(context, "name"), target)))))
                .then(ClientCommands.literal("remove").executes(context -> usage(context, prefix + " remove <name>"))
                        .then(ClientCommands.argument("name", greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        MobPokedex.entries(target).stream().map(DexStore.Entry::name), builder))
                                .executes(context -> run(context, () -> remove(context, getString(context, "name"), target)))));
    }

    private static List<DexStore.Entry> visibleEntries(DexCatalog target) {
        return target == DexCatalog.CURRENT ? MobPokedex.entries() : MobPokedex.entries(target);
    }

    private static int list(CommandContext<FabricClientCommandSource> context, DexCatalog target) {
        requireCatalog(target);
        List<DexStore.Entry> entries = visibleEntries(target);
        boolean combined = target == DexCatalog.CURRENT;
        List<DexCatalog> catalogs = combined ? activeCatalogs() : List.of(target);
        var sources = catalogs.stream().map(scope -> new CatalogSummary(MobPokedex.catalogName(scope),
                entryNames(MobPokedex.entries(scope)))).toList();
        String title = sources.stream().map(CatalogSummary::name).collect(Collectors.joining(" + "));
        feedback(context, "§ePokédex — " + title + " (" + entries.size() + " mobs)");
        for (DexStore.Entry entry : entries) {
            String key = DexProfile.key(entry.name());
            String source = sources.size() <= 1 ? "" : sources.stream().filter(scope -> scope.entryNames().contains(key))
                    .map(CatalogSummary::name).collect(Collectors.joining(" + ", " [", "]"));
            feedback(context, "§f • " + entry.name() + " §7— " + entry.variants().size() + " variants"
                    + (combined ? ", " + MobPokedex.mobs(entry.name()).size() + " currently recognised" : "") + source);
        }
        for (DexCatalog scope : catalogs) reportCatalogError(context, scope);
        if (catalogs.size() > 1) {
            feedback(context, "§7Use /gio dex " + sharedCommandChoices() + " list to inspect a shared catalog.");
        }
        feedback(context, "§7Personal file: " + MobPokedex.file(target));
        return 1;
    }

    private record CatalogSummary(String name, Set<String> entryNames) {}

    private static List<DexCatalog> activeCatalogs() {
        List<DexCatalog> catalogs = new ArrayList<>();
        catalogs.add(DexCatalog.CURRENT);
        for (DexCatalog catalog : DexCatalog.sharedCatalogs()) {
            if (MobPokedex.catalogName(catalog) != null) catalogs.add(catalog);
        }
        return catalogs;
    }

    private static Set<String> entryNames(List<DexStore.Entry> entries) {
        return entries.stream().map(entry -> DexProfile.key(entry.name())).collect(Collectors.toSet());
    }

    private static boolean reportCatalogError(CommandContext<FabricClientCommandSource> context, DexCatalog target) {
        String error = MobPokedex.error(target);
        if (error == null) return false;
        context.getSource().sendError(Component.literal(error + ". Fix the file and use /gio dex reload."));
        return true;
    }

    private static int info(CommandContext<FabricClientCommandSource> context, String name, DexCatalog target) {
        requireCatalog(target);
        String key = DexProfile.key(name);
        DexStore.Entry entry = visibleEntries(target).stream()
                .filter(candidate -> DexProfile.key(candidate.name()).equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mob not in the "
                        + (target == DexCatalog.CURRENT ? "active" : target.displayName()) + " Pokédex: " + name));
        feedback(context, "§e" + entry.name() + " — " + entry.variants().size() + " variants");
        for (int i = 0; i < entry.variants().size(); i++) {
            feedback(context, "§7Variant " + (i + 1) + ":");
            for (var part : entry.variants().get(i).parts()) {
                feedback(context, "§f • " + part.role() + " " + part.signature().type()
                        + " \"" + part.signature().name() + "\" " + part.signature().equipment()
                        + " " + part.signature().appearance() + discriminants(part.signature().baseMaxHealth(), part.signature().size()));
            }
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            var player = client.player;
            var mobs = MobPokedex.mobs(entry.name());
            feedback(context, "§7Live matches: " + mobs.size() + (mobs.size() > 12 ? " (12 nearest shown)" : ""));
            mobs.stream().map(mob -> mob.parts().stream()
                            .filter(part -> part.role() != DexProfile.Role.LABEL).findFirst().orElse(mob.parts().getFirst()).entity())
                    .sorted(Comparator.comparingDouble(player::distanceToSqr)).limit(12).forEach(entity -> {
                        double distance = Math.sqrt(player.distanceToSqr(entity));
                        feedback(context, String.format(Locale.ROOT,
                                "§7 • #%d at %.1f %.1f %.1f (%.1f blocks), Giovanni glow: %s, Minecraft glow: %s",
                                entity.getId(), entity.getX(), entity.getY(), entity.getZ(), distance,
                                GlowOverrideManager.has(entity), client.shouldEntityAppearGlowing(entity)));
                    });
        }
        return 1;
    }

    private static int remove(CommandContext<FabricClientCommandSource> context, String name, DexCatalog target) throws IOException {
        String catalog = requireCatalog(target);
        return switch (MobPokedex.remove(name, target)) {
            case REMOVED -> {
                feedback(context, "§aRemoved personal captures of " + name + " from " + catalog + ".");
                yield 1;
            }
            case REMOVED_BUNDLED_REMAINS -> {
                feedback(context, "§aRemoved personal captures of " + name + " from " + catalog + ". The bundled entry remains available.");
                yield 1;
            }
            case BUNDLED_ONLY -> {
                feedback(context, "§e" + name + " is bundled in " + catalog + " and has no personal captures to remove.");
                yield 0;
            }
            case NOT_FOUND -> {
                feedback(context, "§eMob not found in " + catalog + "."
                        + (target == DexCatalog.CURRENT ? " Use /gio dex " + sharedCommandChoices() + " remove for a shared capture." : ""));
                yield 0;
            }
        };
    }

    private static int help(CommandContext<FabricClientCommandSource> context) {
        feedback(context, "§ePokédex — /gio dex or /gio pokedex");
        feedback(context, "§e/gio dex scan [radius] §7— save loaded parts within the radius (default 7, maximum 128); * marks the proposed capture.");
        feedback(context, "§e/gio dex add <name> §7— automatically capture the targeted or nearby mob.");
        feedback(context, "§e/gio dex capture \"name\" <IDs...>" + CAPTURE_OPTIONS + " §7— capture selected scan IDs.");
        feedback(context, "§e/gio dex variant \"name\" [IDs..." + CAPTURE_OPTIONS + "] §7— add a variant. Omit IDs for automatic selection.");
        feedback(context, "§7--health matches base maximum health. --size matches standing hitbox width and height.");
        feedback(context, "§e/gio dex list §7— list active mobs for the current island or Catacombs, including any shared catalogs.");
        feedback(context, "§e/gio dex info <name> §7— show saved signatures and variants.");
        feedback(context, "§e/gio dex remove <name> §7— remove personal captures from the current island or Catacombs catalog.");
        feedback(context, "§e/gio dex " + sharedCommandChoices() + " §7— manage captures shared across islands.");
        feedback(context, "§e/gio dex reload §7— reload the active catalogs.");
        feedback(context, "§7Repeat add with the same name to add a variant. Then: §e/gio glow <name> §7or §e/gio hide <name>§7.");
        feedback(context, "§7capture and variant with IDs use the latest scan even after movement or despawn. A new scan replaces it; changing area/world clears it.");
        return 1;
    }

    private static int sharedHelp(CommandContext<FabricClientCommandSource> context, DexCatalog target) {
        String prefix = "§e" + prefix(target);
        feedback(context, "§ePokédex — shared " + target.displayName() + " catalog");
        feedback(context, prefix + " add <name> §7— capture a nearby mob for this catalog.");
        feedback(context, prefix + " capture \"name\" <IDs...>" + CAPTURE_OPTIONS + " §7— use parts from the latest scan.");
        feedback(context, prefix + " variant \"name\" [IDs..." + CAPTURE_OPTIONS + "] §7— add a variant to an entry in this catalog.");
        feedback(context, "§7--health matches base maximum health. --size matches standing hitbox width and height.");
        feedback(context, prefix + " list §7— list the shared catalog.");
        feedback(context, prefix + " info <name> §7— inspect an entry in this catalog.");
        feedback(context, prefix + " remove <name> §7— remove personal captures from this catalog.");
        return 1;
    }

    private static int captureUsage(CommandContext<FabricClientCommandSource> context, DexCatalog target) {
        String prefix = prefix(target);
        String sample = exampleName(target);
        usage(context, prefix + " capture \"name\" <IDs from scan...>" + CAPTURE_OPTIONS
                + " — example: " + prefix + " capture " + escapeIfRequired(sample) + " 171357");
        feedback(context, "§7For automatic selection use §e" + prefix + " add " + sample + "§7. capture requires IDs.");
        return 0;
    }

    private static int scan(CommandContext<FabricClientCommandSource> context, double radius) {
        return run(context, () -> {
            DexScan scan = MobPokedex.scan(radius);
            feedback(context, "§eScan saved: " + scan.entities().size() + " parts within " + radius + " blocks — " + MobPokedex.catalogName(DexCatalog.CURRENT)
                    + " (* = proposed capture):");
            for (DexScan.Part part : scan.entities()) {
                feedback(context, (scan.proposedIds().contains(part.id()) ? "§a* " : "§7  ") + DexScan.describe(part)
                        + discriminants(part.baseMaxHealth(), part.size()));
            }
            feedback(context, "§7Capture from the saved snapshot: /gio dex capture \"Mob name\" <IDs...>" + CAPTURE_OPTIONS);
            if (activeCatalogs().size() > 1) {
                feedback(context, "§7For a shared mob: /gio dex " + sharedCommandChoices() + " capture \"Mob name\" 123 124");
            }
            feedback(context, "§7You can use the IDs after movement/despawn. The next scan replaces this snapshot.");
            return 1;
        });
    }

    private static int usage(CommandContext<FabricClientCommandSource> context, String syntax) {
        context.getSource().sendError(Component.literal("Usage: " + syntax));
        return 0;
    }

    private static int variantUsage(CommandContext<FabricClientCommandSource> context, DexCatalog target) {
        String prefix = prefix(target);
        String sample = escapeIfRequired(exampleName(target));
        usage(context, prefix + " variant \"existing name\" [IDs from scan..." + CAPTURE_OPTIONS + "]");
        feedback(context, "§7Examples: §e" + prefix + " variant " + sample + " §7or §e" + prefix + " variant " + sample + " 123 124§7. Use quotes for names with spaces.");
        return 0;
    }

    private static int save(CommandContext<FabricClientCommandSource> context, String name, DexScan.Capture capture,
                            DexCatalog target, boolean variant) throws IOException {
        String catalog = requireCatalog(target);
        boolean added = variant ? MobPokedex.addVariant(name, capture, target) : MobPokedex.add(name, capture, target);
        int variants = MobPokedex.entries(target).stream().filter(entry -> DexProfile.key(entry.name()).equals(DexProfile.key(name)))
                .mapToInt(entry -> entry.variants().size()).findFirst().orElse(0);
        feedback(context, added ? "§a" + (variant ? "Added variant to " : "Saved ") + name.trim() + " in " + catalog
                + ": " + capture.entities().size() + " parts, " + variants + " total variants."
                : "§eThis variant is already registered in " + catalog + ".");
        for (int i = 0; i < capture.entities().size(); i++) {
            DexProfile.Signature signature = capture.profile().parts().get(i).signature();
            feedback(context, "§7 • " + DexScan.describe(capture.entities().get(i))
                    + discriminants(signature.baseMaxHealth(), signature.size()));
        }
        if (capture.profile().typeOnly()) {
            feedback(context, "§eType-only signature: matches all identical " + capture.profile().parts().getFirst().signature().type()
                    + (target == DexCatalog.CURRENT ? " entities in this area." : " entities on every island where " + target.displayName() + " is active."));
        }
        feedback(context, "§7Use /gio glow " + name.trim() + " or /gio hide " + name.trim() + ".");
        return 1;
    }

    private static String discriminants(Double health, DexProfile.Size size) {
        return (health == null ? "" : " (base max HP " + health + ")")
                + (size == null ? "" : " (size " + size.width() + " × " + size.height() + " blocks)");
    }

    private static String requireCatalog(DexCatalog target) {
        String catalog = MobPokedex.catalogName(target);
        if (catalog == null) {
            throw new IllegalStateException(target == DexCatalog.CURRENT
                    ? "Area unknown. Wait for SkyBlock's TAB area or Catacombs scoreboard location."
                    : target.displayName() + " is available on recognised SkyBlock islands outside Catacombs.");
        }
        return catalog;
    }

    private static String prefix(DexCatalog target) {
        return target == DexCatalog.CURRENT ? "/gio dex" : "/gio dex " + target.commandName();
    }

    private static String sharedCommandChoices() {
        return DexCatalog.sharedCatalogs().stream().map(DexCatalog::commandName).collect(Collectors.joining("|", "<", ">"));
    }

    private static String exampleName(DexCatalog target) {
        return switch (target) {
            case CURRENT -> "Pig";
            case FISHING -> "Sea Walker";
            case JERRY -> "Jerry";
            case SPOOKY -> "Spooky Mob";
        };
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

package rocket.giovanniclient.client.features.pokedex;

import rocket.giovanniclient.client.util.PlayerLocator;

import java.util.List;
import java.util.Locale;

/** Explicit capture destinations, in recognition priority order. */
public enum DexCatalog {
    CURRENT("Current area"),
    FISHING("Fishing"),
    JERRY("Jerry"),
    SPOOKY("Spooky");

    private static final List<DexCatalog> ISLAND_CATALOGS = List.of(values());
    private static final List<DexCatalog> SHARED = ISLAND_CATALOGS.subList(1, ISLAND_CATALOGS.size());
    private final String displayName;

    DexCatalog(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    public String commandName() { return name().toLowerCase(Locale.ROOT); }

    public static List<DexCatalog> sharedCatalogs() { return SHARED; }

    static List<DexCatalog> activeFor(String context) {
        if (context == null) return List.of();
        return PlayerLocator.isCatacombsPokedexArea(context) ? List.of(CURRENT) : ISLAND_CATALOGS;
    }

    String id(String context) {
        if (this != CURRENT) return "scope:" + commandName();
        return PlayerLocator.isCatacombsPokedexArea(context) ? "scope:catacombs" : context;
    }
}

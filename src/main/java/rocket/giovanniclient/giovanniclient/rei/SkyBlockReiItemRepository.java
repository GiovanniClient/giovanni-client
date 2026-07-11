package rocket.giovanniclient.giovanniclient.rei;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class SkyBlockReiItemRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger("GiovanniClient");
    private static final String OWNER = "NotEnoughUpdates";
    private static final String REPO = "NotEnoughUpdates-REPO";
    private static final String BRANCH = "master";
    private static final String COMMITS_URL = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/commits/" + BRANCH;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Map<String, String> LEGACY_ITEM_IDS = Map.ofEntries(
            Map.entry("minecraft:skull", "minecraft:player_head"),
            Map.entry("minecraft:stained_glass", "minecraft:white_stained_glass"),
            Map.entry("minecraft:stained_glass_pane", "minecraft:white_stained_glass_pane"),
            Map.entry("minecraft:wool", "minecraft:white_wool"),
            Map.entry("minecraft:log", "minecraft:oak_log"),
            Map.entry("minecraft:log2", "minecraft:acacia_log"),
            Map.entry("minecraft:leaves", "minecraft:oak_leaves"),
            Map.entry("minecraft:leaves2", "minecraft:acacia_leaves"),
            Map.entry("minecraft:stone_slab", "minecraft:smooth_stone_slab"),
            Map.entry("minecraft:wooden_slab", "minecraft:oak_slab"),
            Map.entry("minecraft:monster_egg", "minecraft:stone"),
            Map.entry("minecraft:fireworks", "minecraft:firework_rocket"),
            Map.entry("minecraft:firework_charge", "minecraft:firework_star"),
            Map.entry("minecraft:golden_rail", "minecraft:powered_rail"),
            Map.entry("minecraft:record_13", "minecraft:music_disc_13"),
            Map.entry("minecraft:record_cat", "minecraft:music_disc_cat")
    );

    private static volatile List<SkyBlockReiItem> cachedItems = List.of();
    private static volatile List<SkyBlockReiCraftingDisplay> cachedCraftingRecipes = List.of();
    private static volatile List<SkyBlockReiSimpleRecipeDisplay> cachedSimpleRecipes = List.of();
    private static volatile List<SkyBlockReiMobDropDisplay> cachedMobDrops = List.of();
    private static volatile Map<String, List<String>> cachedParentGroups = Map.of();
    private static volatile Map<String, SkyBlockReiItem> itemsById = Map.of();
    private static volatile boolean loaded;
    private static volatile CompletableFuture<Void> warmup;
    private static final List<Runnable> loadCallbacks = new CopyOnWriteArrayList<>();

    private SkyBlockReiItemRepository() {}

    public static void warmupIfReiIsLoaded() {
        if (!FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) {
            return;
        }

        synchronized (SkyBlockReiItemRepository.class) {
            if (warmup == null) {
                warmup = CompletableFuture.runAsync(SkyBlockReiItemRepository::loadSafely);
            }
        }
    }

    public static List<SkyBlockReiItem> getItemsForRei() {
        if (!loaded) {
            warmupIfReiIsLoaded();
        }
        return cachedItems;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static List<SkyBlockReiCraftingDisplay> getCraftingRecipesForRei() {
        if (!loaded) {
            warmupIfReiIsLoaded();
        }
        return cachedCraftingRecipes;
    }

    public static List<SkyBlockReiSimpleRecipeDisplay> getSimpleRecipesForRei() {
        if (!loaded) {
            warmupIfReiIsLoaded();
        }
        return cachedSimpleRecipes;
    }

    public static List<SkyBlockReiMobDropDisplay> getMobDropsForRei() {
        if (!loaded) {
            warmupIfReiIsLoaded();
        }
        return cachedMobDrops;
    }

    public static Map<String, List<String>> getParentGroupsForRei() {
        if (!loaded) {
            warmupIfReiIsLoaded();
        }
        return cachedParentGroups;
    }

    public static void whenLoaded(Runnable callback) {
        if (loaded) {
            callback.run();
            return;
        }

        loadCallbacks.add(callback);
        if (loaded && loadCallbacks.remove(callback)) {
            callback.run();
        }
    }

    public static SkyBlockReiItem getItemById(String id) {
        if (!loaded) {
            getItemsForRei();
        }
        SkyBlockReiItem item = itemsById.get(id);
        if (item != null) {
            return item.copy();
        }
        if ("SKYBLOCK_COIN".equals(id)) {
            return new SkyBlockReiItem(id, "minecraft:gold_nugget", "Coins", List.of(), null);
        }
        return new SkyBlockReiItem(id, "minecraft:paper", id, List.of(), null);
    }

    private static synchronized void loadSafely() {
        if (loaded) {
            return;
        }

        try {
            Path repoDir = repoDirectory();
            updateRepoIfNeeded(repoDir);
            List<SkyBlockReiItem> items = loadItems(repoDir.resolve("items"));
            Map<String, SkyBlockReiItem> byId = new HashMap<>();
            for (SkyBlockReiItem item : items) {
                byId.put(item.id(), item);
            }
            itemsById = Map.copyOf(byId);
            cachedItems = items;
            LoadedRecipes recipes = loadRecipes(repoDir.resolve("items"));
            cachedCraftingRecipes = recipes.crafting();
            cachedSimpleRecipes = recipes.simple();
            cachedMobDrops = recipes.mobDrops();
            cachedParentGroups = loadParentGroups(repoDir.resolve("constants").resolve("parents.json"));
            cachedSimpleRecipes = appendEssenceRecipes(cachedSimpleRecipes, repoDir.resolve("constants").resolve("essencecosts.json"));
            cachedSimpleRecipes = appendReforgeStoneRecipes(cachedSimpleRecipes, repoDir.resolve("constants").resolve("reforgestones.json"));
            loaded = true;
            LOGGER.info("Loaded {} SkyBlock items, {} crafting recipes, {} extra recipes, {} mob drop recipes, and {} item groups for REI from NEU repo.",
                    cachedItems.size(), cachedCraftingRecipes.size(), cachedSimpleRecipes.size(), cachedMobDrops.size(), cachedParentGroups.size());
            runLoadCallbacks();
        } catch (Exception e) {
            loaded = true;
            LOGGER.error("Failed to load SkyBlock items for REI.", e);
            runLoadCallbacks();
        }
    }

    private static void runLoadCallbacks() {
        for (Runnable callback : loadCallbacks) {
            try {
                callback.run();
            } catch (Exception e) {
                LOGGER.warn("Failed to run SkyBlock REI load callback.", e);
            }
        }
        loadCallbacks.clear();
    }

    private static Path dataDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("giovanniclient").resolve("neu-repo");
    }

    private static Path repoDirectory() {
        return dataDirectory().resolve("repo-extracted");
    }

    public static Path getRepoDirectoryForResources() {
        return repoDirectory();
    }

    private static Path shaFile() {
        return dataDirectory().resolve("loaded-repo-sha.txt");
    }

    private static void updateRepoIfNeeded(Path repoDir) throws IOException, InterruptedException {
        Files.createDirectories(dataDirectory());
        if (Files.isDirectory(repoDir.resolve("items"))) {
            return;
        }

        String latestSha;
        try {
            latestSha = requestLatestSha();
        } catch (IOException e) {
            throw e;
        }

        Path archive = downloadArchive(latestSha);
        deleteDirectory(repoDir);
        extractArchive(archive, repoDir);
        Files.writeString(shaFile(), latestSha, StandardCharsets.UTF_8);
        Files.deleteIfExists(archive);
    }

    private static String requestLatestSha() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(COMMITS_URL))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GiovanniClient")
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IOException("GitHub commit request failed with HTTP " + response.statusCode());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.get("sha").getAsString();
    }

    private static String readCurrentSha() throws IOException {
        Path file = shaFile();
        if (!Files.exists(file)) {
            return "";
        }
        return Files.readString(file, StandardCharsets.UTF_8).trim();
    }

    private static Path downloadArchive(String sha) throws IOException, InterruptedException {
        String archiveUrl = "https://github.com/" + OWNER + "/" + REPO + "/archive/" + sha + ".zip";
        HttpRequest request = HttpRequest.newBuilder(URI.create(archiveUrl))
                .header("User-Agent", "GiovanniClient")
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("GitHub archive download failed with HTTP " + response.statusCode());
        }

        Path archive = Files.createTempFile("giovanni-neu-repo", ".zip");
        try (InputStream input = response.body(); OutputStream output = Files.newOutputStream(archive)) {
            input.transferTo(output);
        }
        return archive;
    }

    private static void extractArchive(Path archive, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String relativeName = entry.getName().substring(entry.getName().indexOf('/') + 1);
                if (relativeName.isBlank()) {
                    continue;
                }

                Path destination = targetDir.resolve(relativeName).normalize();
                if (!destination.startsWith(targetDir)) {
                    throw new IOException("Blocked unsafe NEU repo zip entry: " + entry.getName());
                }

                Files.createDirectories(destination.getParent());
                Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static List<SkyBlockReiItem> loadItems(Path itemsDir) throws IOException {
        if (!Files.isDirectory(itemsDir)) {
            return List.of();
        }

        List<SkyBlockReiItem> items = new ArrayList<>();
        try (var paths = Files.list(itemsDir)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                try {
                    SkyBlockReiItem item = loadItem(path);
                    items.add(item);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load NEU item file {}", path.getFileName(), e);
                }
            }
        }
        return List.copyOf(items);
    }

    private static LoadedRecipes loadRecipes(Path itemsDir) throws IOException {
        if (!Files.isDirectory(itemsDir)) {
            return new LoadedRecipes(List.of(), List.of(), List.of());
        }

        List<SkyBlockReiCraftingDisplay> craftingRecipes = new ArrayList<>();
        List<SkyBlockReiSimpleRecipeDisplay> simpleRecipes = new ArrayList<>();
        List<SkyBlockReiMobDropDisplay> mobDrops = new ArrayList<>();
        try (var paths = Files.list(itemsDir)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                    JsonObject itemJson = JsonParser.parseReader(reader).getAsJsonObject();
                    String internalName = string(itemJson, "internalname", path.getFileName().toString().replace(".json", ""));
                    if (itemJson.has("recipe") && itemJson.get("recipe").isJsonObject()) {
                        addRecipe(craftingRecipes, simpleRecipes, mobDrops, internalName, itemJson.getAsJsonObject("recipe"));
                    }
                    if (itemJson.has("recipes") && itemJson.get("recipes").isJsonArray()) {
                        for (JsonElement recipeElement : itemJson.getAsJsonArray("recipes")) {
                            if (recipeElement.isJsonObject()) {
                                addRecipe(craftingRecipes, simpleRecipes, mobDrops, internalName, recipeElement.getAsJsonObject());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to load NEU recipe file {}", path.getFileName(), e);
                }
            }
        }
        return new LoadedRecipes(List.copyOf(craftingRecipes), List.copyOf(simpleRecipes), List.copyOf(mobDrops));
    }

    private static void addRecipe(List<SkyBlockReiCraftingDisplay> craftingRecipes, List<SkyBlockReiSimpleRecipeDisplay> simpleRecipes, List<SkyBlockReiMobDropDisplay> mobDrops, String outputId, JsonObject recipeJson) {
        String type = string(recipeJson, "type", "crafting");
        switch (type) {
            case "crafting" -> addCraftingRecipe(craftingRecipes, outputId, recipeJson);
            case "forge" -> addForgeRecipe(simpleRecipes, outputId, recipeJson);
            case "katgrade" -> addKatRecipe(simpleRecipes, outputId, recipeJson);
            case "npc_shop" -> addShopRecipe(simpleRecipes, recipeJson);
            case "trade" -> addTradeRecipe(simpleRecipes, outputId, recipeJson);
            case "drops" -> addMobDropRecipe(mobDrops, recipeJson);
            default -> {
            }
        }
    }

    private static void addCraftingRecipe(List<SkyBlockReiCraftingDisplay> recipes, String outputId, JsonObject recipeJson) {
        String[] slots = new String[9];
        String[] keys = {"A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3"};
        boolean hasIngredient = false;
        for (int i = 0; i < keys.length; i++) {
            slots[i] = string(recipeJson, keys[i], "");
            hasIngredient |= !slots[i].isBlank();
        }
        if (!hasIngredient) {
            return;
        }

        String resolvedOutput = string(recipeJson, "overrideOutputId", outputId);
        int count = recipeJson.has("count") && recipeJson.get("count").isJsonPrimitive()
                ? Math.max(1, recipeJson.get("count").getAsInt())
                : 1;
        recipes.add(SkyBlockReiCraftingDisplay.of(slots, resolvedOutput, count));
    }

    private static void addForgeRecipe(List<SkyBlockReiSimpleRecipeDisplay> recipes, String outputId, JsonObject recipeJson) {
        List<String> inputs = readIngredientArray(recipeJson, "inputs");
        if (inputs.isEmpty()) {
            return;
        }

        String resolvedOutput = string(recipeJson, "overrideOutputId", outputId);
        int count = intValue(recipeJson, "count", 1);
        List<String> info = new ArrayList<>();
        int duration = intValue(recipeJson, "duration", 0);
        if (duration > 0) {
            info.add("Time: " + formatDuration(duration));
        }
        recipes.add(SkyBlockReiSimpleRecipeDisplay.of(SkyBlockReiSimpleRecipeDisplay.FORGE_CATEGORY, inputs, resolvedOutput, count, info));
    }

    private static void addKatRecipe(List<SkyBlockReiSimpleRecipeDisplay> recipes, String outputId, JsonObject recipeJson) {
        List<String> inputs = new ArrayList<>();
        String input = string(recipeJson, "input", "");
        if (!input.isBlank()) {
            inputs.add(input + ":1");
        }
        inputs.addAll(readIngredientArray(recipeJson, "items"));
        if (inputs.isEmpty()) {
            return;
        }

        String resolvedOutput = string(recipeJson, "output", outputId);
        List<String> info = new ArrayList<>();
        int coins = intValue(recipeJson, "coins", 0);
        if (coins > 0) {
            info.add("Coins: " + String.format(Locale.ROOT, "%,d", coins));
        }
        int time = intValue(recipeJson, "time", 0);
        if (time > 0) {
            info.add("Time: " + formatDuration(time));
        }
        recipes.add(SkyBlockReiSimpleRecipeDisplay.of(SkyBlockReiSimpleRecipeDisplay.KAT_CATEGORY, inputs, resolvedOutput, 1, info));
    }

    private static void addShopRecipe(List<SkyBlockReiSimpleRecipeDisplay> recipes, JsonObject recipeJson) {
        List<String> cost = readIngredientArray(recipeJson, "cost");
        String result = string(recipeJson, "result", "");
        if (cost.isEmpty() || result.isBlank()) {
            return;
        }
        recipes.add(SkyBlockReiSimpleRecipeDisplay.of(SkyBlockReiSimpleRecipeDisplay.SHOP_CATEGORY, cost, result, 1, List.of()));
    }

    private static void addTradeRecipe(List<SkyBlockReiSimpleRecipeDisplay> recipes, String outputId, JsonObject recipeJson) {
        String cost = string(recipeJson, "cost", "");
        if (cost.isBlank()) {
            return;
        }

        String result = string(recipeJson, "result", outputId);
        recipes.add(SkyBlockReiSimpleRecipeDisplay.of(SkyBlockReiSimpleRecipeDisplay.TRADE_CATEGORY, List.of(cost), result, 1, List.of()));
    }

    private static void addMobDropRecipe(List<SkyBlockReiMobDropDisplay> recipes, JsonObject recipeJson) {
        JsonArray drops = recipeJson.getAsJsonArray("drops");
        if (drops == null || drops.isEmpty()) {
            return;
        }

        List<SkyBlockReiMobDropDisplay.Drop> parsedDrops = new ArrayList<>();
        for (JsonElement dropElement : drops) {
            if (!dropElement.isJsonObject()) {
                continue;
            }
            JsonObject drop = dropElement.getAsJsonObject();
            String id = string(drop, "id", "");
            if (id.isBlank()) {
                continue;
            }
            parsedDrops.add(new SkyBlockReiMobDropDisplay.Drop(
                    id,
                    string(drop, "chance", ""),
                    readStringArray(drop, "extra")
            ));
        }
        if (parsedDrops.isEmpty()) {
            return;
        }

        List<String> info = new ArrayList<>();
        int level = intValue(recipeJson, "level", 0);
        if (level > 0) {
            info.add("Level: " + level);
        }
        int coins = intValue(recipeJson, "coins", 0);
        if (coins > 0) {
            info.add("Coins: " + coins);
        }
        info.addAll(readStringArray(recipeJson, "extra"));
        recipes.add(SkyBlockReiMobDropDisplay.of(string(recipeJson, "name", "Mob Drops"), info, parsedDrops));
    }

    private static List<String> readIngredientArray(JsonObject recipeJson, String key) {
        JsonArray array = recipeJson.getAsJsonArray(key);
        if (array == null) {
            return List.of();
        }

        List<String> inputs = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                String ingredient = element.getAsString();
                if (!ingredient.isBlank()) {
                    inputs.add(ingredient);
                }
            }
        }
        return inputs;
    }

    private static List<String> readStringArray(JsonObject json, String key) {
        JsonArray array = json.getAsJsonArray(key);
        if (array == null) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                lines.add(element.getAsString());
            }
        }
        return lines;
    }

    private static Map<String, List<String>> loadParentGroups(Path parentsFile) throws IOException {
        if (!Files.isRegularFile(parentsFile)) {
            return Map.of();
        }

        JsonObject json;
        try (Reader reader = new InputStreamReader(Files.newInputStream(parentsFile), StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }

        Map<String, List<String>> groups = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            List<String> children = new ArrayList<>();
            for (JsonElement child : entry.getValue().getAsJsonArray()) {
                if (child.isJsonPrimitive()) {
                    children.add(child.getAsString());
                }
            }
            if (!children.isEmpty()) {
                groups.put(entry.getKey(), List.copyOf(children));
            }
        }
        return Map.copyOf(groups);
    }

    private static List<SkyBlockReiSimpleRecipeDisplay> appendEssenceRecipes(List<SkyBlockReiSimpleRecipeDisplay> recipes, Path essenceFile) throws IOException {
        if (!Files.isRegularFile(essenceFile)) {
            return recipes;
        }

        JsonObject json;
        try (Reader reader = new InputStreamReader(Files.newInputStream(essenceFile), StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }

        List<SkyBlockReiSimpleRecipeDisplay> merged = new ArrayList<>(recipes);
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject cost = entry.getValue().getAsJsonObject();
            String essenceType = "ESSENCE_" + string(cost, "type", "WITHER").toUpperCase(Locale.ROOT);
            JsonObject itemCosts = cost.getAsJsonObject("items");
            for (int star = 1; star <= 10; star++) {
                if (!cost.has(String.valueOf(star))) {
                    continue;
                }
                List<String> inputs = new ArrayList<>();
                inputs.add(entry.getKey() + ":1");
                inputs.add(essenceType + ":" + intValue(cost, String.valueOf(star), 1));
                if (itemCosts != null && itemCosts.has(String.valueOf(star))) {
                    inputs.addAll(readIngredientArray(itemCosts, String.valueOf(star)));
                }
                merged.add(SkyBlockReiSimpleRecipeDisplay.of(
                        SkyBlockReiSimpleRecipeDisplay.ESSENCE_CATEGORY,
                        inputs,
                        entry.getKey(),
                        1,
                        List.of("Upgrade: " + star + " star")
                ));
            }
        }
        return List.copyOf(merged);
    }

    private static List<SkyBlockReiSimpleRecipeDisplay> appendReforgeStoneRecipes(List<SkyBlockReiSimpleRecipeDisplay> recipes, Path reforgeStonesFile) throws IOException {
        if (!Files.isRegularFile(reforgeStonesFile)) {
            return recipes;
        }

        JsonObject json;
        try (Reader reader = new InputStreamReader(Files.newInputStream(reforgeStonesFile), StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }

        List<SkyBlockReiSimpleRecipeDisplay> merged = new ArrayList<>(recipes);
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject reforge = entry.getValue().getAsJsonObject();
            List<String> info = new ArrayList<>();
            info.add("Reforge: " + string(reforge, "reforgeName", entry.getKey()));
            info.add("Applies to: " + string(reforge, "itemTypes", "items"));
            JsonArray rarities = reforge.getAsJsonArray("requiredRarities");
            if (rarities != null && !rarities.isEmpty()) {
                List<String> rarityNames = new ArrayList<>();
                for (JsonElement rarity : rarities) {
                    if (rarity.isJsonPrimitive()) {
                        rarityNames.add(rarity.getAsString());
                    }
                }
                info.add("Rarities: " + String.join(", ", rarityNames));
            }
            JsonObject costs = reforge.getAsJsonObject("reforgeCosts");
            if (costs != null && costs.has("LEGENDARY")) {
                info.add("Legendary cost: " + costs.get("LEGENDARY").getAsInt() + " coins");
            }
            JsonElement ability = reforge.get("reforgeAbility");
            if (ability != null && ability.isJsonPrimitive()) {
                info.add(ability.getAsString());
            }
            merged.add(SkyBlockReiSimpleRecipeDisplay.of(
                    SkyBlockReiSimpleRecipeDisplay.REFORGE_CATEGORY,
                    List.of(entry.getKey() + ":1"),
                    entry.getKey(),
                    1,
                    info
            ));
        }
        return List.copyOf(merged);
    }

    private static int intValue(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        return element == null || !element.isJsonPrimitive() ? fallback : element.getAsInt();
    }

    private static String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = seconds % 3600 / 60;
        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "m";
        }
        if (hours > 0) {
            return hours + "h";
        }
        return minutes > 0 ? minutes + "m" : seconds + "s";
    }

    private record LoadedRecipes(List<SkyBlockReiCraftingDisplay> crafting, List<SkyBlockReiSimpleRecipeDisplay> simple, List<SkyBlockReiMobDropDisplay> mobDrops) {}

    private static SkyBlockReiItem loadItem(Path path) throws IOException {
        try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            JsonObject itemJson = JsonParser.parseReader(reader).getAsJsonObject();
            String internalName = string(itemJson, "internalname", path.getFileName().toString().replace(".json", ""));
            String itemId = string(itemJson, "itemid", "minecraft:paper");
            String displayName = string(itemJson, "displayname", internalName);
            JsonElement nbtTag = itemJson.get("nbttag");
            return new SkyBlockReiItem(
                    internalName,
                    itemId,
                    displayName,
                    readRawLore(itemJson),
                    nbtTag == null || nbtTag.isJsonNull() ? null : nbtTag.getAsString()
            );
        }
    }

    public static ItemStack createStack(String internalName, String itemId, String displayName, List<String> lore, String nbtTag) {
        ItemStack stack = new ItemStack(resolveItem(itemId));

        stack.set(DataComponents.CUSTOM_NAME, legacyComponent(displayName));
        List<Component> loreComponents = lore.stream()
                .<Component>map(SkyBlockReiItemRepository::legacyComponent)
                .toList();
        stack.set(DataComponents.LORE, new ItemLore(loreComponents));

        CompoundTag customData = new CompoundTag();
        customData.putString("id", internalName);
        customData.putString("giovanni_skyblock_id", internalName);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

        applyLegacyNeuTag(stack, nbtTag);
        return stack;
    }

    private static void applyLegacyNeuTag(ItemStack stack, String nbtTag) {
        if (nbtTag == null || nbtTag.isBlank()) {
            return;
        }

        try {
            CompoundTag tag = TagParser.parseCompoundFully(nbtTag);
            tag.getString("ItemModel")
                    .map(Identifier::tryParse)
                    .ifPresent(model -> stack.set(DataComponents.ITEM_MODEL, model));
            readProfile(tag).ifPresent(profile -> stack.set(DataComponents.PROFILE, profile));
        } catch (Exception e) {
            LOGGER.debug("Failed to parse NEU item nbttag for REI.", e);
        }
    }

    private static java.util.Optional<ResolvableProfile> readProfile(CompoundTag tag) {
        CompoundTag skullOwner = tag.getCompound("SkullOwner").orElse(null);
        if (skullOwner == null) {
            return java.util.Optional.empty();
        }

        String name = skullOwner.getStringOr("Name", "");
        UUID uuid = readUuid(skullOwner).orElseGet(() -> UUID.nameUUIDFromBytes(("GiovanniClient:" + name).getBytes(StandardCharsets.UTF_8)));
        GameProfile profile = new GameProfile(uuid, name.isBlank() ? "Giovanni" : name);

        skullOwner.getCompound("Properties")
                .flatMap(properties -> properties.getList("textures"))
                .filter(textures -> textures.size() > 0)
                .flatMap(textures -> textures.getCompound(0))
                .flatMap(texture -> texture.getString("Value"))
                .ifPresent(value -> profile.properties().put("textures", new Property("textures", value)));

        return java.util.Optional.of(ResolvableProfile.createResolved(profile));
    }

    private static java.util.Optional<UUID> readUuid(CompoundTag skullOwner) {
        Tag idTag = skullOwner.get("Id");
        if (idTag instanceof IntArrayTag intArrayTag && intArrayTag.size() == 4) {
            int[] data = intArrayTag.getAsIntArray();
            long most = (long) data[0] << 32 | data[1] & 0xffffffffL;
            long least = (long) data[2] << 32 | data[3] & 0xffffffffL;
            return java.util.Optional.of(new UUID(most, least));
        }

        return skullOwner.getString("Id").flatMap(id -> {
            try {
                return java.util.Optional.of(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
                return java.util.Optional.empty();
            }
        });
    }

    private static Item resolveItem(String itemId) {
        String normalized = itemId.toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        normalized = LEGACY_ITEM_IDS.getOrDefault(normalized, normalized);

        Identifier identifier = Identifier.tryParse(normalized);
        if (identifier == null) {
            return Items.PAPER;
        }

        Item item = BuiltInRegistries.ITEM.getValue(identifier);
        return item == null || item == Items.AIR ? Items.PAPER : item;
    }

    private static List<String> readRawLore(JsonObject itemJson) {
        JsonArray lore = itemJson.getAsJsonArray("lore");
        if (lore == null) {
            return List.of();
        }

        List<String> lines = new ArrayList<>(Math.min(lore.size(), ItemLore.MAX_LINES));
        for (JsonElement line : lore) {
            if (lines.size() >= ItemLore.MAX_LINES) {
                break;
            }
            lines.add(line.getAsString());
        }
        return lines;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static MutableComponent legacyComponent(String text) {
        MutableComponent root = Component.literal("").setStyle(Style.EMPTY.withItalic(false));
        Style style = Style.EMPTY.withItalic(false);
        StringBuilder segment = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                appendSegment(root, segment, style);
                ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(++i));
                if (formatting == null) {
                    continue;
                }
                if (formatting == ChatFormatting.RESET) {
                    style = Style.EMPTY.withItalic(false);
                } else if (formatting.isColor()) {
                    style = Style.EMPTY.withItalic(false).withColor(formatting);
                } else {
                    style = style.applyFormat(formatting);
                }
            } else {
                segment.append(c);
            }
        }

        appendSegment(root, segment, style);
        return root;
    }

    private static void appendSegment(MutableComponent root, StringBuilder segment, Style style) {
        if (segment.isEmpty()) {
            return;
        }

        root.append(Component.literal(segment.toString()).setStyle(style));
        segment.setLength(0);
    }
}

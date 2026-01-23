package sb.rocket.giovanniclient.client.features.inventorybuttons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class UiButtonsConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("giovanniclient_buttons.json");

    private static UiButtonsJsonConfig config;

    private UiButtonsConfigManager() {}

    public static UiButtonsJsonConfig get() {
        if (config == null) config = load();
        if (config.buttons == null) config.buttons = new ArrayList<>();
        config.buttons.removeIf(Objects::isNull);
        config.buttons.forEach(UiButtonDef::normalize);
        return config;
    }

    public static void save() {
        if (config == null) return;
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException ignored) {}
    }

    private static UiButtonsJsonConfig load() {
        if (!Files.exists(PATH)) {
            UiButtonsJsonConfig c = new UiButtonsJsonConfig();

            UiButtonDef editBtn = new UiButtonDef();
            editBtn.id = "inv_result_edit";
            editBtn.screen = "inventory";
            editBtn.slot = "result";
            editBtn.command = "/gioeditbuttons";
            editBtn.tooltip = "Edit inventory buttons";
            editBtn.icon = "minecraft:crafting_table";
            editBtn.w = 18;
            editBtn.h = 18;
            editBtn.visible = true;
            editBtn.enabled = true;

            c.buttons.add(editBtn.normalize());

            config = c;
            save();
            return c;
        }

        try {
            UiButtonsJsonConfig loaded = GSON.fromJson(Files.readString(PATH), UiButtonsJsonConfig.class);
            if (loaded == null) loaded = new UiButtonsJsonConfig();
            if (loaded.buttons == null) loaded.buttons = new ArrayList<>();
            loaded.buttons.removeIf(Objects::isNull);
            loaded.buttons.forEach(UiButtonDef::normalize);
            config = loaded;
            return loaded;
        } catch (Exception e) {
            UiButtonsJsonConfig fallback = new UiButtonsJsonConfig();
            fallback.buttons = new ArrayList<>();
            config = fallback;
            return fallback;
        }
    }

    // =========================
    // CRUD generico
    // =========================

    private static String canon(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    public static Optional<UiButtonDef> find(String screen, String slotId) {
        UiButtonsJsonConfig cfg = get();
        String scr = canon(screen);
        String key = canon(slotId);

        return cfg.buttons.stream()
                .filter(b -> canon(b.screen).equals(scr))
                .filter(b -> canon(b.slot).equals(key))
                .findFirst();
    }

    public static UiButtonDef getOrCreate(String screen, String slotId) {
        UiButtonsJsonConfig cfg = get();
        String scr = canon(screen);
        String key = canon(slotId);

        Optional<UiButtonDef> existing = find(scr, key);
        if (existing.isPresent()) return existing.get();

        UiButtonDef def = new UiButtonDef();
        def.screen = scr.isEmpty() ? "inventory" : scr;
        def.slot = slotId == null ? InventoryButtonLayout.DEFAULT_ID : slotId;
        def.id = def.screen + "_" + key;
        def.command = "";
        def.icon = UiButtonDef.DEFAULT_ICON;
        def.tooltip = "";
        def.visible = true;
        def.enabled = true;

        cfg.buttons.add(def.normalize());
        return def;
    }

    public static UiButtonDef upsert(String screen, String slotId, Consumer<UiButtonDef> mut) {
        UiButtonDef def = getOrCreate(screen, slotId);
        if (mut != null) mut.accept(def);
        def.normalize();
        return def;
    }

    public static boolean remove(String screen, String slotId) {
        UiButtonsJsonConfig cfg = get();
        UiButtonDef found = find(screen, slotId).orElse(null);
        if (found == null) return false;
        cfg.buttons.remove(found);
        return true;
    }

    // =========================
    // Convenience inventory
    // =========================

    public static Optional<UiButtonDef> findInventoryDef(String slotId) {
        return find("inventory", slotId);
    }

    public static UiButtonDef getOrCreateInventoryDef(String slotId) {
        return getOrCreate("inventory", slotId);
    }

    public static boolean removeInventoryDef(String slotId) {
        return remove("inventory", slotId);
    }
    public static boolean isEditMode() {
        return sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsEditorState.isEditMode();
    }

    public static void setEditMode(boolean on) {
        sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsEditorState.setEditMode(on);
    }

    public static String getSelectedSlot() {
        return sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsEditorState.getSelectedSlot();
    }

    public static void setSelectedSlot(String s) {
        sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsEditorState.setSelectedSlot(s);
    }

}

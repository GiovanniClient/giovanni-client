package sb.rocket.giovanniclient.client.features.inventorybuttons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class UiButtonsConfigManager {
    private static boolean editMode = false;
    private static String selectedSlot = "right0"; // default

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("giovanniclient_buttons.json");

    private static UiButtonsConfig config;

    private UiButtonsConfigManager() {}

    public static UiButtonsConfig get() {
        if (config == null) config = load();
        if (config.buttons == null) config.buttons = new java.util.ArrayList<>();
        return config;
    }

    public static void save() {
        if (config == null) return;
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException ignored) {}
    }

    private static UiButtonsConfig load() {
        // No config file yet: create defaults and persist them once.
        if (!Files.exists(PATH)) {
            UiButtonsConfig c = new UiButtonsConfig();

            // Default editor button on crafting result slot
            UiButtonDef editBtn = new UiButtonDef();
            editBtn.id = "inv_result_edit";
            editBtn.screen = "inventory";
            editBtn.slot = "result"; // matches InventoryButtonSlot("result", 143, 35)
            editBtn.command = "/gioeditbuttons";
            editBtn.tooltip = "Edit inventory buttons";
            editBtn.icon = "minecraft:textures/block/crafting_table.png";
            editBtn.w = 18;
            editBtn.h = 18;
            editBtn.visible = true;
            editBtn.enabled = true;

            c.buttons.add(editBtn);

            config = c;
            save();
            return c;
        }

        try {
            UiButtonsConfig loaded = GSON.fromJson(Files.readString(PATH), UiButtonsConfig.class);
            if (loaded == null) loaded = new UiButtonsConfig();
            if (loaded.buttons == null) loaded.buttons = new java.util.ArrayList<>();
            config = loaded;
            return loaded;
        } catch (Exception e) {
            // fallback
            UiButtonsConfig fallback = new UiButtonsConfig();
            fallback.buttons = new java.util.ArrayList<>();
            config = fallback;
            return fallback;
        }
    }

    // =========================
    // Editor helpers (inventory)
    // =========================

    private static String canonSlot(String slotId) {
        return slotId == null ? "" : slotId.trim().toLowerCase();
    }

    public static Optional<UiButtonDef> findInventoryDef(String slotId) {
        UiButtonsConfig cfg = get();
        String key = canonSlot(slotId);

        return cfg.buttons.stream()
                .filter(b -> b != null)
                .filter(b -> "inventory".equalsIgnoreCase(b.screen))
                .filter(b -> canonSlot(b.slot).equals(key))
                .findFirst();
    }

    public static UiButtonDef getOrCreateInventoryDef(String slotId) {
        UiButtonsConfig cfg = get();
        String key = canonSlot(slotId);

        Optional<UiButtonDef> existing = findInventoryDef(slotId);
        if (existing.isPresent()) return existing.get();

        UiButtonDef def = new UiButtonDef();
        def.id = "inv_" + key;
        def.screen = "inventory";
        def.slot = slotId;
        def.w = 18;
        def.h = 18;
        def.command = "";
        def.icon = "minecraft:textures/item/paper.png";
        def.tooltip = "";
        def.visible = true;
        def.enabled = true;

        cfg.buttons.add(def);
        return def;
    }

    public static boolean removeInventoryDef(String slotId) {
        UiButtonsConfig cfg = get();
        String key = canonSlot(slotId);

        UiButtonDef found = cfg.buttons.stream()
                .filter(b -> b != null)
                .filter(b -> "inventory".equalsIgnoreCase(b.screen))
                .filter(b -> canonSlot(b.slot).equals(key))
                .findFirst()
                .orElse(null);

        if (found == null) return false;

        cfg.buttons.remove(found);
        return true;
    }

    public static boolean isEditMode() {
        return editMode;
    }

    public static void setEditMode(boolean on) {
        editMode = on;
    }

    public static String getSelectedSlot() {
        return selectedSlot;
    }

    public static void setSelectedSlot(String s) {
        selectedSlot = (s == null ? "right0" : s);
    }
}

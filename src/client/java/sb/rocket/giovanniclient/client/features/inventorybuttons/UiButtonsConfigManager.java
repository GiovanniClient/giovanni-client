package sb.rocket.giovanniclient.client.features.inventorybuttons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UiButtonsConfigManager {
    private static boolean editMode = false;
    public static String selectedSlot = "right0"; // default

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("giovanniclient_buttons.json");

    private static UiButtonsConfig config;

    public static UiButtonsConfig get() {
        if (config == null) config = load();
        return config;
    }

    public static void save() {
        if (config == null) return;
        try {
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

            // If the file exists but has no buttons (or no inventory buttons), inject the same default once.
            ensureInventoryEditDefault(loaded);

            config = loaded;
            return loaded;
        } catch (Exception e) {
            // If parsing fails, fall back to an in-memory config (do not overwrite disk here).
            UiButtonsConfig fallback = new UiButtonsConfig();
            ensureInventoryEditDefault(fallback);
            config = fallback;
            return fallback;
        }
    }

    private static void ensureInventoryEditDefault(UiButtonsConfig cfg) {
        boolean hasResultEdit = cfg.buttons.stream().anyMatch(b ->
                b != null
                        && "inventory".equalsIgnoreCase(b.screen)
                        && b.slot != null
                        && "result".equalsIgnoreCase(b.slot)
        );

        if (hasResultEdit) return;

        UiButtonDef editBtn = new UiButtonDef();
        editBtn.id = "inv_result_edit";
        editBtn.screen = "inventory";
        editBtn.slot = "result";
        editBtn.command = "/gioeditbuttons";
        editBtn.tooltip = "Edit inventory buttons";
        editBtn.icon = "minecraft:textures/block/crafting_table.png";
        editBtn.w = 18;
        editBtn.h = 18;
        editBtn.visible = true;
        editBtn.enabled = true;

        cfg.buttons.add(editBtn);

        // Persist only if we're loading from disk (i.e., config already exists in memory from parsing).
        // Caller decides when to save; we save here only when a real file exists to keep behavior stable.
        if (Files.exists(PATH)) {
            config = cfg;
            save();
        }
    }

    public static boolean isEditMode() {
        return editMode;
    }

    public static void setEditMode(boolean on) {
        editMode = on;
    }

    public static String getSelectedSlot() { return selectedSlot; }
    public static void setSelectedSlot(String s) { selectedSlot = (s == null ? "right0" : s); }

}

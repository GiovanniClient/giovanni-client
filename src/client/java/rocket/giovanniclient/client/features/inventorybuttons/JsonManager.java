package rocket.giovanniclient.client.features.inventorybuttons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("GiovanniClient/InventoryButtons");
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("giovanniclient/buttons.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // The core data map: SlotID -> Data
    public static Map<String, ButtonData> savedButtons = new HashMap<>();

    static {
        load();
    }

    public record ButtonData(String command, String icon) {
        // Default values if someone manually edits the JSON and leaves a field out
        public ButtonData {
            if (command == null) command = "";
            if (icon == null) icon = "minecraft:paper";
        }
    }

    public static synchronized void load() {
        try {
            if (!Files.exists(PATH)) {
                savedButtons = defaultButtons();
                save();
                return;
            }
            String content = Files.readString(PATH);
            Map<String, ButtonData> parsed = GSON.fromJson(content, new TypeToken<Map<String, ButtonData>>(){}.getType());
            if (parsed == null) {
                throw new IllegalStateException("Buttons file is empty");
            }
            savedButtons = new HashMap<>(parsed);
        } catch (Exception e) {
            LOGGER.error("Failed to load inventory buttons; restoring defaults", e);
            backupBrokenButtonsFile();
            savedButtons = defaultButtons();
            save();
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path temporary = Files.createTempFile(PATH.getParent(), "buttons-", ".tmp");
            try {
                Files.writeString(temporary, GSON.toJson(savedButtons));
                try {
                    Files.move(temporary, PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save inventory buttons to {}", PATH, e);
        }
    }

    public static ButtonData get(String slotId) {
        return savedButtons.get(slotId);
    }

    private static Map<String, ButtonData> defaultButtons() {
        Map<String, ButtonData> defaults = new HashMap<>();
        defaults.put("result", new ButtonData("/gioeditbuttons", "minecraft:crafting_table"));
        return defaults;
    }

    private static void backupBrokenButtonsFile() {
        if (!Files.isRegularFile(PATH)) {
            return;
        }

        try {
            Path backup = PATH.resolveSibling("buttons-" + Instant.now().toEpochMilli() + ".bak.json");
            Files.copy(PATH, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Backed up invalid inventory buttons file to {}", backup);
        } catch (Exception exception) {
            LOGGER.error("Failed to back up invalid inventory buttons file {}", PATH, exception);
        }
    }
}

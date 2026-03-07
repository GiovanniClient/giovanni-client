package sb.rocket.giovanniclient.client.features.inventorybuttons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JsonManager {
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

    public static void load() {
        try {
            if (!Files.exists(PATH)) {
                // Default button to open the editor
                savedButtons.put("result", new ButtonData("/gioeditbuttons", "minecraft:crafting_table"));
                save();
                return;
            }
            String content = Files.readString(PATH);
            savedButtons = GSON.fromJson(content, new TypeToken<Map<String, ButtonData>>(){}.getType());
        } catch (Exception e) {
            savedButtons = new HashMap<>();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(savedButtons));
        } catch (Exception ignored) {}
    }

    public static ButtonData get(String slotId) {
        return savedButtons.get(slotId);
    }
}
package sb.rocket.giovanniclient.client.features.inventorybuttons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UiButtonsConfigManager {
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
        if (!Files.exists(PATH)) {
            UiButtonsConfig c = new UiButtonsConfig();

            UiButtonDef b = new UiButtonDef();
            b.id = "example";
            b.screen = "inventory";

            // NEU-style: slot fisso, NON x/y
            b.slot = "RIGHT_0";

            b.command = "/help";
            b.tooltip = "Example";
            b.icon = "minecraft:textures/item/paper.png";
            b.w = 18;
            b.h = 18;

            c.buttons.add(b);

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
            return new UiButtonsConfig();
        }
    }

    private UiButtonsConfigManager() {}
}

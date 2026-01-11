package sb.rocket.giovanniclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.notenoughupdates.moulconfig.common.IMinecraft;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    public static boolean shouldOpenFromCommand = false;

    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    private static final File CONFIG_FILE =
            new File("config/giovanniclient/config.json");

    private static MainConfig config;

    // moulconfig plumbing
    private static MoulConfigProcessor<MainConfig> processor;
    private static ConfigProcessorDriver driver;
    // **no editor created yet**:
    private static MoulConfigEditor<MainConfig> editor;

    // auto‐save thread
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor();

    /** Call once at the top of your onInitializeClient(). */
    public static void init() {
        CONFIG_FILE.getParentFile().mkdirs();
        loadConfig();

        // build & finalize the processor/driver now,
        // but *do not* new-up the MoulConfigEditor yet.
        processor = new MoulConfigProcessor<>(config);
        BuiltinMoulConfigGuis.addProcessors(processor);

        driver = new ConfigProcessorDriver(processor);
        driver.processConfig(config);

        // schedule auto‐save every 60s
        SCHEDULER.scheduleAtFixedRate(
                () -> saveConfig("auto-save"),
                60, 60, TimeUnit.SECONDS
        );
    }

    private static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            config = new MainConfig();
            saveConfig("initial");
            return;
        }
        try (FileReader fr = new FileReader(CONFIG_FILE)) {
            config = GSON.fromJson(fr, MainConfig.class);
            if (config == null) throw new IOException("Empty file");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                File backup = new File(
                        CONFIG_FILE.getParentFile(),
                        "config-" + Instant.now().toEpochMilli() + ".bak.json"
                );
                Files.copy(CONFIG_FILE.toPath(), backup.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                System.err.println("Backed up bad config to " + backup);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            config = new MainConfig();
        }
    }

    public static void saveConfig(String reason) {
        try (FileWriter fw = new FileWriter(CONFIG_FILE)) {
            fw.write(GSON.toJson(config));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static MainConfig getConfig() {
        return config;
    }

    /**
     * Called on your keypress.  Lazily builds the editor *after*
     * all of MinecraftClient (and its textRenderer) are ready.
     */
    public static void openConfigScreen() {
        if (editor == null) {
            // Now that we're on the render thread, textRenderer != null
            editor = new MoulConfigEditor<>(processor);
        }
        IMinecraft.getInstance().openWrappedScreen(editor);
    }

    public static void openConfigScreenFromCommand() {
        shouldOpenFromCommand = true;
    }

    public static void shutdown() {
        SCHEDULER.shutdownNow();
        saveConfig("shutdown");
    }
}
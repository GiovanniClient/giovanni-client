package rocket.giovanniclient.client.config;

import com.google.gson.*;
import io.github.notenoughupdates.moulconfig.common.IMinecraft;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.observer.Property;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import rocket.giovanniclient.giovanniclient.config.ClientConfigState;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("GiovanniClient/Config");
    public static boolean shouldOpenFromCommand = false;

    // Add custom TypeAdapter for Property fields
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .registerTypeAdapter(Property.class, new PropertyTypeAdapter())
            .create();

    private static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("giovanniclient/config.json");
    private static MainConfig config;
    private static MoulConfigProcessor<MainConfig> processor;
    private static ConfigProcessorDriver driver;
    private static MoulConfigEditor<MainConfig> editor;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> autosaveTask;
    private static boolean initialized;

    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            Files.createDirectories(CONFIG_FILE.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create GiovanniClient config directory", exception);
        }
        loadConfig();
        registerClientConfigStateObservers();
        syncClientConfigState();

        processor = new MoulConfigProcessor<>(config);
        BuiltinMoulConfigGuis.addProcessors(processor);
        driver = new ConfigProcessorDriver(processor);
        driver.processConfig(config);

        autosaveTask = SCHEDULER.scheduleAtFixedRate(
                () -> Minecraft.getInstance().execute(ConfigManager::saveConfig),
                60,
                60,
                TimeUnit.SECONDS
        );
    }

    private static void loadConfig() {
        if (!Files.isRegularFile(CONFIG_FILE)) {
            config = new MainConfig();
            saveConfig();
            return;
        }
        try (var reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            config = GSON.fromJson(reader, MainConfig.class);
            if (config == null) throw new IOException("Empty file");
        } catch (Exception e) {
            LOGGER.error("Failed to load config; restoring defaults", e);
            backupBrokenConfig();
            config = new MainConfig();
            saveConfig();
        }
    }

    public static synchronized void saveConfig() {
        if (config == null) {
            return;
        }
        syncClientConfigState();

        try {
            writeAtomically(CONFIG_FILE, GSON.toJson(config));
        } catch (IOException e) {
            LOGGER.error("Failed to save config to {}", CONFIG_FILE, e);
        }
    }

    public static MainConfig getConfig() {
        return config;
    }

    public static void openConfigScreen() {
        if (editor == null) {
            editor = new GiovanniConfigEditor(processor);
        }
        IMinecraft.getInstance().openWrappedScreen(editor);
    }

    public static void openConfigScreenFromCommand() {
        shouldOpenFromCommand = true;
    }

    public static void shutdown() {
        if (autosaveTask != null) {
            autosaveTask.cancel(false);
            autosaveTask = null;
        }
        SCHEDULER.shutdownNow();
        saveConfig();
    }

    private static void backupBrokenConfig() {
        try {
            Path backup = CONFIG_FILE.resolveSibling("config-" + Instant.now().toEpochMilli() + ".bak.json");
            Files.copy(CONFIG_FILE, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Backed up invalid config to {}", backup);
        } catch (IOException exception) {
            LOGGER.error("Failed to back up invalid config {}", CONFIG_FILE, exception);
        }
    }

    private static void writeAtomically(Path destination, String content) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), "config-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    // Custom TypeAdapter to properly serialize/deserialize Property fields
    private static class PropertyTypeAdapter implements JsonSerializer<Property>, JsonDeserializer<Property> {
        @Override
        public JsonElement serialize(Property src, Type typeOfSrc, JsonSerializationContext context) {
            // Serialize only the inner value, not the Property wrapper
            return context.serialize(src.get());
        }

        @Override
        public Property deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            // Deserialize the inner value and wrap it back in a Property
            if (typeOfT instanceof ParameterizedType) {
                Type innerType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
                Object value = context.deserialize(json, innerType);
                return Property.of(value);
            }
            throw new JsonParseException("Cannot deserialize Property without generic type");
        }
    }

    public static void syncClientConfigState() {
        ClientConfigState.proxyEnabled = config != null
                && config.proxyConfig != null
                && Boolean.TRUE.equals(config.proxyConfig.PROXY_TOGGLE.get());
        ClientConfigState.suppressYggdrasilWarnings = config != null
                && config.debugConfig != null
                && Boolean.TRUE.equals(config.debugConfig.YGGDRASIL.get());
    }

    private static void registerClientConfigStateObservers() {
        if (config == null) {
            return;
        }

        if (config.proxyConfig != null && config.proxyConfig.PROXY_TOGGLE != null) {
            config.proxyConfig.PROXY_TOGGLE.addObserver((oldValue, newValue) -> syncClientConfigState());
        }

        if (config.debugConfig != null && config.debugConfig.YGGDRASIL != null) {
            config.debugConfig.YGGDRASIL.addObserver((oldValue, newValue) -> syncClientConfigState());
        }

        if (config.rc != null && config.rc.kuudraAccordion != null) {
            if (config.rc.kuudraAccordion.GLASS_BARRIER_BLOCKS != null) {
                config.rc.kuudraAccordion.GLASS_BARRIER_BLOCKS.addObserver((oldValue, newValue) -> reloadChunks());
            }

            if (config.rc.kuudraAccordion.TRANSPARENT_LAVA != null) {
                config.rc.kuudraAccordion.TRANSPARENT_LAVA.addObserver((oldValue, newValue) -> reloadChunks());
            }
        }
    }

    public static void reloadChunks() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.level != null && client.player != null) {
                client.levelRenderer.allChanged();
            }
        });
    }

    private static class GiovanniConfigEditor extends MoulConfigEditor<MainConfig> {
        private GiovanniConfigEditor(MoulConfigProcessor<MainConfig> processor) {
            super(processor);
        }

        @Override
        public void onAfterClose() {
            super.onAfterClose();
            saveConfig();
            reloadChunks();
        }
    }
}

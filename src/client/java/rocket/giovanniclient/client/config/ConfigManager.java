package rocket.giovanniclient.client.config;

import com.google.gson.*;
import io.github.notenoughupdates.moulconfig.common.IMinecraft;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.observer.Property;
import net.minecraft.client.Minecraft;
import io.github.notenoughupdates.moulconfig.processor.BuiltinMoulConfigGuis;
import io.github.notenoughupdates.moulconfig.processor.ConfigProcessorDriver;
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor;
import rocket.giovanniclient.giovanniclient.config.ClientConfigState;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    public static boolean shouldOpenFromCommand = false;

    // Add custom TypeAdapter for Property fields
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .registerTypeAdapter(Property.class, new PropertyTypeAdapter())
            .create();

    private static final File CONFIG_FILE = new File("config/giovanniclient/config.json");
    private static MainConfig config;
    private static MoulConfigProcessor<MainConfig> processor;
    private static ConfigProcessorDriver driver;
    private static MoulConfigEditor<MainConfig> editor;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    public static void init() {
        CONFIG_FILE.getParentFile().mkdirs();
        loadConfig();
        registerClientConfigStateObservers();
        syncClientConfigState();

        processor = new MoulConfigProcessor<>(config);
        BuiltinMoulConfigGuis.addProcessors(processor);
        driver = new ConfigProcessorDriver(processor);
        driver.processConfig(config);

        SCHEDULER.scheduleAtFixedRate(() -> saveConfig(), 60, 60, TimeUnit.SECONDS);
    }

    private static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            config = new MainConfig();
            saveConfig();
            return;
        }
        try (FileReader fr = new FileReader(CONFIG_FILE)) {
            config = GSON.fromJson(fr, MainConfig.class);
            if (config == null) throw new IOException("Empty file");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                File backup = new File(CONFIG_FILE.getParentFile(), "config-" + Instant.now().toEpochMilli() + ".bak.json");
                Files.copy(CONFIG_FILE.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.err.println("Backed up bad config to " + backup);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            config = new MainConfig();
        }
    }

    public static void saveConfig() {
        syncClientConfigState();

        try (FileWriter fw = new FileWriter(CONFIG_FILE)) {
            fw.write(GSON.toJson(config));
        } catch (IOException e) {
            e.printStackTrace();
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
        SCHEDULER.shutdownNow();
        saveConfig();
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

        if (config.rc != null && config.rc.GLASS_BARRIER_BLOCKS != null) {
            config.rc.GLASS_BARRIER_BLOCKS.addObserver((oldValue, newValue) -> reloadChunks());
        }
    }

    private static void reloadChunks() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.level != null) {
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
        }
    }
}

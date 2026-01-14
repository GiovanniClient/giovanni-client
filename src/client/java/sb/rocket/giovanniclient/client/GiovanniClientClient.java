package sb.rocket.giovanniclient.client;

import moe.nea.libautoupdate.PotentialUpdate;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.FeatureManager;
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonEditorScreen;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsConfigManager;
import sb.rocket.giovanniclient.client.features.misc.InventoryBackgroundColor;
import sb.rocket.giovanniclient.client.features.updater.UpdateManager;
import sb.rocket.giovanniclient.client.util.ScoreboardUtils;
import sb.rocket.giovanniclient.client.util.Utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GiovanniClientClient implements ClientModInitializer {
    public static MinecraftClient mc = MinecraftClient.getInstance();
    public static final String MODID = "giovanniclient";
    public static final String MOD_VERSION_NAME = "1.0 (beta)";
    public static final int MOD_VERSION_CODE = 10001;

    private static final Identifier RELOAD_ID =
            Identifier.of("giovanniclient", "inventory_bg_color_reload");

    public static final UpdateManager UPDATE_MANAGER = new UpdateManager();

    /**
     * When true, we will (a) ensure the player inventory is open and then (b) open the editor
     * as an overlay, using the real InventoryScreen as parent.
     */
    private static boolean OPEN_INV_BUTTON_EDITOR_PENDING = false;
    private static int OPEN_INV_BUTTON_EDITOR_REOPEN_TICKS = 0;

    @Override
    public void onInitializeClient() {
        Runtime.getRuntime().addShutdownHook(new Thread(ConfigManager::shutdown));

        KeyBinding openGioCliConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open Config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "GiovanniClient"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGioCliConfigKey.wasPressed()) client.execute(ConfigManager::openConfigScreen);

            if (ConfigManager.shouldOpenFromCommand) {
                ConfigManager.shouldOpenFromCommand = false;
                client.execute(ConfigManager::openConfigScreen);
            }
        });

        KeyBinding editInventoryButtons = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.giovanniclient.open_inv_button_editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.giovanniclient"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (editInventoryButtons.wasPressed()) {
                if (client.player == null) return;

                UiButtonsConfigManager.setEditMode(true);
                OPEN_INV_BUTTON_EDITOR_PENDING = true;
                OPEN_INV_BUTTON_EDITOR_REOPEN_TICKS = 1;

                client.player.sendMessage(Text.literal("Inventory Buttons Editor: ON"), false);
            }
        });


        // Close edit mode if not in inventory
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!UiButtonsConfigManager.isEditMode()) return;

            // If we're in the middle of opening inventory for the editor, don't auto-disable.
            if (OPEN_INV_BUTTON_EDITOR_PENDING) return;

            if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) {
                UiButtonsConfigManager.setEditMode(false);
            }
        });


        // Pending flow: chat command can be overwritten by ChatScreen closing; do it on tick safely.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!OPEN_INV_BUTTON_EDITOR_PENDING) return;
            if (client.player == null) return;
            if (!UiButtonsConfigManager.isEditMode()) {
                OPEN_INV_BUTTON_EDITOR_PENDING = false;
                return;
            }

            // don't fight with chat screen
            if (client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) return;

            // ensure inventory open
            if (!(client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) {
                client.execute(() -> client.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player)));
                return;
            }

            // force exactly one reopen so InventoryScreen.init() runs while editMode=true
            if (OPEN_INV_BUTTON_EDITOR_REOPEN_TICKS > 0) {
                OPEN_INV_BUTTON_EDITOR_REOPEN_TICKS--;
                client.execute(() -> client.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player)));
                return;
            }

            OPEN_INV_BUTTON_EDITOR_PENDING = false;
        });




        registerClientCommands();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            ConfigManager.init();
            Utils.init(ConfigManager.getConfig().dc);

            FeatureManager.registerAll();

            autoUpdateStuff();

            Utils.debug("GiovanniClient initialized successfully! Version: " + MOD_VERSION_NAME);
        });

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return RELOAD_ID;
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        InventoryBackgroundColor.invalidate();
                    }
                }
        );
    }

    private void registerClientCommands() {
        String[] aliases = {"giovanni", "giovanniclient", "gio", "giocli", "giova", "zoo"};

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String alias : aliases) {
                dispatcher.register(ClientCommandManager.literal(alias).executes(context -> {
                    ConfigManager.openConfigScreenFromCommand();
                    return 1;
                }));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("sidebar").executes(context -> {
                    List<String> lines = ScoreboardUtils.getCleanedSidebarLines();

                    if (lines.isEmpty()) {
                        context.getSource().sendFeedback(Text.literal("No scoreboard sidebar currently displayed or it is empty."));
                        return 0;
                    }

                    context.getSource().sendFeedback(Text.literal("--- Scoreboard Sidebar ---"));
                    for (String line : lines) context.getSource().sendFeedback(Text.literal(line));
                    context.getSource().sendFeedback(Text.literal("--------------------------"));

                    return 1;
                }))
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("gioeditbuttons").executes(ctx -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null) return 0;

                    UiButtonsConfigManager.setEditMode(true);
                    OPEN_INV_BUTTON_EDITOR_PENDING = true;
                    OPEN_INV_BUTTON_EDITOR_REOPEN_TICKS = 1;

                    ctx.getSource().sendFeedback(Text.literal("Inventory Buttons Editor: ON"));
                    return 1;
                }))
        );
    }

    private void autoUpdateStuff() {
        boolean autoCheck = ConfigManager.getConfig().about.AUTO_CHECK_FOR_UPDATES;
        boolean autoDownload = ConfigManager.getConfig().about.AUTO_UPDATE;

        if (autoCheck) {
            CompletableFuture<PotentialUpdate> checkFuture = UPDATE_MANAGER.checkForUpdate();

            if (autoDownload) {
                checkFuture.thenAccept(potentialUpdate -> {
                    if (potentialUpdate != null && potentialUpdate.isUpdateAvailable()) {
                        UPDATE_MANAGER.launchUpdate(potentialUpdate);
                    }
                }).exceptionally(ex -> {
                    Utils.error("Error during auto-update check chain: ", ex);
                    return null;
                });
            }
        }
    }
}

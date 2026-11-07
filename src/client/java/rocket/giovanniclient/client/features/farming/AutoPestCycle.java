package rocket.giovanniclient.client.features.farming;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.PlayerLocator;
import rocket.giovanniclient.client.util.TabListUtils;
import rocket.giovanniclient.client.util.Utils;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class AutoPestCycle extends AbstractFeature {
    private static final FarmingSession SESSION = new FarmingSession();
    private final FarmingConfig config = ConfigManager.getConfig().farming;
    private final PestCycle cycle = new PestCycle(new Random(), message -> Utils.debug("Farming: " + message));
    private final LoadoutController loadouts = new LoadoutController();
    private final FarmingSoundSequence sounds = new FarmingSoundSequence(
            () -> System.nanoTime() / 1_000_000,
            (delay, action) -> {
                Minecraft client = Minecraft.getInstance();
                if (delay == 0) client.execute(action);
                else CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
                        .execute(() -> client.execute(action));
            },
            pitch -> {
                Minecraft client = Minecraft.getInstance();
                if (config.JINGLES && this.sessionRevision == SESSION.revision() && canRun(client)) {
                    client.getSoundManager().play(new SimpleSoundInstance(
                            SoundEvents.NOTE_BLOCK_BANJO.value().location(), SoundSource.MASTER,
                            FarmingSoundSequence.VOLUME, pitch, SoundInstance.createUnseededRandom(),
                            false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
                }
            });
    private final MinecraftActions actions = new MinecraftActions();
    private long sessionRevision = -1;

    public AutoPestCycle() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !refreshSession(Minecraft.getInstance())) return;
            String text = PestTabParser.clean(message.getString());
            loadouts.onMessage(text);
            PestTabParser.Spawn spawn = PestTabParser.parseSpawn(text);
            if (spawn != null) cycle.onSpawn(spawn, System.nanoTime() / 1_000_000);
        });
    }

    public static int toggle() {
        Minecraft client = Minecraft.getInstance();
        boolean inGarden = client.player != null && client.level != null && client.getConnection() != null
                && "Garden".equals(PlayerLocator.getPlayerArea());
        return switch (SESSION.toggle(inGarden)) {
            case ENABLED -> {
                Utils.chat("Farming enabled for this Garden visit.");
                yield 1;
            }
            case DISABLED -> {
                Utils.chat("Farming disabled.");
                yield 1;
            }
            case OUTSIDE_GARDEN -> {
                Utils.chat("Farming can only be enabled when PlayerLocator reports Garden.");
                yield 0;
            }
        };
    }

    @Override
    public void onTick(Minecraft client) {
        sounds.setEnabled(config.JINGLES);
        if (!refreshSession(client)) return;
        var pests = PestTabParser.parse(TabListUtils.getRawLines(true, true));
        var status = new PestTabParser.Status(PlayerLocator.getPlayerArea(), pests.alive(), pests.cooldownReady(), pests.cooldownSeconds());
        cycle.tick(System.nanoTime() / 1_000_000, status, config.settings(), actions);
    }

    private static boolean canRun(Minecraft client) {
        return SESSION.update(client.player != null && client.level != null && client.getConnection() != null,
                PlayerLocator.getPlayerArea());
    }

    private boolean refreshSession(Minecraft client) {
        boolean enabled = canRun(client);
        if (sessionRevision != SESSION.revision()) {
            reset();
            sessionRevision = SESSION.revision();
        }
        return enabled;
    }

    @Override
    public void onWorldLoad(Minecraft client) {
        SESSION.disable();
        reset();
    }

    @Override
    public void onWorldUnload(Minecraft client) {
        SESSION.disable();
        reset();
    }

    private void reset() {
        cycle.reset();
        sounds.cancel();
        loadouts.reset();
    }

    private final class MinecraftActions implements PestCycle.Actions {
        @Override
        public boolean canSendCommand() {
            Minecraft client = Minecraft.getInstance();
            return client.gui.screen() == null && client.player != null
                    && client.player.containerMenu == client.player.inventoryMenu;
        }

        @Override
        public void command(String command) {
            if (command.equals("loadouts")) loadouts.requested();
            Minecraft.getInstance().player.connection.sendCommand(command);
        }

        @Override
        public PestCycle.MenuState loadoutMenu(int button) {
            return loadouts.menuState(button);
        }

        @Override
        public void clickLoadout(int button) {
            loadouts.click(button);
        }

        @Override
        public PestCycle.CloseState closeLoadouts() {
            return loadouts.close();
        }

        @Override
        public void warning(String message) {
            Utils.chat(message);
        }

        @Override
        public void cooldownCaptured(int seconds) {
            String duration = seconds >= 60 ? (seconds / 60) + "m " + (seconds % 60) + "s" : seconds + "s";
            Utils.debug("Farming: Pest cooldown captured: " + duration + ". Internal timer started.", ChatFormatting.BLUE);
        }

        @Override
        public void workStarted() {
            sounds.setEnabled(config.JINGLES);
            sounds.start();
        }

        @Override
        public boolean readyForWork() {
            sounds.setEnabled(config.JINGLES);
            return sounds.isIdle();
        }

        @Override
        public void workFinished() {
            sounds.setEnabled(config.JINGLES);
            sounds.finish();
        }
    }
}

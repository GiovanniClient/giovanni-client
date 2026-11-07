package rocket.giovanniclient.client.features.farming;

import java.util.random.RandomGenerator;
import java.util.function.Consumer;

/** The Garden workflow, independent of Minecraft's command, menu and sound APIs. */
final class PestCycle {
    private static final long RESPONSE_TIMEOUT_MS = 10_000;
    private static final long COOLDOWN_POLL_INTERVAL_MS = 500;
    private static final long COOLDOWN_CAPTURE_TIMEOUT_MS = 30_000;
    private static final int MIN_SPAWN_COOLDOWN_SECONDS = 30;

    record Loadout(boolean enabled, int button) {}
    record Settings(Loadout kill, Loadout farming, Loadout spawn, int minDelay, int maxDelay, boolean returnWithOnePest) {
        boolean canReturn(Integer alive) {
            return alive != null && alive >= 0 && alive <= (returnWithOnePest ? 1 : 0);
        }
    }

    enum MenuState { WAITING, READY, WRONG_MENU, INVALID_BUTTON }
    enum CloseState { CLOSED, WAITING, WRONG_MENU }

    interface Actions {
        boolean canSendCommand();
        void command(String command);
        MenuState loadoutMenu(int button);
        void clickLoadout(int button);
        CloseState closeLoadouts();
        void warning(String message);
        void cooldownCaptured(int seconds);
        boolean readyForWork();
        void workStarted();
        void workFinished();
    }

    private enum Stage {
        IDLE, WAIT_SPAWN_COOLDOWN, SPAWNPOINT, OPEN_LOADOUT, SELECT_LOADOUT, CLOSE_LOADOUT,
        TELEPORT, WAIT_KILLS, WARP_GARDEN, WAIT_COOLDOWN, WAIT_SPAWN, STOPPED
    }

    private final RandomGenerator random;
    private final Consumer<String> debug;
    private Stage stage = Stage.IDLE;
    private Stage afterLoadout;
    private Loadout loadout;
    private PestTabParser.Spawn pendingSpawn;
    private long pendingSpawnAt;
    private PestTabParser.Spawn activeSpawn;
    private boolean observedPests;
    private boolean working;
    private boolean menuDelayStarted;
    private int spawnLeadSeconds;
    private long nextActionAt;
    private long responseDeadline;
    private long loadoutDeadline;
    private long missingTabSince = -1;
    private long nextCooldownPollAt;
    private long captureDeadline;
    private long cooldownEndsAt = Long.MIN_VALUE;

    PestCycle(RandomGenerator random, Consumer<String> debug) {
        this.random = random;
        this.debug = debug;
    }

    void onSpawn(PestTabParser.Spawn spawn, long now) {
        // A duplicate/new notification must not restart an in-progress trip to the pests.
        if (pendingSpawn == null && (stage == Stage.IDLE || stage == Stage.WAIT_COOLDOWN
                || stage == Stage.WAIT_SPAWN || (isLoadoutStage() && afterLoadout == Stage.WAIT_SPAWN))) {
            pendingSpawn = spawn;
            pendingSpawnAt = now;
        }
    }

    void tick(long now, PestTabParser.Status tab, Settings settings, Actions actions) {
        if (tab.area() != null && !tab.inGarden()) {
            reset();
            return;
        }
        if (stage == Stage.STOPPED) return;
        if ((stage == Stage.SELECT_LOADOUT || stage == Stage.CLOSE_LOADOUT) && now >= loadoutDeadline) {
            stop(actions, "Timed out completing the loadout selection within 10 seconds of /loadouts.");
            return;
        }
        if (!tab.inGarden()) {
            if (missingTabSince < 0) missingTabSince = now;
            if (stage != Stage.IDLE && now - missingTabSince >= RESPONSE_TIMEOUT_MS) {
                stop(actions, "Garden TAB data is unavailable.");
            }
            return;
        }
        missingTabSince = -1;

        if (pendingSpawn != null && !isLoadoutStage()) {
            if (!actions.readyForWork()) return;
            activeSpawn = pendingSpawn;
            pendingSpawn = null;
            observedPests = false;
            spawnLeadSeconds = 0;
            cooldownEndsAt = Long.MIN_VALUE;
            nextCooldownPollAt = now;
            captureDeadline = pendingSpawnAt + COOLDOWN_CAPTURE_TIMEOUT_MS;
            enter(Stage.WAIT_SPAWN_COOLDOWN, now);
        }
        if (!observedPests && activeSpawn != null && tab.alive() != null && tab.alive() >= activeSpawn.count()) {
            observedPests = true;
        }

        switch (stage) {
            case IDLE -> {
                if (Boolean.TRUE.equals(tab.cooldownReady())) {
                    if (settings.spawn().enabled() && !actions.readyForWork()) return;
                    if (settings.spawn().enabled()) startWork(actions);
                    beginLoadout(settings.spawn(), Stage.WAIT_SPAWN, now, actions);
                    delay(now, settings);
                } else if (Boolean.FALSE.equals(tab.cooldownReady()) && tab.cooldownSeconds() != null
                        && tab.cooldownSeconds() > 0) {
                    captureCooldown(now, tab.cooldownSeconds(), actions);
                    rollSpawnLead();
                    enter(Stage.WAIT_COOLDOWN, now);
                }
            }
            case WAIT_SPAWN_COOLDOWN -> {
                if (now >= captureDeadline) {
                    stop(actions, "The pest cooldown did not reach a readable duration of at least "
                            + MIN_SPAWN_COOLDOWN_SECONDS + "s after spawning. No Kill swap was sent.");
                } else if (now >= nextCooldownPollAt) {
                    nextCooldownPollAt = now + COOLDOWN_POLL_INTERVAL_MS;
                    if (Boolean.FALSE.equals(tab.cooldownReady()) && tab.cooldownSeconds() != null
                            && tab.cooldownSeconds() >= MIN_SPAWN_COOLDOWN_SECONDS && actions.readyForWork()) {
                        // Ignore short readings from the previous cooldown while TAB updates.
                        // Capture the first reading of at least 30s, without an additional fixed wait.
                        // A TAB update received just before its chat notification is valid as well.
                        captureCooldown(now, tab.cooldownSeconds(), actions);
                        startWork(actions);
                        enter(Stage.SPAWNPOINT, now);
                        delay(now, settings);
                    }
                }
            }
            case SPAWNPOINT -> {
                if (send("setspawn", now, settings, actions)) {
                    beginLoadout(settings.kill(), Stage.TELEPORT, now, actions);
                }
            }
            case OPEN_LOADOUT -> {
                if (send("loadouts", now, settings, actions)) {
                    // One deadline covers loading, refreshes, clicking, confirmation and closing.
                    loadoutDeadline = now + RESPONSE_TIMEOUT_MS;
                    enter(Stage.SELECT_LOADOUT, now);
                }
            }
            case SELECT_LOADOUT -> {
                MenuState menu = actions.loadoutMenu(loadout.button());
                if (menu == MenuState.WAITING) {
                    menuDelayStarted = false;
                }
                if (menu == MenuState.READY && !menuDelayStarted) {
                    menuDelayStarted = true;
                    delay(now, settings);
                }
                if (menu == MenuState.WRONG_MENU) {
                    stop(actions, "Expected page 1 of the Loadouts menu; another screen was opened.");
                } else if (menu == MenuState.INVALID_BUTTON) {
                    stop(actions, "The configured loadout button is empty, locked or unavailable.");
                } else if (menu == MenuState.READY && now >= nextActionAt) {
                    actions.clickLoadout(loadout.button());
                    if (afterLoadout == Stage.WAIT_COOLDOWN) rollSpawnLead();
                    delay(now, settings);
                    enter(Stage.CLOSE_LOADOUT, now);
                }
            }
            case CLOSE_LOADOUT -> {
                if (now < nextActionAt) return;
                CloseState result = actions.closeLoadouts();
                if (result == CloseState.WRONG_MENU) {
                    stop(actions, "The Loadouts menu changed before the selection finished.");
                    return;
                }
                if (result == CloseState.WAITING) return;
                delay(now, settings);
                enter(afterLoadout, now);
            }
            case TELEPORT -> {
                if (send("tptoplot " + activeSpawn.plot(), now, settings, actions)) enter(Stage.WAIT_KILLS, now);
            }
            case WAIT_KILLS -> {
                // Do not mistake the stale pre-spawn Alive: 0 for pests already killed.
                if (observedPests && settings.canReturn(tab.alive()) && actions.readyForWork()) {
                    startWork(actions);
                    enter(Stage.WARP_GARDEN, now);
                    delay(now, settings);
                }
            }
            case WARP_GARDEN -> {
                // If more pests appeared during the delay, keep waiting for the manual kills.
                if (!settings.canReturn(tab.alive())) {
                    enter(Stage.WAIT_KILLS, now);
                } else if (send("warp garden", now, settings, actions)) {
                    activeSpawn = null;
                    beginLoadout(settings.farming(), Stage.WAIT_COOLDOWN, now, actions);
                }
            }
            case WAIT_COOLDOWN -> {
                if (cooldownEndsAt != Long.MIN_VALUE && spawnLeadSeconds > 0
                        && now >= cooldownEndsAt - spawnLeadSeconds * 1000L) {
                    if (settings.spawn().enabled() && !actions.readyForWork()) return;
                    if (settings.spawn().enabled()) startWork(actions);
                    beginLoadout(settings.spawn(), Stage.WAIT_SPAWN, now, actions);
                    delay(now, settings);
                }
            }
            case WAIT_SPAWN, STOPPED -> { }
        }
        if (stage == Stage.WAIT_KILLS || stage == Stage.WAIT_COOLDOWN || stage == Stage.WAIT_SPAWN) {
            finishWork(actions);
        }
    }

    private boolean send(String command, long now, Settings settings, Actions actions) {
        if (now >= responseDeadline) {
            stop(actions, "Timed out waiting to send /" + command + ". Close other menus and try again.");
            return false;
        }
        if (now < nextActionAt) return false;
        if (!actions.canSendCommand()) return false;
        actions.command(command);
        // For /loadouts, start the click delay when its GUI is actually ready.
        if (!command.equals("loadouts")) delay(now, settings);
        return true;
    }

    private void beginLoadout(Loadout selected, Stage next, long now, Actions actions) {
        String role = switch (next) {
            case TELEPORT -> "Kill";
            case WAIT_COOLDOWN -> "Farming";
            case WAIT_SPAWN -> "Spawn";
            default -> next.toString();
        };
        if (!selected.enabled()) {
            if (next == Stage.WAIT_COOLDOWN) rollSpawnLead();
            enter(next, now);
            return;
        }
        if (selected.button() < 1 || selected.button() > 12) {
            stop(actions, "Loadout buttons must be between 1 and 12.");
            return;
        }
        loadout = selected;
        menuDelayStarted = false;
        afterLoadout = next;
        debug.accept("Switching to " + role + " loadout (slot " + selected.button() + ").");
        enter(Stage.OPEN_LOADOUT, now);
    }

    private boolean isLoadoutStage() {
        return stage == Stage.OPEN_LOADOUT || stage == Stage.SELECT_LOADOUT || stage == Stage.CLOSE_LOADOUT;
    }

    private void enter(Stage next, long now) {
        stage = next;
        responseDeadline = now + RESPONSE_TIMEOUT_MS;
    }

    private void delay(long now, Settings settings) {
        int first = Math.clamp(settings.minDelay(), 1200, 1800);
        int second = Math.clamp(settings.maxDelay(), 1200, 1800);
        int milliseconds = random.nextInt(Math.min(first, second), Math.max(first, second) + 1);
        nextActionAt = now + milliseconds;
    }

    private void rollSpawnLead() {
        spawnLeadSeconds = random.nextInt(5, 13);
        debug.accept("Spawn lead: " + spawnLeadSeconds + "s.");
    }

    private void captureCooldown(long now, int seconds, Actions actions) {
        cooldownEndsAt = now + seconds * 1000L;
        actions.cooldownCaptured(seconds);
    }

    private void stop(Actions actions, String reason) {
        stage = Stage.STOPPED;
        pendingSpawn = null;
        pendingSpawnAt = 0;
        finishWork(actions);
        actions.warning("Farming paused: " + reason + " Use /gio farming to switch it off and on to restart.");
    }

    private void startWork(Actions actions) {
        if (working) return;
        working = true;
        actions.workStarted();
    }

    private void finishWork(Actions actions) {
        if (!working) return;
        working = false;
        actions.workFinished();
    }

    void reset() {
        stage = Stage.IDLE;
        loadout = null;
        afterLoadout = null;
        pendingSpawn = null;
        pendingSpawnAt = 0;
        activeSpawn = null;
        observedPests = false;
        working = false;
        menuDelayStarted = false;
        spawnLeadSeconds = 0;
        nextActionAt = 0;
        responseDeadline = 0;
        loadoutDeadline = 0;
        missingTabSince = -1;
        nextCooldownPollAt = 0;
        captureDeadline = 0;
        cooldownEndsAt = Long.MIN_VALUE;
    }
}

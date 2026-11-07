package rocket.giovanniclient.client.features.farming;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Schedules complete melodies using elapsed milliseconds, independently of action delays. */
final class FarmingSoundSequence {
    static final float VOLUME = 100.0f;
    private static final List<Note> START = List.of(
            new Note(0, 1.189207f), new Note(100, 1.059463f), new Note(200, 0.890899f),
            new Note(250, 1.059463f), new Note(300, 0.890899f));
    private static final List<Note> FINISH = List.of(
            new Note(0, 0.890899f), new Note(100, 0.793701f), new Note(200, 0.707107f),
            new Note(250, 0.793701f), new Note(300, 0.890899f));

    @FunctionalInterface
    interface Scheduler {
        void schedule(long delayMs, Runnable action);
    }

    private record Note(int offsetMs, float pitch) {}

    private final LongSupplier clock;
    private final Scheduler scheduler;
    private final Consumer<Float> player;
    private long nextAvailableAt = Long.MIN_VALUE;
    private long generation;
    private boolean enabled = true;

    FarmingSoundSequence(LongSupplier clock, Scheduler scheduler, Consumer<Float> player) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.player = player;
    }

    void start() {
        if (enabled) enqueue(START);
    }

    void finish() {
        if (enabled) enqueue(FINISH);
    }

    void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (!enabled) cancel();
    }

    boolean isIdle() {
        return clock.getAsLong() >= nextAvailableAt;
    }

    private void enqueue(List<Note> notes) {
        long now = clock.getAsLong();
        long startAt = Math.max(now, nextAvailableAt);
        long currentGeneration = generation;
        for (Note note : notes) {
            scheduler.schedule(startAt + note.offsetMs() - now, () -> {
                if (generation == currentGeneration) player.accept(note.pitch());
            });
        }
        // Preserve all five notes if a short action finishes before its opening melody does.
        nextAvailableAt = startAt + 350;
    }

    void cancel() {
        generation++;
        nextAvailableAt = Long.MIN_VALUE;
    }
}

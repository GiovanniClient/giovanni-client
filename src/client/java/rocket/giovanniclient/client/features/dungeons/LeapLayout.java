package rocket.giovanniclient.client.features.dungeons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;

final class LeapLayout {
    private static final DungeonClass[] CLOCKWISE = DungeonClass.values();

    private LeapLayout() {}

    /** Assigns the other party members clockwise from the top, leaving empty vertices null. */
    static <T> List<T> assign(List<T> targets, Function<? super T, DungeonClass> classOf) {
        List<T> vertices = new ArrayList<>(Collections.nCopies(CLOCKWISE.length, null));
        EnumMap<DungeonClass, List<T>> groups = new EnumMap<>(DungeonClass.class);
        List<T> unclassified = new ArrayList<>();
        int targetCount = Math.min(targets.size(), CLOCKWISE.length - 1);
        for (T target : targets.subList(0, targetCount)) {
            DungeonClass dungeonClass = classOf.apply(target);
            if (dungeonClass == null) {
                unclassified.add(target);
            } else {
                groups.computeIfAbsent(dungeonClass, ignored -> new ArrayList<>()).add(target);
            }
        }

        for (DungeonClass dungeonClass : CLOCKWISE) {
            List<T> group = groups.get(dungeonClass);
            if (group != null) vertices.set(dungeonClass.ordinal(), group.getFirst());
        }

        for (DungeonClass dungeonClass : CLOCKWISE) {
            List<T> group = groups.get(dungeonClass);
            if (group == null) continue;
            for (int index = 1; index < group.size(); index++) {
                placeClockwise(vertices, group.get(index), dungeonClass.ordinal() + 1);
            }
        }

        for (T target : unclassified) placeClockwise(vertices, target, 0);
        return Collections.unmodifiableList(vertices);
    }

    private static <T> void placeClockwise(List<T> vertices, T target, int start) {
        int vertex = start % vertices.size();
        while (vertices.get(vertex) != null) {
            vertex = (vertex + 1) % vertices.size();
        }
        vertices.set(vertex, target);
    }
}

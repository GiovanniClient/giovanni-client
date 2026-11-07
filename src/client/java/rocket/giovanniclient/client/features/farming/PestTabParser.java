package rocket.giovanniclient.client.features.farming;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PestTabParser {
    private static final Pattern SPAWN = Pattern.compile(
            "^YUCK!\\s+(\\d+)\\s+[\\p{So}\\p{Co}\\p{Cf}\\s]*Pests?\\s+(?:have|has)\\s+spawned\\s+in\\s+Plot\\s*[-\u2013\u2014:]\\s*(.+?)!$");
    private static final Pattern ALIVE = Pattern.compile("^Alive:\\s*(\\d+)$");
    private static final Pattern DURATION = Pattern.compile("^(?:(\\d+)h\\s*)?(?:(\\d+)m\\s*)?(?:(\\d+)s)?$", Pattern.CASE_INSENSITIVE);

    record Spawn(int count, String plot) {}

    // Null means the corresponding TAB field is unavailable, never zero or READY.
    record Status(String area, Integer alive, Boolean cooldownReady, Integer cooldownSeconds) {
        boolean inGarden() {
            return "Garden".equals(area);
        }
    }

    private PestTabParser() {}

    static Spawn parseSpawn(String message) {
        Matcher matcher = SPAWN.matcher(clean(message));
        if (!matcher.matches()) return null;
        try {
            int count = Integer.parseInt(matcher.group(1));
            String plot = matcher.group(2).strip();
            if (count < 1 || plot.isEmpty() || plot.length() > 64
                    || plot.codePoints().anyMatch(Character::isISOControl)) return null;
            return new Spawn(count, plot);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static Status parse(List<String> entries) {
        String area = null;
        Integer alive = null;
        Boolean ready = null;
        Integer seconds = null;
        boolean inPests = false;
        for (String entry : entries) {
            for (String rawLine : entry.split("\\R")) {
                String line = clean(rawLine);
                if (line.startsWith("Area:")) area = line.substring(5).strip();
                if (line.equals("Pests:")) {
                    inPests = true;
                    continue;
                }
                if (!inPests || line.isEmpty()) continue;
                Matcher matcher = ALIVE.matcher(line);
                if (matcher.matches()) {
                    try {
                        alive = Integer.valueOf(matcher.group(1));
                    } catch (NumberFormatException ignored) {
                        alive = null;
                    }
                } else if (line.startsWith("Cooldown:")) {
                    String value = line.substring(9).strip();
                    ready = value.isEmpty() ? null : value.equals("READY");
                    seconds = parseCooldownSeconds(value);
                } else if (!line.startsWith("Plots:") && !line.startsWith("Spray:") && !line.startsWith("Repellent:")
                        && !line.startsWith("Bonus:")) {
                    inPests = false;
                }
            }
        }
        return new Status(area, alive, ready, seconds);
    }

    private static Integer parseCooldownSeconds(String value) {
        if (value.equals("READY")) return 0;
        if (value.isEmpty()) return null;
        Matcher matcher = DURATION.matcher(value);
        if (!matcher.matches()) return null;
        try {
            int hours = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
            int minutes = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
            int seconds = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
            return Math.addExact(Math.addExact(Math.multiplyExact(hours, 3600), Math.multiplyExact(minutes, 60)), seconds);
        } catch (NumberFormatException | ArithmeticException ignored) {
            return null;
        }
    }

    static String clean(String text) {
        return text.replaceAll("\u00a7.", "").replace('\u00a0', ' ')
                .replaceFirst("^[\\s\\p{So}\\p{Co}\\p{Cf}]+", "").strip();
    }

    static int containerSlot(int button) {
        if (button < 1 || button > 12) throw new IllegalArgumentException("Loadout button must be 1-12");
        return 14 + ((button - 1) / 3) * 9 + (button - 1) % 3;
    }
}

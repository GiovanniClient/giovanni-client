package rocket.giovanniclient.client.features.pokedex;

import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Persistent, world-independent description of one representation of a mob. */
public record DexProfile(List<Part> parts) {
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final Pattern LEVEL = Pattern.compile("(?i)\\[Lv\\s*\\d+\\]\\s*");
    private static final Pattern HEALTH = Pattern.compile(
            "(?i)(?:^|\\s+)[\\d,.]+[kmbt]?(?:/[\\d,.]+[kmbt]?)?\\s*(?:❤|♥|Hits\\b|HP\\b|\\p{Co}).*$");

    public DexProfile {
        parts = List.copyOf(parts);
        if (parts.isEmpty() || parts.size() > 64) {
            throw new IllegalArgumentException("Capture between 1 and 64 mob parts.");
        }
        if (parts.getFirst().horizontalOffset() != 0 || parts.getFirst().yOffset() != 0) {
            throw new IllegalArgumentException("The anchor must have zero offsets.");
        }
    }

    public enum Role { BODY, VISUAL, LABEL }

    public boolean typeOnly() {
        return parts.size() == 1 && !parts.getFirst().signature().distinctive();
    }

    /** The first part is the anchor; offsets use horizontal distance to survive rotation. */
    public record Part(Signature signature, Role role, double horizontalOffset, double yOffset) {
        public Part {
            Objects.requireNonNull(signature);
            Objects.requireNonNull(role);
            if (!Double.isFinite(horizontalOffset) || !Double.isFinite(yOffset)
                    || horizontalOffset < 0 || horizontalOffset > 8 || Math.abs(yOffset) > 8) {
                throw new IllegalArgumentException("Mob parts must be within 8 blocks of their anchor.");
            }
        }
    }

    public record Signature(String type, String name, Map<String, String> equipment, String appearance) {
        public Signature {
            if (type == null || type.isBlank()) throw new IllegalArgumentException("Missing entity type.");
            name = Objects.requireNonNull(name);
            equipment = Map.copyOf(equipment);
            appearance = Objects.requireNonNull(appearance);
        }

        public boolean distinctive() {
            return !name.isBlank() || !equipment.isEmpty() || !appearance.isBlank();
        }
    }

    public static String normalizeName(String raw) {
        if (raw == null) return "";
        String clean = FORMATTING.matcher(raw).replaceAll("");
        clean = LEVEL.matcher(clean).replaceAll("");
        clean = HEALTH.matcher(clean).replaceFirst("");
        return clean.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Aliases are user supplied, so do not strip numbers or level-looking words from them. */
    public static String key(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Profile UUIDs, signatures and timestamps may differ while the skin URL stays the same. */
    public static String texture(String value) {
        try {
            String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            String url = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("textures")
                    .getAsJsonObject("SKIN").get("url").getAsString();
            return url.substring(url.lastIndexOf('/') + 1);
        } catch (RuntimeException ignored) {
            return value;
        }
    }
}

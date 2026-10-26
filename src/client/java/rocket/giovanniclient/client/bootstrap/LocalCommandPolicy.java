package rocket.giovanniclient.client.bootstrap;

import java.util.Locale;
import java.util.Set;

/** Our command namespaces belong to the client even when parsing or registration fails. */
public final class LocalCommandPolicy {
    public static final Set<String> ROOTS = Set.of("gio", "giovanni", "giovanniclient", "zoo");

    private LocalCommandPolicy() {}

    public static boolean isLocal(String command) {
        if (command == null) return false;
        String input = command.stripLeading();
        // Be conservative about accidental extra slashes and whitespace in malformed commands.
        while (input.startsWith("/")) input = input.substring(1).stripLeading();
        int end = 0;
        while (end < input.length() && !Character.isWhitespace(input.charAt(end))) end++;
        return ROOTS.contains(input.substring(0, end).toLowerCase(Locale.ROOT));
    }

    public static boolean isLocalSlashMessage(String message) {
        return message != null && message.stripLeading().startsWith("/") && isLocal(message);
    }
}

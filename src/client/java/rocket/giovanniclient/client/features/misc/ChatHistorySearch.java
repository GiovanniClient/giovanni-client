package rocket.giovanniclient.client.features.misc;

import java.util.List;

/** Keeps the original typed prefix while navigating sent chat, ordered oldest first. */
public final class ChatHistorySearch {
    private String prefix;
    private int position;

    public boolean isActive() {
        return prefix != null;
    }

    public void reset() {
        prefix = null;
    }

    public Selection move(List<String> history, String currentInput, int direction) {
        if (!isActive()) {
            prefix = currentInput;
            position = history.size();
        }
        position = Math.min(position, history.size());
        int step = Integer.signum(direction);
        if (step == 0) return new Selection(position, currentInput);

        for (int index = position + step; index >= 0 && index < history.size(); index += step) {
            String entry = history.get(index);
            if (entry.startsWith(prefix)) {
                position = index;
                return new Selection(position, entry);
            }
        }

        // Moving past the newest match restores the text that started the search.
        if (step > 0) {
            position = history.size();
            return new Selection(position, prefix);
        }
        return new Selection(position, currentInput);
    }

    public record Selection(int index, String value) {}
}

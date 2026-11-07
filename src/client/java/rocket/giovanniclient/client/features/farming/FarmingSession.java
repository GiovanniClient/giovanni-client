package rocket.giovanniclient.client.features.farming;

/** Activation belongs to a single Garden visit, never to the saved configuration. */
final class FarmingSession {
    enum ToggleResult { ENABLED, DISABLED, OUTSIDE_GARDEN }

    private boolean enabled;
    private long revision;

    ToggleResult toggle(boolean inGarden) {
        if (enabled) {
            disable();
            return ToggleResult.DISABLED;
        }
        if (!inGarden) return ToggleResult.OUTSIDE_GARDEN;
        enabled = true;
        revision++;
        return ToggleResult.ENABLED;
    }

    boolean update(boolean connected, String playerLocatorArea) {
        if (!connected || !"Garden".equals(playerLocatorArea)) disable();
        return enabled;
    }

    void disable() {
        if (!enabled) return;
        enabled = false;
        revision++;
    }

    long revision() {
        return revision;
    }
}

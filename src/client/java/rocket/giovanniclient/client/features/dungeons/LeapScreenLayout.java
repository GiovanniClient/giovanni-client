package rocket.giovanniclient.client.features.dungeons;

import java.util.ArrayList;
import java.util.List;

record LeapScreenLayout(int titleY, List<LeapScreenLayout.Card> cards) {
    private static final int VERTICES = 5;
    private static final double VERTEX_ANGLE = Math.TAU / VERTICES;
    private static final double UPPER_SINE = Math.sin(VERTEX_ANGLE);
    private static final double LOWEST_COSINE = Math.cos(2 * VERTEX_ANGLE);
    private static final double LOWER_WIDTH_RATIO = Math.sin(2 * VERTEX_ANGLE) / UPPER_SINE;
    private static final double UPPER_HEIGHT_RATIO = (1 - Math.cos(VERTEX_ANGLE)) / (1 - LOWEST_COSINE);

    static LeapScreenLayout create(int width, int height) {
        int contentWidth = Math.min(424, width - Math.min(24, width / 8));
        int contentHeight = Math.min(216, height - Math.min(24, height / 8));
        int titleY = (height - contentHeight) / 2;
        int headerHeight = Math.min(20, contentHeight / 4);
        int cardsHeight = contentHeight - headerHeight;
        int gap = Math.min(8, Math.min(contentWidth, cardsHeight) / 10);

        // Keep the bottom pair apart and leave room between the top and side rows.
        int cardWidth = fitCard(contentWidth, gap, LOWER_WIDTH_RATIO, 140);
        int cardHeight = fitCard(cardsHeight, gap, UPPER_HEIGHT_RATIO, 48);
        double xRadius = (contentWidth - cardWidth) / (2 * UPPER_SINE);
        double yRadius = (cardsHeight - cardHeight) / (1 - LOWEST_COSINE);

        List<Card> cards = new ArrayList<>(VERTICES);
        for (int vertex = 0; vertex < VERTICES; vertex++) {
            double angle = vertex * VERTEX_ANGLE;
            int x = (int) Math.round((width - cardWidth) / 2.0 + Math.sin(angle) * xRadius);
            int y = titleY + headerHeight + (int) Math.round((1 - Math.cos(angle)) * yRadius);
            cards.add(new Card(x, y, cardWidth, cardHeight));
        }
        return new LeapScreenLayout(titleY, List.copyOf(cards));
    }

    private static int fitCard(int available, int gap, double spacingRatio, int maximum) {
        return Math.clamp((int) ((spacingRatio * available - gap) / (1 + spacingRatio)), 1, maximum);
    }

    record Card(int x, int y, int width, int height) {}
}

package rocket.giovanniclient.client.features.dungeons;

enum DungeonClass {
    MAGE("Mage", 0xFF55C8FF),
    BERSERKER("Berserker", 0xFFFF8100),
    HEALER("Healer", 0xFFFF85CB),
    TANK("Tank", 0xFF6ADA8A),
    ARCHER("Archer", 0xFFF00000);

    private final String label;
    private final int color;

    DungeonClass(String label, int color) {
        this.label = label;
        this.color = color;
    }

    String label() {
        return label;
    }

    int color() {
        return color;
    }
}

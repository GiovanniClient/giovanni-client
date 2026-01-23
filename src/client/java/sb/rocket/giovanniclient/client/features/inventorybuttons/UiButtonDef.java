package sb.rocket.giovanniclient.client.features.inventorybuttons;

public final class UiButtonDef {
    public static final String DEFAULT_ICON = "minecraft:textures/item/paper.png";

    public String id = "btn";
    public String screen = "inventory";
    public String slot = "right0";

    public int w = 18;
    public int h = 18;

    public String command = "";
    public String icon = DEFAULT_ICON;
    public String tooltip = "";
    public boolean enabled = true;
    public boolean visible = true;

    public UiButtonDef normalize() {
        if (id == null || id.isBlank()) id = "btn";
        if (screen == null || screen.isBlank()) screen = "inventory";
        if (slot == null || slot.isBlank()) slot = InventoryButtonLayout.DEFAULT_ID;

        if (w <= 0) w = 18;
        if (h <= 0) h = 18;

        if (command == null) command = "";
        if (icon == null) icon = DEFAULT_ICON;
        if (tooltip == null) tooltip = "";

        return this;
    }

    public boolean isPlaceholder() {
        return command == null || command.isBlank();
    }

    /**
     * Se placeholder -> niente icona (così renderizzi “vuoto”).
     * Se ha command ma icon vuota -> paper.
     */
    public String effectiveIcon() {
        if (isPlaceholder()) return "";
        if (icon == null || icon.isBlank()) return DEFAULT_ICON;
        return icon;
    }
}

package sb.rocket.giovanniclient.client.features.inventorybuttons;

public final class UiButtonDef {
    public String id = "btn";
    public String screen = "inventory";

    // NUOVO: slot fisso in stile NEU
    public String slot = "right0"; // invece di RIGHT_0

    public int w = 18;
    public int h = 18;

    public String command = "/help";
    public String icon = "minecraft:textures/item/paper.png";
    public String tooltip = "";
    public boolean enabled = true;
    public boolean visible = true;
}

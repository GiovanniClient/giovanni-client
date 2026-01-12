package sb.rocket.giovanniclient.client.features.inventorybuttons;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class InventoryButtonEditorScreen extends Screen {

    // Inventario vanilla (resource pack friendly)
    private static final Identifier INV_TEX =
            Identifier.of("minecraft", "textures/gui/container/inventory.png");

    private static final int INV_W = 176;
    private static final int INV_H = 166;

    // Spazio a destra per pannello editor
    private static final int PANEL_W = 230;

    // Stato selezione: ID slot (es: "right0", "craft00", ...)
    private String selectedId = "right0";

    private TextFieldWidget commandField;
    private TextFieldWidget iconField;

    public InventoryButtonEditorScreen() {
        super(Text.literal("Inventory Buttons Editor"));
    }

    @Override
    protected void init() {
        // Calcola layout (inventario + pannello)
        int baseX = (this.width - (INV_W + 10 + PANEL_W)) / 2;
        int baseY = (this.height - INV_H) / 2;

        int panelX = baseX + INV_W + 10;
        int panelY = baseY;

        // Text fields lunghi
        this.commandField = new TextFieldWidget(this.textRenderer, panelX + 10, panelY + 50, PANEL_W - 20, 18, Text.literal("Command"));
        this.iconField    = new TextFieldWidget(this.textRenderer, panelX + 10, panelY + 95, PANEL_W - 20, 18, Text.literal("Icon"));

        this.commandField.setMaxLength(1024);
        this.iconField.setMaxLength(1024);

        loadFieldsFromConfig();

        this.addDrawableChild(this.commandField);
        this.addDrawableChild(this.iconField);
    }

    private void loadFieldsFromConfig() {
        UiButtonDef def = getOrCreateDefForSelected();
        this.commandField.setText(def.command == null ? "" : def.command);
        this.iconField.setText(def.icon == null ? "" : def.icon);
    }

    private UiButtonDef getOrCreateDefForSelected() {
        UiButtonsConfig cfg = UiButtonsConfigManager.get();

        Optional<UiButtonDef> existing = cfg.buttons.stream()
                .filter(b -> "inventory".equals(b.screen))
                .filter(b -> selectedId.equalsIgnoreCase(b.slot))
                .findFirst();

        if (existing.isPresent()) return existing.get();

        // Crea entry per questo slot se non esiste ancora
        UiButtonDef def = new UiButtonDef();
        def.id = "inv_" + selectedId.toLowerCase();
        def.screen = "inventory";
        def.slot = selectedId;
        def.w = 18;
        def.h = 18;
        def.command = "";
        def.icon = "minecraft:textures/item/paper.png";
        def.tooltip = "";
        def.visible = true;
        def.enabled = true;

        cfg.buttons.add(def);
        UiButtonsConfigManager.save();
        return def;
    }

    private void saveFieldsToConfig() {
        UiButtonDef def = getOrCreateDefForSelected();
        def.command = this.commandField.getText().trim();
        def.icon = this.iconField.getText().trim();
        UiButtonsConfigManager.save();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Prima lascia gestire i widget (text fields)
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int baseX = (this.width - (INV_W + 10 + PANEL_W)) / 2;
        int baseY = (this.height - INV_H) / 2;

        // Click sugli slot possibili (overlay)
        for (InventoryButtonSlot slot : InventoryButtonSlot.all()) {
            int x = baseX + slot.relX();
            int y = baseY + slot.relY();

            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                // salva i campi del precedente prima di cambiare selezione (comodo)
                saveFieldsToConfig();

                this.selectedId = slot.id();
                loadFieldsFromConfig();
                return true;
            }
        }

        return false;
    }

    @Override
    public void close() {
        saveFieldsToConfig();
        super.close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Niente renderBackground() -> evita blur crash
        ctx.fill(0, 0, this.width, this.height, 0xAA000000);

        int baseX = (this.width - (INV_W + 10 + PANEL_W)) / 2;
        int baseY = (this.height - INV_H) / 2;

        // Disegna inventario vanilla
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED,
                INV_TEX,
                baseX, baseY,
                0, 0,
                INV_W, INV_H,
                INV_W, INV_H
        );

        // Overlay slot possibili
        for (InventoryButtonSlot slot : InventoryButtonSlot.all()) {
            int x = baseX + slot.relX();
            int y = baseY + slot.relY();

            boolean sel = slot.id().equalsIgnoreCase(selectedId);
            int overlay = sel ? 0x8040A0FF : 0x40111111;
            int border  = sel ? 0xFF40A0FF : 0xFF3A3A3A;

            ctx.fill(x, y, x + 18, y + 18, overlay);
            ctx.fill(x, y, x + 18, y + 1, border);
            ctx.fill(x, y + 17, x + 18, y + 18, border);
            ctx.fill(x, y, x + 1, y + 18, border);
            ctx.fill(x + 17, y, x + 18, y + 18, border);
        }

        // Pannello destro
        int panelX = baseX + INV_W + 10;
        int panelY = baseY;
        int panelH = INV_H;

        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, 0xCC111111);
        int b = 0xFF3A3A3A;
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, b);
        ctx.fill(panelX, panelY + panelH - 1, panelX + PANEL_W, panelY + panelH, b);
        ctx.fill(panelX, panelY, panelX + 1, panelY + panelH, b);
        ctx.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + panelH, b);

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Slot: " + selectedId), panelX + 10, panelY + 10, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Command (blank = hidden)"), panelX + 10, panelY + 35, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Icon texture id"), panelX + 10, panelY + 80, 0xFFFFFF);

        super.render(ctx, mouseX, mouseY, delta);
    }
}

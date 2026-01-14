package sb.rocket.giovanniclient.client.features.inventorybuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class InventoryButtonEditorScreen extends Screen {
    private final Screen parent;
    private boolean parentInited = false;

    private static final int INV_W = 176;
    private static final int INV_H = 166;

    private static final int PANEL_W = 230;

    private static final int SLOT = 18;
    private static final int GAP = 10;
    private static final int PAD = 10;
    private static final int FIELD_H = 18;

    private InventoryButtonSlot selectedSlot = InventoryButtonSlot.defaultSlot();
    private TextFieldWidget commandField;
    private TextFieldWidget iconField;

    private boolean dirty = false;
    private boolean suppressFieldDirty = false;

    // Cache in memoria
    private UiButtonsConfig cfg;
    private final java.util.Map<String, UiButtonDef> bySlot = new java.util.HashMap<>();

    private Layout layout;

    public InventoryButtonEditorScreen(Screen parent) {
        super(Text.literal("Inventory Buttons Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Keep parent in sync with current screen size. InventoryScreen must be initialized
        // or its render will glitch (wrong x/y, recipebook state, etc.).
        if (!parentInited && parent != null) {
            parentInited = true;
            parent.init(this.client, this.width, this.height);
        }

        recalcLayout(this.width, this.height);

        // load config once
        this.cfg = UiButtonsConfigManager.get();
        indexInventoryButtons(cfg);

        this.commandField = new TextFieldWidget(
                this.textRenderer,
                layout.panelX + PAD,
                layout.panelY + 50,
                PANEL_W - PAD * 2,
                FIELD_H,
                Text.literal("Command")
        );

        this.iconField = new TextFieldWidget(
                this.textRenderer,
                layout.panelX + PAD,
                layout.panelY + 95,
                PANEL_W - PAD * 2,
                FIELD_H,
                Text.literal("Icon")
        );

        this.commandField.setMaxLength(254);
        this.iconField.setMaxLength(1024);

        this.commandField.setChangedListener(s -> {
            if (!suppressFieldDirty) dirty = true;
        });
        this.iconField.setChangedListener(s -> {
            if (!suppressFieldDirty) dirty = true;
        });

        loadFieldsFromSelected();

        this.addDrawableChild(this.commandField);
        this.addDrawableChild(this.iconField);
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);

        // Parent must also be resized or its render becomes misaligned.
        if (parent != null) parent.resize(client, width, height);

        recalcLayout(width, height);

        if (commandField != null) {
            commandField.setX(layout.panelX + PAD);
            commandField.setY(layout.panelY + 50);
        }
        if (iconField != null) {
            iconField.setX(layout.panelX + PAD);
            iconField.setY(layout.panelY + 95);
        }
    }

    private void recalcLayout(int w, int h) {
        int baseX = computeParentBaseX(w);
        int baseY = computeParentBaseY(h);

        // Panel aligned to the right of the real inventory area.
        int panelX = baseX + INV_W + GAP;
        int panelY = baseY;

        this.layout = new Layout(baseX, baseY, panelX, panelY);
    }

    /**
     * Best-effort to match the real InventoryScreen position.
     *
     * If parent is a HandledScreen, we can approximate its origin by centering INV_W/INV_H.
     * This is still imperfect when recipe book shifts the GUI, but it is stable and non-glitchy.
     *
     * Pixel-perfect alignment requires an accessor mixin for HandledScreen.x/y; do that later
     * if you want exact positioning.
     */
    private int computeParentBaseX(int w) {
        // Default center.
        return (w - INV_W) / 2;
    }

    private int computeParentBaseY(int h) {
        return (h - INV_H) / 2;
    }

    private void indexInventoryButtons(UiButtonsConfig cfg) {
        bySlot.clear();
        for (UiButtonDef b : cfg.buttons) {
            if (b == null) continue;
            if (!"inventory".equals(b.screen)) continue;
            if (b.slot == null) continue;
            bySlot.put(canonSlot(b.slot), b);
        }
    }

    private static String canonSlot(String slot) {
        return slot.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * IMPORTANT BEHAVIOR CHANGE:
     * - We no longer add a new entry to cfg.buttons just because the user selects a slot.
     * - We create an in-memory default definition and only materialize into cfg on save
     *   if the user actually sets a non-blank command.
     */
    private UiButtonDef defFor(InventoryButtonSlot slot) {
        String key = canonSlot(slot.id());
        UiButtonDef def = bySlot.get(key);
        if (def != null) return def;

        // Virtual default (not added to cfg until user commits something).
        return defaultDefFor(slot);
    }

    private UiButtonDef defForOrCreateAndIndex(InventoryButtonSlot slot) {
        String key = canonSlot(slot.id());
        UiButtonDef def = bySlot.get(key);
        if (def != null) return def;

        def = defaultDefFor(slot);
        bySlot.put(key, def);
        cfg.buttons.add(def);
        return def;
    }

    private UiButtonDef defaultDefFor(InventoryButtonSlot slot) {
        UiButtonDef def = new UiButtonDef();
        def.id = "inv_" + canonSlot(slot.id());
        def.screen = "inventory";
        def.slot = slot.id();
        def.w = SLOT;
        def.h = SLOT;
        def.command = "";
        def.icon = "minecraft:textures/item/paper.png";
        def.tooltip = "";
        def.visible = true;
        def.enabled = true;
        return def;
    }

    private void loadFieldsFromSelected() {
        UiButtonDef def = defFor(selectedSlot);

        suppressFieldDirty = true;
        try {
            commandField.setText(def.command == null ? "" : def.command);
            iconField.setText(def.icon == null ? "" : def.icon);
        } finally {
            suppressFieldDirty = false;
        }

        dirty = false;
    }

    private void persistIfDirty() {
        if (!dirty) return;

        String cmd = commandField.getText().trim();
        String icon = iconField.getText().trim();

        // If user cleared the command, interpret as "no button for this slot".
        // If an entry exists in config, remove it.
        if (cmd.isEmpty()) {
            removeEntryForSelectedSlotIfPresent();
            UiButtonsConfigManager.save();
            dirty = false;
            return;
        }

        // Otherwise ensure an entry exists and write fields.
        UiButtonDef def = defForOrCreateAndIndex(selectedSlot);
        def.command = cmd;
        def.icon = icon;
        UiButtonsConfigManager.save();
        dirty = false;
    }

    private void removeEntryForSelectedSlotIfPresent() {
        String key = canonSlot(selectedSlot.id());
        UiButtonDef existing = bySlot.remove(key);
        if (existing != null) {
            cfg.buttons.remove(existing);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // Click slot overlay
        for (InventoryButtonSlot slot : InventoryButtonSlot.all()) {
            int x = layout.baseX + slot.relX();
            int y = layout.baseY + slot.relY();

            if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
                persistIfDirty();
                this.selectedSlot = slot;
                loadFieldsFromSelected();
                return true;
            }
        }

        return false;
    }

    @Override
    public void close() {
        persistIfDirty();
        super.close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Render the real inventory under us. If it ever throws, do not crash the render thread.
        if (parent != null) {
            try {
                parent.render(ctx, mouseX, mouseY, delta);
            } catch (Throwable t) {
                // Fallback background if something goes wrong; do not propagate.
                this.renderBackground(ctx, mouseX, mouseY, delta);
            }
        } else {
            this.renderBackground(ctx, mouseX, mouseY, delta);
        }

        // Dim overlay
        ctx.fill(0, 0, this.width, this.height, 0x33000000);

        renderOverlayAndPanel(ctx);

        super.render(ctx, mouseX, mouseY, delta);
    }


    private void renderOverlayAndPanel(DrawContext ctx) {
        // Slot overlays
        for (InventoryButtonSlot slot : InventoryButtonSlot.all()) {
            int x = layout.baseX + slot.relX();
            int y = layout.baseY + slot.relY();

            boolean sel = slot == selectedSlot;
            int overlay = sel ? 0x8040A0FF : 0x40111111;
            int border = sel ? 0xFF40A0FF : 0xFF3A3A3A;

            ctx.fill(x, y, x + SLOT, y + SLOT, overlay);
            ctx.fill(x, y, x + SLOT, y + 1, border);
            ctx.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, border);
            ctx.fill(x, y, x + 1, y + SLOT, border);
            ctx.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, border);
        }

        // Right panel
        ctx.fill(layout.panelX, layout.panelY, layout.panelX + PANEL_W, layout.panelY + INV_H, 0xCC111111);
        int b = 0xFF3A3A3A;
        ctx.fill(layout.panelX, layout.panelY, layout.panelX + PANEL_W, layout.panelY + 1, b);
        ctx.fill(layout.panelX, layout.panelY + INV_H - 1, layout.panelX + PANEL_W, layout.panelY + INV_H, b);
        ctx.fill(layout.panelX, layout.panelY, layout.panelX + 1, layout.panelY + INV_H, b);
        ctx.fill(layout.panelX + PANEL_W - 1, layout.panelY, layout.panelX + PANEL_W, layout.panelY + INV_H, b);

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Slot: " + selectedSlot.id()),
                layout.panelX + PAD, layout.panelY + 10, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Command (blank = hidden)"),
                layout.panelX + PAD, layout.panelY + 35, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Icon texture id"),
                layout.panelX + PAD, layout.panelY + 80, 0xFFFFFF);
    }

    private record Layout(int baseX, int baseY, int panelX, int panelY) {}
}

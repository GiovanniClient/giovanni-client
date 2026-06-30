package rocket.giovanniclient.client.features.inventorybuttons.overlay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import rocket.giovanniclient.client.features.inventorybuttons.JsonManager;
import rocket.giovanniclient.client.features.inventorybuttons.LayoutManager;
import rocket.giovanniclient.client.mixin.invbuttons.ScreenInvoker;

public class EditModeOverlay extends AbstractOverlay {
    private String selectedSlotId = "right0";

    public final EditBox commandField;
    public final EditBox iconField;

    // Instance variables calculated relative to the GUI
    private final int panelX;
    private final int panelY;
    private static final int PANEL_W = 162;
    private static final int PANEL_H = 77;

    public EditModeOverlay(InventoryScreen screen) {
        super(screen);

        // Compute exact position based on the Inventory background texture coordinates
        // This anchors it to the GUI so it survives window resizes
        this.panelX = this.guiX + 7;
        this.panelY = this.guiY + 83;

        // Command field (shifted right to leave room for the "/")
        this.commandField = new EditBox(
                screen.getFont(),
                this.panelX + 20,
                this.panelY + 18,
                PANEL_W - 32,
                16,
                Component.literal("Command")
        );

        // Icon field
        this.iconField = new EditBox(
                screen.getFont(),
                this.panelX + 12,
                this.panelY + 54,
                PANEL_W - 24,
                16,
                Component.literal("Icon")
        );

        this.commandField.setMaxLength(254);
        this.iconField.setMaxLength(254);

        // Make the text easy to read on the dark background
        this.commandField.setTextColor(0xFFFFFFFF);
        this.iconField.setTextColor(0xFFFFFFFF);
        this.commandField.setTextColorUneditable(0xFFAAAAAA);
        this.iconField.setTextColorUneditable(0xFFAAAAAA);

        this.commandField.setSuggestion("say hello world!");
        this.iconField.setSuggestion("paper");

        loadDataIntoFields();

        // Listeners to auto-save when you type
        this.commandField.setResponder(s -> saveCurrentSlot());
        this.iconField.setResponder(s -> saveCurrentSlot());

        // Attach to screen so they receive normal text input
        ((ScreenInvoker) screen).giovanni$addRenderableWidget(this.commandField);
        ((ScreenInvoker) screen).giovanni$addRenderableWidget(this.iconField);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY) {
        // 1. Draw the slot ghosts
        for (LayoutManager slot : LayoutManager.getAvailableSlots()) {
            var data = JsonManager.get(slot.id());
            boolean selected = slot.id().equals(selectedSlotId);
            boolean hovered = isMouseOverSlot(slot, mouseX, mouseY);

            // Yellow if selected, Green if it has data, Grey if empty
            int borderColor = selected ? 0xFFFF00FF : (data != null ? 0xFF919191 : 0xFF555555);

            drawButton(ctx, slot, data != null ? data.icon() : null, hovered, borderColor);
        }

        // 2. Draw the Editor Panel Background
        int x1 = this.panelX;
        int y1 = this.panelY;
        int x2 = this.panelX + PANEL_W;
        int y2 = this.panelY + PANEL_H;

        ctx.fill(x1, y1, x2, y2, 0xFF1c1a21);
        ctx.renderOutline(x1, y1, PANEL_W, PANEL_H, 0xFFFFFFFF);

        // 3. Draw the Labels
        ctx.drawString(screen.getFont(), "Command", this.panelX + 12, this.panelY + 6, 0xFF00FFFF, false);
        ctx.drawString(screen.getFont(), "/", this.panelX + 12, this.panelY + 22, 0xFFFFFFFF, false);

        ctx.drawString(screen.getFont(), "Icon", this.panelX + 12, this.panelY + 42, 0xFF00FFFF, false);

        this.commandField.render(ctx, mouseX, mouseY, 0);
        this.iconField.render(ctx, mouseX, mouseY, 0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x();
        double my = click.y();

        // 1. Check if the click is anywhere inside the grey editor panel
        if (mx >= this.panelX && mx <= this.panelX + PANEL_W && my >= this.panelY && my <= this.panelY + PANEL_H) {

            // 2a. Check if clicking the Command field
            if (mx >= commandField.getX() && mx <= commandField.getX() + commandField.getWidth() &&
                    my >= commandField.getY() && my <= commandField.getY() + commandField.getHeight()) {

                commandField.setFocused(true);
                iconField.setFocused(false);
                screen.setFocused(commandField);
                commandField.mouseClicked(click, doubled); // Let widget handle cursor placement
                return true; // Consume click to block inventory
            }

            // 2b. Check if clicking the Icon field
            if (mx >= iconField.getX() && mx <= iconField.getX() + iconField.getWidth() &&
                    my >= iconField.getY() && my <= iconField.getY() + iconField.getHeight()) {

                iconField.setFocused(true);
                commandField.setFocused(false);
                screen.setFocused(iconField);
                iconField.mouseClicked(click, doubled);
                return true; // Consume click
            }

            // 2c. Clicked inside the panel but NOT in a text box
            // Return true to "swallow" the click so it doesn't interact with items underneath
            return true;
        }

        // 3. Clicked outside the panel: Unfocus fields
        commandField.setFocused(false);
        iconField.setFocused(false);

        // 4. Handle slot selection
        for (LayoutManager slot : LayoutManager.getAvailableSlots()) {
            if (isMouseOverSlot(slot, mx, my)) {
                this.selectedSlotId = slot.id();
                loadDataIntoFields();
                return true; // Consume the click
            }
        }

        return false;
    }

    public boolean isMouseOverPanel(double mx, double my) {
        return mx >= this.panelX && mx <= this.panelX + PANEL_W && my >= this.panelY && my <= this.panelY + PANEL_H;
    }

    private void loadDataIntoFields() {
        var data = JsonManager.get(selectedSlotId);

        // Temporarily detach listeners so loading doesn't trigger a save
        commandField.setResponder(null);
        iconField.setResponder(null);

        commandField.setValue(data != null ? data.command() : "");
        iconField.setValue(data != null ? data.icon() : "");

        // Reattach listeners
        commandField.setResponder(s -> saveCurrentSlot());
        iconField.setResponder(s -> saveCurrentSlot());
    }

    private void saveCurrentSlot() {
        String cmd = commandField.getValue() == null ? "" : commandField.getValue().trim();
        String icon = iconField.getValue() == null ? "" : iconField.getValue().trim();

        if (cmd.isEmpty() && icon.isEmpty()) {
            JsonManager.savedButtons.remove(selectedSlotId);
        } else {
            if (icon.isEmpty()) icon = "minecraft:paper"; // Default to paper if left blank
            JsonManager.savedButtons.put(
                    selectedSlotId,
                    new JsonManager.ButtonData(cmd, icon)
            );
        }

        JsonManager.save();
    }
}
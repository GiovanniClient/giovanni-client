package rocket.giovanniclient.client.features.inventorybuttons.overlay;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import rocket.giovanniclient.client.features.inventorybuttons.LayoutManager;
import rocket.giovanniclient.client.features.inventorybuttons.icons.SimpleIconRenderer;
import rocket.giovanniclient.client.mixin.invbuttons.HandledScreenAccessor;

public abstract class AbstractOverlay {
    protected final InventoryScreen screen;
    protected final int guiX;
    protected final int guiY;

    public AbstractOverlay(InventoryScreen screen) {
        this.screen = screen;
        this.guiX = ((HandledScreenAccessor) screen).giovanni$getX();
        this.guiY = ((HandledScreenAccessor) screen).giovanni$getY();
    }

    public abstract void render(GuiGraphicsExtractor ctx, int mouseX, int mouseY);

    public abstract boolean mouseClicked(MouseButtonEvent click, boolean doubled);

    protected void drawButton(GuiGraphicsExtractor ctx, LayoutManager slot, String icon, boolean hovered, int borderColor) {
        int x = guiX + slot.relX();
        int y = guiY + slot.relY();

        ctx.fill(x, y, x + 18, y + 18, hovered ? 0x77FFFFFF : 0x44000000);
        ctx.outline(x, y, 18, 18, borderColor);

        if (icon != null && !icon.isBlank()) {
            SimpleIconRenderer.render(ctx, icon, x + 1, y + 1);
        }
    }

    protected boolean isMouseOverSlot(LayoutManager slot, double mouseX, double mouseY) {
        int x = guiX + slot.relX();
        int y = guiY + slot.relY();
        return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
    }
}
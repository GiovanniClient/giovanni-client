package sb.rocket.giovanniclient.client.features.inventorybuttons.overlay;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import sb.rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import sb.rocket.giovanniclient.client.features.inventorybuttons.JsonManager;
import sb.rocket.giovanniclient.client.features.inventorybuttons.LayoutManager;

public class NormalModeOverlay extends AbstractOverlay {
    public NormalModeOverlay(InventoryScreen screen) { super(screen); }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY) {
        JsonManager.savedButtons.forEach((id, data) -> {
            LayoutManager slot = LayoutManager.getSlotById(id);
            if (slot != null) {
                drawButton(ctx, slot, data.icon(), isMouseOverSlot(slot, mouseX, mouseY), 0xFF444444);
            }
        });
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        for (var entry : JsonManager.savedButtons.entrySet()) {
            LayoutManager slot = LayoutManager.getSlotById(entry.getKey());

            if (slot != null && isMouseOverSlot(slot, click.x(), click.y())) {
                String cmd = entry.getValue().command();
                if (cmd == null || cmd.isBlank()) return true;

                cmd = cmd.trim();

                // Live-swap to Edit Mode without closing the screen
                if (cmd.equals("/gioeditbuttons") || cmd.equals("gioeditbuttons")) {
                    EditModeState.setEditMode(true);
                    OverlayManager.activeOverlay = new EditModeOverlay(this.screen);
                    return true;
                }

                // Normal commands get sent to the server
                if (cmd.startsWith("/")) cmd = cmd.substring(1).trim();
                if (cmd.isBlank()) return true;

                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.networkHandler.sendChatCommand(cmd);
                }
                return true;
            }
        }
        return false;
    }
}
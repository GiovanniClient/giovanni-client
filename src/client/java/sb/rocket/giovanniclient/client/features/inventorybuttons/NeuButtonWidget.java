package sb.rocket.giovanniclient.client.features.inventorybuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class NeuButtonWidget extends ClickableWidget {

    private final UiButtonDef def;

    public NeuButtonWidget(int x, int y, UiButtonDef def) {
        super(x, y, def.w, def.h, Text.empty());
        this.def = def;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hover = this.isHovered();

        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        // === BACKGROUND (NEU style) ===
        int bg = hover
                ? 0xAA3A3A3A  // hover: leggermente più chiaro
                : 0xAA2A2A2A; // base

        ctx.fill(x, y, x + w, y + h, bg);

        // === BORDER (1px) ===
        int border = 0xFF5A5A5A;
        ctx.fill(x, y, x + w, y + 1, border);           // top
        ctx.fill(x, y + h - 1, x + w, y + h, border);   // bottom
        ctx.fill(x, y, x + 1, y + h, border);           // left
        ctx.fill(x + w - 1, y, x + w, y + h, border);   // right

        // === ICON (14x14 centrata) ===
        Identifier icon = Identifier.of(def.icon);

        int ix = x + (w - 14) / 2;
        int iy = y + (h - 14) / 2;

        ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                icon,
                ix, iy,
                0, 0,
                14, 14,
                16, 16
        );
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (UiButtonsConfigManager.get().editMode) return;

        var mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        String cmd = def.command.trim();
        if (cmd.startsWith("/")) {
            mc.player.networkHandler.sendChatCommand(cmd.substring(1));
        } else {
            mc.player.networkHandler.sendChatMessage(cmd);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {

    }
}

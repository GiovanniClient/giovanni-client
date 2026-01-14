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

    private static final Identifier PAPER_ICON =
            Identifier.of("minecraft", "textures/item/paper.png");

    public NeuButtonWidget(int x, int y, UiButtonDef def, boolean editMode) {
        super(x, y,
                (def != null && def.w > 0) ? def.w : 18,
                (def != null && def.h > 0) ? def.h : 18,
                Text.empty()
        );
        this.def = def;

        // In edit mode, selection is handled elsewhere; don't execute.
        this.active = !editMode && def != null && def.enabled;
        this.visible = def == null || def.visible;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (def == null) return;

        String cmd = (def.command == null) ? "" : def.command.trim();
        if (cmd.isBlank()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // sendChatCommand expects NO leading slash.
        if (cmd.startsWith("/")) cmd = cmd.substring(1).trim();
        if (!cmd.isBlank()) client.player.networkHandler.sendChatCommand(cmd);
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean hover = this.isHovered();

        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        boolean emptyCommand = def == null || def.command == null || def.command.isBlank();

        // Background
        int bg = hover ? 0xAA3A3A3A : 0xAA2A2A2A;
        ctx.fill(x, y, x + w, y + h, bg);

        // Border
        int border;
        if (emptyCommand) {
            border = hover ? 0xFFDDDDDD : 0xFF999999;
        } else {
            border = hover ? 0xFFFFFFFF : 0xFF444444;
        }
        ctx.fill(x, y, x + w, y + 1, border);
        ctx.fill(x, y + h - 1, x + w, y + h, border);
        ctx.fill(x, y, x + 1, y + h, border);
        ctx.fill(x + w - 1, y, x + w, y + h, border);

        // Placeholder: NO ICON
        if (emptyCommand) return;

        // Icon
        Identifier icon = safeIcon(def == null ? null : def.icon);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icon, x + 1, y + 1, 0, 0, w - 2, h - 2, w - 2, h - 2);
    }

    private static Identifier safeIcon(String raw) {
        if (raw == null || raw.isBlank()) return PAPER_ICON;
        Identifier parsed = Identifier.tryParse(raw.trim());
        return parsed != null ? parsed : PAPER_ICON;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // optional
    }
}

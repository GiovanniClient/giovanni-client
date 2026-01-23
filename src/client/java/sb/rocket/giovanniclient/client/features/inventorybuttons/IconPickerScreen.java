package sb.rocket.giovanniclient.client.features.inventorybuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class IconPickerScreen extends Screen {
    private final Screen parent;
    private final Consumer<String> onPick;

    private TextFieldWidget search;
    private final List<net.minecraft.item.ItemStack> allStacks = new ArrayList<>();
    private final List<net.minecraft.item.ItemStack> filteredStacks = new ArrayList<>();

    private int scrollRows = 0;
    private int maxScrollRows = 0;

    // Layout
    private int panelX, panelY, panelW, panelH;
    private int gridX, gridY, cols, rows;

    private static final int PAD = 10;
    private static final int SLOT = 18;
    private static final int TITLE_H = 16;
    private static final int SEARCH_H = 18;

    // Scrollbar drag
    private boolean draggingBar = false;
    private int dragGrabOffsetY = 0;

    public IconPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Text.literal("Icon Selector"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        super.init();

        panelW = 230;
        panelH = 220;

        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int searchX = panelX + PAD;
        int searchY = panelY + PAD + TITLE_H + 6;

        this.search = new TextFieldWidget(
                this.textRenderer,
                searchX, searchY,
                panelW - PAD * 2,
                SEARCH_H,
                Text.literal("Search")
        );
        this.search.setMaxLength(128);
        this.search.setChangedListener(s -> rebuildFilter());

        addDrawableChild(this.search);

        allStacks.clear();

        var rei = sb.rocket.giovanniclient.client.compat.ReiCompat.tryGetAllItemStacksFromRei();
        if (!rei.isEmpty()) {
            allStacks.addAll(rei);
        } else {
            net.minecraft.registry.Registries.ITEM.stream().forEach(it -> allStacks.add(it.getDefaultStack()));
        }

        allStacks.sort(Comparator.comparing(st -> {
            var id = net.minecraft.registry.Registries.ITEM.getId(st.getItem());
            return id == null ? "" : id.toString();
        }));

        rebuildFilter();
        this.search.setFocused(true);
    }

    private void rebuildFilter() {
        filteredStacks.clear();

        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            filteredStacks.addAll(allStacks);
        } else {
            for (var st : allStacks) {
                var id = net.minecraft.registry.Registries.ITEM.getId(st.getItem());
                if (id == null) continue;
                String s = id.toString().toLowerCase(Locale.ROOT);
                if (s.contains(q)) filteredStacks.add(st);
            }
        }

        gridX = panelX + PAD;
        gridY = panelY + PAD + TITLE_H + 6 + SEARCH_H + 10;

        int gridW = panelW - PAD * 2;
        int gridH = panelH - (gridY - panelY) - PAD;

        cols = Math.max(1, gridW / SLOT);
        rows = Math.max(1, gridH / SLOT);

        int totalRows = (int) Math.ceil(filteredStacks.size() / (double) cols);
        maxScrollRows = Math.max(0, totalRows - rows);
        scrollRows = clamp(scrollRows, 0, maxScrollRows);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        int delta = (verticalAmount > 0) ? -1 : 1;
        scrollRows = clamp(scrollRows + delta, 0, maxScrollRows);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // scrollbar grab?
            if (maxScrollRows > 0) {
                int[] knob = scrollbarKnobRect();
                int kx = knob[0], ky = knob[1], kw = knob[2], kh = knob[3];
                if (mouseX >= kx && mouseX < kx + kw && mouseY >= ky && mouseY < ky + kh) {
                    draggingBar = true;
                    dragGrabOffsetY = (int) mouseY - ky;
                    return true;
                }
            }

            int idx = gridIndexAt((int) mouseX, (int) mouseY);
            if (idx >= 0 && idx < filteredStacks.size()) {
                var st = filteredStacks.get(idx);
                var id = net.minecraft.registry.Registries.ITEM.getId(st.getItem());
                if (id != null) {
                    onPick.accept("item:" + id);
                }
                close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingBar && maxScrollRows > 0) {
            int[] track = scrollbarTrackRect();
            int tx = track[0], ty = track[1], tw = track[2], th = track[3];

            int[] knob = scrollbarKnobRect();
            int kh = knob[3];

            int newKnobY = (int) mouseY - dragGrabOffsetY;
            int minY = ty;
            int maxY = ty + th - kh;

            newKnobY = clamp(newKnobY, minY, maxY);

            float t = (maxY == minY) ? 0f : (newKnobY - minY) / (float) (maxY - minY);
            scrollRows = clamp(Math.round(t * maxScrollRows), 0, maxScrollRows);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingBar) {
            draggingBar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int gridIndexAt(int mx, int my) {
        int gx0 = gridX;
        int gy0 = gridY;
        int gx1 = gridX + cols * SLOT;
        int gy1 = gridY + rows * SLOT;

        if (mx < gx0 || mx >= gx1 || my < gy0 || my >= gy1) return -1;

        int col = (mx - gx0) / SLOT;
        int row = (my - gy0) / SLOT;

        int visibleIndex = row * cols + col;
        return (scrollRows * cols) + visibleIndex;
    }

    private int[] scrollbarTrackRect() {
        int gx0 = gridX - 1;
        int gy0 = gridY - 1;
        int gx1 = gridX + cols * SLOT + 1;
        int gy1 = gridY + rows * SLOT + 1;

        int barX = gx1 + 6;
        int barY0 = gy0;
        int barH = (gy1 - gy0);
        return new int[]{barX, barY0, 4, barH};
    }

    private int[] scrollbarKnobRect() {
        int[] tr = scrollbarTrackRect();
        int barX = tr[0], barY0 = tr[1], barW = tr[2], barH = tr[3];

        float t = (maxScrollRows == 0) ? 0f : (scrollRows / (float) maxScrollRows);
        int knobH = Math.max(10, (int) (barH * (rows / (float) (rows + maxScrollRows))));
        int knobY = barY0 + (int) ((barH - knobH) * t);

        return new int[]{barX, knobY, barW, knobH};
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xAA000000);

        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC000000);

        ctx.drawText(this.textRenderer, "Icon Selector", panelX + PAD, panelY + PAD, 0xFFFFFFFF, false);

        super.render(ctx, mouseX, mouseY, delta);

        int gx0 = gridX - 1;
        int gy0 = gridY - 1;
        int gx1 = gridX + cols * SLOT + 1;
        int gy1 = gridY + rows * SLOT + 1;

        ctx.fill(gx0, gy0, gx1, gy0 + 1, 0xFF555555);
        ctx.fill(gx0, gy1 - 1, gx1, gy1, 0xFF555555);
        ctx.fill(gx0, gy0, gx0 + 1, gy1, 0xFF555555);
        ctx.fill(gx1 - 1, gy0, gx1, gy1, 0xFF555555);

        int start = scrollRows * cols;
        int end = Math.min(filteredStacks.size(), start + (cols * rows));

        for (int i = start; i < end; i++) {
            int local = i - start;
            int col = local % cols;
            int row = local / cols;

            int x = gridX + col * SLOT + 1;
            int y = gridY + row * SLOT + 1;

            ctx.drawItem(filteredStacks.get(i), x, y);

            if (mouseX >= x - 1 && mouseX < x - 1 + SLOT && mouseY >= y - 1 && mouseY < y - 1 + SLOT) {
                ctx.fill(x - 1, y - 1, x - 1 + SLOT, y - 1 + SLOT, 0x55FFFFFF);
            }
        }

        if (maxScrollRows > 0) {
            int[] tr = scrollbarTrackRect();
            ctx.fill(tr[0], tr[1], tr[0] + tr[2], tr[1] + tr[3], 0xFF333333);

            int[] knob = scrollbarKnobRect();
            ctx.fill(knob[0], knob[1], knob[0] + knob[2], knob[1] + knob[3], 0xFF888888);
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}

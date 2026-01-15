package sb.rocket.giovanniclient.client.features.inventorybuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
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


    private int scrollRows = 0;     // row offset
    private int maxScrollRows = 0;

    // Layout
    private int panelX, panelY, panelW, panelH;
    private int gridX, gridY, cols, rows;
    private static final int PAD = 10;
    private static final int SLOT = 18;
    private static final int TITLE_H = 16;
    private static final int SEARCH_H = 18;

    public IconPickerScreen(Screen parent, Consumer<String> onPick) {
        super(Text.literal("Icon Selector"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        super.init();

        // panel sizing close to mockup proportions
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

        // Build items list once
        allStacks.clear();

        // 1) prova REI
        var rei = sb.rocket.giovanniclient.client.compat.ReiCompat.tryGetAllItemStacksFromRei();
        if (!rei.isEmpty()) {
            allStacks.addAll(rei);
        } else {
            // 2) fallback vanilla registry
            net.minecraft.registry.Registries.ITEM.stream().forEach(it -> allStacks.add(it.getDefaultStack()));
        }

        // ordina per id item
        allStacks.sort(java.util.Comparator.comparing(s -> {
            var id = net.minecraft.registry.Registries.ITEM.getId(s.getItem());
            return id == null ? "" : id.toString();
        }));

        rebuildFilter();
        this.search.setFocused(true);
    }

    private void rebuildFilter() {
        filteredStacks.clear();
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(java.util.Locale.ROOT);

        if (q.isBlank()) {
            filteredStacks.addAll(allStacks);
        } else {
            for (var st : allStacks) {
                var id = net.minecraft.registry.Registries.ITEM.getId(st.getItem());
                if (id == null) continue;
                String s = id.toString().toLowerCase(java.util.Locale.ROOT);
                if (s.contains(q)) filteredStacks.add(st);
            }
        }

        // grid area
        gridX = panelX + PAD;
        gridY = panelY + PAD + TITLE_H + 6 + SEARCH_H + 10;

        int gridW = panelW - PAD * 2;
        int gridH = panelH - (gridY - panelY) - PAD;

        cols = Math.max(1, gridW / SLOT);
        rows = Math.max(1, gridH / SLOT);

        int totalRows = (int) Math.ceil(filteredStacks.size() / (double) cols);
        maxScrollRows = Math.max(0, totalRows - rows);
        scrollRows = Math.min(scrollRows, maxScrollRows);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Esc
        if (keyCode == 256) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount == 0) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);

        // scroll rows
        int delta = (verticalAmount > 0) ? -1 : 1;
        scrollRows = clamp(scrollRows + delta, 0, maxScrollRows);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int idx = gridIndexAt((int) mouseX, (int) mouseY);
            if (idx >= 0 && idx < filteredStacks.size()) {
                var st = filteredStacks.get(idx);
                var id = net.minecraft.registry.Registries.ITEM.getId(st.getItem());
                onPick.accept("item:" + id);
                close();
                return true;
            }

        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        int absoluteIndex = (scrollRows * cols) + visibleIndex;
        return absoluteIndex;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // NON chiamare renderBackground(ctx) perché applica blur e può crashare se un'altra screen lo ha già fatto nello stesso frame
        ctx.fill(0, 0, this.width, this.height, 0xAA000000);

        // Panel
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC000000);

        // Title
        ctx.drawText(this.textRenderer, "Icon Selector", panelX + PAD, panelY + PAD, 0xFFFFFFFF, false);

        // Widgets (search box)
        super.render(ctx, mouseX, mouseY, delta);

        // Grid frame
        int gx0 = gridX - 1;
        int gy0 = gridY - 1;
        int gx1 = gridX + cols * SLOT + 1;
        int gy1 = gridY + rows * SLOT + 1;

        ctx.fill(gx0, gy0, gx1, gy0 + 1, 0xFF555555);
        ctx.fill(gx0, gy1 - 1, gx1, gy1, 0xFF555555);
        ctx.fill(gx0, gy0, gx0 + 1, gy1, 0xFF555555);
        ctx.fill(gx1 - 1, gy0, gx1, gy1, 0xFF555555);

        // Items
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


        // Scrollbar
        if (maxScrollRows > 0) {
            int barX = gx1 + 6;
            int barY0 = gy0;
            int barH = (gy1 - gy0);

            ctx.fill(barX, barY0, barX + 4, barY0 + barH, 0xFF333333);

            float t = scrollRows / (float) maxScrollRows;
            int knobH = Math.max(10, (int) (barH * (rows / (float) (rows + maxScrollRows))));
            int knobY = barY0 + (int) ((barH - knobH) * t);

            ctx.fill(barX, knobY, barX + 4, knobY + knobH, 0xFF888888);
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}

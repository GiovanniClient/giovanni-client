package sb.rocket.giovanniclient.client.compat;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitorWidget;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import sb.rocket.giovanniclient.client.mixin.invbuttons.ScreenInvoker;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

@SuppressWarnings("unused") // loaded via reflection
public final class ReiIconDropSupport {
    private ReiIconDropSupport() {}

    public static void attachIfPresent(InventoryScreen screen, Consumer<ItemStack> onDrop) {
        ClickableWidget iconField = findLikelyIconField(screen);
        if (iconField == null) return;

        int x0 = iconField.getX();
        int y0 = iconField.getY();
        int w  = iconField.getWidth();
        int h  = iconField.getHeight();

        Rectangle drop = new Rectangle(x0, y0, w, h + 34);

        ReiDropWidget widget = new ReiDropWidget(drop, onDrop);

        ((ScreenInvoker) (Object) screen).giovanni$addDrawableChild(widget);
    }

    private static ClickableWidget findLikelyIconField(InventoryScreen screen) {
        ClickableWidget best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Element el : screen.children()) {
            if (!(el instanceof ClickableWidget cw)) continue;
            if (cw.getHeight() != 18) continue;

            int score = 0;
            int w = cw.getWidth();

            if (w >= 140 && w <= 210) score += 10;
            if (cw.getX() > screen.width / 2) score += 5;
            score += Math.min(20, w / 10);

            if (score > bestScore) {
                bestScore = score;
                best = cw;
            }
        }
        return best;
    }

    private static final class ReiDropWidget extends ClickableWidget implements DraggableStackVisitorWidget {
        private final Rectangle bounds;
        private final Consumer<ItemStack> onDrop;

        private ReiDropWidget(Rectangle bounds, Consumer<ItemStack> onDrop) {
            super(bounds.x, bounds.y, bounds.width, bounds.height, Text.empty());
            this.bounds = bounds;
            this.onDrop = onDrop;
            this.visible = false;
            this.active = true;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            // invisible by default
            // debug:
            // context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x3300FF00);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            // no narration
        }

        @Override
        public DraggedAcceptorResult acceptDraggedStack(DraggingContext<Screen> context, DraggableStack stack) {
            if (stack == null) return DraggedAcceptorResult.PASS;

            var pos = context.getCurrentPosition();
            if (pos == null) return DraggedAcceptorResult.PASS;

            if (!bounds.contains(pos.x, pos.y)) return DraggedAcceptorResult.PASS;

            EntryStack<?> entry = stack.getStack();
            if (entry == null || entry.isEmpty()) return DraggedAcceptorResult.PASS;

            if (!Objects.equals(entry.getType(), VanillaEntryTypes.ITEM)) return DraggedAcceptorResult.PASS;

            ItemStack mcStack = (ItemStack) entry.castValue();
            if (mcStack == null || mcStack.isEmpty()) return DraggedAcceptorResult.PASS;

            onDrop.accept(mcStack.copy());
            return DraggedAcceptorResult.CONSUMED;
        }

        /**
         * REI 20.x expects BoundsProvider(s) that yield a VoxelShape (NOT Rectangle streams).
         * We build a screen-space cuboid using pixel coords; Z is arbitrary [0..1].
         */
        @Override
        public Stream<DraggableStackVisitor.BoundsProvider> getDraggableAcceptingBounds(DraggingContext<Screen> context, DraggableStack stack) {
            return Stream.of(() -> rectToShape(bounds));
        }

        private static VoxelShape rectToShape(Rectangle r) {
            double x1 = r.x;
            double y1 = r.y;
            double x2 = r.x + r.width;
            double y2 = r.y + r.height;
            return VoxelShapes.cuboid(x1, y1, 0.0, x2, y2, 1.0);
        }
    }
}

package rocket.giovanniclient.client.features.inventorybuttons.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackProvider;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.EditModeOverlay;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.OverlayManager;

import java.util.stream.Stream;

public class GiovanniReiPlugin implements REIClientPlugin {
    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDraggableStackProvider(new InventoryButtonReiStackProvider());
        registry.registerDraggableStackVisitor(new InventoryButtonIconDropTarget());
    }

    private static EditModeOverlay getEditOverlay() {
        if (!EditModeState.isEditMode()) {
            return null;
        }

        if (OverlayManager.activeOverlay instanceof EditModeOverlay editOverlay) {
            return editOverlay;
        }

        return null;
    }

    private static final class InventoryButtonReiStackProvider implements DraggableStackProvider<Screen> {
        @Override
        public DraggableStack getHoveredStack(DraggingContext<Screen> context, double mouseX, double mouseY) {
            if (getEditOverlay() == null) {
                return null;
            }

            return ReiOverlayHelper.getHoveredEntryStack(mouseX, mouseY)
                    .map(InventoryButtonReiDraggableStack::new)
                    .orElse(null);
        }

        @Override
        public <R extends Screen> boolean isHandingScreen(R screen) {
            return getEditOverlay() != null;
        }

        @Override
        public double getPriority() {
            return 1000;
        }
    }

    private record InventoryButtonReiDraggableStack(EntryStack<?> stack) implements DraggableStack {
        @Override
        public EntryStack<?> getStack() {
            return stack;
        }

        @Override
        public void drag() {
        }
    }

    private static final class InventoryButtonIconDropTarget implements DraggableStackVisitor<Screen> {
        @Override
        public DraggedAcceptorResult acceptDraggedStack(DraggingContext<Screen> context, DraggableStack stack) {
            EditModeOverlay editOverlay = getEditOverlay();
            if (editOverlay == null) {
                return DraggedAcceptorResult.PASS;
            }

            Point position = context.getCurrentPosition();
            if (position == null || !editOverlay.isMouseOverPanel(position.x, position.y)) {
                return DraggedAcceptorResult.PASS;
            }

            ItemStack itemStack = stack.getStack().cheatsAs().getValue();
            editOverlay.applyDraggedIcon(itemStack.copy());
            return DraggedAcceptorResult.CONSUMED;
        }

        @Override
        public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<Screen> context, DraggableStack stack) {
            EditModeOverlay editOverlay = getEditOverlay();
            if (editOverlay == null) {
                return Stream.empty();
            }

            Rectangle bounds = new Rectangle(
                    editOverlay.getPanelX(),
                    editOverlay.getPanelY(),
                    editOverlay.getPanelWidth(),
                    editOverlay.getPanelHeight()
            );
            return Stream.of(BoundsProvider.ofRectangle(bounds));
        }

        @Override
        public <R extends Screen> boolean isHandingScreen(R screen) {
            return getEditOverlay() != null;
        }

        @Override
        public double getPriority() {
            return 1000;
        }
    }
}

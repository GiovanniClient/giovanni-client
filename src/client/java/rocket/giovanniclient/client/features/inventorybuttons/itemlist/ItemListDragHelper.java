package rocket.giovanniclient.client.features.inventorybuttons.itemlist;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.EditModeOverlay;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.OverlayManager;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ItemListDragHelper {
    private static ItemStack draggingStack = ItemStack.EMPTY;

    private ItemListDragHelper() {}

    public static boolean beginDrag(Screen screen, MouseButtonEvent click) {
        if (!canHandle() || click.button() != 0 || !(OverlayManager.activeOverlay instanceof EditModeOverlay)) {
            return false;
        }

        ItemStack stack = getHoveredStack(screen, click.x(), click.y());
        if (stack.isEmpty()) {
            return false;
        }

        draggingStack = stack.copy();
        return true;
    }

    public static boolean finishDrag(MouseButtonEvent click) {
        if (draggingStack.isEmpty()) {
            return false;
        }

        try {
            if (OverlayManager.activeOverlay instanceof EditModeOverlay editOverlay
                    && editOverlay.isMouseOverPanel(click.x(), click.y())) {
                editOverlay.applyDraggedIcon(draggingStack.copy());
                return true;
            }

            return false;
        } finally {
            draggingStack = ItemStack.EMPTY;
        }
    }

    public static void cancelDrag() {
        draggingStack = ItemStack.EMPTY;
    }

    public static void renderDraggedStack(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!draggingStack.isEmpty()) {
            graphics.item(draggingStack, mouseX - 8, mouseY - 8);
        }
    }

    public static boolean isMouseOverItemList(Screen screen, double mouseX, double mouseY) {
        return canHandle() && !getHoveredStack(screen, mouseX, mouseY).isEmpty();
    }

    private static boolean canHandle() {
        return EditModeState.isEditMode() && FabricLoader.getInstance().isModLoaded("skyblock-item-list");
    }

    private static ItemStack getHoveredStack(Screen screen, double mouseX, double mouseY) {
        if (screen == null) {
            return ItemStack.EMPTY;
        }

        for (GuiEventListener child : screen.children()) {
            ItemStack stack = getHoveredStack(child, mouseX, mouseY);
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getHoveredStack(Object widget, double mouseX, double mouseY) {
        if (widget == null || !isItemListClass(widget.getClass())) {
            return ItemStack.EMPTY;
        }

        Object hovered = getChildAt(widget, mouseX, mouseY).orElse(widget);
        ItemStack stack = readHoveredStack(hovered);
        if (!stack.isEmpty()) {
            return stack;
        }

        if (hovered != widget) {
            return getHoveredStack(hovered, mouseX, mouseY);
        }

        return ItemStack.EMPTY;
    }

    private static Optional<?> getChildAt(Object widget, double mouseX, double mouseY) {
        try {
            Method method = findMethod(widget.getClass(), "getChildAt", double.class, double.class);
            method.setAccessible(true);
            Object result = method.invoke(widget, mouseX, mouseY);
            if (result instanceof Optional<?> optional) {
                return optional;
            }
        } catch (Throwable ignored) {
        }

        return Optional.empty();
    }

    private static ItemStack readHoveredStack(Object widget) {
        try {
            Method method = findMethod(widget.getClass(), "getHoveredStack");
            method.setAccessible(true);
            Object result = method.invoke(widget);
            if (result instanceof ItemStack stack) {
                return stack;
            }
        } catch (Throwable ignored) {
        }

        return ItemStack.EMPTY;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }

        throw new NoSuchMethodException(name);
    }

    private static boolean isItemListClass(Class<?> type) {
        Package pkg = type.getPackage();
        return pkg != null && pkg.getName().startsWith("com.operationpotato.itemlist.");
    }
}

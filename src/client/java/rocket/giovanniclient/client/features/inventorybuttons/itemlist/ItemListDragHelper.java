package rocket.giovanniclient.client.features.inventorybuttons.itemlist;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import rocket.giovanniclient.client.features.inventorybuttons.icons.IconStackCodec;
import rocket.giovanniclient.client.mixin.invbuttons.ScreenInvoker;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.EditModeOverlay;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.OverlayManager;
import rocket.giovanniclient.client.util.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Optional;

public final class ItemListDragHelper {
    private ItemListDragHelper() {}

    public static boolean handleMiddleClick(Screen screen, MouseButtonEvent click) {
        if (!canHandle() || click.button() != 2 || !(OverlayManager.activeOverlay instanceof EditModeOverlay editOverlay)) {
            return false;
        }

        ItemStack stack = getHoveredStack(screen, click.x(), click.y());
        if (stack.isEmpty()) {
            Utils.debug("ItemList stack: <empty>");
            return false;
        }

        String encoded = IconStackCodec.encode(stack);
        Utils.debug("ItemList stack: " + encoded);
        editOverlay.applyDraggedIcon(stack.copy());
        return true;
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

        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ItemStack stack = getHoveredStackFromSkyBlockItemListPanels(mouseX, mouseY, visited);
        if (!stack.isEmpty()) {
            return stack;
        }

        for (Renderable renderable : ((ScreenInvoker) screen).giovanni$getRenderables()) {
            stack = getHoveredStack(renderable, mouseX, mouseY, visited);
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        for (GuiEventListener child : screen.children()) {
            stack = getHoveredStack(child, mouseX, mouseY, visited);
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getHoveredStack(Object widget, double mouseX, double mouseY, Set<Object> visited) {
        if (widget == null || !isItemListClass(widget.getClass())) {
            return ItemStack.EMPTY;
        }

        if (!visited.add(widget)) {
            return ItemStack.EMPTY;
        }

        if (!isMouseOver(widget, mouseX, mouseY)) {
            return ItemStack.EMPTY;
        }

        Optional<?> hoveredChild = getChildAt(widget, mouseX, mouseY);
        Object hovered = hoveredChild.isPresent() ? hoveredChild.get() : widget;
        ItemStack stack = readHoveredStack(hovered);
        if (!stack.isEmpty()) {
            return stack;
        }

        if (hovered != widget) {
            return getHoveredStack(hovered, mouseX, mouseY, visited);
        }

        for (Object child : getChildren(widget)) {
            stack = getHoveredStack(child, mouseX, mouseY, visited);
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean isMouseOver(Object widget, double mouseX, double mouseY) {
        try {
            Method method = findMethod(widget.getClass(), "isMouseOver", double.class, double.class);
            method.setAccessible(true);
            Object result = method.invoke(widget, mouseX, mouseY);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return true;
        }
    }

    private static Optional<?> getChildAt(Object widget, double mouseX, double mouseY) {
        try {
            Method method = findMethod(widget.getClass(), "getChildAt", double.class, double.class);
            method.setAccessible(true);
            Object result = method.invoke(widget, mouseX, mouseY);
            if (result instanceof Optional<?> optional) {
                return optional;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }

        return Optional.empty();
    }

    private static List<?> getChildren(Object widget) {
        try {
            Method method = findMethod(widget.getClass(), "children");
            method.setAccessible(true);
            Object result = method.invoke(widget);
            if (result instanceof List<?> children) {
                return children;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }

        return List.of();
    }

    private static ItemStack readHoveredStack(Object widget) {
        try {
            invokeIfPresent(widget, "createStackIfEmpty");
            Method method = findMethod(widget.getClass(), "getHoveredStack");
            method.setAccessible(true);
            Object result = method.invoke(widget);
            if (result instanceof ItemStack stack) {
                return stack;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getHoveredStackFromSkyBlockItemListPanels(double mouseX, double mouseY, Set<Object> visited) {
        try {
            Class<?> itemListClass = Class.forName("com.operationpotato.itemlist.SkyBlockItemList");
            ItemStack stack = getHoveredStack(getStaticField(itemListClass, "instance"), mouseX, mouseY, visited);
            if (!stack.isEmpty()) {
                return stack;
            }

            return getHoveredStack(getStaticField(itemListClass, "favoriteInstance"), mouseX, mouseY, visited);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static Object getStaticField(Class<?> type, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void invokeIfPresent(Object target, String name) {
        try {
            Method method = findMethod(target.getClass(), name);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
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

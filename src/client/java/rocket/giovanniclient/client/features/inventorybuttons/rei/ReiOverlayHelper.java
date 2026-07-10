package rocket.giovanniclient.client.features.inventorybuttons.rei;

import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.overlay.OverlayListWidget;
import me.shedaniel.rei.api.common.entry.EntryStack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public final class ReiOverlayHelper {
    private ReiOverlayHelper() {}

    public static boolean isMouseOverEntryList(double mouseX, double mouseY) {
        try {
            REIRuntime runtime = REIRuntime.getInstance();
            if (!runtime.isOverlayVisible()) {
                return false;
            }

            Point point = new Point(mouseX, mouseY);
            return runtime.getOverlay()
                    .map(overlay -> overlay.getEntryList().containsMouse(point)
                            || overlay.getFavoritesList()
                            .map(favorites -> favorites.containsMouse(point))
                            .orElse(false))
                    .orElse(false);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Optional<EntryStack<?>> getHoveredEntryStack(double mouseX, double mouseY) {
        try {
            REIRuntime runtime = REIRuntime.getInstance();
            if (!runtime.isOverlayVisible()) {
                return Optional.empty();
            }

            Point point = new Point(mouseX, mouseY);
            return runtime.getOverlay()
                    .flatMap(overlay -> {
                        Optional<OverlayListWidget> hoveredList;
                        if (overlay.getEntryList().containsMouse(point)) {
                            hoveredList = Optional.of(overlay.getEntryList());
                        } else {
                            hoveredList = overlay.getFavoritesList()
                                    .filter(favorites -> favorites.containsMouse(point));
                        }

                        return hoveredList.flatMap(list -> getStackAt(list, mouseX, mouseY));
                    });
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private static Optional<EntryStack<?>> getStackAt(OverlayListWidget list, double mouseX, double mouseY) {
        Optional<EntryStack<?>> directHit = getStackFromEntryWidgets(list, mouseX, mouseY);
        if (directHit.isPresent()) {
            return directHit;
        }

        EntryStack<?> focusedStack = list.getFocusedStack();
        if (focusedStack == null || focusedStack.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(focusedStack.copy());
    }

    private static Optional<EntryStack<?>> getStackFromEntryWidgets(OverlayListWidget list, double mouseX, double mouseY) {
        try {
            Method getEntryWidgets = findMethod(list.getClass(), "getEntryWidgets");
            getEntryWidgets.setAccessible(true);

            Object widgetsObject = getEntryWidgets.invoke(list);
            if (!(widgetsObject instanceof List<?> widgets)) {
                return Optional.empty();
            }

            for (Object widget : widgets) {
                Method containsMouse = findMethod(widget.getClass(), "containsMouse", double.class, double.class);
                containsMouse.setAccessible(true);
                if (!Boolean.TRUE.equals(containsMouse.invoke(widget, mouseX, mouseY))) {
                    continue;
                }

                Method getCurrentEntry = findMethod(widget.getClass(), "getCurrentEntry");
                getCurrentEntry.setAccessible(true);
                Object entry = getCurrentEntry.invoke(widget);
                if (entry instanceof EntryStack<?> stack && !stack.isEmpty()) {
                    return Optional.of(stack.copy());
                }
            }
        } catch (Throwable ignored) {
        }

        return Optional.empty();
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
}

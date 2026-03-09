package rocket.giovanniclient.client.compat;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ReiIconDropSupport {
    private ReiIconDropSupport() {}

    private static final boolean DEBUG = true;

    public static void attachIfPresent(
            InventoryScreen screen,
            Supplier<int[]> iconFieldBoundsSupplier,
            Consumer<ItemStack> onDrop
    ) {
        try {
            Class<?> screenRegistryClz = Class.forName("me.shedaniel.rei.api.client.registry.screen.ScreenRegistry");
            Object screenRegistry = screenRegistryClz.getMethod("getInstance").invoke(null);

            Class<?> visitorClz = Class.forName("me.shedaniel.rei.api.client.gui.drag.component.DraggableComponentVisitor");

            Object visitor = Proxy.newProxyInstance(
                    ReiIconDropSupport.class.getClassLoader(),
                    new Class<?>[]{visitorClz},
                    (proxy, method, args) -> handle(method, args, screen, iconFieldBoundsSupplier, onDrop)
            );

            screenRegistryClz.getMethod("registerDraggableComponentVisitor", visitorClz).invoke(screenRegistry, visitor);
        } catch (Throwable ignored) {
        }
    }

    private static Object handle(
            Method method,
            Object[] args,
            InventoryScreen ourScreen,
            Supplier<int[]> boundsSupplier,
            Consumer<ItemStack> onDrop
    ) throws Throwable {
        String name = method.getName();

        if ("compareTo".equals(name) && args != null && args.length == 1) return 0;
        if ("getPriority".equals(name) && (args == null || args.length == 0)) return 50.0;

        if ("isHandingScreen".equals(name) && args != null && args.length == 1) {
            return args[0] == ourScreen;
        }

        if ("getDraggableAcceptingBounds".equals(name) && args != null && args.length == 2) {
            int[] b = safeBounds(boundsSupplier);
            Object rect = newReiRectangle(b[0], b[1], b[2], b[3]);

            Class<?> boundsProviderClz = Class.forName("me.shedaniel.rei.api.client.gui.drag.DraggableBoundsProvider");
            Method ofRectangle = boundsProviderClz.getMethod("ofRectangle", Class.forName("me.shedaniel.math.Rectangle"));
            Object provider = ofRectangle.invoke(null, rect);

            return Stream.of(provider);
        }

        if ("acceptDragged".equals(name) && args != null && args.length == 2) {
            Object context = args[0];
            Object component = args[1];

            if (!isMouseInsideBounds(context, boundsSupplier)) return draggedResult("PASS");

            ItemStack stack = resolveItemStackFromComponent(component);

            if (DEBUG) {
                System.out.println("[GiovanniClient] REI drop component=" + (component == null ? "null" : component.getClass().getName())
                        + " resolved=" + (stack == null ? "null" : stack.getItem().toString())
                        + " empty=" + (stack == null || stack.isEmpty()));
            }

            if (stack == null || stack.isEmpty()) return draggedResult("PASS");

            onDrop.accept(stack.copy());
            return draggedResult("CONSUMED");
        }

        // NON tornare mai null su DraggedAcceptorResult
        if ("me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult".equals(method.getReturnType().getName())) {
            return draggedResult("PASS");
        }

        Class<?> rt = method.getReturnType();
        if (rt == boolean.class) return false;
        if (rt == int.class) return 0;
        if (rt == double.class) return 0.0;
        if ("java.util.stream.Stream".equals(rt.getName())) return Stream.empty();
        return null;
    }

    private static boolean isMouseInsideBounds(Object draggingContext, Supplier<int[]> boundsSupplier) {
        try {
            Method getCurrentPosition = draggingContext.getClass().getMethod("getCurrentPosition");
            Object point = getCurrentPosition.invoke(draggingContext);

            int mx = (int) point.getClass().getField("x").get(point);
            int my = (int) point.getClass().getField("y").get(point);

            int[] b = safeBounds(boundsSupplier);
            int x = b[0], y = b[1], w = b[2], h = b[3];
            return mx >= x && mx < (x + w) && my >= y && my < (y + h);
        } catch (Throwable t) {
            return false;
        }
    }

    private static int[] safeBounds(Supplier<int[]> boundsSupplier) {
        int[] b = boundsSupplier.get();
        if (b == null || b.length < 4) return new int[]{0, 0, 0, 0};
        return new int[]{b[0], b[1], b[2], b[3]};
    }

    private static Object newReiRectangle(int x, int y, int w, int h) throws Exception {
        Class<?> rectClz = Class.forName("me.shedaniel.math.Rectangle");
        return rectClz.getConstructor(int.class, int.class, int.class, int.class).newInstance(x, y, w, h);
    }

    private static Object draggedResult(String enumName) throws Exception {
        Class<?> resClz = Class.forName("me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult");
        @SuppressWarnings("unchecked")
        Object val = Enum.valueOf((Class<? extends Enum>) resClz.asSubclass(Enum.class), enumName);
        return val;
    }

    private static ItemStack resolveItemStackFromComponent(Object component) {
        if (component == null) return null;

        // 1) prova: getStack()
        Object entryStack = tryInvokeNoArgs(component, "getStack");
        ItemStack s = resolveItemStackFromEntryStack(entryStack);
        if (s != null) return s;

        // 2) prova: getEntryStack(), entryStack(), stack()
        entryStack = tryInvokeNoArgs(component, "getEntryStack");
        s = resolveItemStackFromEntryStack(entryStack);
        if (s != null) return s;

        entryStack = tryInvokeNoArgs(component, "entryStack");
        s = resolveItemStackFromEntryStack(entryStack);
        if (s != null) return s;

        entryStack = tryInvokeNoArgs(component, "stack");
        s = resolveItemStackFromEntryStack(entryStack);
        if (s != null) return s;

        // 3) prova: Supplier#get()
        Object supplied = tryInvokeNoArgs(component, "get");
        s = resolveItemStackFromEntryStack(supplied);
        if (s != null) return s;

        // 4) ultima spiaggia: cerca un metodo no-args che ritorna EntryStack
        try {
            for (Method m : component.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                Class<?> rt = m.getReturnType();
                if (rt != null && rt.getName().equals("me.shedaniel.rei.api.common.entry.EntryStack")) {
                    Object v = m.invoke(component);
                    s = resolveItemStackFromEntryStack(v);
                    if (s != null) return s;
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private static Object tryInvokeNoArgs(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ItemStack resolveItemStackFromEntryStack(Object entryStack) {
        if (entryStack == null) return null;

        // già ItemStack?
        if (entryStack instanceof ItemStack is) return is;

        // REI EntryStack compat: type+castValue + cheatsAs()
        try {
            Class<?> entryStackClz = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");

            if (!entryStackClz.isInstance(entryStack)) return null;

            Object type = entryStackClz.getMethod("getType").invoke(entryStack);
            Class<?> vanillaTypesClz = Class.forName("me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes");
            Object itemType = vanillaTypesClz.getField("ITEM").get(null);

            Method castValue = entryStackClz.getMethod("castValue");

            if (itemType != null && itemType.equals(type)) {
                Object v = castValue.invoke(entryStack);
                return (v instanceof ItemStack is) ? is : null;
            }

            // custom: prova cheatsAs() -> EntryStack<ItemStack>
            Method cheatsAs = entryStackClz.getMethod("cheatsAs");
            Object cheated = cheatsAs.invoke(entryStack);
            if (cheated == null) return null;

            boolean empty = (boolean) entryStackClz.getMethod("isEmpty").invoke(cheated);
            if (empty) return null;

            Object v = castValue.invoke(cheated);
            return (v instanceof ItemStack is) ? is : null;

        } catch (Throwable ignored) {}

        // fallback: alcuni fork hanno toStack()/asItemStack()
        try {
            Method toStack = entryStack.getClass().getMethod("toStack");
            if (ItemStack.class.isAssignableFrom(toStack.getReturnType())) {
                return (ItemStack) toStack.invoke(entryStack);
            }
        } catch (Throwable ignored) {}

        try {
            Method asItemStack = entryStack.getClass().getMethod("asItemStack");
            if (ItemStack.class.isAssignableFrom(asItemStack.getReturnType())) {
                return (ItemStack) asItemStack.invoke(entryStack);
            }
        } catch (Throwable ignored) {}

        return null;
    }
}

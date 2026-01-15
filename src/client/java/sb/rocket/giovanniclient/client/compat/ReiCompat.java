package sb.rocket.giovanniclient.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReiCompat {
    private ReiCompat() {}

    public static boolean isReiLoaded() {
        return FabricLoader.getInstance().isModLoaded("roughlyenoughitems");
    }

    /**
     * Ritorna stacks dal pannello items di REI (include mod), oppure lista vuota se REI assente/errore.
     * Implementazione via reflection per evitare NoClassDefFoundError e mismatch API.
     */
    public static List<ItemStack> tryGetAllItemStacksFromRei() {
        if (!isReiLoaded()) return List.of();

        try {
            Object entryRegistry = resolveEntryRegistry();
            if (entryRegistry == null) return List.of();

            List<?> entryStacks = invokeFirstListMethod(entryRegistry,
                    "getEntryStacks",      // più comune
                    "getEntryStack",       // raro
                    "getEntries",          // fallback
                    "getAll",              // fallback
                    "getAllEntries"        // fallback
            );
            if (entryStacks == null || entryStacks.isEmpty()) return List.of();

            List<ItemStack> out = new ArrayList<>(entryStacks.size());
            for (Object entryStack : entryStacks) {
                ItemStack stack = extractItemStack(entryStack);
                if (stack != null && !stack.isEmpty()) out.add(stack.copy());
            }
            return out;
        } catch (Throwable t) {
            return List.of();
        }
    }

    // -----------------------
    // Internals (reflection)
    // -----------------------

    /**
     * Prova a risolvere l'EntryRegistry client. REI ha cambiato spesso la "entry point",
     * quindi tentiamo più percorsi.
     */
    private static Object resolveEntryRegistry() throws Throwable {
        // 1) me.shedaniel.rei.api.client.registry.entry.EntryRegistry.getInstance()
        Object reg = tryStaticGetInstance("me.shedaniel.rei.api.client.registry.entry.EntryRegistry",
                "getInstance", "getInstanceOrNull");
        if (reg != null) return reg;

        // 2) me.shedaniel.rei.api.client.ClientHelper.getInstance() -> getEntryRegistry()
        Object helper = tryStaticGetInstance("me.shedaniel.rei.api.client.ClientHelper",
                "getInstance");
        if (helper != null) {
            Object maybe = tryInvokeNoArgs(helper, "getEntryRegistry", "entryRegistry", "getRegistry");
            if (maybe != null) return maybe;
        }

        // 3) me.shedaniel.rei.impl.ClientInternals.getEntryRegistry() / getClientEntryRegistry()
        Object internals = tryStaticGetInstance("me.shedaniel.rei.impl.ClientInternals",
                "getEntryRegistry", "getClientEntryRegistry", "getRegistry");
        if (internals != null) return internals;

        return null;
    }

    private static Object tryStaticGetInstance(String className, String... methodNames) {
        try {
            Class<?> c = Class.forName(className);
            for (String m : methodNames) {
                try {
                    Method mm = c.getDeclaredMethod(m);
                    mm.setAccessible(true);
                    return mm.invoke(null);
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Object tryInvokeNoArgs(Object target, String... methodNames) {
        try {
            Class<?> c = target.getClass();
            for (String m : methodNames) {
                try {
                    Method mm = c.getMethod(m);
                    mm.setAccessible(true);
                    return mm.invoke(target);
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static List<?> invokeFirstListMethod(Object target, String... methodNames) {
        Class<?> c = target.getClass();
        for (String m : methodNames) {
            try {
                Method mm = c.getMethod(m);
                mm.setAccessible(true);
                Object res = mm.invoke(target);
                if (res instanceof List<?> list) return list;
            } catch (Throwable ignored) {}
        }
        return Collections.emptyList();
    }

    /**
     * EntryStack<?> in REI ha quasi sempre:
     * - getValue() -> può essere ItemStack
     * - oppure cast/toItemStack()
     */
    private static ItemStack extractItemStack(Object entryStack) {
        if (entryStack == null) return null;

        // 1) getValue()
        try {
            Method getValue = entryStack.getClass().getMethod("getValue");
            getValue.setAccessible(true);
            Object v = getValue.invoke(entryStack);
            if (v instanceof ItemStack s) return s;
        } catch (Throwable ignored) {}

        // 2) castValue()
        try {
            Method castValue = entryStack.getClass().getMethod("castValue");
            castValue.setAccessible(true);
            Object v = castValue.invoke(entryStack);
            if (v instanceof ItemStack s) return s;
        } catch (Throwable ignored) {}

        // 3) toItemStack()
        try {
            Method toItemStack = entryStack.getClass().getMethod("toItemStack");
            toItemStack.setAccessible(true);
            Object v = toItemStack.invoke(entryStack);
            if (v instanceof ItemStack s) return s;
        } catch (Throwable ignored) {}

        return null;
    }
}

package rocket.giovanniclient.client.features.autosolvers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.InventoryUtils;
import rocket.giovanniclient.client.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static rocket.giovanniclient.client.util.InventoryUtils.clickSlot;

public class AutoExperiments extends AbstractFeature {

    private enum ExperimentType {
        CHRONOMATRON,
        ULTRASEQUENCER,
        SUPERPAIRS, // not implemented
        END,
        NONE
    }

    private static final int START_DELAY_MIN = 234;
    private static final int START_DELAY_MAX = 678;
    private static final int END_DELAY_MIN = 777;
    private static final int END_DELAY_MAX = 3333;

    private final AutoSolversConfig cfg = ConfigManager.getConfig().asc;
    private final Random rng = new Random();

    private ExperimentType currentExperiment = ExperimentType.NONE;
    private final ArrayList<Integer> chronomatronOrder = new ArrayList<>(28);
    private final Map<Integer, Integer> ultrasequencerOrder = new HashMap<>();

    private int lastAdded = 0, clicks = 0;
    private long startDelay = -1, endDelay = -1, clickDelay = -1;
    private boolean sequenceAdded = false;

    private int tick_counter = 0;

    @Override
    public void onScreenOpen(Screen screen) {
        if (!cfg.autoExperimentsAccordion.AUTOEXPERIMENTS_TOGGLE) {
            clearAll();
            return;
        }

        if (!(screen instanceof ContainerScreen)) {
            clearAll();
            return;
        }

        String title = screen.getTitle().getString();
        if (title.startsWith("Chronomatron (")) {
            Utils.debug("Chronomatron detected");
            currentExperiment = ExperimentType.CHRONOMATRON;
        } else if (title.startsWith("Ultrasequencer (")) {
            Utils.debug("Ultrasequencer detected");
            currentExperiment = ExperimentType.ULTRASEQUENCER;
        } else if (title.contains("Over")) {
            Utils.debug("Experiment over.");
            currentExperiment = ExperimentType.END;
        } else {
            clearAll();
        }
    }

    @Override
    public void onTick(Minecraft client) {
        if (!cfg.autoExperimentsAccordion.AUTOEXPERIMENTS_TOGGLE ||
                currentExperiment == ExperimentType.NONE ||
                client.player == null) {
            return;
        }

        if (!(client.screen instanceof ContainerScreen)) {
            clearAll();
            return;
        }

        AbstractContainerMenu handler = client.player.containerMenu;
        if (!hasRequiredSlots(handler, 49)) {
            clearAll();
            return;
        }
        long now = System.currentTimeMillis();

        if (startDelay == -1) {
            startDelay = now + randomDelay(START_DELAY_MIN, START_DELAY_MAX);
            Utils.debug("Start delay: " + (startDelay - now));
        }

        if (now < startDelay) return;

        switch (currentExperiment) {
            case CHRONOMATRON -> tickChrono(client, handler, now);
            case ULTRASEQUENCER -> tickUltra(client, handler, now);
            case END -> tickEnd(client, now);
            default -> {}
        }
    }

    private void tickEnd(Minecraft client, long now) {
        if (endDelay == -1) {
            endDelay = now + randomDelay(END_DELAY_MIN, END_DELAY_MAX);
            Utils.debug("End delay: " + (endDelay - now) + "ms");
        }

        if (now > endDelay && cfg.autoExperimentsAccordion.AUTOEXPERIMENTS_AUTOQUIT) {
            client.player.closeContainer();
            clearAll();
        }
    }

    private void tickChrono(Minecraft client, AbstractContainerMenu handler, long now) {
        if (!hasRequiredSlots(handler, 49)) {
            clearAll();
            return;
        }
        ItemStack flag = handler.slots.get(49).getItem();
        tick_counter++;
        if (tick_counter % 9 == 0) Utils.debug("Flag Slot: " + flag.toString());
        NonNullList<Slot> container = handler.slots;

        if (flag.is(Items.GLOWSTONE) &&
                !container.get(lastAdded).getItem().isEnchanted()) {
            sequenceAdded = false;
            if (chronomatronOrder.size() > (11 - cfg.autoExperimentsAccordion.METAPHYSICAL_SERUM.toInt())) {
                client.player.closeContainer();
            }
        }

        if (!sequenceAdded && flag.is(Items.CLOCK)) {
            for (int i = 10; i <= 43; i++) {
                ItemStack stack = container.get(i).getItem();
                if (!stack.isEmpty() && stack.toString().contains("terracotta")) {
                    chronomatronOrder.add(i);
                    Utils.debug("Added terracotta slot: " + i);
                    lastAdded = i;
                    sequenceAdded = true;
                    clicks = 0;
                    break;
                }
            }

            if (!sequenceAdded) {
                Utils.debug("No terracotta items found.");
                sequenceAdded = true;
            }
        }

        if (sequenceAdded && flag.is(Items.CLOCK) &&
                chronomatronOrder.size() > clicks) {

            if (clickDelay == -1) {
                clickDelay = now + randomDelay(
                        cfg.autoExperimentsAccordion.delays.MIN,
                        cfg.autoExperimentsAccordion.delays.MAX
                );
                Utils.debug("Chrono Click " + (clicks + 1) + " in " + (clickDelay - now) + "ms");
            }

            if (now > clickDelay) {
                clickSlot(client, handler, chronomatronOrder.get(clicks), InventoryUtils.MouseButton.MIDDLE, ContainerInput.CLONE);
                clicks++;
                clickDelay = -1;
            }
        }
    }

    private void tickUltra(Minecraft client, AbstractContainerMenu handler, long now) {
        if (!hasRequiredSlots(handler, 49)) {
            clearAll();
            return;
        }
        ItemStack flag = handler.slots.get(49).getItem();
        NonNullList<Slot> container = handler.slots;

        if (flag.is(Items.CLOCK)) {
            sequenceAdded = false;
        }

        if (!sequenceAdded && flag.is(Items.GLOWSTONE)) {
            if (!container.get(44).hasItem()) return;

            ultrasequencerOrder.clear();

            for (int i = 9; i <= 44; i++) {
                ItemStack stack = container.get(i).getItem();
                Item item = stack.getItem();
                if (item instanceof DyeItem || item == Items.BONE_MEAL || item == Items.INK_SAC ||
                        item == Items.LAPIS_LAZULI || item == Items.COCOA_BEANS) {
                    ultrasequencerOrder.put(stack.getCount() - 1, i);
                }
            }

            sequenceAdded = true;
            clicks = 0;
        }

        if (flag.is(Items.CLOCK) && ultrasequencerOrder.containsKey(clicks)) {
            if (clickDelay == -1) {
                clickDelay = now + randomDelay(
                        cfg.autoExperimentsAccordion.delays.MIN,
                        cfg.autoExperimentsAccordion.delays.MAX
                );
                Utils.debug("Ultra Click " + (clicks + 1) + " in " + (clickDelay - now) + "ms");
            }

            if (now > clickDelay) {
                if (ultrasequencerOrder.size() > (9 - cfg.autoExperimentsAccordion.METAPHYSICAL_SERUM.toInt())) {
                    client.player.closeContainer();
                }

                Integer slot = ultrasequencerOrder.get(clicks);
                if (slot != null) {
                    clickSlot(client, handler, slot, InventoryUtils.MouseButton.MIDDLE, ContainerInput.CLONE);
                    clicks++;
                    clickDelay = -1;
                }
            }
        }
    }

    private void clearAll() {
        currentExperiment = ExperimentType.NONE;
        chronomatronOrder.clear();
        ultrasequencerOrder.clear();
        sequenceAdded = false;
        lastAdded = 0;
        clickDelay = -1;
        endDelay = -1;
        startDelay = -1;
    }

    private boolean hasRequiredSlots(AbstractContainerMenu handler, int highestRequiredSlot) {
        return handler != null && handler.slots.size() > highestRequiredSlot;
    }

    private int randomDelay(int configuredMin, int configuredMax) {
        int min = Math.max(0, configuredMin);
        int max = Math.max(min, configuredMax);
        return min + rng.nextInt(max - min + 1);
    }
}

package rocket.giovanniclient.client.features.autosolvers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
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

    private final int START_DELAY_MIN = 234;
    private final int START_DELAY_MAX = 678;
    private final int END_DELAY_MIN = 777;
    private final int END_DELAY_MAX = 3333;

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

        if (!(screen instanceof GenericContainerScreen)) {
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
    public void onTick(MinecraftClient client) {
        if (!cfg.autoExperimentsAccordion.AUTOEXPERIMENTS_TOGGLE ||
                currentExperiment == ExperimentType.NONE ||
                client.player == null) {
            return;
        }

        if (!(client.currentScreen instanceof GenericContainerScreen)) {
            clearAll();
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        ItemStack center = handler.slots.get(49).getStack();
        long now = System.currentTimeMillis();

        if (startDelay == -1) {
            startDelay = now + rng.nextInt(START_DELAY_MAX - START_DELAY_MIN) + START_DELAY_MIN;
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

    private void tickEnd(MinecraftClient client, long now) {
        if (endDelay == -1) {
            endDelay = now + rng.nextInt(END_DELAY_MAX - END_DELAY_MIN) + END_DELAY_MIN;
            Utils.debug("End delay: " + (endDelay - now) + "ms");
        }

        if (now > endDelay && cfg.autoExperimentsAccordion.AUTOEXPERIMENTS_AUTOQUIT) {
            client.player.closeHandledScreen();
            clearAll();
        }
    }

    private void tickChrono(MinecraftClient client, ScreenHandler handler, long now) {
        ItemStack flag = handler.slots.get(49).getStack();
        tick_counter++;
        if (tick_counter % 9 == 0) Utils.debug("Flag Slot: " + flag.toString());
        DefaultedList<Slot> container = handler.slots;

        if (flag.isOf(Items.GLOWSTONE) &&
                !container.get(lastAdded).getStack().hasGlint()) {
            sequenceAdded = false;
            if (chronomatronOrder.size() > (11 - cfg.autoExperimentsAccordion.METAPHYSICAL_SERUM.toInt())) {
                client.player.closeHandledScreen();
            }
        }

        if (!sequenceAdded && flag.isOf(Items.CLOCK)) {
            for (int i = 10; i <= 43; i++) {
                ItemStack stack = container.get(i).getStack();
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

        if (sequenceAdded && flag.isOf(Items.CLOCK) &&
                chronomatronOrder.size() > clicks) {

            if (clickDelay == -1) {

                clickDelay = now + rng.nextInt(cfg.autoExperimentsAccordion.delays.MAX - cfg.autoExperimentsAccordion.delays.MIN) + cfg.autoExperimentsAccordion.delays.MIN;

                int min = cfg.autoExperimentsAccordion.delays.MIN;
                int max = cfg.autoExperimentsAccordion.delays.MAX;
                int bound = Math.max(1, max - min);

                clickDelay = now + rng.nextInt(bound) + min;                Utils.debug("Chrono Click " + (clicks + 1) + " in " + (clickDelay - now) + "ms");
            }

            if (now > clickDelay) {
                clickSlot(client, handler, chronomatronOrder.get(clicks), InventoryUtils.MouseButton.MIDDLE, SlotActionType.CLONE);
                clicks++;
                clickDelay = -1;
            }
        }
    }

    private void tickUltra(MinecraftClient client, ScreenHandler handler, long now) {
        ItemStack flag = handler.slots.get(49).getStack();
        DefaultedList<Slot> container = handler.slots;

        if (flag.isOf(Items.CLOCK)) {
            sequenceAdded = false;
        }

        if (!sequenceAdded && flag.isOf(Items.GLOWSTONE)) {
            if (!container.get(44).hasStack()) return;

            ultrasequencerOrder.clear();

            for (int i = 9; i <= 44; i++) {
                ItemStack stack = container.get(i).getStack();
                Item item = stack.getItem();
                if (item instanceof DyeItem || item == Items.BONE_MEAL || item == Items.INK_SAC ||
                        item == Items.LAPIS_LAZULI || item == Items.COCOA_BEANS) {
                    ultrasequencerOrder.put(stack.getCount() - 1, i);
                }
            }

            sequenceAdded = true;
            clicks = 0;
        }

        if (flag.isOf(Items.CLOCK) && ultrasequencerOrder.containsKey(clicks)) {
            if (clickDelay == -1) {
                clickDelay = now + rng.nextInt(cfg.autoExperimentsAccordion.delays.MAX - cfg.autoExperimentsAccordion.delays.MIN) + cfg.autoExperimentsAccordion.delays.MIN;
                Utils.debug("Ultra Click " + (clicks + 1) + " in " + (clickDelay - now) + "ms");
            }

            if (now > clickDelay) {
                if (ultrasequencerOrder.size() > (9 - cfg.autoExperimentsAccordion.METAPHYSICAL_SERUM.toInt())) {
                    client.player.closeHandledScreen();
                }

                Integer slot = ultrasequencerOrder.get(clicks);
                if (slot != null) {
                    clickSlot(client, handler, slot, InventoryUtils.MouseButton.MIDDLE, SlotActionType.CLONE);
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
}

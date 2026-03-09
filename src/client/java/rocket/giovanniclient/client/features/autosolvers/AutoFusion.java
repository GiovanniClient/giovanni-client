package rocket.giovanniclient.client.features.autosolvers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.InventoryUtils;
import rocket.giovanniclient.client.util.Utils;

import java.util.Random;

import static rocket.giovanniclient.client.util.InventoryUtils.clickSlot;

public class AutoFusion extends AbstractFeature {
    private final AutoSolversConfig cfg = ConfigManager.getConfig().asc;
    private final Random rng = new Random();

    private enum State {
        FUSION_BOX,
        CONFIRM_FUSION,
        NONE
    }

    private State currentState = State.NONE;
    private long clickDelay = -1;

    @Override
    public void onScreenOpen(Screen screen) {
        if (!cfg.autoFusionAccordtion.AUTOFUSION)
            return;

        if (!(screen instanceof GenericContainerScreen)) {
            currentState = State.NONE;
            return;
        }

        String name = screen.getTitle().getString();
        if (name.contains("Fusion Box")) {
            Utils.debug("Fusion Box detected");
            currentState = State.FUSION_BOX;
        } else if (name.contains("Confirm Fusion")) {
            Utils.debug("Confirm Fusion detected");
            currentState = State.CONFIRM_FUSION;
        } else {
            currentState = State.NONE;
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (!cfg.autoFusionAccordtion.AUTOFUSION
                || currentState == State.NONE
                || client.player == null
                || !(client.currentScreen instanceof GenericContainerScreen)) {
            currentState = State.NONE;
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        long now = System.currentTimeMillis();

        switch (currentState) {
            case FUSION_BOX -> tryClick(client, handler, 47, "minecraft:player_head", now);
            case CONFIRM_FUSION -> tryClick(client, handler, 33, "minecraft:lime_terracotta", now);
            default -> currentState = State.NONE;
        }
    }

    private void tryClick(MinecraftClient client,
                          ScreenHandler handler,
                          int slot,
                          String expectedItemId,
                          long now) {

        Slot s = handler.slots.get(slot);
        String itemId = s.getStack().getItem().toString();

        if (itemId.equals(expectedItemId)) {
            if (clickDelay == -1) {
                clickDelay = now +
                        rng.nextInt(cfg.autoFusionAccordtion.AUTOFUSION_CLICK_DELAY_MAX -
                                cfg.autoFusionAccordtion.AUTOFUSION_CLICK_DELAY_MIN) +
                        cfg.autoFusionAccordtion.AUTOFUSION_CLICK_DELAY_MIN;

                Utils.debug("Click delay: " + (clickDelay - now) + "ms");
            }

            if (now > clickDelay) {
                clickSlot(client, handler, slot, InventoryUtils.MouseButton.LEFT, SlotActionType.PICKUP);
                clickDelay = -1;
            }
        }
    }
}

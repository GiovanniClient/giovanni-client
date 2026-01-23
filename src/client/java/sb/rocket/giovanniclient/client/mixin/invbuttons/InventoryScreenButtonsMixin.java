package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.features.inventorybuttons.*;

import java.util.function.Supplier;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenButtonsMixin {

    @Unique private TextFieldWidget giovanni$commandField;
    @Unique private TextFieldWidget giovanni$iconField;

    @Unique private String giovanni$lastSelectedSlot = null;
    @Unique private boolean giovanni$suppressSave = false;

    @Unique private boolean giovanni$dirty = false;
    @Unique private long giovanni$lastEditMs = 0L;
    @Unique private static final long SAVE_DEBOUNCE_MS = 250L;

    @Unique private static final int PANEL_W = 230;
    @Unique private static final int PANEL_H = 120;
    @Unique private static final int FIELD_H = 18;
    @Unique private static final int SLASH_PAD = 10;

    @Unique private boolean giovanni$reiDropAttached = false;

    @Inject(method = "init", at = @At("HEAD"))
    private void giovanni$onInitHead(CallbackInfo ci) {
        giovanni$commandField = null;
        giovanni$iconField = null;

        giovanni$lastSelectedSlot = null;
        giovanni$suppressSave = false;

        giovanni$dirty = false;
        giovanni$lastEditMs = 0L;

        giovanni$reiDropAttached = false;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void giovanni$onInitTail(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        if (self.getScreenHandler() == null) return;

        giovanni$addButtons(self);

        if (UiButtonsConfigManager.isEditMode()) {
            giovanni$ensureWidgets(self);
        }
    }

    // Salva su tick (NON render)
    @Inject(method = "handledScreenTick", at = @At("TAIL"))
    private void giovanni$onTick(CallbackInfo ci) {
        if (!UiButtonsConfigManager.isEditMode()) return;
        giovanni$flushSaveIfDue();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void giovanni$onRender(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!UiButtonsConfigManager.isEditMode()) return;

        InventoryScreen self = (InventoryScreen) (Object) this;

        giovanni$ensureWidgets(self);
        if (giovanni$commandField == null || giovanni$iconField == null) return;

        String current = UiButtonsConfigManager.getSelectedSlot();
        if (giovanni$lastSelectedSlot == null || !giovanni$lastSelectedSlot.equalsIgnoreCase(current)) {
            giovanni$lastSelectedSlot = current;
            giovanni$loadFields();
        }

        int[] p = giovanni$panelPos(self);
        int px = p[0], py = p[1] - 16;

        // Panel background
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xAA000000);

        // Title + labels
        ctx.drawText(self.getTextRenderer(), "Inventory Button Editor", px + 10, py + 8, 0xFFFFFFFF, false);
        ctx.drawText(self.getTextRenderer(), "Slot: " + UiButtonsConfigManager.getSelectedSlot(), px + 10, py + 24, 0xFFCCCCCC, false);

        ctx.drawText(self.getTextRenderer(), "Command:", px + 10, py + 40, 0xFFCCCCCC, false);
        ctx.drawText(self.getTextRenderer(), "Icon:", px + 10, py + 72, 0xFFCCCCCC, false);

        // Hint for icon field (user-friendly)
        ctx.drawText(self.getTextRenderer(), "Tip: drag an item from REI into the icon box.", px + 10, py + 104, 0xFF888888, false);

        // Slash for command
        int sx = giovanni$commandField.getX() - 8;
        int sy = giovanni$commandField.getY() + (FIELD_H - self.getTextRenderer().fontHeight) / 2;
        ctx.drawText(self.getTextRenderer(), "/", sx + 1, sy + 1, 0xFFFFFFFF, false);
    }

    @Unique
    private void giovanni$addButtons(InventoryScreen self) {
        int guiX = ((HandledScreenAccessor) self).giovanni$getX();
        int guiY = ((HandledScreenAccessor) self).giovanni$getY();

        boolean edit = UiButtonsConfigManager.isEditMode();

        if (edit) {
            for (InventoryButtonLayout slot : InventoryButtonLayout.all()) {
                UiButtonDef def = UiButtonsConfigManager.getOrCreateInventoryDef(slot.id());

                if (def.id == null || def.id.isBlank()) def.id = "inv_" + slot.id();
                def.screen = "inventory";
                def.slot = slot.id();
                def.visible = true;
                def.enabled = true;
                def.w = 18;
                def.h = 18;

                int x = guiX + slot.relX();
                int y = guiY + slot.relY();

                NeuButtonWidget w = new NeuButtonWidget(x, y, def, true);
                ((ScreenInvoker) self).giovanni$addDrawableChild(w);
            }
            return;
        }

        UiButtonsJsonConfig cfg = UiButtonsConfigManager.get();
        for (UiButtonDef def : cfg.buttons) {
            if (def == null) continue;
            if (!"inventory".equalsIgnoreCase(def.screen)) continue;
            if (!def.visible) continue;
            if (def.command == null || def.command.isBlank()) continue;

            InventoryButtonLayout slot = InventoryButtonLayout.fromId(def.slot);
            if (slot == null) continue;

            int x = guiX + slot.relX();
            int y = guiY + slot.relY();

            NeuButtonWidget w = new NeuButtonWidget(x, y, def, false);
            ((ScreenInvoker) self).giovanni$addDrawableChild(w);
        }
    }

    @Unique
    private void giovanni$ensureWidgets(InventoryScreen self) {
        if (!UiButtonsConfigManager.isEditMode()) return;
        if (giovanni$commandField != null && giovanni$iconField != null) return;

        int[] p = giovanni$panelPos(self);
        int px = p[0], py = p[1];

        giovanni$commandField = new TextFieldWidget(
                self.getTextRenderer(),
                px + 10 + SLASH_PAD, py + 50 - 16,
                PANEL_W - 20 - SLASH_PAD, FIELD_H,
                Text.literal("Command")
        );

        giovanni$iconField = new TextFieldWidget(
                self.getTextRenderer(),
                px + 10, py + 82 - 16,
                PANEL_W - 20, FIELD_H,
                Text.literal("Icon")
        );

        giovanni$commandField.setMaxLength(250);
        giovanni$iconField.setMaxLength(8192);

        giovanni$loadFields();
        giovanni$lastSelectedSlot = UiButtonsConfigManager.getSelectedSlot();

        // LIVE sync
        giovanni$commandField.setChangedListener(s -> giovanni$onFieldsChangedLive());
        giovanni$iconField.setChangedListener(s -> giovanni$onFieldsChangedLive());

        ((ScreenInvoker) self).giovanni$addDrawableChild(giovanni$commandField);
        ((ScreenInvoker) self).giovanni$addDrawableChild(giovanni$iconField);

        if (!giovanni$reiDropAttached) {
            giovanni$reiDropAttached = true;
            giovanni$tryAttachReiDrop(self);
        }
    }

    @Unique
    private void giovanni$onFieldsChangedLive() {
        if (giovanni$suppressSave) return;

        giovanni$dirty = true;
        giovanni$lastEditMs = System.currentTimeMillis();

        giovanni$syncDefFromFieldsInMemory();
    }

    @Unique
    private void giovanni$syncDefFromFieldsInMemory() {
        if (!UiButtonsConfigManager.isEditMode()) return;
        if (giovanni$commandField == null || giovanni$iconField == null) return;

        String slotId = UiButtonsConfigManager.getSelectedSlot();
        UiButtonDef def = UiButtonsConfigManager.getOrCreateInventoryDef(slotId);

        String cmd = giovanni$commandField.getText() == null ? "" : giovanni$commandField.getText().trim();
        String icon = giovanni$iconField.getText() == null ? "" : giovanni$iconField.getText().trim();

        def.command = cmd;
        def.icon = icon;
    }

    @Unique
    private void giovanni$tryAttachReiDrop(InventoryScreen self) {
        try {
            Class<?> clazz = Class.forName("sb.rocket.giovanniclient.client.compat.ReiIconDropSupport");

            // Preferred signature (with bounds): attachIfPresent(InventoryScreen, Supplier<int[]>, Consumer<ItemStack>)
            try {
                Supplier<int[]> bounds = () -> {
                    if (giovanni$iconField == null) return new int[]{0, 0, 0, 0};
                    return new int[]{
                            giovanni$iconField.getX(),
                            giovanni$iconField.getY(),
                            giovanni$iconField.getWidth(),
                            giovanni$iconField.getHeight()
                    };
                };

                clazz.getMethod("attachIfPresent", InventoryScreen.class, java.util.function.Supplier.class, java.util.function.Consumer.class)
                        .invoke(null, self, bounds, (java.util.function.Consumer<ItemStack>) this::giovanni$applyDroppedStackAsIcon);

                System.out.println("[GiovanniClient] REI drop attached (bounds)");
                return;
            } catch (Throwable ignored) {
                // fallback below
            }

            // Fallback signature: attachIfPresent(InventoryScreen, Consumer<ItemStack>)
            clazz.getMethod("attachIfPresent", InventoryScreen.class, java.util.function.Consumer.class)
                    .invoke(null, self, (java.util.function.Consumer<ItemStack>) this::giovanni$applyDroppedStackAsIcon);

            System.out.println("[GiovanniClient] REI drop attached");
        } catch (Throwable t) {
            System.out.println("[GiovanniClient] REI drop NOT attached: " + t.getClass().getSimpleName() + " " + t.getMessage());
        }
    }

    @Unique
    private void giovanni$applyDroppedStackAsIcon(ItemStack stack) {
        if (giovanni$iconField == null) return;

        String spec = IconSpec.encodeStackSpec(stack);

        giovanni$suppressSave = true;
        try {
            giovanni$iconField.setText(spec);
        } finally {
            giovanni$suppressSave = false;
        }

        String slotId = UiButtonsConfigManager.getSelectedSlot();
        UiButtonDef def = UiButtonsConfigManager.getOrCreateInventoryDef(slotId);

        def.icon = spec;

        if (giovanni$commandField != null) {
            String cmd = giovanni$commandField.getText() == null ? "" : giovanni$commandField.getText().trim();
            def.command = cmd;
        }

        UiButtonsConfigManager.save();

        giovanni$dirty = false;
        giovanni$lastEditMs = System.currentTimeMillis();
    }

    @Unique
    private void giovanni$loadFields() {
        if (giovanni$commandField == null || giovanni$iconField == null) return;

        String slotId = UiButtonsConfigManager.getSelectedSlot();
        UiButtonDef def = UiButtonsConfigManager.findInventoryDef(slotId).orElse(null);

        giovanni$suppressSave = true;
        try {
            giovanni$commandField.setText(def == null || def.command == null ? "" : def.command);
            giovanni$iconField.setText(def == null || def.icon == null ? "" : def.icon);
        } finally {
            giovanni$suppressSave = false;
        }

        UiButtonDef stable = UiButtonsConfigManager.getOrCreateInventoryDef(slotId);
        if (stable.screen == null || stable.screen.isBlank()) stable.screen = "inventory";
        stable.slot = slotId;

        giovanni$dirty = false;
    }

    @Unique
    private void giovanni$flushSaveIfDue() {
        if (!giovanni$dirty) return;
        long now = System.currentTimeMillis();
        if (now - giovanni$lastEditMs < SAVE_DEBOUNCE_MS) return;

        giovanni$dirty = false;
        giovanni$persistFromFields();
    }

    @Unique
    private void giovanni$persistFromFields() {
        if (giovanni$suppressSave) return;
        if (giovanni$commandField == null || giovanni$iconField == null) return;

        String slotId = UiButtonsConfigManager.getSelectedSlot();

        String cmd = giovanni$commandField.getText() == null ? "" : giovanni$commandField.getText().trim();
        String icon = giovanni$iconField.getText() == null ? "" : giovanni$iconField.getText().trim();

        if (cmd.isEmpty() && icon.isEmpty()) {
            boolean removed = UiButtonsConfigManager.removeInventoryDef(slotId);
            if (removed) UiButtonsConfigManager.save();
            return;
        }

        // If command present but icon empty -> default
        if (!cmd.isEmpty() && icon.isEmpty()) {
            icon = IconSpec.DEFAULT_ITEM_ID;
            giovanni$suppressSave = true;
            try {
                giovanni$iconField.setText(icon);
            } finally {
                giovanni$suppressSave = false;
            }
        }

        UiButtonDef def = UiButtonsConfigManager.getOrCreateInventoryDef(slotId);
        def.command = cmd;
        def.icon = icon;
        def.screen = "inventory";
        def.slot = slotId;
        def.visible = true;
        def.enabled = true;
        def.w = 18;
        def.h = 18;

        UiButtonsConfigManager.save();
    }

    @Unique
    private static int[] giovanni$panelPos(InventoryScreen self) {
        int guiX = ((HandledScreenAccessor) self).giovanni$getX();
        int guiY = ((HandledScreenAccessor) self).giovanni$getY();
        int bgW = ((HandledScreenAccessor) self).giovanni$getBackgroundWidth();

        // Center panel above the inventory background
        int panelX = guiX + (bgW / 2) - (PANEL_W / 2);
        panelX = Math.max(8, Math.min(panelX, self.width - PANEL_W - 8));

        int panelY = guiY - PANEL_H - 8;
        panelY = Math.max(8, panelY);

        return new int[]{panelX, panelY};
    }
}

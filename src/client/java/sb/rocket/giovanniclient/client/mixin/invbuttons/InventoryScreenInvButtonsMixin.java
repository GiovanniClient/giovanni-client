package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.features.inventorybuttons.*;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenInvButtonsMixin {

    @Unique private TextFieldWidget giovanni$commandField;
    @Unique private TextFieldWidget giovanni$iconField;
    @Unique private ButtonWidget giovanni$pickIconBtn;

    @Unique private boolean giovanni$widgetsAdded = false;
    @Unique private String giovanni$lastSelectedSlot = null;
    @Unique private boolean giovanni$suppressSave = false;

    @Unique private boolean giovanni$dirty = false;
    @Unique private long giovanni$lastEditMs = 0L;
    @Unique private static final long SAVE_DEBOUNCE_MS = 250L;

    @Unique private static final int PANEL_W = 230;
    @Unique private static final int FIELD_H = 18;
    @Unique private static final int PANEL_OFFSET_X = 48;
    @Unique private static final int SLASH_PAD = 10;

    @Inject(method = "init", at = @At("TAIL"))
    private void giovanni$onInit(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        if (self.getScreenHandler() == null) return;

        giovanni$addButtons(self);

        if (UiButtonsConfigManager.isEditMode()) {
            giovanni$ensureWidgets(self);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void giovanni$onRender(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!UiButtonsConfigManager.isEditMode()) return;

        InventoryScreen self = (InventoryScreen) (Object) this;

        giovanni$ensureWidgets(self);
        if (!giovanni$widgetsAdded || giovanni$commandField == null || giovanni$iconField == null) return;

        String current = UiButtonsConfigManager.getSelectedSlot();
        if (giovanni$lastSelectedSlot == null || !giovanni$lastSelectedSlot.equalsIgnoreCase(current)) {
            giovanni$lastSelectedSlot = current;
            giovanni$loadFields();
        }

        giovanni$flushSaveIfDue();

        int[] p = giovanni$panelPos(self);
        int px = p[0], py = p[1];

        ctx.fill(px, py, px + PANEL_W, py + 170, 0xAA000000);
        ctx.drawText(self.getTextRenderer(), "Inventory Button Editor", px + 10, py + 10, 0xFFFFFFFF, false);
        ctx.drawText(self.getTextRenderer(), "Slot: " + UiButtonsConfigManager.getSelectedSlot(), px + 10, py + 28, 0xFFCCCCCC, false);
        ctx.drawText(self.getTextRenderer(), "Command:", px + 10, py + 40, 0xFFCCCCCC, false);
        ctx.drawText(self.getTextRenderer(), "Icon:", px + 10, py + 85, 0xFFCCCCCC, false);

        int sx = giovanni$commandField.getX() - 8;
        int sy = giovanni$commandField.getY() + (FIELD_H - self.getTextRenderer().fontHeight) / 2;
        ctx.drawText(self.getTextRenderer(), "/", sx, sy + 2, 0xFFFFFFFF, false);

        // Icon preview (below icon field)
        String raw = giovanni$iconField.getText() == null ? "" : giovanni$iconField.getText().trim();
        if (raw.isBlank()) raw = IconSpec.DEFAULT_TEXTURE;

        int previewX = px + 10;
        int previewY = py + 125;

        // frame
        ctx.fill(previewX - 1, previewY - 1, previewX + 18 + 1, previewY + 18 + 1, 0xFF555555);
        ctx.fill(previewX, previewY, previewX + 18, previewY + 18, 0xAA2A2A2A);
        IconSpec.renderIcon(ctx, raw, previewX + 1, previewY + 1);
        ctx.drawText(self.getTextRenderer(), "Preview", previewX + 24, previewY + 5, 0xFFCCCCCC, false);
    }

    @Unique
    private void giovanni$addButtons(InventoryScreen self) {
        int guiX = ((HandledScreenAccessor) self).giovanni$getX();
        int guiY = ((HandledScreenAccessor) self).giovanni$getY();

        boolean edit = UiButtonsConfigManager.isEditMode();

        if (edit) {
            for (InventoryButtonSlot slot : InventoryButtonSlot.all()) {
                UiButtonDef def = UiButtonsConfigManager.findInventoryDef(slot.id())
                        .orElseGet(() -> giovanni$placeholderDef(slot.id()));

                int x = guiX + slot.relX();
                int y = guiY + slot.relY();

                NeuButtonWidget w = new NeuButtonWidget(x, y, def, true);
                ((ScreenInvoker) self).giovanni$addDrawableChild(w);
            }
            return;
        }

        UiButtonsConfig cfg = UiButtonsConfigManager.get();
        for (UiButtonDef def : cfg.buttons) {
            if (def == null) continue;
            if (!"inventory".equalsIgnoreCase(def.screen)) continue;
            if (!def.visible) continue;
            if (def.command == null || def.command.isBlank()) continue;

            InventoryButtonSlot slot = InventoryButtonSlot.fromId(def.slot);
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
        if (giovanni$widgetsAdded) return;

        int[] p = giovanni$panelPos(self);
        int px = p[0], py = p[1];

        giovanni$commandField = new TextFieldWidget(
                self.getTextRenderer(),
                px + 10 + SLASH_PAD, py + 50,
                PANEL_W - 20 - SLASH_PAD, FIELD_H,
                Text.literal("Command")
        );
        giovanni$iconField = new TextFieldWidget(
                self.getTextRenderer(),
                px + 10, py + 95,
                PANEL_W - 20 - 44, FIELD_H, // leave space for Pick
                Text.literal("Icon")
        );

        giovanni$pickIconBtn = ButtonWidget.builder(Text.literal("Pick"), b -> giovanni$openIconPicker(self))
                .dimensions(px + PANEL_W - 10 - 40, py + 95, 40, FIELD_H)
                .build();

        giovanni$commandField.setMaxLength(250);
        giovanni$iconField.setMaxLength(2048);

        giovanni$loadFields();
        giovanni$lastSelectedSlot = UiButtonsConfigManager.getSelectedSlot();

        giovanni$commandField.setChangedListener(s -> giovanni$markDirty());
        giovanni$iconField.setChangedListener(s -> giovanni$markDirty());

        ((ScreenInvoker) self).giovanni$addDrawableChild(giovanni$commandField);
        ((ScreenInvoker) self).giovanni$addDrawableChild(giovanni$iconField);
        ((ScreenInvoker) self).giovanni$addDrawableChild(giovanni$pickIconBtn);

        giovanni$widgetsAdded = true;
    }

    @Unique
    private void giovanni$openIconPicker(InventoryScreen self) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        client.execute(() -> client.setScreen(new IconPickerScreen(self, picked -> {
            if (giovanni$iconField == null) return;

            giovanni$suppressSave = true;
            try {
                giovanni$iconField.setText(picked);
            } finally {
                giovanni$suppressSave = false;
            }
            giovanni$markDirty();
        })));
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

        giovanni$dirty = false;
    }

    @Unique
    private void giovanni$markDirty() {
        if (giovanni$suppressSave) return;
        giovanni$dirty = true;
        giovanni$lastEditMs = System.currentTimeMillis();
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

        String cmd = giovanni$commandField.getText().trim();
        String icon = giovanni$iconField.getText().trim();

        if (cmd.isEmpty()) {
            boolean removed = UiButtonsConfigManager.removeInventoryDef(slotId);
            if (removed) UiButtonsConfigManager.save();
            return;
        }

        // If empty icon: choose a sane default (keep your previous behavior)
        if (icon.isEmpty()) {
            icon = IconSpec.DEFAULT_TEXTURE;
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
        UiButtonsConfigManager.save();
    }

    @Unique
    private static UiButtonDef giovanni$placeholderDef(String slotId) {
        UiButtonDef d = new UiButtonDef();
        d.id = "placeholder_" + slotId;
        d.screen = "inventory";
        d.slot = slotId;
        d.command = "";
        d.icon = "";
        d.visible = true;
        d.enabled = true;
        d.w = 18;
        d.h = 18;
        return d;
    }

    @Unique
    private static int[] giovanni$panelPos(InventoryScreen self) {
        int guiX = ((HandledScreenAccessor) self).giovanni$getX();
        int guiY = ((HandledScreenAccessor) self).giovanni$getY();
        int bgW = ((HandledScreenAccessor) self).giovanni$getBackgroundWidth();

        int wantedX = guiX + bgW + PANEL_OFFSET_X;
        int panelX = Math.min(wantedX, self.width - PANEL_W - 8);
        int panelY = guiY;

        return new int[]{panelX, panelY};
    }
}

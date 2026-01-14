package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonDef;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsConfigManager;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenEditorOverlayMixin {

    @Unique private TextFieldWidget giovanni$commandField;
    @Unique private TextFieldWidget giovanni$iconField;

    @Unique private boolean giovanni$widgetsAdded = false;

    @Unique private static final int PANEL_W = 230;
    @Unique private static final int FIELD_H = 18;

    // spingi più a destra
    @Unique private static final int PANEL_OFFSET_X = 48;

    // “slash finto”: spazio dedicato prima del textfield
    @Unique private static final int SLASH_PAD = 10;

    @Unique private static final String DEFAULT_ICON = "minecraft:textures/item/paper.png";

    @Unique private String giovanni$lastSelectedSlot = null;
    @Unique private boolean giovanni$suppressSave = false;

    @Inject(method = "init", at = @At("TAIL"))
    private void giovanni$initEditorWidgets(CallbackInfo ci) {
        // init classico (ma non affidarti SOLO a questo)
        giovanni$ensureWidgets();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void giovanni$renderEditorOverlay(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!UiButtonsConfigManager.isEditMode()) return;

        // CRITICO: se editMode è stato acceso dopo init(), crea i widget qui.
        giovanni$ensureWidgets();
        if (!giovanni$widgetsAdded || giovanni$commandField == null || giovanni$iconField == null) return;

        String current = UiButtonsConfigManager.getSelectedSlot();
        if (giovanni$lastSelectedSlot == null || !giovanni$lastSelectedSlot.equalsIgnoreCase(current)) {
            giovanni$lastSelectedSlot = current;
            giovanni$loadFields();
        }

        InventoryScreen self = (InventoryScreen) (Object) this;

        int guiX = giovanni$getGuiX(self);
        int guiY = giovanni$getGuiY(self);
        int bgW = ((HandledScreenAccessor) (Object) self).giovanni$getBackgroundWidth();

        int wantedX = guiX + bgW + PANEL_OFFSET_X;
        int panelX = Math.min(wantedX, self.width - PANEL_W - 8);
        int panelY = guiY;

        // panel bg
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + 140, 0xAA000000);
        ctx.drawTextWithShadow(self.getTextRenderer(), "Inventory Button Editor", panelX + 10, panelY + 10, 0xFFFFFF);
        ctx.drawTextWithShadow(self.getTextRenderer(), "Slot: " + UiButtonsConfigManager.getSelectedSlot(), panelX + 10, panelY + 28, 0xCCCCCC);
        ctx.drawTextWithShadow(self.getTextRenderer(), "Command:", panelX + 10, panelY + 40, 0xCCCCCC);
        ctx.drawTextWithShadow(self.getTextRenderer(), "Icon:", panelX + 10, panelY + 85, 0xCCCCCC);

        // slash finto: disegnalo solo se field esiste
        int sx = giovanni$commandField.getX() - 8;
        int sy = giovanni$commandField.getY() + (FIELD_H - self.getTextRenderer().fontHeight) / 2;
        ctx.drawTextWithShadow(self.getTextRenderer(), "/", sx, sy, 0xFFFFFF);
    }

    @Unique
    private void giovanni$ensureWidgets() {
        if (!UiButtonsConfigManager.isEditMode()) return;
        if (giovanni$widgetsAdded) return;

        InventoryScreen self = (InventoryScreen) (Object) this;

        int guiX = giovanni$getGuiX(self);
        int guiY = giovanni$getGuiY(self);
        int bgW = ((HandledScreenAccessor) (Object) self).giovanni$getBackgroundWidth();

        int wantedX = guiX + bgW + PANEL_OFFSET_X;
        int panelX = Math.min(wantedX, self.width - PANEL_W - 8);
        int panelY = guiY;

        giovanni$commandField = new TextFieldWidget(
                self.getTextRenderer(),
                panelX + 10 + SLASH_PAD, panelY + 50,
                PANEL_W - 20 - SLASH_PAD, FIELD_H,
                Text.literal("Command")
        );
        giovanni$iconField = new TextFieldWidget(
                self.getTextRenderer(),
                panelX + 10, panelY + 95,
                PANEL_W - 20, FIELD_H,
                Text.literal("Icon")
        );

        giovanni$commandField.setMaxLength(1024);
        giovanni$iconField.setMaxLength(2048);

        giovanni$loadFields();
        giovanni$lastSelectedSlot = UiButtonsConfigManager.getSelectedSlot();

        giovanni$commandField.setChangedListener(s -> giovanni$persistFromFields());
        giovanni$iconField.setChangedListener(s -> giovanni$persistFromFields());

        ((ScreenInvoker) self).giovanni$addDrawableChild(giovanni$commandField);
        ((ScreenInvoker) self).giovanni$addDrawableChild(giovanni$iconField);

        giovanni$widgetsAdded = true;
    }

    @Unique
    private static int giovanni$getGuiX(InventoryScreen self) {
        int bgW = ((HandledScreenAccessor) self).giovanni$getBackgroundWidth();
        return (self.width - bgW) / 2;
    }

    @Unique
    private static int giovanni$getGuiY(InventoryScreen self) {
        int bgH = ((HandledScreenAccessor) self).giovanni$getBackgroundHeight();
        return (self.height - bgH) / 2;
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

        // Se metti un comando ma l'icona è vuota -> default paper (e popola il campo)
        if (icon.isEmpty()) {
            icon = DEFAULT_ICON;
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
}

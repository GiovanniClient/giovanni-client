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
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonSlot;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonDef;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsConfig;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsConfigManager;

import java.util.Optional;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenEditorOverlayMixin {

    @Unique private TextFieldWidget giovanni$commandField;
    @Unique private TextFieldWidget giovanni$iconField;

    @Unique private static final int PANEL_W = 230;
    @Unique private static final int FIELD_H = 18;

    @Unique private String giovanni$lastSelectedSlot = null;
    @Unique private boolean giovanni$suppressSave = false;

    @Inject(method = "init", at = @At("TAIL"))
    private void giovanni$initEditorWidgets(CallbackInfo ci) {
        if (!UiButtonsConfigManager.EDIT_MODE) return;

        InventoryScreen self = (InventoryScreen)(Object)this;
        int guiX = giovanni$getGuiX(self);
        int guiY = giovanni$getGuiY(self);

        int bgW = ((HandledScreenAccessor)(Object)self).giovanni$getBackgroundWidth();
        int bgH = ((HandledScreenAccessor)(Object)self).giovanni$getBackgroundHeight();

        int panelX = Math.min(guiX + bgW + 10, self.width - PANEL_W - 8);
        int panelY = guiY;

        giovanni$commandField = new TextFieldWidget(self.getTextRenderer(), panelX + 10, panelY + 50, PANEL_W - 20, FIELD_H, Text.literal("Command"));
        giovanni$iconField    = new TextFieldWidget(self.getTextRenderer(), panelX + 10, panelY + 95, PANEL_W - 20, FIELD_H, Text.literal("Icon"));

        giovanni$commandField.setMaxLength(1024);
        giovanni$iconField.setMaxLength(2048);

        giovanni$loadFields();

        giovanni$lastSelectedSlot = UiButtonsConfigManager.EDIT_SELECTED_SLOT;

        giovanni$commandField.setChangedListener(s -> giovanni$saveFields());
        giovanni$iconField.setChangedListener(s -> giovanni$saveFields());

        // serve invoker: vedi blocco 2 sotto
        ((ScreenInvoker)(Object)self).giovanni$addDrawableChild(giovanni$commandField);
        ((ScreenInvoker)(Object)self).giovanni$addDrawableChild(giovanni$iconField);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void giovanni$renderEditorOverlay(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!UiButtonsConfigManager.EDIT_MODE) return;

        String current = UiButtonsConfigManager.EDIT_SELECTED_SLOT;
        if (giovanni$lastSelectedSlot == null || !giovanni$lastSelectedSlot.equalsIgnoreCase(current)) {
            giovanni$lastSelectedSlot = current;
            giovanni$loadFields();
        }

        InventoryScreen self = (InventoryScreen)(Object)this;
        int guiX = giovanni$getGuiX(self);
        int guiY = giovanni$getGuiY(self);

        int bgW = ((HandledScreenAccessor)(Object)self).giovanni$getBackgroundWidth();
        int bgH = ((HandledScreenAccessor)(Object)self).giovanni$getBackgroundHeight();

        // pannello destro
        int panelX = Math.min(guiX + bgW + 10, self.width - PANEL_W - 8);
        int panelY = guiY;

        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + bgH, 0xCC111111);
        int br = 0xFF3A3A3A;
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + 1, br);
        ctx.fill(panelX, panelY + bgH - 1, panelX + PANEL_W, panelY + bgH, br);
        ctx.fill(panelX, panelY, panelX + 1, panelY + bgH, br);
        ctx.fill(panelX + PANEL_W - 1, panelY, panelX + PANEL_W, panelY + bgH, br);

        ctx.drawTextWithShadow(self.getTextRenderer(), Text.literal("Slot: " + UiButtonsConfigManager.EDIT_SELECTED_SLOT), panelX + 10, panelY + 10, 0xFFFFFF);
        ctx.drawTextWithShadow(self.getTextRenderer(), Text.literal("Command (blank = hidden)"), panelX + 10, panelY + 35, 0xFFFFFF);
        ctx.drawTextWithShadow(self.getTextRenderer(), Text.literal("Icon texture id"), panelX + 10, panelY + 80, 0xFFFFFF);

        // overlay slot (disegnalo DOPO per vederlo sempre)
        for (InventoryButtonSlot slot : InventoryButtonSlot.all()) {
            int x = guiX + slot.relX();
            int y = guiY + slot.relY();

            boolean sel = slot.id().equalsIgnoreCase(UiButtonsConfigManager.EDIT_SELECTED_SLOT);
            int overlay = sel ? 0x8040A0FF : 0x40111111;
            int border  = sel ? 0xFF40A0FF : 0xFF3A3A3A;

            ctx.fill(x, y, x + 18, y + 18, overlay);
            ctx.fill(x, y, x + 18, y + 1, border);
            ctx.fill(x, y + 17, x + 18, y + 18, border);
            ctx.fill(x, y, x + 1, y + 18, border);
            ctx.fill(x + 17, y, x + 18, y + 18, border);
        }
    }

    @Unique
    private static int giovanni$getGuiX(InventoryScreen self) {
        int bgW = ((HandledScreenAccessor)(Object)self).giovanni$getBackgroundWidth();
        return (self.width - bgW) / 2;
    }

    @Unique
    private static int giovanni$getGuiY(InventoryScreen self) {
        int bgH = ((HandledScreenAccessor)(Object)self).giovanni$getBackgroundHeight();
        return (self.height - bgH) / 2;
    }

    @Unique
    private UiButtonDef giovanni$getOrCreateDef() {
        UiButtonsConfig cfg = UiButtonsConfigManager.get();
        String slotId = UiButtonsConfigManager.EDIT_SELECTED_SLOT;

        Optional<UiButtonDef> existing = cfg.buttons.stream()
                .filter(b -> "inventory".equals(b.screen))
                .filter(b -> slotId.equalsIgnoreCase(b.slot))
                .findFirst();

        if (existing.isPresent()) return existing.get();

        UiButtonDef def = new UiButtonDef();
        def.id = "inv_" + slotId.toLowerCase();
        def.screen = "inventory";
        def.slot = slotId;
        def.w = 18;
        def.h = 18;
        def.command = "";
        def.icon = "minecraft:textures/item/paper.png";
        def.tooltip = "";
        def.visible = true;
        def.enabled = true;

        cfg.buttons.add(def);
        UiButtonsConfigManager.save();
        return def;
    }

    @Unique
    private void giovanni$loadFields() {
        if (giovanni$commandField == null || giovanni$iconField == null) return;

        UiButtonDef def = giovanni$getOrCreateDef();

        giovanni$suppressSave = true;
        giovanni$commandField.setText(def.command == null ? "" : def.command);
        giovanni$iconField.setText(def.icon == null ? "" : def.icon);
        giovanni$suppressSave = false;
    }


    @Unique
    private void giovanni$saveFields() {
        if (giovanni$suppressSave) return;
        if (giovanni$commandField == null || giovanni$iconField == null) return;

        UiButtonDef def = giovanni$getOrCreateDef();
        def.command = giovanni$commandField.getText().trim();
        def.icon = giovanni$iconField.getText().trim();
        UiButtonsConfigManager.save();
    }



}

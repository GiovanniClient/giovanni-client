package rocket.giovanniclient.client.features.dungeons;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.component.ResolvableProfile;
import rocket.giovanniclient.client.features.dungeons.LeapTargets.LeapTarget;
import rocket.giovanniclient.client.util.InventoryUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class LeapScreen extends ContainerScreen {
    private static final int CARD_COLOR = 0xFF1A222F;
    private static final int TEXT_COLOR = 0xFFF3F5F8;
    private static final int MUTED_COLOR = 0xFFAAB6C7;

    private final List<LeapButton> buttons = new ArrayList<>();
    private List<LeapTarget> layout = LeapLayout.assign(List.of(), LeapTarget::dungeonClass);
    private float titleScale;
    private float titleY;

    LeapScreen(ContainerScreen original, Inventory inventory) {
        super(original.getMenu(), inventory, original.getTitle());
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();

        var window = minecraft.getWindow();
        int maximumScale = window.calculateScale(0, minecraft.isEnforceUnicode());
        int menuWidth = Math.max(1, Math.ceilDiv(window.getWidth(), maximumScale));
        int menuHeight = Math.max(1, Math.ceilDiv(window.getHeight(), maximumScale));
        float scaleX = (float) width / menuWidth;
        float scaleY = (float) height / menuHeight;
        titleScale = Math.min(scaleX, scaleY);

        LeapScreenLayout geometry = LeapScreenLayout.create(menuWidth, menuHeight);
        titleY = geometry.titleY() * scaleY;
        // Keep widget bounds in Minecraft's input coordinates so hover and clicks follow the rendered cards.
        for (LeapScreenLayout.Card card : geometry.cards()) {
            int x = Math.round(card.x() * scaleX);
            int y = Math.round(card.y() * scaleY);
            int right = Math.round((card.x() + card.width()) * scaleX);
            int bottom = Math.round((card.y() + card.height()) * scaleY);
            buttons.add(addWidget(new LeapButton(x, y, right - x, bottom - y, card)));
        }
        containerTick();
    }

    @Override
    protected void containerTick() {
        LocalPlayer player = minecraft.player;
        List<LeapTarget> targets = player == null ? List.of() : LeapTargets.read(getMenu(), player);
        layout = LeapLayout.assign(targets, LeapTarget::dungeonClass);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the dungeon visible behind the leap buttons.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(width / 2F, titleY);
        graphics.pose().scale(titleScale);
        graphics.centeredText(font, getTitle(), 0, 0, TEXT_COLOR);
        graphics.pose().popMatrix();

        for (int vertex = 0; vertex < buttons.size(); vertex++) {
            LeapButton button = buttons.get(vertex);
            button.setTarget(layout.get(vertex));
            button.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        return false;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ContainerInput input) {
        // Only the visible leap buttons may interact with the server menu.
    }

    private void select(LeapTarget selected) {
        LocalPlayer player = minecraft.player;
        if (selected == null || selected.dead() || player == null || minecraft.gui.screen() != this
                || !getMenu().getCarried().isEmpty()) return;

        for (LeapTarget live : LeapTargets.read(getMenu(), player)) {
            if (live.slot() == selected.slot() && !live.dead() && live.name().equalsIgnoreCase(selected.name())) {
                InventoryUtils.clickSlot(minecraft, getMenu(), live.slot(), InventoryUtils.MouseButton.LEFT,
                        ContainerInput.PICKUP);
                return;
            }
        }
    }

    private final class LeapButton extends AbstractButton {
        private final LeapScreenLayout.Card card;
        private LeapTarget target;
        private ResolvableProfile profile;
        private String subtitle = "";

        private LeapButton(int x, int y, int width, int height, LeapScreenLayout.Card card) {
            super(x, y, width, height, Component.empty());
            this.card = card;
            active = false;
        }

        private void setTarget(LeapTarget target) {
            if (Objects.equals(this.target, target)) return;
            this.target = target;
            this.active = target != null && !target.dead();
            if (target == null) {
                setMessage(Component.empty());
                setTooltip(null);
                profile = null;
                subtitle = "";
                return;
            }

            DungeonClass dungeonClass = target.dungeonClass();
            subtitle = dungeonClass == null ? "" : dungeonClass.label();
            if (target.dead()) subtitle = subtitle.isEmpty() ? "Dead" : subtitle + " · Dead";
            Component message = Component.literal(subtitle.isEmpty() ? target.name() : target.name() + " · " + subtitle);
            setMessage(message);
            setTooltip(Tooltip.create(message));
            ResolvableProfile headProfile = target.head().get(DataComponents.PROFILE);
            profile = headProfile != null ? headProfile : ResolvableProfile.createUnresolved(target.name());
        }

        @Override
        public void onPress(InputWithModifiers input) {
            select(target);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(getX(), getY());
            graphics.pose().scale((float) width / card.width(), (float) height / card.height());
            drawCard(graphics);
            graphics.pose().popMatrix();
        }

        private void drawCard(GuiGraphicsExtractor graphics) {
            int cardWidth = card.width();
            int cardHeight = card.height();
            if (target == null) {
                graphics.outline(0, 0, cardWidth, cardHeight, 0xB364748A);
                int centerX = cardWidth / 2;
                int centerY = cardHeight / 2;
                int markWidth = Math.min(4, centerX);
                graphics.fill(centerX - markWidth, centerY, centerX + markWidth, centerY + 1, MUTED_COLOR);
                return;
            }

            DungeonClass dungeonClass = target.dungeonClass();
            int accent = dungeonClass == null ? MUTED_COLOR : dungeonClass.color();
            boolean highlighted = active && isHoveredOrFocused();
            int background = ARGB.srgbLerp(highlighted ? 0.24F : 0.12F, CARD_COLOR, accent);
            int border = highlighted ? accent : ARGB.srgbLerp(target.dead() ? 0.26F : 0.5F, CARD_COLOR, accent);
            graphics.fill(0, 0, cardWidth, cardHeight, background);
            graphics.outline(0, 0, cardWidth, cardHeight, border);

            int faceSize = Math.min(32, Math.min(cardWidth / 4, cardHeight - 6));
            if (faceSize > 0) {
                PlayerFaceExtractor.extractRenderState(graphics, profile, 7, (cardHeight - faceSize) / 2, faceSize);
            }
            int textX = Math.max(0, faceSize) + 14;
            int textWidth = cardWidth - textX - 7;
            boolean showSubtitle = !subtitle.isEmpty() && cardHeight >= 23;
            int textY = (cardHeight - (showSubtitle ? 22 : 8)) / 2;
            drawFittedText(graphics, target.name(), textX, textY, textWidth, target.dead() ? MUTED_COLOR : TEXT_COLOR);
            if (showSubtitle) drawFittedText(graphics, subtitle, textX, textY + 13, textWidth, accent);
        }

        private void drawFittedText(GuiGraphicsExtractor graphics, String text, int x, int y, int available, int color) {
            if (available <= 0) return;
            int textWidth = font.width(text);
            if (textWidth <= available) {
                graphics.text(font, text, x, y, color);
                return;
            }
            float scale = (float) available / textWidth;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y + 4 * (1 - scale));
            graphics.pose().scale(scale);
            graphics.text(font, text, 0, 0, color);
            graphics.pose().popMatrix();
        }
    }
}

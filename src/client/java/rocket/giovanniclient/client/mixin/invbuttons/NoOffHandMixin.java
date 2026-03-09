package rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerScreenHandler.class)
public abstract class NoOffHandMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void disableOffhandSlot(PlayerInventory inventory, boolean onServer, PlayerEntity owner, CallbackInfo ci) {
        // Cast 'this' to ScreenHandler to access the inherited 'slots' list
        ScreenHandler handler = (ScreenHandler) (Object) this;

        // Find the slot that represents the offhand (inventory index 40)
        for (int i = 0; i < handler.slots.size(); i++) {
            Slot slot = handler.slots.get(i);

            if (slot.inventory == inventory && slot.getIndex() == 40) {
                // Replace it with a disabled slot at the same coordinates
                handler.slots.set(i, new Slot(inventory, 40, slot.x, slot.y) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean canTakeItems(PlayerEntity playerEntity) {
                        return false;
                    }

                    @Override
                    public ItemStack takeStack(int amount) {
                        return ItemStack.EMPTY;
                    }

                    @Override
                    public void setStack(ItemStack stack) {
                        // Prevent any item from being set
                    }

                    @Override
                    public Identifier getBackgroundSprite() {
                        return PlayerScreenHandler.EMPTY_OFF_HAND_SLOT_TEXTURE;
                    }
                });
                break;
            }
        }
    }
}
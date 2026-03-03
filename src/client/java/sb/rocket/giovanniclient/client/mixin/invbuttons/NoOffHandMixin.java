package sb.rocket.giovanniclient.client.mixin.invbuttons;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonsConfig;

import java.util.List;

@Mixin(PlayerScreenHandler.class)
public class NoOffHandMixin {
    /**
     * Redirects the addSlot call that occurs AFTER addPlayerSlots.
     * We target ScreenHandler here because that's where the method is defined.
     *
     * @return
     */
    /*
    @Redirect(
            method = "<init>(Lnet/minecraft/entity/player/PlayerInventory;ZLnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/screen/PlayerScreenHandler;addSlot(Lnet/minecraft/screen/slot/Slot;)Lnet/minecraft/screen/slot/Slot;"
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/screen/PlayerScreenHandler;addPlayerSlots(Lnet/minecraft/inventory/Inventory;II)V"
                    )
            )
    )
    private Slot removeOffhandSlot(PlayerScreenHandler instance, Slot slot) {
        // By returning the slot without calling instance.addSlot(slot),
        // the offhand slot is never added to the 'slots' list.
        return slot;
    }
*/
    @Shadow
    public Slot addSlot(Slot slot) {
        return null;
    }

    @Redirect(
            method = "<init>(Lnet/minecraft/entity/player/PlayerInventory;ZLnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/screen/ScreenHandler;addSlot(Lnet/minecraft/screen/slot/Slot;)Lnet/minecraft/screen/slot/Slot;"
            )
    )
    private Slot redirectOffhandAddSlot(Slot slot) {
        if (ConfigManager.getConfig().ibc.EQUIPMENT.get() != InventoryButtonsConfig.EquipmentSide.Right
                && slot.getIndex() == 40) {
            return slot; // Skip adding to slots list (return value discarded in constructor)
        }
        return this.addSlot(slot); // Invoke shadowed original
    }
    /**
     * Prevents an IndexOutOfBoundsException in quickMove.
     * Since slot 45 no longer exists, we must prevent the code from checking it.
     */
    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void protectQuickMove(PlayerEntity player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index == 45) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}

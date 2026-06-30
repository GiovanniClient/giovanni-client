package rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface HandledScreenAccessor {
    @Accessor("leftPos") int giovanni$getX();
    @Accessor("topPos") int giovanni$getY();

    @Accessor("imageWidth") int giovanni$getBackgroundWidth();
    @Accessor("imageHeight") int giovanni$getBackgroundHeight();
}

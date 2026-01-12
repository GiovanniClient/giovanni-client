package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x") int giovanni$getX();
    @Accessor("y") int giovanni$getY();

    @Accessor("backgroundWidth") int giovanni$getBackgroundWidth();
    @Accessor("backgroundHeight") int giovanni$getBackgroundHeight();
}

package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface SlotAccessor {
    @Accessor("x") @Mutable void setX(int x);
    @Accessor("y") @Mutable void setY(int y);
}

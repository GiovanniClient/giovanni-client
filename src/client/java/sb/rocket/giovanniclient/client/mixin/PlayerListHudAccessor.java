package sb.rocket.giovanniclient.client.mixin;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.hud.PlayerListHud;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {
    @Accessor("header")
    Text giovanni$getHeader();

    @Accessor("footer")
    Text giovanni$getFooter();
}

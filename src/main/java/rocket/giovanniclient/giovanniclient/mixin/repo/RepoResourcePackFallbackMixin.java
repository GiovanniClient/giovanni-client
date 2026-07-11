package rocket.giovanniclient.giovanniclient.mixin.repo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.giovanniclient.repo.GiovanniRepoResourcePack;

import java.util.Optional;

@Mixin(ReloadableResourceManager.class)
public class RepoResourcePackFallbackMixin {
    @ModifyReturnValue(method = "getResource", at = @At("RETURN"))
    private Optional<Resource> giovanni$getRepoResourceFallback(Optional<Resource> original, @Local(argsOnly = true) Identifier identifier) {
        return original.or(() -> GiovanniRepoResourcePack.createResourceDirectly(identifier));
    }
}

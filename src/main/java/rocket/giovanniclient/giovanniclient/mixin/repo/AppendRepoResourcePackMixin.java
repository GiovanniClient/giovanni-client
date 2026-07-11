package rocket.giovanniclient.giovanniclient.mixin.repo;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.resource.v1.pack.ModPackResources;
import net.fabricmc.fabric.impl.resource.pack.ModPackResourcesSorter;
import net.fabricmc.fabric.impl.resource.pack.ModPackResourcesUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.giovanniclient.repo.GiovanniRepoResourcePack;

import java.util.List;

@Mixin(ModPackResourcesUtil.class)
public class AppendRepoResourcePackMixin {
    @Inject(
            method = "getModResourcePacks",
            at = @At(value = "INVOKE", target = "Lnet/fabricmc/fabric/impl/resource/pack/ModPackResourcesSorter;getPacks()Ljava/util/List;"),
            require = 0
    )
    private static void giovanni$appendRepoResourcePack(
            FabricLoader fabricLoader,
            PackType type,
            @Nullable String subPath,
            CallbackInfoReturnable<List<ModPackResources>> cir,
            @Local ModPackResourcesSorter sorter
    ) {
        GiovanniRepoResourcePack.append(sorter);
    }
}

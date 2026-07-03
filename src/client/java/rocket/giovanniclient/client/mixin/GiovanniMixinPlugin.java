package rocket.giovanniclient.client.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class GiovanniMixinPlugin implements IMixinConfigPlugin {

    // Must match the list in your main class exactly!
    private static final List<String> SUPPORTED_VERSIONS = List.of("26.1.2");

    private boolean isSupported;

    @Override
    public void onLoad(String mixinPackage) {
        // This is called BEFORE any mixins are applied
        String currentMcVersion = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .get()
                .getMetadata()
                .getVersion()
                .getFriendlyString();

        this.isSupported = SUPPORTED_VERSIONS.contains(currentMcVersion);

        if (!isSupported) {
            System.err.println("[Giovanni] WARNING: Unsupported MC Version (" + currentMcVersion + "). Disabling Mixins to prevent crash.");
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // If the version is not supported, we return FALSE to stop the Mixin from loading
        return isSupported;
    }

    // --- defaults, don't touch ---
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
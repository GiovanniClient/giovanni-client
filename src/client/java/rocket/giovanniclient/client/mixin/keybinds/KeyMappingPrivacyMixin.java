package rocket.giovanniclient.client.mixin.keybinds;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.bootstrap.ClientKeybinds;

import java.util.Map;

@Mixin(KeyMapping.class)
public abstract class KeyMappingPrivacyMixin {

    /**
     * Treat our mappings as unknown only when resolving rich-text keybinds.
     * Returning null preserves vanilla's unknown-key translation fallback,
     * including server resource packs. Input handling and saved bindings still
     * use the complete registry. See https://wurst.wiki/sign_translation_vulnerability.
     */
    @WrapOperation(
            method = "createNameSupplier(Ljava/lang/String;)Ljava/util/function/Supplier;",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object giovanni$hideOwnKeybinds(Map<?, ?> mappings, Object name, Operation<Object> original) {
        if (name instanceof String key && ClientKeybinds.isOwnKeybind(key)) return null;
        return original.call(mappings, name);
    }
}

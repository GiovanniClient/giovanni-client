package rocket.giovanniclient.giovanniclient.mixin;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.giovanniclient.config.ClientConfigState;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

@Mixin(value = YggdrasilServicesKeyInfo.class, remap = false)
public abstract class YggdrasilServicesKeyInfoMixin {
    @Shadow public abstract Signature signature();

    @Inject(method = "validateProperty", at = @At("HEAD"), cancellable = true)
    private void giovanni$validatePropertySilently(Property property, CallbackInfoReturnable<Boolean> cir) {
        if (!ClientConfigState.suppressYggdrasilWarnings) return;

        byte[] expected;
        try {
            expected = Base64.getDecoder().decode(property.signature());
        } catch (IllegalArgumentException exception) {
            cir.setReturnValue(false);
            return;
        }

        try {
            Signature signature = signature();
            signature.update(property.value().getBytes(StandardCharsets.UTF_8));
            cir.setReturnValue(signature.verify(expected));
        } catch (SignatureException exception) {
            cir.setReturnValue(false);
        }
    }
}

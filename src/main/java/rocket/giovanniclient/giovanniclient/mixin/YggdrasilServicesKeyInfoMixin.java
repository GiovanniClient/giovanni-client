package rocket.giovanniclient.giovanniclient.mixin;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.giovanniclient.config.ClientConfigState;

import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

@Mixin(value = YggdrasilServicesKeyInfo.class, remap = false)
public abstract class YggdrasilServicesKeyInfoMixin {

    @Shadow
    public abstract Signature signature();

    @Inject(method = "validateProperty", at = @At("HEAD"), cancellable = true)
    private void giovanni$validatePropertySilently(
            final Property property,
            final CallbackInfoReturnable<Boolean> cir
    ) {
        if (!ClientConfigState.suppressYggdrasilWarnings) {
            return;
        }

        final Signature signature = this.signature();

        final byte[] expected;
        try {
            expected = Base64.getDecoder().decode(property.signature());
        } catch (final IllegalArgumentException e) {
            cir.setReturnValue(false);
            return;
        }

        try {
            signature.update(property.value().getBytes());
            cir.setReturnValue(signature.verify(expected));
        } catch (final SignatureException e) {
            cir.setReturnValue(false);
        }
    }
}

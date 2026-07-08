package rocket.giovanniclient.giovanniclient.mixin;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilServicesKeyInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

@Mixin(value = YggdrasilServicesKeyInfo.class, remap = false)
public abstract class YggdrasilServicesKeyInfoMixin {

    @Shadow
    public abstract Signature signature();

    /**
     * @author stopbuggingbeintellij
     * @reason Remove noisy logging from malformed/invalid Yggdrasil property signatures.
     */
    @Overwrite
    public boolean validateProperty(final Property property) {
        final Signature signature = this.signature();

        final byte[] expected;
        try {
            expected = Base64.getDecoder().decode(property.signature());
        } catch (final IllegalArgumentException e) {
            return false;
        }

        try {
            signature.update(property.value().getBytes());
            return signature.verify(expected);
        } catch (final SignatureException e) {
            return false;
        }
    }
}
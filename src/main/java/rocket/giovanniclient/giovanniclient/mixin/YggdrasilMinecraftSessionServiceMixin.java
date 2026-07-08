package rocket.giovanniclient.giovanniclient.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.TextureUrlChecker;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import org.spongepowered.asm.mixin.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Mixin(value = YggdrasilMinecraftSessionService.class, remap = false)
public abstract class YggdrasilMinecraftSessionServiceMixin {

    @Shadow
    @Final
    private Gson gson;

    @Shadow
    @Final
    private ServicesKeySet servicesKeySet;

    /**
     * @author stopbuggingbeintellij
     * @reason Suppress noisy texture payload decode/url validation logging.
     */
    @Overwrite
    public MinecraftProfileTextures unpackTextures(final Property packedTextures) {
        final String value = packedTextures.value();
        final SignatureState signatureState = this.getPropertySignatureStateSilent(packedTextures);

        final MinecraftTexturesPayload result;
        try {
            final String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            result = this.gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (final JsonParseException | IllegalArgumentException e) {
            return MinecraftProfileTextures.EMPTY;
        }

        if (result == null || result.textures() == null || result.textures().isEmpty()) {
            return MinecraftProfileTextures.EMPTY;
        }

        final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = result.textures();
        for (final Map.Entry<MinecraftProfileTexture.Type, MinecraftProfileTexture> entry : textures.entrySet()) {
            final String url = entry.getValue().getUrl();
            if (url == null || !TextureUrlChecker.isAllowedTextureDomain(url)) {
                return MinecraftProfileTextures.EMPTY;
            }
        }

        return new MinecraftProfileTextures(
                textures.get(MinecraftProfileTexture.Type.SKIN),
                textures.get(MinecraftProfileTexture.Type.CAPE),
                textures.get(MinecraftProfileTexture.Type.ELYTRA),
                signatureState
        );
    }

    @Unique
    private SignatureState getPropertySignatureStateSilent(final Property property) {
        if (!property.hasSignature()) {
            return SignatureState.UNSIGNED;
        }

        if (this.servicesKeySet.keys(ServicesKeyType.PROFILE_PROPERTY)
                .stream()
                .noneMatch(key -> key.validateProperty(property))) {
            return SignatureState.INVALID;
        }

        return SignatureState.SIGNED;
    }
}
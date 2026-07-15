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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.giovanniclient.config.ClientConfigState;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Mixin(value = YggdrasilMinecraftSessionService.class, remap = false)
public abstract class YggdrasilMinecraftSessionServiceMixin {
    @Shadow @Final private Gson gson;
    @Shadow @Final private ServicesKeySet servicesKeySet;

    @Inject(method = "unpackTextures", at = @At("HEAD"), cancellable = true)
    private void giovanni$unpackTexturesSilently(
            Property packedTextures,
            CallbackInfoReturnable<MinecraftProfileTextures> cir
    ) {
        if (!ClientConfigState.suppressYggdrasilWarnings) return;

        SignatureState signatureState = getPropertySignatureStateSilent(packedTextures);
        MinecraftTexturesPayload result;
        try {
            String json = new String(Base64.getDecoder().decode(packedTextures.value()), StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (JsonParseException | IllegalArgumentException exception) {
            cir.setReturnValue(MinecraftProfileTextures.EMPTY);
            return;
        }

        if (result == null || result.textures() == null || result.textures().isEmpty()) {
            cir.setReturnValue(MinecraftProfileTextures.EMPTY);
            return;
        }

        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = result.textures();
        for (MinecraftProfileTexture texture : textures.values()) {
            if (texture.getUrl() == null || !TextureUrlChecker.isAllowedTextureDomain(texture.getUrl())) {
                cir.setReturnValue(MinecraftProfileTextures.EMPTY);
                return;
            }
        }

        cir.setReturnValue(new MinecraftProfileTextures(
                textures.get(MinecraftProfileTexture.Type.SKIN),
                textures.get(MinecraftProfileTexture.Type.CAPE),
                textures.get(MinecraftProfileTexture.Type.ELYTRA),
                signatureState
        ));
    }

    @Unique
    private SignatureState getPropertySignatureStateSilent(Property property) {
        if (!property.hasSignature()) return SignatureState.UNSIGNED;
        return servicesKeySet.keys(ServicesKeyType.PROFILE_PROPERTY).stream()
                .anyMatch(key -> key.validateProperty(property))
                ? SignatureState.SIGNED
                : SignatureState.INVALID;
    }
}

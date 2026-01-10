/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.skin;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.skin.Cape;
import org.geysermc.geyser.api.skin.Skin;
import org.geysermc.geyser.api.skin.SkinData;
import org.geysermc.geyser.api.skin.SkinGeometry;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.player.AvatarEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.auth.GameProfile.Texture;
import org.geysermc.mcprotocollib.auth.GameProfile.TextureType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for modifying a player's skin when wearing a player head
 */
public class FakeHeadProvider {
    private static final LoadingCache<FakeHeadEntry, SkinData> MERGED_SKINS_LOADING_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build(new CacheLoader<>() {
                @Override
                public SkinData load(@NonNull FakeHeadEntry fakeHeadEntry) throws Exception {
                    SkinData skinData = SkinProvider.getOrDefault(SkinProvider.requestSkinData(fakeHeadEntry.getEntity(), fakeHeadEntry.getSession()), null, 5);

                    if (skinData == null) {
                        throw new Exception("Couldn't load player's original skin");
                    }

                    Skin skin = skinData.skin();
                    Cape cape = skinData.cape();
                    SkinGeometry geometry = skinData.geometry().geometryName().equals("{\"geometry\" :{\"default\" :\"geometry.humanoid.customSlim\"}}")
                            ? SkinProvider.WEARING_CUSTOM_SKULL_SLIM : SkinProvider.WEARING_CUSTOM_SKULL;

                    Skin headSkin = SkinProvider.getOrDefault(
                            SkinProvider.requestSkin(fakeHeadEntry.getEntity().uuid(), fakeHeadEntry.getFakeHeadSkinUrl(), false), SkinProvider.EMPTY_SKIN, 5);
                    BufferedImage originalSkinImage = SkinProvider.imageDataToBufferedImage(skin.skinData(), 64, skin.skinData().length / 4 / 64);
                    BufferedImage headSkinImage = SkinProvider.imageDataToBufferedImage(headSkin.skinData(), 64, headSkin.skinData().length / 4 / 64);

                    Graphics2D graphics2D = originalSkinImage.createGraphics();
                    graphics2D.setComposite(AlphaComposite.Clear);
                    graphics2D.fillRect(0, 0, 64, 16);
                    graphics2D.setComposite(AlphaComposite.SrcOver);
                    graphics2D.drawImage(headSkinImage, 0, 0, 64, 16, 0, 0, 64, 16, null);
                    graphics2D.dispose();

                    // Make the skin key a combination of the current skin data and the new skin data
                    // Don't tie it to a player - that player *can* change skins in-game
                    String skinKey = "customPlayerHead_" + fakeHeadEntry.getFakeHeadSkinUrl() + "_" + skin.textureUrl();
                    byte[] targetSkinData = SkinProvider.bufferedImageToImageData(originalSkinImage);
                    Skin mergedSkin = new Skin(skinKey, targetSkinData);

                    // Avoiding memory leak
                    fakeHeadEntry.setEntity(null);
                    fakeHeadEntry.setSession(null);

                    return new SkinData(mergedSkin, cape, geometry);
                }
            });

    public static void setHead(GeyserSession session, AvatarEntity entity, @Nullable ResolvableProfile profile) {
        if (profile == null) {
            return;
        }

        ResolvableProfile current = session.getPlayerWithCustomHeads().get(entity.uuid());
        if (profile.equals(current)) {
            // We already did this, no need to re-compute
            return;
        }

        SkinManager.resolveProfile(profile).whenCompleteAsync((resolved, throwable) -> {
            if (throwable != null) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.skin.fail", entity.uuid()), throwable);
                return;
            }
            loadHeadFromProfile(session, entity, profile, resolved);
        });
    }

    private static void loadHeadFromProfile(GeyserSession session, AvatarEntity entity, ResolvableProfile original, GameProfile resolved) {
        Texture skinTexture = SkinManager.getTextureDataFromProfile(resolved, TextureType.SKIN);
        String originalTextures = entity.getTexturesProperty();
        if (skinTexture != null) {
            session.getPlayerWithCustomHeads().put(entity.uuid(), original);
            SkinProvider.getExecutorService().execute(() -> {
                try {
                    SkinData mergedSkinData = MERGED_SKINS_LOADING_CACHE.get(new FakeHeadEntry(originalTextures, skinTexture.getURL(), entity, session));
                    SkinManager.sendSkinPacket(session, entity, mergedSkinData);
                } catch (ExecutionException e) {
                    GeyserImpl.getInstance().getLogger().error("Couldn't merge skin of " + entity.getUsername() + " with head skin " + resolved, e);
                }
            });
        }
    }

    public static void restoreOriginalSkin(GeyserSession session, LivingEntity livingEntity) {
        if (!(livingEntity instanceof AvatarEntity entity)) {
            return;
        }

        if (session.getPlayerWithCustomHeads().remove(entity.uuid()) == null) {
            return;
        }

        SkinProvider.requestSkinData(entity, session).whenCompleteAsync((skinData, throwable) -> {
            if (throwable != null) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.skin.fail", entity.uuid()), throwable);
                return;
            }

            SkinManager.sendSkinPacket(session, entity, skinData);
        });
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class FakeHeadEntry {
        private final String texturesProperty;
        private final String fakeHeadSkinUrl;
        private AvatarEntity entity;
        private GeyserSession session;

        @Override
        public boolean equals(Object o) {
            // We don't care about the equality of the entity as that is not used for caching purposes
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FakeHeadEntry that = (FakeHeadEntry) o;
            return Objects.equals(texturesProperty, that.texturesProperty) && Objects.equals(fakeHeadSkinUrl, that.fakeHeadSkinUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(texturesProperty, fakeHeadSkinUrl);
        }
    }

}

/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerSkinPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;

import javax.annotation.Nonnull;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for modifying a player's skin when wearing a player head
 */
public class FakeHeadProvider {
    private static final LoadingCache<FakeHeadEntry, SkinProvider.SkinData> MERGED_SKINS_LOADING_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build(new CacheLoader<>() {
                @Override
                public SkinProvider.SkinData load(@Nonnull FakeHeadEntry fakeHeadEntry) throws Exception {
                    SkinProvider.SkinData skinData = SkinProvider.getOrDefault(SkinProvider.requestSkinData(fakeHeadEntry.getEntity()), null, 5);

                    if (skinData == null) {
                        throw new Exception("Couldn't load player's original skin");
                    }

                    SkinProvider.Skin skin = skinData.skin();
                    SkinProvider.Cape cape = skinData.cape();
                    SkinProvider.SkinGeometry geometry = skinData.geometry().getGeometryName().equals("{\"geometry\" :{\"default\" :\"geometry.humanoid.customSlim\"}}")
                            ? SkinProvider.WEARING_CUSTOM_SKULL_SLIM : SkinProvider.WEARING_CUSTOM_SKULL;

                    SkinProvider.Skin headSkin = SkinProvider.getOrDefault(
                            SkinProvider.requestSkin(fakeHeadEntry.getEntity().getUuid(), fakeHeadEntry.getFakeHeadSkinUrl(), false), SkinProvider.EMPTY_SKIN, 5);
                    BufferedImage originalSkinImage = SkinProvider.imageDataToBufferedImage(skin.getSkinData(), 64, skin.getSkinData().length / 4 / 64);
                    BufferedImage headSkinImage = SkinProvider.imageDataToBufferedImage(headSkin.getSkinData(), 64, headSkin.getSkinData().length / 4 / 64);

                    Graphics2D graphics2D = originalSkinImage.createGraphics();
                    graphics2D.setComposite(AlphaComposite.Clear);
                    graphics2D.fillRect(0, 0, 64, 16);
                    graphics2D.setComposite(AlphaComposite.SrcOver);
                    graphics2D.drawImage(headSkinImage, 0, 0, 64, 16, 0, 0, 64, 16, null);
                    graphics2D.dispose();

                    // Make the skin key a combination of the current skin data and the new skin data
                    // Don't tie it to a player - that player *can* change skins in-game
                    String skinKey = "customPlayerHead_" + fakeHeadEntry.getFakeHeadSkinUrl() + "_" + skin.getTextureUrl();
                    byte[] targetSkinData = SkinProvider.bufferedImageToImageData(originalSkinImage);
                    SkinProvider.Skin mergedSkin = new SkinProvider.Skin(fakeHeadEntry.getEntity().getUuid(), skinKey, targetSkinData, System.currentTimeMillis(), false, false);

                    // Avoiding memory leak
                    fakeHeadEntry.setEntity(null);

                    return new SkinProvider.SkinData(mergedSkin, cape, geometry);
                }
            });

    public static void setHead(GeyserSession session, PlayerEntity entity, CompoundTag profileTag) {
        SkinManager.GameProfileData gameProfileData = SkinManager.GameProfileData.from(profileTag);
        if (gameProfileData == null) {
            return;
        }
        String fakeHeadSkinUrl = gameProfileData.skinUrl();

        session.getPlayerWithCustomHeads().add(entity.getUuid());

        String texturesProperty = entity.getTexturesProperty();

        SkinProvider.EXECUTOR_SERVICE.execute(() -> {
            try {
                SkinProvider.SkinData mergedSkinData = MERGED_SKINS_LOADING_CACHE.get(new FakeHeadEntry(texturesProperty, fakeHeadSkinUrl, entity));

                sendSkinPacket(session, entity, mergedSkinData);
            } catch (ExecutionException e) {
                GeyserImpl.getInstance().getLogger().error("Couldn't merge skin of " + entity.getUsername() + " with head skin url " + fakeHeadSkinUrl, e);
            }
        });
    }

    public static void restoreOriginalSkin(GeyserSession session, LivingEntity livingEntity) {
        if (!(livingEntity instanceof PlayerEntity entity)) {
            return;
        }

        if (!session.getPlayerWithCustomHeads().remove(entity.getUuid())) {
            return;
        }

        SkinProvider.requestSkinData(entity).whenCompleteAsync((skinData, throwable) -> {
            if (throwable != null) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.skin.fail", entity.getUuid()), throwable);
                return;
            }

            sendSkinPacket(session, entity, skinData);
        });
    }

    private static void sendSkinPacket(GeyserSession session, PlayerEntity entity, SkinProvider.SkinData skinData) {
        SkinProvider.Skin skin = skinData.skin();
        SkinProvider.Cape cape = skinData.cape();
        SkinProvider.SkinGeometry geometry = skinData.geometry();

        if (entity.getUuid().equals(session.getPlayerEntity().getUuid())) {
            PlayerListPacket.Entry updatedEntry = SkinManager.buildEntryManually(
                    session,
                    entity.getUuid(),
                    entity.getUsername(),
                    entity.getGeyserId(),
                    skin.getTextureUrl(),
                    skin.getSkinData(),
                    cape.getCapeId(),
                    cape.getCapeData(),
                    geometry
            );

            PlayerListPacket playerAddPacket = new PlayerListPacket();
            playerAddPacket.setAction(PlayerListPacket.Action.ADD);
            playerAddPacket.getEntries().add(updatedEntry);
            session.sendUpstreamPacket(playerAddPacket);
        } else {
            PlayerSkinPacket packet = new PlayerSkinPacket();
            packet.setUuid(entity.getUuid());
            packet.setOldSkinName("");
            packet.setNewSkinName(skin.getTextureUrl());
            packet.setSkin(getSkin(skin.getTextureUrl(), skin, cape, geometry));
            packet.setTrustedSkin(true);
            session.sendUpstreamPacket(packet);
        }
    }

    private static SerializedSkin getSkin(String skinId, SkinProvider.Skin skin, SkinProvider.Cape cape, SkinProvider.SkinGeometry geometry) {
        return SerializedSkin.of(skinId, "", geometry.getGeometryName(),
                ImageData.of(skin.getSkinData()), Collections.emptyList(),
                ImageData.of(cape.getCapeData()), geometry.getGeometryData(),
                "", true, false, false, cape.getCapeId(), skinId);
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class FakeHeadEntry {
        private final String texturesProperty;
        private final String fakeHeadSkinUrl;
        private PlayerEntity entity;

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

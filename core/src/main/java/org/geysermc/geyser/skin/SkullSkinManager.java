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

import org.cloudburstmc.protocol.bedrock.data.skin.ImageData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.skin.Skin;
import org.geysermc.geyser.api.skin.SkinData;
import org.geysermc.geyser.entity.type.player.AvatarEntity;
import org.geysermc.geyser.entity.type.player.SkullPlayerEntity;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.PlayerListUtils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SkullSkinManager extends SkinManager {

    public static SerializedSkin buildSkullEntryManually(GeyserSession session, String skinId, byte[] skinData) {
        skinId = skinId + "_skull";
        return SerializedSkin.builder()
            .skinId(skinId)
            .skinResourcePatch(SkinProvider.SKULL_GEOMETRY.geometryName())
            .skinData(ImageData.of(skinData))
            .capeData(ImageData.of(SkinProvider.EMPTY_CAPE.capeData()))
            .geometryData(SkinProvider.SKULL_GEOMETRY.geometryData())
            .premium(true)
            .capeId(SkinProvider.EMPTY_CAPE.capeId())
            .fullSkinId(skinId)
            .geometryDataEngineVersion(session.getClientData().getGameVersion())
            .build();
    }

    public static void requestAndHandleSkin(AvatarEntity entity, GeyserSession session, Consumer<Skin> skinConsumer) {
        BiConsumer<Skin, Throwable> applySkin = (skin, throwable) -> {
            SerializedSkin serializedSkin = buildSkullEntryManually(session, skin.textureUrl(), skin.skinData());
            if (GameProtocol.is1_21_130orHigher(session.protocolVersion())) {
                PlayerListUtils.sendSkinUsingPlayerList(session, PlayerListUtils.forSkullPlayerEntity(entity, serializedSkin), entity, false);
            } else {
                PlayerSkinPacket packet = new PlayerSkinPacket();
                packet.setUuid(entity.getUuid());
                packet.setOldSkinName("");
                packet.setNewSkinName(skin.textureUrl());
                packet.setSkin(serializedSkin);
                packet.setTrustedSkin(true);
                session.sendUpstreamPacket(packet);
            }

            if (skinConsumer != null) {
                skinConsumer.accept(skin);
            }
        };

        GameProfileData data = GameProfileData.from(entity);
        if (data == null) {
            if (entity instanceof SkullPlayerEntity skullEntity) {
                GeyserImpl.getInstance().getLogger().debug("Using fallback skin for skull at " + skullEntity.getSkullPosition() +
                    " with texture value: " + entity.getTexturesProperty() + " and UUID: " + skullEntity.getSkullUUID());
                // No texture available, fallback using the UUID
                SkinData fallback = SkinProvider.determineFallbackSkinData(skullEntity.getSkullUUID());
                applySkin.accept(fallback.skin(), null);
            }
        } else {
            SkinProvider.requestSkin(entity.getUuid(), data.skinUrl(), true)
                    .whenCompleteAsync(applySkin);
        }
    }

}

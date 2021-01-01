/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.skin;

import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

public class SkullSkinManager extends SkinManager {

    public static PlayerListPacket.Entry buildSkullEntryManually(UUID uuid, String username, long geyserId,
                                                                 String skinId, byte[] skinData) {
        // Prevents https://cdn.discordapp.com/attachments/613194828359925800/779458146191147008/unknown.png
        skinId = skinId + "_skull";
        SerializedSkin serializedSkin = SerializedSkin.of(
                skinId, SkinProvider.SKULL_GEOMETRY.getGeometryName(), ImageData.of(skinData), Collections.emptyList(),
                ImageData.of(SkinProvider.EMPTY_CAPE.getCapeData()), SkinProvider.SKULL_GEOMETRY.getGeometryData(),
                "", true, false, false, SkinProvider.EMPTY_CAPE.getCapeId(), skinId
        );

        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(uuid);
        entry.setName(username);
        entry.setEntityId(geyserId);
        entry.setSkin(serializedSkin);
        entry.setXuid("");
        entry.setPlatformChatId("");
        entry.setTeacher(false);
        entry.setTrustedSkin(true);
        return entry;
    }

    public static void requestAndHandleSkin(PlayerEntity entity, GeyserSession session,
                                            Consumer<SkinProvider.Skin> skinConsumer) {
        GameProfileData data = GameProfileData.from(entity.getProfile());

        SkinProvider.requestSkin(entity.getUuid(), data.getSkinUrl(), false)
                .whenCompleteAsync((skin, throwable) -> {
                    try {
                        if (session.getUpstream().isInitialized()) {
                            PlayerListPacket.Entry updatedEntry = buildSkullEntryManually(
                                    entity.getUuid(),
                                    entity.getUsername(),
                                    entity.getGeyserId(),
                                    skin.getTextureUrl(),
                                    skin.getSkinData()
                            );

                            PlayerListPacket playerAddPacket = new PlayerListPacket();
                            playerAddPacket.setAction(PlayerListPacket.Action.ADD);
                            playerAddPacket.getEntries().add(updatedEntry);
                            session.sendUpstreamPacket(playerAddPacket);

                            // It's a skull. We don't want them in the player list.
                            PlayerListPacket playerRemovePacket = new PlayerListPacket();
                            playerRemovePacket.setAction(PlayerListPacket.Action.REMOVE);
                            playerRemovePacket.getEntries().add(updatedEntry);
                            session.sendUpstreamPacket(playerRemovePacket);
                        }
                    } catch (Exception e) {
                        GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.skin.fail", entity.getUuid()), e);
                    }

                    if (skinConsumer != null) {
                        skinConsumer.accept(skin);
                    }
                });
    }

}

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

package org.geysermc.geyser.skin;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.auth.AuthType;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.text.GeyserLocale;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

public class SkinManager {

    /**
     * Builds a Bedrock player list entry from our existing, cached Bedrock skin information
     */
    public static PlayerListPacket.Entry buildCachedEntry(GeyserSession session, PlayerEntity playerEntity) {
        GameProfileData data = GameProfileData.from(playerEntity.getProfile());
        SkinProvider.Cape cape = SkinProvider.getCachedCape(data.capeUrl());
        SkinProvider.SkinGeometry geometry = SkinProvider.SkinGeometry.getLegacy(data.isAlex());

        SkinProvider.Skin skin = SkinProvider.getCachedSkin(data.skinUrl());
        if (skin == null) {
            skin = SkinProvider.EMPTY_SKIN;
        }

        return buildEntryManually(
                session,
                playerEntity.getProfile().getId(),
                playerEntity.getProfile().getName(),
                playerEntity.getGeyserId(),
                skin.getTextureUrl(),
                skin.getSkinData(),
                cape.getCapeId(),
                cape.getCapeData(),
                geometry
        );
    }

    /**
     * With all the information needed, build a Bedrock player entry with translated skin information.
     */
    public static PlayerListPacket.Entry buildEntryManually(GeyserSession session, UUID uuid, String username, long geyserId,
                                                            String skinId, byte[] skinData,
                                                            String capeId, byte[] capeData,
                                                            SkinProvider.SkinGeometry geometry) {
        SerializedSkin serializedSkin = SerializedSkin.of(
                skinId, "", geometry.getGeometryName(), ImageData.of(skinData), Collections.emptyList(),
                ImageData.of(capeData), geometry.getGeometryData(), "", true, false,
                !capeId.equals(SkinProvider.EMPTY_CAPE.getCapeId()), capeId, skinId
        );

        // This attempts to find the XUID of the player so profile images show up for Xbox accounts
        String xuid = "";
        GeyserSession playerSession = GeyserImpl.getInstance().connectionByUuid(uuid);

        if (playerSession != null) {
            xuid = playerSession.getAuthData().xuid();
        }

        PlayerListPacket.Entry entry;

        // If we are building a PlayerListEntry for our own session we use our AuthData UUID instead of the Java UUID
        // as Bedrock expects to get back its own provided UUID
        if (session.getPlayerEntity().getUuid().equals(uuid)) {
            entry = new PlayerListPacket.Entry(session.getAuthData().uuid());
        } else {
            entry = new PlayerListPacket.Entry(uuid);
        }

        entry.setName(username);
        entry.setEntityId(geyserId);
        entry.setSkin(serializedSkin);
        entry.setXuid(xuid);
        entry.setPlatformChatId("");
        entry.setTeacher(false);
        entry.setTrustedSkin(true);
        return entry;
    }

    public static void requestAndHandleSkinAndCape(PlayerEntity entity, GeyserSession session,
                                                   Consumer<SkinProvider.SkinAndCape> skinAndCapeConsumer) {
        SkinProvider.requestSkinData(entity).whenCompleteAsync((skinData, throwable) -> {
            if (skinData == null) {
                if (skinAndCapeConsumer != null) {
                    skinAndCapeConsumer.accept(null);
                }

                return;
            }

            if (skinData.geometry() != null) {
                SkinProvider.Skin skin = skinData.skin();
                SkinProvider.Cape cape = skinData.cape();
                SkinProvider.SkinGeometry geometry = skinData.geometry();

                PlayerListPacket.Entry updatedEntry = buildEntryManually(
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

                if (!entity.isPlayerList()) {
                    PlayerListPacket playerRemovePacket = new PlayerListPacket();
                    playerRemovePacket.setAction(PlayerListPacket.Action.REMOVE);
                    playerRemovePacket.getEntries().add(updatedEntry);
                    session.sendUpstreamPacket(playerRemovePacket);
                }
            }

            if (skinAndCapeConsumer != null) {
                skinAndCapeConsumer.accept(new SkinProvider.SkinAndCape(skinData.skin(), skinData.cape()));
            }
        });
    }

    public static void handleBedrockSkin(PlayerEntity playerEntity, BedrockClientData clientData) {
        GeyserImpl geyser = GeyserImpl.getInstance();
        if (geyser.getConfig().isDebugMode()) {
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.skin.bedrock.register", playerEntity.getUsername(), playerEntity.getUuid()));
        }

        try {
            byte[] skinBytes = Base64.getDecoder().decode(clientData.getSkinData().getBytes(StandardCharsets.UTF_8));
            byte[] capeBytes = clientData.getCapeData();

            byte[] geometryNameBytes = Base64.getDecoder().decode(clientData.getGeometryName().getBytes(StandardCharsets.UTF_8));
            byte[] geometryBytes = Base64.getDecoder().decode(clientData.getGeometryData().getBytes(StandardCharsets.UTF_8));

            if (skinBytes.length <= (128 * 128 * 4) && !clientData.isPersonaSkin()) {
                SkinProvider.storeBedrockSkin(playerEntity.getUuid(), clientData.getSkinId(), skinBytes);
                SkinProvider.storeBedrockGeometry(playerEntity.getUuid(), geometryNameBytes, geometryBytes);
            } else if (geyser.getConfig().isDebugMode()) {
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.skin.bedrock.fail", playerEntity.getUsername()));
                geyser.getLogger().debug("The size of '" + playerEntity.getUsername() + "' skin is: " + clientData.getSkinImageWidth() + "x" + clientData.getSkinImageHeight());
            }

            if (!clientData.getCapeId().equals("")) {
                SkinProvider.storeBedrockCape(playerEntity.getUuid(), capeBytes);
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to cache skin for bedrock user (" + playerEntity.getUsername() + "): ", e);
        }
    }

    public record GameProfileData(String skinUrl, String capeUrl, boolean isAlex) {
        /**
         * Generate the GameProfileData from the given CompoundTag representing a GameProfile
         *
         * @param tag tag to build the GameProfileData from
         * @return The built GameProfileData, or null if this wasn't a valid tag
         */
        public static @Nullable GameProfileData from(CompoundTag tag) {
            if (!(tag.get("Properties") instanceof CompoundTag propertiesTag)) {
                return null;
            }
            if (!(propertiesTag.get("textures") instanceof ListTag texturesTag) || texturesTag.size() == 0) {
                return null;
            }
            if (!(texturesTag.get(0) instanceof CompoundTag texturesData)) {
                return null;
            }
            if (!(texturesData.get("Value") instanceof StringTag skinDataValue)) {
                return null;
            }

            try {
                return loadFromJson(skinDataValue.getValue());
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().debug("Something went wrong while processing skin for tag " + tag);
                if (GeyserImpl.getInstance().getConfig().isDebugMode()) {
                    e.printStackTrace();
                }
                return null;
            }
        }

        /**
         * Generate the GameProfileData from the given GameProfile
         *
         * @param profile GameProfile to build the GameProfileData from
         * @return The built GameProfileData
         */
        public static GameProfileData from(GameProfile profile) {
            try {
                GameProfile.Property skinProperty = profile.getProperty("textures");

                if (skinProperty == null) {
                    // Likely offline mode
                    return loadBedrockOrOfflineSkin(profile);
                }
                return loadFromJson(skinProperty.getValue());
            } catch (IOException exception) {
                GeyserImpl.getInstance().getLogger().debug("Something went wrong while processing skin for " + profile.getName());
                if (GeyserImpl.getInstance().getConfig().isDebugMode()) {
                    exception.printStackTrace();
                }
                return loadBedrockOrOfflineSkin(profile);
            }
        }

        private static GameProfileData loadFromJson(String encodedJson) throws IOException {
            JsonNode skinObject = GeyserImpl.JSON_MAPPER.readTree(new String(Base64.getDecoder().decode(encodedJson), StandardCharsets.UTF_8));
            JsonNode textures = skinObject.get("textures");

            JsonNode skinTexture = textures.get("SKIN");
            String skinUrl = skinTexture.get("url").asText().replace("http://", "https://");

            boolean isAlex = skinTexture.has("metadata");

            String capeUrl = null;
            JsonNode capeTexture = textures.get("CAPE");
            if (capeTexture != null) {
                capeUrl = capeTexture.get("url").asText().replace("http://", "https://");
            }

            return new GameProfileData(skinUrl, capeUrl, isAlex);
        }

        /**
         * @return default skin with default cape when texture data is invalid, or the Bedrock player's skin if this
         * is a Bedrock player.
         */
        private static GameProfileData loadBedrockOrOfflineSkin(GameProfile profile) {
            // Fallback to the offline mode of working it out
            boolean isAlex = (Math.abs(profile.getId().hashCode() % 2) == 1);

            String skinUrl = isAlex ? SkinProvider.EMPTY_SKIN_ALEX.getTextureUrl() : SkinProvider.EMPTY_SKIN.getTextureUrl();
            String capeUrl = SkinProvider.EMPTY_CAPE.getTextureUrl();
            if (("steve".equals(skinUrl) || "alex".equals(skinUrl)) && GeyserImpl.getInstance().getConfig().getRemote().getAuthType() != AuthType.ONLINE) {
                GeyserSession session = GeyserImpl.getInstance().connectionByUuid(profile.getId());

                if (session != null) {
                    skinUrl = session.getClientData().getSkinId();
                    capeUrl = session.getClientData().getCapeId();
                }
            }
            return new GameProfileData(skinUrl, capeUrl, isAlex);
        }
    }
}

/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.nukkitx.protocol.bedrock.data.skin.ImageData;
import com.nukkitx.protocol.bedrock.data.skin.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.common.AuthType;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.event.EventManager;
import org.geysermc.connector.event.events.geyser.LoadBedrockSkinEvent;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockClientData;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

public class SkinUtils {

    public static PlayerListPacket.Entry buildCachedEntry(GeyserSession session, PlayerEntity playerEntity) {
        GameProfileData data = GameProfileData.from(playerEntity.getProfile());
        SkinProvider.Cape cape = SkinProvider.getCachedCape(data.getCapeUrl());

        SkinProvider.SkinGeometry geometry = playerEntity.getGeometry();
        if (geometry == null) {
            geometry = SkinProvider.SkinGeometry.getLegacy(data.isAlex());
        }

        SkinProvider.Skin skin = SkinProvider.getCachedSkin(data.getSkinUrl());
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
                geometry.getGeometryName(),
                geometry.getGeometryData()
        );
    }

    public static PlayerListPacket.Entry buildEntryManually(GeyserSession session, UUID uuid, String username, long geyserId,
                                                            String skinId, byte[] skinData,
                                                            String capeId, byte[] capeData,
                                                            String geometryName, String geometryData) {
        SerializedSkin serializedSkin = SerializedSkin.of(
                skinId, geometryName, ImageData.of(skinData), Collections.emptyList(),
                ImageData.of(capeData), geometryData, "", true, false, !capeId.equals(SkinProvider.EMPTY_CAPE.getCapeId()), capeId, skinId
        );

        // This attempts to find the xuid of the player so profile images show up for xbox accounts
        String xuid = "";
        for (GeyserSession player : GeyserConnector.getInstance().getPlayers()) {
            if (player.getPlayerEntity().getUuid().equals(uuid)) {
                xuid = player.getAuthData().getXboxUUID();
                break;
            }
        }

        PlayerListPacket.Entry entry;

        // If we are building a PlayerListEntry for our own session we use our AuthData UUID instead of the Java UUID
        // as bedrock expects to get back its own provided uuid
        if (session.getPlayerEntity().getUuid().equals(uuid)) {
            entry = new PlayerListPacket.Entry(session.getAuthData().getUUID());
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
        GameProfileData data = GameProfileData.from(entity.getProfile());

        SkinProvider.requestSkinAndCape(entity.getUuid(), data.getSkinUrl(), data.getCapeUrl())
                .whenCompleteAsync((skinAndCape, throwable) -> {
                    try {
                        SkinProvider.Skin skin = skinAndCape.getSkin();
                        SkinProvider.Cape cape = skinAndCape.getCape();

                        if (!entity.getUsername().isEmpty()) {
                                if (cape.isFailed()) {
                                    cape = SkinProvider.getOrDefault(SkinProvider.requestBedrockCape(
                                            entity.getUuid(), false
                                    ), SkinProvider.EMPTY_CAPE, 3);
                                }

                                if (cape.isFailed() && SkinProvider.ALLOW_THIRD_PARTY_CAPES) {
                                    cape = SkinProvider.getOrDefault(SkinProvider.requestUnofficialCape(
                                            cape, entity.getUuid(),
                                            entity.getUsername(), false
                                    ), SkinProvider.EMPTY_CAPE, SkinProvider.CapeProvider.VALUES.length * 3);
                                }
                            }

                        SkinProvider.SkinGeometry geometry = entity.getGeometry();
                            if (geometry == null) {
                                geometry = SkinProvider.SkinGeometry.getLegacy(data.isAlex());
                            }

                        geometry = SkinProvider.getOrDefault(SkinProvider.requestBedrockGeometry(
                                geometry, entity.getUuid(), false
                        ), geometry, 3);


                            // Not a bedrock player check for ears
                            if (geometry.isFailed() && SkinProvider.ALLOW_THIRD_PARTY_EARS) {
                                boolean isEars ;

                            // Its deadmau5, gotta support his skin :)
                            if (entity.getUuid().toString().equals("1e18d5ff-643d-45c8-b509-43b8461d8614")) {
                                isEars = true;
                            } else {
                                // Get the ears texture for the player
                                skin = SkinProvider.getOrDefault(SkinProvider.requestUnofficialEars(
                                        skin, entity.getUuid(), entity.getUsername(), false
                                ), skin, 3);

                                isEars = skin.isEars();
                            }

                            // Does the skin have an ears texture
                            if (isEars) {
                                // Get the new geometry
                                geometry = SkinProvider.SkinGeometry.getEars(data.isAlex());

                                // Store the skin and geometry for the ears
                                SkinProvider.storeEarSkin(entity.getUuid(), skin);
                                SkinProvider.storeEarGeometry(entity.getUuid(), data.isAlex());
                            }
                        }

                        entity.setLastSkinUpdate(skin.getRequestedOn());

                        if (session.getUpstream().isInitialized()) {
                            PlayerListPacket.Entry updatedEntry = buildEntryManually(
                                    session,
                                    entity.getUuid(),
                                    entity.getName(),
                                    entity.getGeyserId(),
                                    skin.getTextureUrl(),
                                    skin.getSkinData(),
                                    cape.getCapeId(),
                                    cape.getCapeData(),
                                    geometry.getGeometryName(),
                                    geometry.getGeometryData()
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
                    } catch (Exception e) {
                        GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.skin.fail", entity.getUuid()), e);
                    }

                    if (skinAndCapeConsumer != null) {
                        skinAndCapeConsumer.accept(skinAndCape);
                    }
                });
    }

    public static void handleBedrockSkin(PlayerEntity playerEntity, BedrockClientData clientData) {
        if (EventManager.getInstance().triggerEvent(new LoadBedrockSkinEvent(playerEntity, clientData)).getEvent().isCancelled()) {
            return;
        }

        GeyserConnector.getInstance().getLogger().info(LanguageUtils.getLocaleStringLog("geyser.skin.bedrock.register", playerEntity.getUsername(), playerEntity.getUuid()));

        try {
            byte[] skinBytes = Base64.getDecoder().decode(clientData.getSkinData().getBytes(StandardCharsets.UTF_8));
            byte[] capeBytes = clientData.getCapeData();

            byte[] geometryNameBytes = Base64.getDecoder().decode(clientData.getGeometryName().getBytes(StandardCharsets.UTF_8));
            byte[] geometryBytes = Base64.getDecoder().decode(clientData.getGeometryData().getBytes(StandardCharsets.UTF_8));

            if (skinBytes.length <= (128 * 128 * 4) && !clientData.isPersonaSkin()) {
                SkinProvider.storeBedrockSkin(playerEntity.getUuid(), clientData.getSkinId(), skinBytes);
                SkinProvider.storeBedrockGeometry(playerEntity.getUuid(), geometryNameBytes, geometryBytes);
            } else {
                GeyserConnector.getInstance().getLogger().info(LanguageUtils.getLocaleStringLog("geyser.skin.bedrock.fail", playerEntity.getUsername()));
                GeyserConnector.getInstance().getLogger().debug("The size of '" + playerEntity.getUsername() + "' skin is: " + clientData.getSkinImageWidth() + "x" + clientData.getSkinImageHeight());
            }

            if (!clientData.getCapeId().equals("")) {
                SkinProvider.storeBedrockCape(playerEntity.getUuid(), capeBytes);
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to cache skin for bedrock user (" + playerEntity.getUsername() + "): ", e);
        }
    }

    @AllArgsConstructor
    @Getter
    public static class GameProfileData {
        private final String skinUrl;
        private final String capeUrl;
        private final boolean alex;

        /**
         * Generate the GameProfileData from the given GameProfile
         *
         * @param profile GameProfile to build the GameProfileData from
         * @return The built GameProfileData
         */
        public static GameProfileData from(GameProfile profile) {
            // Fallback to the offline mode of working it out
            boolean isAlex = (Math.abs(profile.getId().hashCode() % 2) == 1);

            try {
                GameProfile.Property skinProperty = profile.getProperty("textures");

                // TODO: Remove try/catch here
                JsonNode skinObject = new ObjectMapper().readTree(new String(Base64.getDecoder().decode(skinProperty.getValue()), StandardCharsets.UTF_8));
                JsonNode textures = skinObject.get("textures");

                JsonNode skinTexture = textures.get("SKIN");
                String skinUrl = skinTexture.get("url").asText().replace("http://", "https://");

                isAlex = skinTexture.has("metadata");

                String capeUrl = null;
                if (textures.has("CAPE")) {
                    JsonNode capeTexture = textures.get("CAPE");
                    capeUrl = capeTexture.get("url").asText().replace("http://", "https://");
                }

                return new GameProfileData(skinUrl, capeUrl, isAlex);
            } catch (Exception exception) {
                if (GeyserConnector.getInstance().getAuthType() != AuthType.OFFLINE) {
                    GeyserConnector.getInstance().getLogger().debug("Got invalid texture data for " + profile.getName() + " " + exception.getMessage());
                }
                // return default skin with default cape when texture data is invalid
                String skinUrl = isAlex ? SkinProvider.EMPTY_SKIN_ALEX.getTextureUrl() : SkinProvider.EMPTY_SKIN.getTextureUrl();
                if ("steve".equals(skinUrl) || "alex".equals(skinUrl)) {
                    for (GeyserSession session : GeyserConnector.getInstance().getPlayers()) {
                        if (session.getPlayerEntity().getUuid().equals(profile.getId())) {
                            skinUrl = session.getClientData().getSkinId();
                            break;
                        }
                    }
                }
                return new GameProfileData(skinUrl, SkinProvider.EMPTY_CAPE.getTextureUrl(), isAlex);
            }
        }
    }
}

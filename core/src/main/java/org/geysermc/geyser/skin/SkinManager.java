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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.skin.ImageData;
import org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.skin.Cape;
import org.geysermc.geyser.api.skin.Skin;
import org.geysermc.geyser.api.skin.SkinData;
import org.geysermc.geyser.api.skin.SkinGeometry;
import org.geysermc.geyser.entity.type.player.AvatarEntity;
import org.geysermc.geyser.entity.type.player.SkullPlayerEntity;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.util.JsonUtils;
import org.geysermc.geyser.util.PlayerListUtils;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SkinManager {

    private static final Map<ResolvableProfile, CompletableFuture<GameProfile>> requestedProfiles = new ConcurrentHashMap<>();
    private static final Cache<ResolvableProfile, GameProfile> RESOLVED_PROFILES_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build();
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    static final String GEOMETRY = new String(FileUtils.readAllBytes("bedrock/geometries/geo.json"), StandardCharsets.UTF_8);

    /**
     * Builds a Bedrock player list entry from our existing, cached Bedrock skin information
     */
    public static PlayerListPacket.Entry buildEntryFromCachedSkin(GeyserSession session, AvatarEntity playerEntity) {
        // First: see if we have the cached skin texture ID.
        GameProfileData data = GameProfileData.from(playerEntity);
        Skin skin = null;
        Cape cape = null;
        SkinGeometry geometry = SkinGeometry.WIDE;
        if (data != null) {
            // GameProfileData is not null = server provided us with textures data to work with.
            skin = SkinProvider.getCachedSkin(data.skinUrl());
            cape = SkinProvider.getCachedCape(data.capeUrl());
            geometry = data.isAlex() ? SkinGeometry.SLIM : SkinGeometry.WIDE;
        }

        if (skin == null || cape == null) {
            // The server either didn't have a texture to send, or we didn't have the texture ID cached.
            // Let's see if this player is a Bedrock player, and if so, let's pull their skin.
            // Otherwise, grab the default player skin
            SkinData fallbackSkinData = SkinProvider.determineFallbackSkinData(playerEntity.getUuid());
            if (skin == null) {
                skin = fallbackSkinData.skin();
                geometry = fallbackSkinData.geometry();
            }
            if (cape == null) {
                cape = fallbackSkinData.cape();
            }
        }

        return PlayerListUtils.buildEntryManually(
                session,
                playerEntity.getUuid(),
                playerEntity.getUsername(),
                playerEntity.getGeyserId(),
                getSkin(session, skin.textureUrl(), skin, cape, geometry),
                // Default to white when waypoint colour is unknown, which is the most visible
                session.getWaypointCache().getWaypointColor(playerEntity.getUuid()).orElse(Color.WHITE)
        );
    }

    public static void sendSkinPacket(GeyserSession session, AvatarEntity entity, SkinData skinData) {
        Skin skin = skinData.skin();
        Cape cape = skinData.cape();
        SkinGeometry geometry = skinData.geometry();

        // Since 1.21.130: PlayerSkinPacket only works if player is listed; might as well always use the player list packet
        if (entity.getUuid().equals(session.getPlayerEntity().getUuid()) || (GameProtocol.is1_21_130orHigher(session.protocolVersion()) && !entity.isListed())) {
            PlayerListPacket.Entry entry = PlayerListUtils.buildEntryManually(
                session,
                entity.getUuid(),
                entity.getUsername(),
                entity.getGeyserId(),
                getSkin(session, skin.textureUrl(), skin, cape, geometry),
                session.getWaypointCache().getWaypointColor(entity.getUuid()).orElse(Color.WHITE)
            );

            // Slight delay ensures skins are actually shown
            session.scheduleInEventLoop(() -> {
                PlayerListUtils.sendSkinUsingPlayerList(session, entry, entity, entity.isListed());
            }, 100, TimeUnit.MILLISECONDS);
        } else {
            PlayerSkinPacket packet = new PlayerSkinPacket();
            packet.setUuid(entity.getUuid());
            packet.setOldSkinName("");
            packet.setNewSkinName(skin.textureUrl());
            packet.setSkin(getSkin(session, skin.textureUrl(), skin, cape, geometry));
            packet.setTrustedSkin(true);
            session.sendUpstreamPacket(packet);
        }
    }

    private static SerializedSkin getSkin(GeyserSession session, String skinId, Skin skin, Cape cape, SkinGeometry geometry) {
        return SerializedSkin.builder()
            .skinId(skinId)
            .skinResourcePatch(geometry.geometryName())
            .skinData(ImageData.of(skin.skinData()))
            .capeData(ImageData.of(cape.capeData()))
            .geometryData(geometry.geometryData().isBlank() ? GEOMETRY : geometry.geometryData())
            .premium(true)
            .capeId(cape.capeId())
            .fullSkinId(skinId)
            .geometryDataEngineVersion(session.getClientData().getGameVersion())
            .overridingPlayerAppearance(true)
            .build();
    }

    public static CompletableFuture<GameProfile> resolveProfile(ResolvableProfile profile) {
        GameProfile partial = profile.getProfile();
        if (!profile.isDynamic()) {
            // This is easy: the server has provided the entire profile for us (or however much it knew),
            // and is asking us to use this
            return CompletableFuture.completedFuture(partial);
        } else if (!partial.getProperties().isEmpty() || (partial.getId() == null && partial.getName() == null)) {
            // If properties have been provided to us, or no ID and no name have been provided, create a static profile from
            // what we do know
            // This replicates vanilla Java client behaviour
            String name = partial.getName() == null ? "" : partial.getName();
            UUID uuid = partial.getName() == null ? EMPTY_UUID : createOfflinePlayerUUID(partial.getName());
            GameProfile completed = new GameProfile(uuid, name);
            completed.setProperties(partial.getProperties());
            return CompletableFuture.completedFuture(completed);
        }

        GameProfile cached = RESOLVED_PROFILES_CACHE.getIfPresent(profile);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return requestedProfiles.computeIfAbsent(profile, resolvableProfile -> {
            CompletableFuture<GameProfile> future = (partial.getName() != null
                    ? SkinProvider.requestUUIDFromUsername(partial.getName()).thenApply(uuid -> new GameProfile(uuid, partial.getName()))
                    : SkinProvider.requestUsernameFromUUID(partial.getId()).thenApply(name -> new GameProfile(partial.getId(), name))
                ).thenCompose(nameAndUUID -> {
                        if (nameAndUUID.getId() == null || nameAndUUID.getName() == null) {
                            return CompletableFuture.completedFuture(partial);
                        }

                        return SkinProvider.requestTexturesFromUUID(nameAndUUID.getId())
                            .thenApply(encoded -> {
                                if (encoded == null) return partial;
                                nameAndUUID.setProperties(List.of(new GameProfile.Property("textures", encoded)));
                                return nameAndUUID;
                            });
                    })
                    .thenApply(resolved -> {
                        RESOLVED_PROFILES_CACHE.put(resolvableProfile, resolved);
                        return resolved;
                    });
            return future.whenComplete((r, t) -> requestedProfiles.remove(resolvableProfile));
        });
    }

    public static GameProfile.@Nullable Texture getTextureDataFromProfile(GameProfile profile, GameProfile.TextureType type) {
        Map<GameProfile.TextureType, GameProfile.Texture> textures;
        try {
            textures = profile.getTextures(false);
        } catch (IllegalStateException e) {
            GeyserImpl.getInstance().getLogger().debug("Could not decode textures from game profile %s, got: %s".formatted(profile, e.getMessage()));
            return null;
        }

        if (textures == null) {
            return null;
        }
        return textures.get(type);
    }

    public static void requestAndHandleSkinAndCape(AvatarEntity entity, GeyserSession session, Consumer<SkinProvider.SkinAndCape> skinAndCapeConsumer) {
        SkinProvider.requestSkinData(entity, session).whenCompleteAsync((skinData, throwable) -> {
            if (skinData != null && skinData.geometry() != null) {
                sendSkinPacket(session, entity, skinData);
            }

            if (skinAndCapeConsumer != null) {
                skinAndCapeConsumer.accept(skinData == null ? null : new SkinProvider.SkinAndCape(skinData.skin(), skinData.cape()));
            }
        });
    }

    public static void handleBedrockSkin(AvatarEntity playerEntity, BedrockClientData clientData) {
        GeyserImpl geyser = GeyserImpl.getInstance();
        if (geyser.config().debugMode()) {
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.skin.bedrock.register", playerEntity.getUsername(), playerEntity.getUuid()));
        }

        try {
            byte[] skinBytes = clientData.getSkinData();
            byte[] capeBytes = clientData.getCapeData();

            byte[] geometryNameBytes = clientData.getGeometryName();
            byte[] geometryBytes = clientData.getGeometryData();

            if (skinBytes.length <= (128 * 128 * 4) && !clientData.isPersonaSkin()) {
                SkinProvider.storeBedrockSkin(playerEntity.getUuid(), clientData.getSkinId(), skinBytes);
                SkinProvider.storeBedrockGeometry(playerEntity.getUuid(), geometryNameBytes, geometryBytes);
            } else if (geyser.config().debugMode()) {
                geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.skin.bedrock.fail", playerEntity.getUsername()));
                geyser.getLogger().debug("The size of '" + playerEntity.getUsername() + "' skin is: " + clientData.getSkinImageWidth() + "x" + clientData.getSkinImageHeight());
            }

            if (!clientData.getCapeId().equals("")) {
                SkinProvider.storeBedrockCape(clientData.getCapeId(), capeBytes);
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to cache skin for bedrock user (" + playerEntity.getUsername() + "): ", e);
        }
    }

    public static UUID createOfflinePlayerUUID(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

    public record GameProfileData(String skinUrl, String capeUrl, boolean isAlex) {
        /**
         * Generate the GameProfileData from the given CompoundTag representing a GameProfile
         *
         * @param tag tag to build the GameProfileData from
         * @return The built GameProfileData, or null if this wasn't a valid tag
         */
        public static @Nullable GameProfileData from(NbtMap tag) {
            NbtMap properties = tag.getCompound("Properties", null);
            if (properties == null) {
                return null;
            }
            List<NbtMap> textures = properties.getList("textures", NbtType.COMPOUND);
            if (textures.isEmpty()) {
                return null;
            }
            String skinDataValue = textures.get(0).getString("Value", null);
            if (skinDataValue == null) {
                return null;
            }

            try {
                return loadFromJson(skinDataValue);
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().debug("Something went wrong while processing skin for tag " + tag);
                if (GeyserImpl.getInstance().config().debugMode()) {
                    e.printStackTrace();
                }
                return null;
            }
        }

        /**
         * Generate the GameProfileData from the given player entity
         *
         * @param entity entity to build the GameProfileData from
         * @return The built GameProfileData
         */
        public static @Nullable GameProfileData from(AvatarEntity entity) {
            String texturesProperty = entity.getTexturesProperty();
            if (texturesProperty == null) {
                // Likely offline mode
                return null;
            }

            try {
                return loadFromJson(texturesProperty);
            } catch (Exception exception) {
                if (entity instanceof SkullPlayerEntity skullEntity) {
                    GeyserImpl.getInstance().getLogger().debug("Something went wrong while processing skin for skull at " + skullEntity.getSkullPosition() + " with Value: " + texturesProperty);
                } else {
                    GeyserImpl.getInstance().getLogger().debug("Something went wrong while processing skin for " + entity.getUsername() + " with Value: " + texturesProperty);
                }
                if (GeyserImpl.getInstance().config().debugMode()) {
                    exception.printStackTrace();
                }
            }
            return null;
        }

        public static @Nullable GameProfileData loadFromJson(String encodedJson) throws IOException, IllegalArgumentException {
            JsonObject skinObject;
            try {
                skinObject = JsonUtils.parseJson(new String(Base64.getDecoder().decode(encodedJson), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                GeyserImpl.getInstance().getLogger().debug("Invalid base64 encoded skin entry: " + encodedJson);
                return null;
            }

            if (!(skinObject.get("textures") instanceof JsonObject textures)) {
                return null;
            }

            if (!(textures.get("SKIN") instanceof JsonObject skinTexture)) {
                return null;
            }

            String skinUrl;
            if (skinTexture.get("url") instanceof JsonPrimitive skinUrlNode && skinUrlNode.isString()) {
                skinUrl = skinUrlNode.getAsString().replace("http://", "https://");
            } else {
                return null;
            }

            if (DEFAULT_FLOODGATE_STEVE.equals(skinUrl)) {
                // https://github.com/GeyserMC/Floodgate/commit/00b8b1b6364116ff4bc9b00e2015ce35bae8abb1 ensures that
                // Bedrock players on online-mode servers will always have a textures property. However, this skin is
                // also sent our way, and isn't overwritten. It's very likely that this skin is *only* a placeholder,
                // and no one should ever be using it outside of Floodgate, and therefore no one wants to see this
                // specific Steve skin.
                return null;
            }

            boolean isAlex = skinTexture.has("metadata");

            String capeUrl = null;
            if (textures.get("CAPE") instanceof JsonObject capeTexture) {
                if (capeTexture.get("url") instanceof JsonPrimitive capeUrlNode && capeUrlNode.isString()) {
                    capeUrl = capeUrlNode.getAsString().replace("http://", "https://");
                }
            }

            return new GameProfileData(skinUrl, capeUrl, isAlex);
        }

        private static final String DEFAULT_FLOODGATE_STEVE = "https://textures.minecraft.net/texture/31f477eb1a7beee631c2ca64d06f8f68fa93a3386d04452ab27f43acdf1b60cb";
    }
}

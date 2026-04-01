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

#include "com.google.common.cache.Cache"
#include "com.google.common.cache.CacheBuilder"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.skin.ImageData"
#include "org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerSkinPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.skin.Cape"
#include "org.geysermc.geyser.api.skin.Skin"
#include "org.geysermc.geyser.api.skin.SkinData"
#include "org.geysermc.geyser.api.skin.SkinGeometry"
#include "org.geysermc.geyser.entity.type.player.AvatarEntity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.auth.BedrockClientData"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.util.FileUtils"
#include "org.geysermc.geyser.util.PlayerListUtils"
#include "org.geysermc.geyser.util.WebUtils"
#include "org.geysermc.mcprotocollib.auth.GameProfile"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile"

#include "java.awt.*"
#include "java.nio.charset.StandardCharsets"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Objects"
#include "java.util.UUID"
#include "java.util.concurrent.CompletableFuture"
#include "java.util.concurrent.ConcurrentHashMap"
#include "java.util.concurrent.TimeUnit"
#include "java.util.function.Consumer"

public class SkinManager {

    private static final Map<ResolvableProfile, CompletableFuture<GameProfile>> requestedProfiles = new ConcurrentHashMap<>();
    private static final Cache<ResolvableProfile, GameProfile> RESOLVED_PROFILES_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .build();
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    static final std::string GEOMETRY = new std::string(FileUtils.readAllBytes("bedrock/geometries/geo.json"), StandardCharsets.UTF_8);


    public static PlayerListPacket.Entry buildEntryFromCachedSkin(GeyserSession session, AvatarEntity playerEntity) {

        GameProfileData data = GameProfileData.from(playerEntity);
        Skin skin = null;
        Cape cape = null;
        SkinGeometry geometry = SkinGeometry.WIDE;
        if (data != null) {

            skin = SkinProvider.getCachedSkin(data.skinUrl());
            cape = SkinProvider.getCachedCape(data.capeUrl());
            geometry = data.isSlim() ? SkinGeometry.SLIM : SkinGeometry.WIDE;
        }

        if (skin == null || cape == null) {



            SkinData fallbackSkinData = SkinProvider.determineFallbackSkinData(playerEntity.uuid());
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
                playerEntity.uuid(),
                playerEntity.getUsername(),
                playerEntity.geyserId(),
                getSkin(session, skin.textureUrl(), skin, cape, geometry),

                session.getWaypointCache().getWaypointColor(playerEntity.uuid()).orElse(Color.WHITE)
        );
    }

    public static void sendSkinPacket(GeyserSession session, AvatarEntity entity, SkinData skinData) {
        Skin skin = skinData.skin();
        Cape cape = skinData.cape();
        SkinGeometry geometry = skinData.geometry();


        if (entity.uuid().equals(session.getPlayerEntity().uuid()) || !entity.isListed()) {
            PlayerListPacket.Entry entry = PlayerListUtils.buildEntryManually(
                session,
                entity.uuid(),
                entity.getUsername(),
                entity.geyserId(),
                getSkin(session, skin.textureUrl(), skin, cape, geometry),
                session.getWaypointCache().getWaypointColor(entity.uuid()).orElse(Color.WHITE)
            );


            session.scheduleInEventLoop(() -> {
                PlayerListUtils.sendSkinUsingPlayerList(session, entry, entity, entity.isListed());
            }, 100, TimeUnit.MILLISECONDS);
        } else {
            PlayerSkinPacket packet = new PlayerSkinPacket();
            packet.setUuid(entity.uuid());
            packet.setOldSkinName("");
            packet.setNewSkinName(skin.textureUrl());
            packet.setSkin(getSkin(session, skin.textureUrl(), skin, cape, geometry));
            packet.setTrustedSkin(true);
            session.sendUpstreamPacket(packet);
        }
    }

    private static SerializedSkin getSkin(GeyserSession session, std::string skinId, Skin skin, Cape cape, SkinGeometry geometry) {
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


            return CompletableFuture.completedFuture(partial);
        } else if (!partial.getProperties().isEmpty() || (partial.getId() == null && partial.getName() == null)) {



            std::string name = partial.getName() == null ? "" : partial.getName();
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

    public static GameProfile.Texture getTextureDataFromProfile(GameProfile profile, GameProfile.TextureType type) {
        Map<GameProfile.TextureType, GameProfile.Texture> textures;
        try {
            textures = profile.getTextures(false);
        } catch (IllegalStateException e) {
            GeyserImpl.getInstance().getLogger().debug("Could not decode textures from game profile (%s)! Got: %s", profile, e);
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
            geyser.getLogger().info(GeyserLocale.getLocaleStringLog("geyser.skin.bedrock.register", playerEntity.getUsername(), playerEntity.uuid()));
        }

        try {
            byte[] skinBytes = clientData.getSkinData();
            byte[] capeBytes = clientData.getCapeData();

            byte[] geometryNameBytes = clientData.getGeometryName();
            byte[] geometryBytes = clientData.getGeometryData();

            if (skinBytes.length <= (128 * 128 * 4) && !clientData.isPersonaSkin()) {
                SkinProvider.storeBedrockSkin(playerEntity.uuid(), clientData.getSkinId(), skinBytes);
                SkinProvider.storeBedrockGeometry(playerEntity.uuid(), geometryNameBytes, geometryBytes);
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

    public static UUID createOfflinePlayerUUID(std::string username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

    public record GameProfileData(std::string skinUrl, std::string capeUrl, bool isSlim) {

        public static GameProfileData from(AvatarEntity entity) {
            Map<GameProfile.TextureType, GameProfile.Texture> textures = entity.getTextures();
            if (textures == null) {


                return null;
            }

            GameProfile.Texture skin = textures.get(GameProfile.TextureType.SKIN);
            if (skin == null) {
                return null;
            }

            std::string skinUrl = WebUtils.toHttps(skin.getURL());
            if (Objects.equals(DEFAULT_FLOODGATE_STEVE, skinUrl)) {





                return null;
            }

            GameProfile.Texture cape = textures.get(GameProfile.TextureType.CAPE);
            std::string capeUrl = cape == null ? null : WebUtils.toHttps(cape.getURL());
            return new GameProfileData(skinUrl, capeUrl, skin.getModel() == GameProfile.TextureModel.SLIM);
        }

        private static final std::string DEFAULT_FLOODGATE_STEVE = "https://textures.minecraft.net/texture/31f477eb1a7beee631c2ca64d06f8f68fa93a3386d04452ab27f43acdf1b60cb";
    }
}

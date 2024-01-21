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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.network.AuthType;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.util.WebUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class SkinProvider {
    private static final boolean ALLOW_THIRD_PARTY_CAPES = GeyserImpl.getInstance().getConfig().isAllowThirdPartyCapes();
    private static ExecutorService EXECUTOR_SERVICE;

    static final Skin EMPTY_SKIN;
    static final Cape EMPTY_CAPE = new Cape("", "no-cape", ByteArrays.EMPTY_ARRAY, -1, true);

    private static final Cache<String, Cape> CACHED_JAVA_CAPES = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    private static final Cache<String, Skin> CACHED_JAVA_SKINS = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    private static final Cache<String, Cape> CACHED_BEDROCK_CAPES = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    private static final Cache<String, Skin> CACHED_BEDROCK_SKINS = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    private static final Map<String, CompletableFuture<Cape>> requestedCapes = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Skin>> requestedSkins = new ConcurrentHashMap<>();

    private static final Map<UUID, SkinGeometry> cachedGeometry = new ConcurrentHashMap<>();

    /**
     * Citizens NPCs use UUID version 2, while legitimate Minecraft players use version 4, and
     * offline mode players use version 3.
     */
    private static final Predicate<UUID> IS_NPC = uuid -> uuid.version() == 2;

    private static final boolean ALLOW_THIRD_PARTY_EARS = GeyserImpl.getInstance().getConfig().isAllowThirdPartyEars();
    private static final String EARS_GEOMETRY;
    private static final String EARS_GEOMETRY_SLIM;
    static final SkinGeometry SKULL_GEOMETRY;
    static final SkinGeometry WEARING_CUSTOM_SKULL;
    static final SkinGeometry WEARING_CUSTOM_SKULL_SLIM;

    static {
        // Generate the empty texture to use as an emergency fallback
        final int pink = -524040;
        final int black = -16777216;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(64 * 4 + 64 * 4);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int rgba;
                if (y > 32) {
                    rgba = x >= 32 ? pink : black;
                } else {
                    rgba = x >= 32 ? black : pink;
                }
                outputStream.write((rgba >> 16) & 0xFF); // Red
                outputStream.write((rgba >> 8) & 0xFF); // Green
                outputStream.write(rgba & 0xFF); // Blue
                outputStream.write((rgba >> 24) & 0xFF); // Alpha
            }
        }
        EMPTY_SKIN = new Skin(-1, "geysermc:empty", outputStream.toByteArray());

        /* Load in the normal ears geometry */
        EARS_GEOMETRY = new String(FileUtils.readAllBytes("bedrock/skin/geometry.humanoid.ears.json"), StandardCharsets.UTF_8);

        /* Load in the slim ears geometry */
        EARS_GEOMETRY_SLIM = new String(FileUtils.readAllBytes("bedrock/skin/geometry.humanoid.earsSlim.json"), StandardCharsets.UTF_8);

        /* Load in the custom skull geometry */
        String skullData = new String(FileUtils.readAllBytes("bedrock/skin/geometry.humanoid.customskull.json"), StandardCharsets.UTF_8);
        SKULL_GEOMETRY = new SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.customskull\"}}", skullData, false);

        /* Load in the player head skull geometry */
        String wearingCustomSkull = new String(FileUtils.readAllBytes("bedrock/skin/geometry.humanoid.wearingCustomSkull.json"), StandardCharsets.UTF_8);
        WEARING_CUSTOM_SKULL = new SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.wearingCustomSkull\"}}", wearingCustomSkull, false);
        String wearingCustomSkullSlim = new String(FileUtils.readAllBytes("bedrock/skin/geometry.humanoid.wearingCustomSkullSlim.json"), StandardCharsets.UTF_8);
        WEARING_CUSTOM_SKULL_SLIM = new SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.wearingCustomSkullSlim\"}}", wearingCustomSkullSlim, false);
    }

    public static ExecutorService getExecutorService() {
        if (EXECUTOR_SERVICE == null) {
            EXECUTOR_SERVICE = Executors.newFixedThreadPool(ALLOW_THIRD_PARTY_CAPES ? 21 : 14);
        }
        return EXECUTOR_SERVICE;
    }

    public static void shutdown() {
        if (EXECUTOR_SERVICE != null) {
            EXECUTOR_SERVICE.shutdown();
            EXECUTOR_SERVICE = null;
        }
    }

    public static void registerCacheImageTask(GeyserImpl geyser) {
        // Schedule Daily Image Expiry if we are caching them
        if (geyser.getConfig().getCacheImages() > 0) {
            geyser.getScheduledThread().scheduleAtFixedRate(() -> {
                File cacheFolder = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("images").toFile();
                if (!cacheFolder.exists()) {
                    return;
                }

                int count = 0;
                final long expireTime = ((long) GeyserImpl.getInstance().getConfig().getCacheImages()) * ((long)1000 * 60 * 60 * 24);
                for (File imageFile : Objects.requireNonNull(cacheFolder.listFiles())) {
                    if (imageFile.lastModified() < System.currentTimeMillis() - expireTime) {
                        //noinspection ResultOfMethodCallIgnored
                        imageFile.delete();
                        count++;
                    }
                }

                if (count > 0) {
                    GeyserImpl.getInstance().getLogger().debug(String.format("Removed %d cached image files as they have expired", count));
                }
            }, 10, 1440, TimeUnit.MINUTES);
        }
    }

    /**
     * Search our cached database for an already existing, translated skin of this Java URL.
     */
    static Skin getCachedSkin(String skinUrl) {
        return CACHED_JAVA_SKINS.getIfPresent(skinUrl);
    }

    /**
     * If skin data fails to apply, or there is no skin data to apply, determine what skin we should give as a fallback.
     */
    static SkinData determineFallbackSkinData(UUID uuid) {
        Skin skin = null;
        Cape cape = null;
        SkinGeometry geometry = SkinGeometry.WIDE;

        if (GeyserImpl.getInstance().getConfig().getRemote().authType() != AuthType.ONLINE) {
            // Let's see if this player is a Bedrock player, and if so, let's pull their skin.
            GeyserSession session = GeyserImpl.getInstance().connectionByUuid(uuid);
            if (session != null) {
                String skinId = session.getClientData().getSkinId();
                skin = CACHED_BEDROCK_SKINS.getIfPresent(skinId);
                String capeId = session.getClientData().getCapeId();
                cape = CACHED_BEDROCK_CAPES.getIfPresent(capeId);
                geometry = cachedGeometry.getOrDefault(uuid, geometry);
            }
        }

        if (skin == null) {
            // We don't have a skin for the player right now. Fall back to a default.
            ProvidedSkins.ProvidedSkin providedSkin = ProvidedSkins.getDefaultPlayerSkin(uuid);
            skin = providedSkin.getData();
            geometry = providedSkin.isSlim() ? SkinProvider.SkinGeometry.SLIM : SkinProvider.SkinGeometry.WIDE;
        }

        if (cape == null) {
            cape = EMPTY_CAPE;
        }

        return new SkinData(skin, cape, geometry);
    }

    /**
     * Used as a fallback if an official Java cape doesn't exist for this user.
     */
    @NonNull
    private static Cape getCachedBedrockCape(UUID uuid) {
        GeyserSession session = GeyserImpl.getInstance().connectionByUuid(uuid);
        if (session != null) {
            String capeId = session.getClientData().getCapeId();
            Cape bedrockCape = CACHED_BEDROCK_CAPES.getIfPresent(capeId);
            if (bedrockCape != null) {
                return bedrockCape;
            }
        }
        return EMPTY_CAPE;
    }

    @Nullable
    static Cape getCachedCape(String capeUrl) {
        if (capeUrl == null) {
            return null;
        }
        return CACHED_JAVA_CAPES.getIfPresent(capeUrl);
    }

    static CompletableFuture<SkinProvider.SkinData> requestSkinData(PlayerEntity entity) {
        SkinManager.GameProfileData data = SkinManager.GameProfileData.from(entity);
        if (data == null) {
            // This player likely does not have a textures property
            return CompletableFuture.completedFuture(determineFallbackSkinData(entity.getUuid()));
        }

        return requestSkinAndCape(entity.getUuid(), data.skinUrl(), data.capeUrl())
                .thenApplyAsync(skinAndCape -> {
                    try {
                        Skin skin = skinAndCape.skin();
                        Cape cape = skinAndCape.cape();
                        SkinGeometry geometry = data.isAlex() ? SkinGeometry.SLIM : SkinGeometry.WIDE;

                        // Whether we should see if this player has a Bedrock skin we should check for on failure of
                        // any skin property
                        boolean checkForBedrock = entity.getUuid().version() != 4;

                        if (cape.failed() && checkForBedrock) {
                            cape = getCachedBedrockCape(entity.getUuid());
                        }

                        if (cape.failed() && ALLOW_THIRD_PARTY_CAPES) {
                            cape = getOrDefault(requestUnofficialCape(
                                    cape, entity.getUuid(),
                                    entity.getUsername(), false
                            ), EMPTY_CAPE, CapeProvider.VALUES.length * 3);
                        }

                        boolean isDeadmau5 = "deadmau5".equals(entity.getUsername());
                        // Not a bedrock player check for ears
                        if (geometry.failed() && (ALLOW_THIRD_PARTY_EARS || isDeadmau5)) {
                            boolean isEars;

                            // Its deadmau5, gotta support his skin :)
                            if (isDeadmau5) {
                                isEars = true;
                            } else {
                                // Get the ears texture for the player
                                skin = getOrDefault(requestUnofficialEars(
                                        skin, entity.getUuid(), entity.getUsername(), false
                                ), skin, 3);

                                isEars = skin.isEars();
                            }

                            // Does the skin have an ears texture
                            if (isEars) {
                                // Get the new geometry
                                geometry = SkinGeometry.getEars(data.isAlex());

                                // Store the skin and geometry for the ears
                                storeEarSkin(skin);
                                storeEarGeometry(entity.getUuid(), data.isAlex());
                            }
                        }

                        return new SkinData(skin, cape, geometry);
                    } catch (Exception e) {
                        GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.skin.fail", entity.getUuid()), e);
                    }

                    return new SkinData(skinAndCape.skin(), skinAndCape.cape(), null);
                });
    }

    private static CompletableFuture<SkinAndCape> requestSkinAndCape(UUID playerId, String skinUrl, String capeUrl) {
        return CompletableFuture.supplyAsync(() -> {
            long time = System.currentTimeMillis();

            CapeProvider provider = capeUrl != null ? CapeProvider.MINECRAFT : null;
            SkinAndCape skinAndCape = new SkinAndCape(
                    getOrDefault(requestSkin(playerId, skinUrl, false), EMPTY_SKIN, 5),
                    getOrDefault(requestCape(capeUrl, provider, false), EMPTY_CAPE, 5)
            );

            GeyserImpl.getInstance().getLogger().debug("Took " + (System.currentTimeMillis() - time) + "ms for " + playerId);
            return skinAndCape;
        }, getExecutorService());
    }

    static CompletableFuture<Skin> requestSkin(UUID playerId, String textureUrl, boolean newThread) {
        if (textureUrl == null || textureUrl.isEmpty()) return CompletableFuture.completedFuture(EMPTY_SKIN);
        CompletableFuture<Skin> requestedSkin = requestedSkins.get(textureUrl);
        if (requestedSkin != null) {
            // already requested
            return requestedSkin;
        }

        Skin cachedSkin = CACHED_JAVA_SKINS.getIfPresent(textureUrl);
        if (cachedSkin != null) {
            return CompletableFuture.completedFuture(cachedSkin);
        }

        CompletableFuture<Skin> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplySkin(playerId, textureUrl), getExecutorService())
                    .whenCompleteAsync((skin, throwable) -> {
                        skin.updated = true;
                        CACHED_JAVA_SKINS.put(textureUrl, skin);
                        requestedSkins.remove(textureUrl);
                    });
            requestedSkins.put(textureUrl, future);
        } else {
            Skin skin = supplySkin(playerId, textureUrl);
            future = CompletableFuture.completedFuture(skin);
            CACHED_JAVA_SKINS.put(textureUrl, skin);
        }
        return future;
    }

    private static CompletableFuture<Cape> requestCape(String capeUrl, CapeProvider provider, boolean newThread) {
        if (capeUrl == null || capeUrl.isEmpty()) return CompletableFuture.completedFuture(EMPTY_CAPE);
        CompletableFuture<Cape> requestedCape = requestedCapes.get(capeUrl);
        if (requestedCape != null) {
            return requestedCape;
        }

        Cape cachedCape = CACHED_JAVA_CAPES.getIfPresent(capeUrl);
        if (cachedCape != null) {
            return CompletableFuture.completedFuture(cachedCape);
        }

        CompletableFuture<Cape> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplyCape(capeUrl, provider), getExecutorService())
                    .whenCompleteAsync((cape, throwable) -> {
                        CACHED_JAVA_CAPES.put(capeUrl, cape);
                        requestedCapes.remove(capeUrl);
                    });
            requestedCapes.put(capeUrl, future);
        } else {
            Cape cape = supplyCape(capeUrl, provider); // blocking
            future = CompletableFuture.completedFuture(cape);
            CACHED_JAVA_CAPES.put(capeUrl, cape);
        }
        return future;
    }

    private static CompletableFuture<Cape> requestUnofficialCape(Cape officialCape, UUID playerId,
                                                                String username, boolean newThread) {
        if (officialCape.failed() && ALLOW_THIRD_PARTY_CAPES) {
            for (CapeProvider provider : CapeProvider.VALUES) {
                if (provider.type != CapeUrlType.USERNAME && IS_NPC.test(playerId)) {
                    continue;
                }

                Cape cape1 = getOrDefault(
                        requestCape(provider.getUrlFor(playerId, username), provider, newThread),
                        EMPTY_CAPE, 4
                );
                if (!cape1.failed()) {
                    return CompletableFuture.completedFuture(cape1);
                }
            }
        }
        return CompletableFuture.completedFuture(officialCape);
    }

    private static CompletableFuture<Skin> requestEars(String earsUrl, boolean newThread, Skin skin) {
        if (earsUrl == null || earsUrl.isEmpty()) return CompletableFuture.completedFuture(skin);

        CompletableFuture<Skin> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplyEars(skin, earsUrl), getExecutorService())
                    .whenCompleteAsync((outSkin, throwable) -> { });
        } else {
            Skin ears = supplyEars(skin, earsUrl); // blocking
            future = CompletableFuture.completedFuture(ears);
        }
        return future;
    }

    /**
     * Try and find an ear texture for a Java player
     *
     * @param officialSkin The current players skin
     * @param playerId The players UUID
     * @param username The players username
     * @param newThread Should we start in a new thread
     * @return The updated skin with ears
     */
    private static CompletableFuture<Skin> requestUnofficialEars(Skin officialSkin, UUID playerId, String username, boolean newThread) {
        for (EarsProvider provider : EarsProvider.VALUES) {
            if (provider.type != CapeUrlType.USERNAME && IS_NPC.test(playerId)) {
                continue;
            }

            Skin skin1 = getOrDefault(
                    requestEars(provider.getUrlFor(playerId, username), newThread, officialSkin),
                    officialSkin, 4
            );
            if (skin1.isEars()) {
                return CompletableFuture.completedFuture(skin1);
            }
        }

        return CompletableFuture.completedFuture(officialSkin);
    }

    static void storeBedrockSkin(UUID playerID, String skinId, byte[] skinData) {
        Skin skin = new Skin(playerID, skinId, skinData, System.currentTimeMillis(), true, false);
        CACHED_BEDROCK_SKINS.put(skin.getTextureUrl(), skin);
    }

    static void storeBedrockCape(String capeId, byte[] capeData) {
        Cape cape = new Cape(capeId, capeId, capeData, System.currentTimeMillis(), false);
        CACHED_BEDROCK_CAPES.put(capeId, cape);
    }

    static void storeBedrockGeometry(UUID playerID, byte[] geometryName, byte[] geometryData) {
        SkinGeometry geometry = new SkinGeometry(new String(geometryName), new String(geometryData), false);
        cachedGeometry.put(playerID, geometry);
    }

    /**
     * Stores the adjusted skin with the ear texture to the cache
     *
     * @param skin The skin to cache
     */
    public static void storeEarSkin(Skin skin) {
        CACHED_JAVA_SKINS.put(skin.getTextureUrl(), skin);
    }

    /**
     * Stores the geometry for a Java player with ears
     *
     * @param playerID The UUID to cache it against
     * @param isSlim If the player is using an slim base
     */
    private static void storeEarGeometry(UUID playerID, boolean isSlim) {
        cachedGeometry.put(playerID, SkinGeometry.getEars(isSlim));
    }

    private static Skin supplySkin(UUID uuid, String textureUrl) {
        try {
            byte[] skin = requestImageData(textureUrl, null);
            return new Skin(uuid, textureUrl, skin, System.currentTimeMillis(), false, false);
        } catch (Exception ignored) {} // just ignore I guess

        return new Skin(uuid, "empty", EMPTY_SKIN.getSkinData(), System.currentTimeMillis(), false, false);
    }

    private static Cape supplyCape(String capeUrl, CapeProvider provider) {
        byte[] cape = EMPTY_CAPE.capeData();
        try {
            cape = requestImageData(capeUrl, provider);
        } catch (Exception ignored) {
        } // just ignore I guess

        String[] urlSection = capeUrl.split("/"); // A real url is expected at this stage

        return new Cape(
                capeUrl,
                urlSection[urlSection.length - 1], // get the texture id and use it as cape id
                cape,
                System.currentTimeMillis(),
                cape.length == 0
        );
    }

    /**
     * Get the ears texture and place it on the skin from the given URL
     *
     * @param existingSkin The players current skin
     * @param earsUrl The URL to get the ears texture from
     * @return The updated skin with ears
     */
    private static Skin supplyEars(Skin existingSkin, String earsUrl) {
        try {
            // Get the ears texture
            BufferedImage ears = ImageIO.read(new URL(earsUrl));
            if (ears == null) throw new NullPointerException();

            // Convert the skin data to a BufferedImage
            int height = (existingSkin.getSkinData().length / 4 / 64);
            BufferedImage skinImage = imageDataToBufferedImage(existingSkin.getSkinData(), 64, height);

            // Create a new image with the ears texture over it
            BufferedImage newSkin = new BufferedImage(skinImage.getWidth(), skinImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) newSkin.getGraphics();
            g.drawImage(skinImage, 0, 0, null);
            g.drawImage(ears, 24, 0, null);

            // Turn the buffered image back into an array of bytes
            byte[] data = bufferedImageToImageData(newSkin);
            skinImage.flush();

            // Create a new skin object with the new infomation
            return new Skin(
                    existingSkin.getSkinOwner(),
                    existingSkin.getTextureUrl(),
                    data,
                    System.currentTimeMillis(),
                    true,
                    true
            );
        } catch (Exception ignored) {} // just ignore I guess

        return existingSkin;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static BufferedImage requestImage(String imageUrl, CapeProvider provider) throws IOException {
        BufferedImage image = null;

        // First see if we have a cached file. We also update the modification stamp so we know when the file was last used
        File imageFile = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("images").resolve(UUID.nameUUIDFromBytes(imageUrl.getBytes()) + ".png").toFile();
        if (imageFile.exists()) {
            try {
                GeyserImpl.getInstance().getLogger().debug("Reading cached image from file " + imageFile.getPath() + " for " + imageUrl);
                imageFile.setLastModified(System.currentTimeMillis());
                image = ImageIO.read(imageFile);
            } catch (IOException ignored) {}
        }

        // If no image we download it
        if (image == null) {
            image = downloadImage(imageUrl, provider);
            GeyserImpl.getInstance().getLogger().debug("Downloaded " + imageUrl);

            // Write to cache if we are allowed
            if (GeyserImpl.getInstance().getConfig().getCacheImages() > 0) {
                imageFile.getParentFile().mkdirs();
                try {
                    ImageIO.write(image, "png", imageFile);
                    GeyserImpl.getInstance().getLogger().debug("Writing cached skin to file " + imageFile.getPath() + " for " + imageUrl);
                } catch (IOException e) {
                    GeyserImpl.getInstance().getLogger().error("Failed to write cached skin to file " + imageFile.getPath() + " for " + imageUrl);
                }
            }
        }

        // if the requested image is a cape
        if (provider != null) {
            if (image.getWidth() > 64 || image.getHeight() > 32) {
                // Prevent weirdly-scaled capes from being cut off
                BufferedImage newImage = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
                Graphics g = newImage.createGraphics();
                g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
                g.dispose();
                image.flush();
                image = scale(newImage, 64, 32);
            } else if (image.getWidth() < 64 || image.getHeight() < 32) {
                // Bedrock doesn't like smaller-sized capes, either.
                BufferedImage newImage = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
                Graphics g = newImage.createGraphics();
                g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
                g.dispose();
                image.flush();
                image = newImage;
            }
        } else {
            // Very rarely, skins can be larger than Minecraft's default.
            // Bedrock will not render anything above a width of 128.
            if (image.getWidth() > 128) {
                // On Height: Scale by the amount we divided width by, or simply cut down to 128
                image = scale(image, 128, image.getHeight() >= 256 ? (image.getHeight() / (image.getWidth() / 128)) : 128);
            }

            // TODO remove alpha channel
        }

        return image;
    }

    private static byte[] requestImageData(String imageUrl, CapeProvider provider) throws Exception {
        BufferedImage image = requestImage(imageUrl, provider);
        byte[] data = bufferedImageToImageData(image);
        image.flush();
        return data;
    }

    /**
     * Request textures from a player's UUID
     *
     * @param uuid the player's UUID without any hyphens
     * @return a completable GameProfile with textures included
     */
    public static CompletableFuture<@Nullable String> requestTexturesFromUUID(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode node = WebUtils.getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
                JsonNode properties = node.get("properties");
                if (properties == null) {
                    GeyserImpl.getInstance().getLogger().debug("No properties found in Mojang response for " + uuid);
                    return null;
                }
                return node.get("properties").get(0).get("value").asText();
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().debug("Unable to request textures for " + uuid);
                if (GeyserImpl.getInstance().getConfig().isDebugMode()) {
                    e.printStackTrace();
                }
                return null;
            }
        }, getExecutorService());
    }

    /**
     * Request textures from a player's username
     *
     * @param username the player's username
     * @return a completable GameProfile with textures included
     */
    public static CompletableFuture<@Nullable String> requestTexturesFromUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Offline skin, or no present UUID
                JsonNode node = WebUtils.getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
                JsonNode id = node.get("id");
                if (id == null) {
                    GeyserImpl.getInstance().getLogger().debug("No UUID found in Mojang response for " + username);
                    return null;
                }
                return id.asText();
            } catch (Exception e) {
                if (GeyserImpl.getInstance().getConfig().isDebugMode()) {
                    e.printStackTrace();
                }
                return null;
            }
        }, getExecutorService()).thenCompose(uuid -> {
            if (uuid == null) {
                return CompletableFuture.completedFuture(null);
            }
            return requestTexturesFromUUID(uuid);
        });
    }

    private static BufferedImage downloadImage(String imageUrl, CapeProvider provider) throws IOException {
        BufferedImage image;
        if (provider == CapeProvider.FIVEZIG) {
            image = readFiveZigCape(imageUrl);
        } else {
            HttpURLConnection con = (HttpURLConnection) new URL(imageUrl).openConnection();
            con.setRequestProperty("User-Agent", "Geyser-" + GeyserImpl.getInstance().getPlatformType().toString() + "/" + GeyserImpl.VERSION);
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);

            image = ImageIO.read(con.getInputStream());
        }

        if (image == null) {
            throw new IllegalArgumentException("Failed to read image from: %s (cape provider=%s)".formatted(imageUrl, provider));
        }
        return image;
    }

    private static @Nullable BufferedImage readFiveZigCape(String url) throws IOException {
        JsonNode element = GeyserImpl.JSON_MAPPER.readTree(WebUtils.getBody(url));
        if (element != null && element.isObject()) {
            JsonNode capeElement = element.get("d");
            if (capeElement == null || capeElement.isNull()) return null;
            return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(capeElement.textValue())));
        }
        return null;
    }

    public static BufferedImage scale(BufferedImage bufferedImage, int newWidth, int newHeight) {
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(bufferedImage, 0, 0, newWidth, newHeight, null);
        g2.dispose();
        bufferedImage.flush();
        return resized;
    }

    /**
     * Get the RGBA int for a given index in some image data
     *
     * @param index Index to get
     * @param data Image data to find in
     * @return An int representing RGBA
     */
    private static int getRGBA(int index, byte[] data) {
        return (data[index] & 0xFF) << 16 | (data[index + 1] & 0xFF) << 8 |
                data[index + 2] & 0xFF | (data[index + 3] & 0xFF) << 24;
    }

    /**
     * Convert a byte[] to a BufferedImage
     *
     * @param imageData The byte[] to convert
     * @param imageWidth The width of the target image
     * @param imageHeight The height of the target image
     * @return The converted BufferedImage
     */
    public static BufferedImage imageDataToBufferedImage(byte[] imageData, int imageWidth, int imageHeight) {
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        int index = 0;
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                image.setRGB(x, y, getRGBA(index, imageData));
                index += 4;
            }
        }

        return image;
    }

    /**
     * Convert a BufferedImage to a byte[]
     *
     * @param image The BufferedImage to convert
     * @return The converted byte[]
     */
    public static byte[] bufferedImageToImageData(BufferedImage image) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(image.getWidth() * 4 + image.getHeight() * 4);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgba = image.getRGB(x, y);
                outputStream.write((rgba >> 16) & 0xFF);
                outputStream.write((rgba >> 8) & 0xFF);
                outputStream.write(rgba & 0xFF);
                outputStream.write((rgba >> 24) & 0xFF);
            }
        }
        return outputStream.toByteArray();
    }

    public static <T> T getOrDefault(CompletableFuture<T> future, T defaultValue, int timeoutInSeconds) {
        try {
            return future.get(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
        return defaultValue;
    }

    public record SkinAndCape(Skin skin, Cape cape) {
    }

    /**
     * Represents a full package of skin, cape, and geometry.
     */
    public record SkinData(Skin skin, Cape cape, SkinGeometry geometry) {
    }

    @AllArgsConstructor
    @Getter
    public static class Skin {
        private UUID skinOwner;
        private final String textureUrl;
        private final byte[] skinData;
        private final long requestedOn;
        private boolean updated;
        private boolean ears;

        Skin(long requestedOn, String textureUrl, byte[] skinData) {
            this.requestedOn = requestedOn;
            this.textureUrl = textureUrl;
            this.skinData = skinData;
        }
    }

    public record Cape(String textureUrl, String capeId, byte[] capeData, long requestedOn, boolean failed) {
    }

    public record SkinGeometry(String geometryName, String geometryData, boolean failed) {
        public static SkinGeometry WIDE = getLegacy(false);
        public static SkinGeometry SLIM = getLegacy(true);

        /**
         * Generate generic geometry
         *
         * @param isSlim Should it be the alex model
         * @return The generic geometry object
         */
        private static SkinGeometry getLegacy(boolean isSlim) {
            return new SkinProvider.SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.custom" + (isSlim ? "Slim" : "") + "\"}}", "", true);
        }

        /**
         * Generate basic geometry with ears
         *
         * @param isSlim Should it be the alex model
         * @return The generated geometry for the ears model
         */
        private static SkinGeometry getEars(boolean isSlim) {
            return new SkinProvider.SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.ears" + (isSlim ? "Slim" : "") + "\"}}", (isSlim ? EARS_GEOMETRY_SLIM : EARS_GEOMETRY), false);
        }
    }

    /*
     * Sorted by 'priority'
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public enum CapeProvider {
        MINECRAFT,
        OPTIFINE("https://optifine.net/capes/%s.png", CapeUrlType.USERNAME),
        LABYMOD("https://dl.labymod.net/capes/%s", CapeUrlType.UUID_DASHED),
        FIVEZIG("https://textures.5zigreborn.eu/profile/%s", CapeUrlType.UUID_DASHED),
        MINECRAFTCAPES("https://api.minecraftcapes.net/profile/%s/cape", CapeUrlType.UUID);

        public static final CapeProvider[] VALUES = Arrays.copyOfRange(values(), 1, 5);
        private String url;
        private CapeUrlType type;

        public String getUrlFor(String type) {
            return String.format(url, type);
        }

        public String getUrlFor(UUID uuid, String username) {
            return getUrlFor(toRequestedType(type, uuid, username));
        }

        public static String toRequestedType(CapeUrlType type, UUID uuid, String username) {
            return switch (type) {
                case UUID -> uuid.toString().replace("-", "");
                case UUID_DASHED -> uuid.toString();
                default -> username;
            };
        }
    }

    public enum CapeUrlType {
        USERNAME,
        UUID,
        UUID_DASHED
    }

    /*
     * Sorted by 'priority'
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public enum EarsProvider {
        MINECRAFTCAPES("https://api.minecraftcapes.net/profile/%s/ears", CapeUrlType.UUID);

        public static final EarsProvider[] VALUES = values();
        private String url;
        private CapeUrlType type;

        public String getUrlFor(String type) {
            return String.format(url, type);
        }

        public String getUrlFor(UUID uuid, String username) {
            return getUrlFor(toRequestedType(type, uuid, username));
        }

        public static String toRequestedType(CapeUrlType type, UUID uuid, String username) {
            return switch (type) {
                case UUID -> uuid.toString().replace("-", "");
                case UUID_DASHED -> uuid.toString();
                default -> username;
            };
        }
    }
}

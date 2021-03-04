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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.WebUtils;

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
import java.util.List;
import java.util.*;
import java.util.concurrent.*;

public class SkinProvider {
    public static final boolean ALLOW_THIRD_PARTY_CAPES = GeyserConnector.getInstance().getConfig().isAllowThirdPartyCapes();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(ALLOW_THIRD_PARTY_CAPES ? 21 : 14);

    public static final byte[] STEVE_SKIN = new ProvidedSkin("bedrock/skin/skin_steve.png").getSkin();
    public static final Skin EMPTY_SKIN = new Skin(-1, "steve", STEVE_SKIN);
    public static final byte[] ALEX_SKIN = new ProvidedSkin("bedrock/skin/skin_alex.png").getSkin();
    public static final Skin EMPTY_SKIN_ALEX = new Skin(-1, "alex", ALEX_SKIN);
    private static final Map<String, Skin> permanentSkins = new HashMap<String, Skin>() {{
        put("steve", EMPTY_SKIN);
        put("alex", EMPTY_SKIN_ALEX);
    }};
    private static final Cache<String, Skin> cachedSkins = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    private static final Map<String, CompletableFuture<Skin>> requestedSkins = new ConcurrentHashMap<>();

    public static final Cape EMPTY_CAPE = new Cape("", "no-cape", new byte[0], -1, true);
    private static final Cache<String, Cape> cachedCapes = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    private static final Map<String, CompletableFuture<Cape>> requestedCapes = new ConcurrentHashMap<>();

    private static final Map<UUID, SkinGeometry> cachedGeometry = new ConcurrentHashMap<>();

    public static final boolean ALLOW_THIRD_PARTY_EARS = GeyserConnector.getInstance().getConfig().isAllowThirdPartyEars();
    public static final String EARS_GEOMETRY;
    public static final String EARS_GEOMETRY_SLIM;
    public static final SkinGeometry SKULL_GEOMETRY;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        /* Load in the normal ears geometry */
        EARS_GEOMETRY = new String(FileUtils.readAllBytes(FileUtils.getResource("bedrock/skin/geometry.humanoid.ears.json")), StandardCharsets.UTF_8);

        /* Load in the slim ears geometry */
        EARS_GEOMETRY_SLIM = new String(FileUtils.readAllBytes(FileUtils.getResource("bedrock/skin/geometry.humanoid.earsSlim.json")), StandardCharsets.UTF_8);

        /* Load in the custom skull geometry */
        String skullData = new String(FileUtils.readAllBytes(FileUtils.getResource("bedrock/skin/geometry.humanoid.customskull.json")), StandardCharsets.UTF_8);
        SKULL_GEOMETRY = new SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.customskull\"}}", skullData, false);

        // Schedule Daily Image Expiry if we are caching them
        if (GeyserConnector.getInstance().getConfig().getCacheImages() > 0) {
            GeyserConnector.getInstance().getGeneralThreadPool().scheduleAtFixedRate(() -> {
                File cacheFolder = GeyserConnector.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("images").toFile();
                if (!cacheFolder.exists()) {
                    return;
                }

                int count = 0;
                final long expireTime = ((long)GeyserConnector.getInstance().getConfig().getCacheImages()) * ((long)1000 * 60 * 60 * 24);
                for (File imageFile : Objects.requireNonNull(cacheFolder.listFiles())) {
                    if (imageFile.lastModified() < System.currentTimeMillis() - expireTime) {
                        //noinspection ResultOfMethodCallIgnored
                        imageFile.delete();
                        count++;
                    }
                }

                if (count > 0) {
                    GeyserConnector.getInstance().getLogger().debug(String.format("Removed %d cached image files as they have expired", count));
                }
            }, 10, 1440, TimeUnit.MINUTES);
        }
    }

    public static boolean hasCapeCached(String capeUrl) {
        return cachedCapes.getIfPresent(capeUrl) != null;
    }

    public static Skin getCachedSkin(String skinUrl) {
        return permanentSkins.getOrDefault(skinUrl, cachedSkins.getIfPresent(skinUrl));
    }

    public static Cape getCachedCape(String capeUrl) {
        Cape cape = capeUrl != null ? cachedCapes.getIfPresent(capeUrl) : EMPTY_CAPE;
        return cape != null ? cape : EMPTY_CAPE;
    }

    public static CompletableFuture<SkinAndCape> requestSkinAndCape(UUID playerId, String skinUrl, String capeUrl) {
        return CompletableFuture.supplyAsync(() -> {
            long time = System.currentTimeMillis();
            String newSkinUrl = skinUrl;

            if ("steve".equals(skinUrl) || "alex".equals(skinUrl)) {
                GeyserSession session = GeyserConnector.getInstance().getPlayerByUuid(playerId);

                if (session != null) {
                    newSkinUrl = session.getClientData().getSkinId();
                }
            }

            CapeProvider provider = capeUrl != null ? CapeProvider.MINECRAFT : null;
            SkinAndCape skinAndCape = new SkinAndCape(
                    getOrDefault(requestSkin(playerId, newSkinUrl, false), EMPTY_SKIN, 5),
                    getOrDefault(requestCape(capeUrl, provider, false), EMPTY_CAPE, 5)
            );

            GeyserConnector.getInstance().getLogger().debug("Took " + (System.currentTimeMillis() - time) + "ms for " + playerId);
            return skinAndCape;
        }, EXECUTOR_SERVICE);
    }

    public static CompletableFuture<Skin> requestSkin(UUID playerId, String textureUrl, boolean newThread) {
        if (textureUrl == null || textureUrl.isEmpty()) return CompletableFuture.completedFuture(EMPTY_SKIN);
        if (requestedSkins.containsKey(textureUrl)) return requestedSkins.get(textureUrl); // already requested

        Skin cachedSkin = getCachedSkin(textureUrl);
        if (cachedSkin != null) {
            return CompletableFuture.completedFuture(cachedSkin);
        }

        CompletableFuture<Skin> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplySkin(playerId, textureUrl), EXECUTOR_SERVICE)
                    .whenCompleteAsync((skin, throwable) -> {
                        skin.updated = true;
                        cachedSkins.put(textureUrl, skin);
                        requestedSkins.remove(textureUrl);
                    });
            requestedSkins.put(textureUrl, future);
        } else {
            Skin skin = supplySkin(playerId, textureUrl);
            future = CompletableFuture.completedFuture(skin);
            cachedSkins.put(textureUrl, skin);
        }
        return future;
    }

    public static CompletableFuture<Cape> requestCape(String capeUrl, CapeProvider provider, boolean newThread) {
        if (capeUrl == null || capeUrl.isEmpty()) return CompletableFuture.completedFuture(EMPTY_CAPE);
        if (requestedCapes.containsKey(capeUrl)) return requestedCapes.get(capeUrl); // already requested

        Cape cachedCape = cachedCapes.getIfPresent(capeUrl);
        if (cachedCape != null) {
            return CompletableFuture.completedFuture(cachedCape);
        }

        CompletableFuture<Cape> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplyCape(capeUrl, provider), EXECUTOR_SERVICE)
                    .whenCompleteAsync((cape, throwable) -> {
                        cachedCapes.put(capeUrl, cape);
                        requestedCapes.remove(capeUrl);
                    });
            requestedCapes.put(capeUrl, future);
        } else {
            Cape cape = supplyCape(capeUrl, provider); // blocking
            future = CompletableFuture.completedFuture(cape);
            cachedCapes.put(capeUrl, cape);
        }
        return future;
    }

    public static CompletableFuture<Cape> requestUnofficialCape(Cape officialCape, UUID playerId,
                                                                String username, boolean newThread) {
        if (officialCape.isFailed() && ALLOW_THIRD_PARTY_CAPES) {
            for (CapeProvider provider : CapeProvider.VALUES) {
                Cape cape1 = getOrDefault(
                        requestCape(provider.getUrlFor(playerId, username), provider, newThread),
                        EMPTY_CAPE, 4
                );
                if (!cape1.isFailed()) {
                    return CompletableFuture.completedFuture(cape1);
                }
            }
        }
        return CompletableFuture.completedFuture(officialCape);
    }

    public static CompletableFuture<Skin> requestEars(String earsUrl, boolean newThread, Skin skin) {
        if (earsUrl == null || earsUrl.isEmpty()) return CompletableFuture.completedFuture(skin);

        CompletableFuture<Skin> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplyEars(skin, earsUrl), EXECUTOR_SERVICE)
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
    public static CompletableFuture<Skin> requestUnofficialEars(Skin officialSkin, UUID playerId, String username, boolean newThread) {
        for (EarsProvider provider : EarsProvider.VALUES) {
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

    public static CompletableFuture<Cape> requestBedrockCape(UUID playerID) {
        Cape bedrockCape = cachedCapes.getIfPresent(playerID.toString() + ".Bedrock");
        if (bedrockCape == null) {
            bedrockCape = EMPTY_CAPE;
        }
        return CompletableFuture.completedFuture(bedrockCape);
    }

    public static CompletableFuture<SkinGeometry> requestBedrockGeometry(SkinGeometry currentGeometry, UUID playerID) {
        SkinGeometry bedrockGeometry = cachedGeometry.getOrDefault(playerID, currentGeometry);
        return CompletableFuture.completedFuture(bedrockGeometry);
    }

    public static void storeBedrockSkin(UUID playerID, String skinID, byte[] skinData) {
        Skin skin = new Skin(playerID, skinID, skinData, System.currentTimeMillis(), true, false);
        cachedSkins.put(skin.getTextureUrl(), skin);
    }

    public static void storeBedrockCape(UUID playerID, byte[] capeData) {
        Cape cape = new Cape(playerID.toString() + ".Bedrock", playerID.toString(), capeData, System.currentTimeMillis(), false);
        cachedCapes.put(playerID.toString() + ".Bedrock", cape);
    }

    public static void storeBedrockGeometry(UUID playerID, byte[] geometryName, byte[] geometryData) {
        SkinGeometry geometry = new SkinGeometry(new String(geometryName), new String(geometryData), false);
        cachedGeometry.put(playerID, geometry);
    }

    /**
     * Stores the adjusted skin with the ear texture to the cache
     *
     * @param skin The skin to cache
     */
    public static void storeEarSkin(Skin skin) {
        cachedSkins.put(skin.getTextureUrl(), skin);
    }

    /**
     * Stores the geometry for a Java player with ears
     *
     * @param playerID The UUID to cache it against
     * @param isSlim If the player is using an slim base
     */
    public static void storeEarGeometry(UUID playerID, boolean isSlim) {
        cachedGeometry.put(playerID, SkinGeometry.getEars(isSlim));
    }

    private static Skin supplySkin(UUID uuid, String textureUrl) {
        try {
            byte[] skin = requestImage(textureUrl, null);
            return new Skin(uuid, textureUrl, skin, System.currentTimeMillis(), false, false);
        } catch (Exception ignored) {} // just ignore I guess

        return new Skin(uuid, "empty", EMPTY_SKIN.getSkinData(), System.currentTimeMillis(), false, false);
    }

    private static Cape supplyCape(String capeUrl, CapeProvider provider) {
        byte[] cape = EMPTY_CAPE.getCapeData();
        try {
            cape = requestImage(capeUrl, provider);
        } catch (Exception ignored) {} // just ignore I guess

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
    private static byte[] requestImage(String imageUrl, CapeProvider provider) throws Exception {
        BufferedImage image = null;

        // First see if we have a cached file. We also update the modification stamp so we know when the file was last used
        File imageFile = GeyserConnector.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("images").resolve(UUID.nameUUIDFromBytes(imageUrl.getBytes()).toString() + ".png").toFile();
        if (imageFile.exists()) {
            try {
                GeyserConnector.getInstance().getLogger().debug("Reading cached image from file " + imageFile.getPath() + " for " + imageUrl);
                imageFile.setLastModified(System.currentTimeMillis());
                image = ImageIO.read(imageFile);
            } catch (IOException ignored) {}
        }

        // If no image we download it
        if (image == null) {
            image = downloadImage(imageUrl, provider);
            GeyserConnector.getInstance().getLogger().debug("Downloaded " + imageUrl);

            // Write to cache if we are allowed
            if (GeyserConnector.getInstance().getConfig().getCacheImages() > 0) {
                imageFile.getParentFile().mkdirs();
                try {
                    ImageIO.write(image, "png", imageFile);
                    GeyserConnector.getInstance().getLogger().debug("Writing cached skin to file " + imageFile.getPath() + " for " + imageUrl);
                } catch (IOException e) {
                    GeyserConnector.getInstance().getLogger().error("Failed to write cached skin to file " + imageFile.getPath() + " for " + imageUrl);
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

        byte[] data = bufferedImageToImageData(image);
        image.flush();
        return data;
    }

    /**
     * If a skull has a username but no textures, request them.
     * @param skullOwner the CompoundTag of the skull with no textures
     * @return a completable GameProfile with textures included
     */
    public static CompletableFuture<GameProfile> requestTexturesFromUsername(CompoundTag skullOwner) {
        return CompletableFuture.supplyAsync(() -> {
            Tag uuidTag = skullOwner.get("Id");
            String uuidToString = "";
            JsonNode node;
            GameProfile gameProfile = new GameProfile(UUID.randomUUID(), "");
            boolean retrieveUuidFromInternet = !(uuidTag instanceof IntArrayTag); // also covers null check

            if (!retrieveUuidFromInternet) {
                int[] uuidAsArray = ((IntArrayTag) uuidTag).getValue();
                // thank u viaversion
                UUID uuid = new UUID((long) uuidAsArray[0] << 32 | ((long) uuidAsArray[1] & 0xFFFFFFFFL),
                        (long) uuidAsArray[2] << 32 | ((long) uuidAsArray[3] & 0xFFFFFFFFL));
                retrieveUuidFromInternet = uuid.version() != 4;
                uuidToString = uuid.toString().replace("-", "");
            }

            try {
                if (retrieveUuidFromInternet) {
                    // Offline skin, or no present UUID
                    node = WebUtils.getJson("https://api.mojang.com/users/profiles/minecraft/" + skullOwner.get("Name").getValue());
                    JsonNode id = node.get("id");
                    if (id == null) {
                        GeyserConnector.getInstance().getLogger().debug("No UUID found in Mojang response for " + skullOwner.get("Name").getValue());
                        return null;
                    }
                    uuidToString = id.asText();
                }

                // Get textures from UUID
                node = WebUtils.getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidToString);
                List<GameProfile.Property> profileProperties = new ArrayList<>();
                JsonNode properties = node.get("properties");
                if (properties == null) {
                    GeyserConnector.getInstance().getLogger().debug("No properties found in Mojang response for " + uuidToString);
                    return null;
                }
                profileProperties.add(new GameProfile.Property("textures", node.get("properties").get(0).get("value").asText()));
                gameProfile.setProperties(profileProperties);
                return gameProfile;
            } catch (Exception e) {
                if (GeyserConnector.getInstance().getConfig().isDebugMode()) {
                    e.printStackTrace();
                }
                return null;
            }
        }, EXECUTOR_SERVICE);
    }

    private static BufferedImage downloadImage(String imageUrl, CapeProvider provider) throws IOException {
        if (provider == CapeProvider.FIVEZIG)
            return readFiveZigCape(imageUrl);

        HttpURLConnection con = (HttpURLConnection) new URL(imageUrl).openConnection();
        con.setRequestProperty("User-Agent", "Geyser-" + GeyserConnector.getInstance().getPlatformType().toString() + "/" + GeyserConnector.VERSION);

        BufferedImage image = ImageIO.read(con.getInputStream());
        if (image == null) throw new NullPointerException();
        return image;
    }

    private static BufferedImage readFiveZigCape(String url) throws IOException {
        JsonNode element = OBJECT_MAPPER.readTree(WebUtils.getBody(url));
        if (element != null && element.isObject()) {
            JsonNode capeElement = element.get("d");
            if (capeElement == null || capeElement.isNull()) return null;
            return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(capeElement.textValue())));
        }
        return null;
    }

    private static BufferedImage scale(BufferedImage bufferedImage, int newWidth, int newHeight) {
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

    @AllArgsConstructor
    @Getter
    public static class SkinAndCape {
        private final Skin skin;
        private final Cape cape;
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

        private Skin(long requestedOn, String textureUrl, byte[] skinData) {
            this.requestedOn = requestedOn;
            this.textureUrl = textureUrl;
            this.skinData = skinData;
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Cape {
        private final String textureUrl;
        private final String capeId;
        private final byte[] capeData;
        private final long requestedOn;
        private final boolean failed;
    }

    @AllArgsConstructor
    @Getter
    public static class SkinGeometry {
        private final String geometryName;
        private final String geometryData;
        private final boolean failed;

        /**
         * Generate generic geometry
         *
         * @param isSlim Should it be the alex model
         * @return The generic geometry object
         */
        public static SkinGeometry getLegacy(boolean isSlim) {
            return new SkinProvider.SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.custom" + (isSlim ? "Slim" : "") + "\"}}", "", true);
        }

        /**
         * Generate basic geometry with ears
         *
         * @param isSlim Should it be the alex model
         * @return The generated geometry for the ears model
         */
        public static SkinGeometry getEars(boolean isSlim) {
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
        MINECRAFTCAPES("https://minecraftcapes.net/profile/%s/cape", CapeUrlType.UUID);

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
            switch (type) {
                case UUID: return uuid.toString().replace("-", "");
                case UUID_DASHED: return uuid.toString();
                default: return username;
            }
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
        MINECRAFTCAPES("https://minecraftcapes.net/profile/%s/ears", CapeUrlType.UUID);

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
            switch (type) {
                case UUID: return uuid.toString().replace("-", "");
                case UUID_DASHED: return uuid.toString();
                default: return username;
            }
        }
    }
}

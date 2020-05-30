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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.geysermc.connector.GeyserConnector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class SkinProvider {
    public static final boolean ALLOW_THIRD_PARTY_CAPES = GeyserConnector.getInstance().getConfig().isAllowThirdPartyCapes();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(ALLOW_THIRD_PARTY_CAPES ? 21 : 14);

    public static final byte[] STEVE_SKIN = new ProvidedSkin("bedrock/skin/skin_steve.png").getSkin();
    public static final Skin EMPTY_SKIN = new Skin(-1, "steve", STEVE_SKIN);
    public static final byte[] ALEX_SKIN = new ProvidedSkin("bedrock/skin/skin_alex.png").getSkin();
    public static final Skin EMPTY_SKIN_ALEX = new Skin(-1, "alex", ALEX_SKIN);
    private static Map<UUID, Skin> cachedSkins = new ConcurrentHashMap<>();
    private static Map<UUID, CompletableFuture<Skin>> requestedSkins = new ConcurrentHashMap<>();

    public static final Cape EMPTY_CAPE = new Cape("", "no-cape", new byte[0], -1, true);
    private static Map<String, Cape> cachedCapes = new ConcurrentHashMap<>();
    private static Map<String, CompletableFuture<Cape>> requestedCapes = new ConcurrentHashMap<>();

    public static final SkinGeometry EMPTY_GEOMETRY = SkinProvider.SkinGeometry.getLegacy(false);
    private static Map<UUID, SkinGeometry> cachedGeometry = new ConcurrentHashMap<>();

    public static final boolean ALLOW_THIRD_PARTY_EARS = GeyserConnector.getInstance().getConfig().isAllowThirdPartyEars();
    public static String EARS_GEOMETRY;
    public static String EARS_GEOMETRY_SLIM;
    public static SkinGeometry SKULL_GEOMETRY;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int CACHE_INTERVAL = 8 * 60 * 1000; // 8 minutes

    static {
        /* Load in the normal ears geometry */
        InputStream earsStream = FileUtils.getResource("bedrock/skin/geometry.humanoid.ears.json");

        StringBuilder earsDataBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(earsStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                earsDataBuilder.append((char) c);
            }
        } catch (IOException e) {
            throw new AssertionError("Unable to load ears geometry", e);
        }

        EARS_GEOMETRY = earsDataBuilder.toString();


        /* Load in the slim ears geometry */
        earsStream = FileUtils.getResource("bedrock/skin/geometry.humanoid.earsSlim.json");

        earsDataBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(earsStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                earsDataBuilder.append((char) c);
            }
        } catch (IOException e) {
            throw new AssertionError("Unable to load ears geometry", e);
        }

        EARS_GEOMETRY_SLIM = earsDataBuilder.toString();

        /* Load in the custom skull geometry */
        InputStream skullStream = FileUtils.getResource("bedrock/skin/geometry.humanoid.customskull.json");

        StringBuilder skullDataBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader(skullStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                skullDataBuilder.append((char) c);
            }
        } catch (IOException e) {
            throw new AssertionError("Unable to load skull geometry", e);
        }

        SKULL_GEOMETRY = new SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.customskull" + "\"}}", skullDataBuilder.toString(), false);
    }

    public static boolean hasSkinCached(UUID uuid) {
        return cachedSkins.containsKey(uuid);
    }

    public static boolean hasCapeCached(String capeUrl) {
        return cachedCapes.containsKey(capeUrl);
    }

    public static Skin getCachedSkin(UUID uuid) {
        return cachedSkins.getOrDefault(uuid, EMPTY_SKIN);
    }

    public static Cape getCachedCape(String capeUrl) {
        return capeUrl != null ? cachedCapes.getOrDefault(capeUrl, EMPTY_CAPE) : EMPTY_CAPE;
    }

    public static CompletableFuture<SkinAndCape> requestSkinAndCape(UUID playerId, String skinUrl, String capeUrl) {
        return CompletableFuture.supplyAsync(() -> {
            long time = System.currentTimeMillis();

            CapeProvider provider = capeUrl != null ? CapeProvider.MINECRAFT : null;
            SkinAndCape skinAndCape = new SkinAndCape(
                    getOrDefault(requestSkin(playerId, skinUrl, false), EMPTY_SKIN, 5),
                    getOrDefault(requestCape(capeUrl, provider, false), EMPTY_CAPE, 5)
            );

            GeyserConnector.getInstance().getLogger().debug("Took " + (System.currentTimeMillis() - time) + "ms for " + playerId);
            return skinAndCape;
        }, EXECUTOR_SERVICE);
    }

    public static CompletableFuture<Skin> requestSkin(UUID playerId, String textureUrl, boolean newThread) {
        if (textureUrl == null || textureUrl.isEmpty()) return CompletableFuture.completedFuture(EMPTY_SKIN);
        if (requestedSkins.containsKey(playerId)) return requestedSkins.get(playerId); // already requested

        if ((System.currentTimeMillis() - CACHE_INTERVAL) < cachedSkins.getOrDefault(playerId, EMPTY_SKIN).getRequestedOn()) {
            // no need to update, still cached
            return CompletableFuture.completedFuture(cachedSkins.get(playerId));
        }

        CompletableFuture<Skin> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplySkin(playerId, textureUrl), EXECUTOR_SERVICE)
                    .whenCompleteAsync((skin, throwable) -> {
                        if (!cachedSkins.getOrDefault(playerId, EMPTY_SKIN).getTextureUrl().equals(textureUrl)) {
                            skin.updated = true;
                            cachedSkins.put(playerId, skin);
                        }
                        requestedSkins.remove(skin.getSkinOwner());
                    });
            requestedSkins.put(playerId, future);
        } else {
            Skin skin = supplySkin(playerId, textureUrl);
            future = CompletableFuture.completedFuture(skin);
            cachedSkins.put(playerId, skin);
        }
        return future;
    }

    public static CompletableFuture<Cape> requestCape(String capeUrl, CapeProvider provider, boolean newThread) {
        if (capeUrl == null || capeUrl.isEmpty()) return CompletableFuture.completedFuture(EMPTY_CAPE);
        if (requestedCapes.containsKey(capeUrl)) return requestedCapes.get(capeUrl); // already requested

        boolean officialCape = provider == CapeProvider.MINECRAFT;
        boolean validCache = (System.currentTimeMillis() - CACHE_INTERVAL) < cachedCapes.getOrDefault(capeUrl, EMPTY_CAPE).getRequestedOn();

        if ((cachedCapes.containsKey(capeUrl) && officialCape) || validCache) {
            // the cape is an official cape (static) or the cape doesn't need a update yet
            return CompletableFuture.completedFuture(cachedCapes.get(capeUrl));
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

    public static CompletableFuture<Skin> requestEars(String earsUrl, EarsProvider provider, boolean newThread, Skin skin) {
        if (earsUrl == null || earsUrl.isEmpty()) return CompletableFuture.completedFuture(skin);

        CompletableFuture<Skin> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplyEars(skin, earsUrl, provider), EXECUTOR_SERVICE)
                    .whenCompleteAsync((outSkin, throwable) -> { });
        } else {
            Skin ears = supplyEars(skin, earsUrl, provider); // blocking
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
                    requestEars(provider.getUrlFor(playerId, username), provider, newThread, officialSkin),
                    officialSkin, 4
            );
            if (skin1.isEars()) {
                return CompletableFuture.completedFuture(skin1);
            }
        }

        return CompletableFuture.completedFuture(officialSkin);
    }

    public static CompletableFuture<Cape> requestBedrockCape(UUID playerID, boolean newThread) {
        Cape bedrockCape = cachedCapes.getOrDefault(playerID.toString() + ".Bedrock", EMPTY_CAPE);
        return CompletableFuture.completedFuture(bedrockCape);
    }

    public static CompletableFuture<SkinGeometry> requestBedrockGeometry(SkinGeometry currentGeometry, UUID playerID, boolean newThread) {
        SkinGeometry bedrockGeometry = cachedGeometry.getOrDefault(playerID, currentGeometry);
        return CompletableFuture.completedFuture(bedrockGeometry);
    }

    public static void storeBedrockSkin(UUID playerID, String skinID, byte[] skinData) {
        Skin skin = new Skin(playerID, skinID, skinData, System.currentTimeMillis(), true, false);
        cachedSkins.put(playerID, skin);
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
     * Stores the ajusted skin with the ear texture to the cache
     *
     * @param playerID The UUID to cache it against
     * @param skin The skin to cache
     */
    public static void storeEarSkin(UUID playerID, Skin skin) {
        cachedSkins.put(playerID, skin);
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
        byte[] skin = EMPTY_SKIN.getSkinData();
        try {
            skin = requestImage(textureUrl, null);
        } catch (Exception ignored) {} // just ignore I guess
        return new Skin(uuid, textureUrl, skin, System.currentTimeMillis(), false, false);
    }

    private static Cape supplyCape(String capeUrl, CapeProvider provider) {
        byte[] cape = new byte[0];
        try {
            cape = requestImage(capeUrl, provider);
        } catch (Exception ignored) {} // just ignore I guess

        String[] urlSection = capeUrl.split("/"); // A real url is expected at this stage

        return new Cape(
                capeUrl,
                urlSection[urlSection.length - 1], // get the texture id and use it as cape id
                cape.length > 0 ? cape : EMPTY_CAPE.getCapeData(),
                System.currentTimeMillis(),
                cape.length == 0
        );
    }

    /**
     * Get the ears texture and place it on the skin from the given URL
     *
     * @param existingSkin The players current skin
     * @param earsUrl The URL to get the ears texture from
     * @param provider The ears texture provider
     * @return The updated skin with ears
     */
    private static Skin supplyEars(Skin existingSkin, String earsUrl, EarsProvider provider) {
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

    private static byte[] requestImage(String imageUrl, CapeProvider provider) throws Exception {
        BufferedImage image = downloadImage(imageUrl, provider);
        GeyserConnector.getInstance().getLogger().debug("Downloaded " + imageUrl);

        // if the requested image is an cape
        if (provider != null) {
            while(image.getWidth() > 64) {
                image = scale(image);
            }
            BufferedImage newImage = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics g = newImage.createGraphics();
            g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
            g.dispose();
            image = newImage;
        }

        byte[] data = bufferedImageToImageData(image);
        image.flush();
        return data;
    }

    private static BufferedImage downloadImage(String imageUrl, CapeProvider provider) throws IOException {
        if (provider == CapeProvider.FIVEZIG)
            return readFiveZigCape(imageUrl);
        BufferedImage image = ImageIO.read(new URL(imageUrl));
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

    private static BufferedImage scale(BufferedImage bufferedImage) {
        BufferedImage resized = new BufferedImage(bufferedImage.getWidth() / 2, bufferedImage.getHeight() / 2, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(bufferedImage, 0, 0, bufferedImage.getWidth() / 2, bufferedImage.getHeight() / 2, null);
        g2.dispose();
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
        private Skin skin;
        private Cape cape;
    }

    @AllArgsConstructor
    @Getter
    public static class Skin {
        private UUID skinOwner;
        private String textureUrl;
        private byte[] skinData;
        private long requestedOn;
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
        private String textureUrl;
        private String capeId;
        private byte[] capeData;
        private long requestedOn;
        private boolean failed;
    }

    @AllArgsConstructor
    @Getter
    public static class SkinGeometry {
        private String geometryName;
        private String geometryData;
        private boolean failed;

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

        /**
         * Generate basic geometry for custom skulls
         *
         * @return The generated geometry for the skull model
         */
        public static SkinGeometry getSkull() {
            return SKULL_GEOMETRY;
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
        OPTIFINE("http://s.optifine.net/capes/%s.png", CapeUrlType.USERNAME),
        LABYMOD("https://www.labymod.net/page/php/getCapeTexture.php?uuid=%s", CapeUrlType.UUID_DASHED),
        FIVEZIG("https://textures.5zigreborn.eu/profile/%s", CapeUrlType.UUID_DASHED),
        MINECRAFTCAPES("https://www.minecraftcapes.co.uk/getCape/%s", CapeUrlType.UUID);

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
        MINECRAFTCAPES("https://www.minecraftcapes.co.uk/getEars/%s", CapeUrlType.UUID);

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

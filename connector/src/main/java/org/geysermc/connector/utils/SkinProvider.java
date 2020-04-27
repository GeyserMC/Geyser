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

import org.geysermc.connector.GeyserConnector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class SkinProvider {
    public static final boolean ALLOW_THIRD_PARTY_CAPES = GeyserConnector.getInstance().getConfig().isAllowThirdPartyCapes();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(ALLOW_THIRD_PARTY_CAPES ? 21 : 14);

    public static final byte[] STEVE_SKIN = new ProvidedSkin("bedrock/skin/skin_steve.png").getSkin();
    public static final Skin EMPTY_SKIN = new Skin(-1, "steve", STEVE_SKIN);
    private static Map<UUID, Skin> cachedSkins = new ConcurrentHashMap<>();
    private static Map<UUID, CompletableFuture<Skin>> requestedSkins = new ConcurrentHashMap<>();

    public static final Cape EMPTY_CAPE = new Cape("", "no-cape", new byte[0], -1, true);
    private static Map<String, Cape> cachedCapes = new ConcurrentHashMap<>();
    private static Map<String, CompletableFuture<Cape>> requestedCapes = new ConcurrentHashMap<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int CACHE_INTERVAL = 8 * 60 * 1000; // 8 minutes

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

            SkinAndCape skinAndCape = new SkinAndCape(
                    getOrDefault(requestSkin(playerId, skinUrl, false), EMPTY_SKIN, 5),
                    getOrDefault(requestCape(capeUrl, false), EMPTY_CAPE, 5)
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

    public static CompletableFuture<Cape> requestCape(String capeUrl, boolean newThread) {
        if (capeUrl == null || capeUrl.isEmpty()) return CompletableFuture.completedFuture(EMPTY_CAPE);
        if (requestedCapes.containsKey(capeUrl)) return requestedCapes.get(capeUrl); // already requested

        boolean officialCape = capeUrl.startsWith("https://textures.minecraft.net");
        boolean validCache = (System.currentTimeMillis() - CACHE_INTERVAL) < cachedCapes.getOrDefault(capeUrl, EMPTY_CAPE).getRequestedOn();

        if ((cachedCapes.containsKey(capeUrl) && officialCape) || validCache) {
            // the cape is an official cape (static) or the cape doesn't need a update yet
            return CompletableFuture.completedFuture(cachedCapes.get(capeUrl));
        }

        CompletableFuture<Cape> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplyCape(capeUrl), EXECUTOR_SERVICE)
                    .whenCompleteAsync((cape, throwable) -> {
                        cachedCapes.put(capeUrl, cape);
                        requestedCapes.remove(capeUrl);
                    });
            requestedCapes.put(capeUrl, future);
        } else {
            Cape cape = supplyCape(capeUrl); // blocking
            future = CompletableFuture.completedFuture(cape);
            cachedCapes.put(capeUrl, cape);
        }
        return future;
    }

    public static CompletableFuture<Cape> requestUnofficialCape(Cape officialCape, UUID playerId,
                                                                String username, boolean newThread) {
        if (officialCape.isFailed() && ALLOW_THIRD_PARTY_CAPES) {
            for (UnofficalCape cape : UnofficalCape.VALUES) {
                Cape cape1 = getOrDefault(
                        requestCape(cape.getUrlFor(playerId, username), newThread),
                        EMPTY_CAPE, 4
                );
                if (!cape1.isFailed()) {
                    return CompletableFuture.completedFuture(cape1);
                }
            }
        }
        return CompletableFuture.completedFuture(officialCape);
    }

    private static Skin supplySkin(UUID uuid, String textureUrl) {
        byte[] skin = EMPTY_SKIN.getSkinData();
        try {
            skin = requestImage(textureUrl, false);
        } catch (Exception ignored) {} // just ignore I guess
        return new Skin(uuid, textureUrl, skin, System.currentTimeMillis(), false);
    }

    private static Cape supplyCape(String capeUrl) {
        byte[] cape = new byte[0];
        try {
            cape = requestImage(capeUrl, true);
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

    private static byte[] requestImage(String imageUrl, boolean cape) throws Exception {
        BufferedImage image;
        if (imageUrl.startsWith(UnofficalCape.FIVEZIG.url)) {
            image = readCapeFromJson(imageUrl);
        } else {
            image = ImageIO.read(new URL(imageUrl));
        }
        GeyserConnector.getInstance().getLogger().debug("Downloaded " + imageUrl);

        if (cape) {
            image = image.getWidth() > 64 ? scale(image) : image;
            BufferedImage newImage = new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB);
            Graphics g = newImage.createGraphics();
            g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
            g.dispose();
            image = newImage;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(image.getWidth() * 4 + image.getHeight() * 4)) {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgba = image.getRGB(x, y);
                    outputStream.write((rgba >> 16) & 0xFF);
                    outputStream.write((rgba >> 8) & 0xFF);
                    outputStream.write(rgba & 0xFF);
                    outputStream.write((rgba >> 24) & 0xFF);
                }
            }
            image.flush();
            return outputStream.toByteArray();
        }
    }

    private static BufferedImage readCapeFromJson(String url) throws IOException {
        JsonNode element = OBJECT_MAPPER.readTree(WebUtils.getBody(url));
        if (element != null && element.isObject()) {
            JsonNode capeElement = element.get("d");
            if (capeElement == null || capeElement.isNull()) return null;
            return ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(capeElement.textValue())));
        }
        return null;
    }

    private static BufferedImage scale(BufferedImage bufferedImage) {
        BufferedImage resized = new BufferedImage(bufferedImage.getWidth() / 2, bufferedImage.getHeight() / 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(bufferedImage, 0, 0, bufferedImage.getWidth() / 2, bufferedImage.getHeight() / 2, null);
        g2.dispose();
        return resized;
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

    /*
     * Sorted by 'priority'
     */
    @AllArgsConstructor
    @Getter
    public enum UnofficalCape {
        OPTIFINE("http://s.optifine.net/capes/%s.png", CapeUrlType.USERNAME),
        LABYMOD("https://www.labymod.net/page/php/getCapeTexture.php?uuid=%s", CapeUrlType.UUID_DASHED),
        FIVEZIG("https://textures.5zigreborn.eu/profile/%s", CapeUrlType.UUID_DASHED),
        MINECRAFTCAPES("https://www.minecraftcapes.co.uk/getCape/%s", CapeUrlType.UUID);

        public static final UnofficalCape[] VALUES = values();
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
                case UUID:
                    return uuid.toString().replace("-", "");
                case UUID_DASHED:
                    return uuid.toString();
                default:
                    return username;
            }
        }
    }

    public enum CapeUrlType {
        USERNAME,
        UUID,
        UUID_DASHED
    }
}

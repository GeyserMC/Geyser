package org.geysermc.connector.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.api.Geyser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class SkinProvider {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(14);
    @Getter private static final Gson gson = new GsonBuilder().create();

    private static Map<UUID, Skin> cachedSkins = new ConcurrentHashMap<>();
    private static Map<String, Cape> cachedCapes = new ConcurrentHashMap<>();

    public static final Skin EMPTY_SKIN = new Skin(-1, "");
    public static final Cape EMPTY_CAPE = new Cape("", new byte[0]);
    private static final int CACHE_INTERVAL = 8 * 60 * 1000; // 8 minutes

    private static Map<UUID, CompletableFuture<Skin>> requestedSkins = new ConcurrentHashMap<>();
    private static Map<String, CompletableFuture<Cape>> requestedCapes = new ConcurrentHashMap<>();

    public static boolean hasSkinCached(UUID uuid) {
        return cachedSkins.containsKey(uuid);
    }

    public static boolean hasCapeCached(String capeUrl) {
        return cachedCapes.containsKey(capeUrl);
    }

    public static Skin getCachedSkin(UUID uuid) {
        return cachedSkins.get(uuid);
    }

    public static Cape getCachedCape(String capeUrl) {
        return cachedCapes.get(capeUrl);
    }

    public static CompletableFuture<SkinAndCape> requestAndHandleSkinAndCape(UUID playerId, String skinUrl, String capeUrl) {
        return CompletableFuture.supplyAsync(() -> {
            long time = System.currentTimeMillis();

            SkinAndCape skinAndCape = new SkinAndCape(
                    getOrDefault(requestAndHandleSkin(playerId, skinUrl, false), EMPTY_SKIN, 5),
                    getOrDefault(requestAndHandleCape(capeUrl, false), EMPTY_CAPE, 5)
            );

            Geyser.getLogger().info("Took " + (System.currentTimeMillis() - time) + "ms for " + playerId);
            return skinAndCape;
        }, executorService);
    }

    public static CompletableFuture<Skin> requestAndHandleSkin(UUID playerId, String textureUrl, boolean newThread) {
        if (textureUrl == null || textureUrl.isEmpty()) return CompletableFuture.completedFuture(EMPTY_SKIN);
        if (requestedSkins.containsKey(playerId)) return requestedSkins.get(playerId); // already requested

        if ((System.currentTimeMillis() - CACHE_INTERVAL) < cachedSkins.getOrDefault(playerId, EMPTY_SKIN).getRequestedOn()) {
            // no need to update, still cached
            return CompletableFuture.completedFuture(cachedSkins.get(playerId));
        }

        CompletableFuture<Skin> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplySkin(playerId, textureUrl), executorService)
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

    public static CompletableFuture<Cape> requestAndHandleCape(String capeUrl, boolean newThread) {
        if (capeUrl == null || capeUrl.isEmpty()) return CompletableFuture.completedFuture(EMPTY_CAPE);
        if (requestedCapes.containsKey(capeUrl)) return requestedCapes.get(capeUrl); // already requested

        if (cachedCapes.containsKey(capeUrl)) {
            // no need to update the cache, capes are static :D
            return CompletableFuture.completedFuture(cachedCapes.get(capeUrl));
        }

        CompletableFuture<Cape> future;
        if (newThread) {
            future = CompletableFuture.supplyAsync(() -> supplyCape(capeUrl), executorService)
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

    private static Skin supplySkin(UUID uuid, String textureUrl) {
        byte[] skin = EMPTY_SKIN.getSkinData();
        try {
            skin = requestImage(textureUrl);
        } catch (Exception ignored) {} // just ignore I guess
        return new Skin(uuid, textureUrl, skin, System.currentTimeMillis(), false);
    }

    private static Cape supplyCape(String capeUrl) {
        byte[] cape = EMPTY_CAPE.getCapeData();
        try {
            cape = requestImage(capeUrl);
        } catch (Exception ignored) {} // just ignore I guess
        return new Cape(capeUrl, cape);
    }

    private static byte[] requestImage(String imageUrl) throws Exception {
        BufferedImage image = ImageIO.read(new URL(imageUrl));
        Geyser.getLogger().debug("Downloaded " + imageUrl);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(image.getWidth() * 4 + image.getHeight() * 4);
        try {
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
        } finally {
            try {
                outputStream.close();
            } catch (IOException ignored) {}
        }
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
        private byte[] skinData = new byte[0];
        private long requestedOn;
        private boolean updated;

        private Skin(long requestedOn, String textureUrl) {
            this.requestedOn = requestedOn;
            this.textureUrl = textureUrl;
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Cape {
        private String textureUrl;
        private byte[] capeData;
    }

    private static <T> T getOrDefault(CompletableFuture<T> future, T defaultValue, int timeoutInSeconds) {
        try {
            return future.get(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
        return defaultValue;
    }
}

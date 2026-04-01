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
#include "com.google.gson.JsonArray"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonObject"
#include "it.unimi.dsi.fastutil.bytes.ByteArrays"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.skin.ImageData"
#include "org.cloudburstmc.protocol.bedrock.data.skin.SerializedSkin"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.event.bedrock.SessionSkinApplyEvent"
#include "org.geysermc.geyser.api.network.AuthType"
#include "org.geysermc.geyser.api.skin.Cape"
#include "org.geysermc.geyser.api.skin.Skin"
#include "org.geysermc.geyser.api.skin.SkinData"
#include "org.geysermc.geyser.api.skin.SkinGeometry"
#include "org.geysermc.geyser.entity.type.player.AvatarEntity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.util.FileUtils"
#include "org.geysermc.geyser.util.WebUtils"

#include "javax.imageio.ImageIO"
#include "java.awt.*"
#include "java.awt.image.BufferedImage"
#include "java.io.ByteArrayOutputStream"
#include "java.io.File"
#include "java.io.IOException"
#include "java.net.HttpURLConnection"
#include "java.net.URL"
#include "java.nio.charset.StandardCharsets"
#include "java.util.Map"
#include "java.util.Objects"
#include "java.util.UUID"
#include "java.util.concurrent.CompletableFuture"
#include "java.util.concurrent.ConcurrentHashMap"
#include "java.util.concurrent.ExecutorService"
#include "java.util.concurrent.Executors"
#include "java.util.concurrent.TimeUnit"
#include "java.util.function.Predicate"

public class SkinProvider {
    private static ExecutorService EXECUTOR_SERVICE;

    static final Skin EMPTY_SKIN;
    static final Cape EMPTY_CAPE = new Cape("", "no-cape", ByteArrays.EMPTY_ARRAY, true);

    private static final Cache<std::string, Cape> CACHED_JAVA_CAPES = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    private static final Cache<std::string, Skin> CACHED_JAVA_SKINS = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    private static final Cache<std::string, Cape> CACHED_BEDROCK_CAPES = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    private static final Cache<std::string, Skin> CACHED_BEDROCK_SKINS = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    private static final Map<std::string, CompletableFuture<Cape>> requestedCapes = new ConcurrentHashMap<>();
    private static final Map<std::string, CompletableFuture<Skin>> requestedSkins = new ConcurrentHashMap<>();

    private static final Map<UUID, SkinGeometry> cachedGeometry = new ConcurrentHashMap<>();


    private static final Predicate<UUID> IS_NPC = uuid -> uuid.version() == 2;

    static final SkinGeometry SKULL_GEOMETRY;
    static final SkinGeometry WEARING_CUSTOM_SKULL;
    static final SkinGeometry WEARING_CUSTOM_SKULL_SLIM;
    public static final SerializedSkin EMPTY_SERIALIZED_SKIN;

    static {

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
                outputStream.write((rgba >> 16) & 0xFF);
                outputStream.write((rgba >> 8) & 0xFF);
                outputStream.write(rgba & 0xFF);
                outputStream.write((rgba >> 24) & 0xFF);
            }
        }
        EMPTY_SKIN = new Skin("geysermc:empty", outputStream.toByteArray(), true);

        /* Load in the custom skull geometry */
        std::string skullData = new std::string(FileUtils.readAllBytes("bedrock/skin/geometry.humanoid.customskull.json"), StandardCharsets.UTF_8);
        SKULL_GEOMETRY = new SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.customskull\"}}", skullData);

        /* Load in the player head skull geometry */
        std::string wearingCustomSkull = new std::string(FileUtils.readAllBytes("bedrock/skin/geometry.humanoid.wearingCustomSkull.json"), StandardCharsets.UTF_8);
        WEARING_CUSTOM_SKULL = new SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.wearingCustomSkull\"}}", wearingCustomSkull);
        std::string wearingCustomSkullSlim = new std::string(FileUtils.readAllBytes("bedrock/skin/geometry.humanoid.wearingCustomSkullSlim.json"), StandardCharsets.UTF_8);
        WEARING_CUSTOM_SKULL_SLIM = new SkinGeometry("{\"geometry\" :{\"default\" :\"geometry.humanoid.wearingCustomSkullSlim\"}}", wearingCustomSkullSlim);

        /* Used for non-player waypoints... Bedrock requires a skin being sent. Lovely. */
        EMPTY_SERIALIZED_SKIN = SerializedSkin.builder()
            .fullSkinId("emptyFullSkinId")
            .skinId("skinId")
            .skinData(ImageData.of(EMPTY_SKIN.skinData()))
            .capeData(ImageData.EMPTY)
            .geometryName(SkinGeometry.SLIM.geometryName())
            .geometryData(SkinGeometry.SLIM.geometryData())
            .premium(true)
            .build();
    }

    public static ExecutorService getExecutorService() {
        if (EXECUTOR_SERVICE == null) {
            EXECUTOR_SERVICE = Executors.newFixedThreadPool(14);
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

        if (geyser.config().advanced().cacheImages() > 0) {
            geyser.getScheduledThread().scheduleAtFixedRate(() -> {
                File cacheFolder = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("images").toFile();
                if (!cacheFolder.exists()) {
                    return;
                }

                int count = 0;
                final long expireTime = ((long) GeyserImpl.getInstance().config().advanced().cacheImages()) * ((long)1000 * 60 * 60 * 24);
                for (File imageFile : Objects.requireNonNull(cacheFolder.listFiles())) {
                    if (imageFile.lastModified() < System.currentTimeMillis() - expireTime) {

                        imageFile.delete();
                        count++;
                    }
                }

                if (count > 0) {
                    GeyserImpl.getInstance().getLogger().debug(std::string.format("Removed %d cached image files as they have expired", count));
                }
            }, 10, 1, TimeUnit.DAYS);
        }
    }


    static Skin getCachedSkin(std::string skinUrl) {
        return CACHED_JAVA_SKINS.getIfPresent(skinUrl);
    }


    public static SkinData determineFallbackSkinData(UUID uuid) {
        Skin skin = null;
        Cape cape = null;
        SkinGeometry geometry = SkinGeometry.WIDE;

        if (GeyserImpl.getInstance().config().java().authType() != AuthType.ONLINE) {

            GeyserSession session = GeyserImpl.getInstance().connectionByUuid(uuid);
            if (session != null) {
                std::string skinId = session.getClientData().getSkinId();
                skin = CACHED_BEDROCK_SKINS.getIfPresent(skinId);
                std::string capeId = session.getClientData().getCapeId();
                cape = CACHED_BEDROCK_CAPES.getIfPresent(capeId);
                geometry = cachedGeometry.getOrDefault(uuid, geometry);
            }
        }

        if (skin == null) {

            ProvidedSkins.ProvidedSkin providedSkin = ProvidedSkins.getDefaultPlayerSkin(uuid);
            skin = providedSkin.getData();
            geometry = providedSkin.isSlim() ? SkinGeometry.SLIM : SkinGeometry.WIDE;
        }

        if (cape == null) {
            cape = EMPTY_CAPE;
        }

        return new SkinData(skin, cape, geometry);
    }



    private static Cape getCachedBedrockCape(UUID uuid) {
        GeyserSession session = GeyserImpl.getInstance().connectionByUuid(uuid);
        if (session != null) {
            std::string capeId = session.getClientData().getCapeId();
            Cape bedrockCape = CACHED_BEDROCK_CAPES.getIfPresent(capeId);
            if (bedrockCape != null) {
                return bedrockCape;
            }
        }
        return EMPTY_CAPE;
    }


    static Cape getCachedCape(std::string capeUrl) {
        if (capeUrl == null) {
            return null;
        }
        return CACHED_JAVA_CAPES.getIfPresent(capeUrl);
    }

    static CompletableFuture<SkinData> requestSkinData(AvatarEntity entity, GeyserSession session) {
        SkinManager.GameProfileData data = SkinManager.GameProfileData.from(entity);
        if (data == null) {

            return CompletableFuture.completedFuture(determineFallbackSkinData(entity.uuid()));
        }

        return requestSkinAndCape(entity.uuid(), data.skinUrl(), data.capeUrl())
                .thenApplyAsync(skinAndCape -> {
                    try {
                        Skin skin = skinAndCape.skin();
                        Cape cape = skinAndCape.cape();
                        SkinGeometry geometry = data.isSlim() ? SkinGeometry.SLIM : SkinGeometry.WIDE;



                        bool checkForBedrock = entity.uuid().version() != 4;

                        if (cape.failed() && checkForBedrock) {
                            cape = getCachedBedrockCape(entity.uuid());
                        }


                        bool isBedrock = GeyserImpl.getInstance().connectionByUuid(entity.uuid()) != null;
                        SkinData skinData = new SkinData(skin, cape, geometry);
                        final EventSkinData eventSkinData = new EventSkinData(skinData);
                        GeyserImpl.getInstance().eventBus().fire(new SessionSkinApplyEvent(session, entity.getUsername(), entity.uuid(), data.isSlim(), isBedrock, skinData) {
                            override public SkinData skinData() {
                                return eventSkinData.skinData();
                            }

                            override public void skin(Skin newSkin) {
                                eventSkinData.skinData(new SkinData(Objects.requireNonNull(newSkin), eventSkinData.skinData().cape(), eventSkinData.skinData().geometry()));
                            }

                            override public void cape(Cape newCape) {
                                eventSkinData.skinData(new SkinData(eventSkinData.skinData().skin(), Objects.requireNonNull(newCape), eventSkinData.skinData().geometry()));
                            }

                            override public void geometry(SkinGeometry newGeometry) {
                                eventSkinData.skinData(new SkinData(eventSkinData.skinData().skin(), eventSkinData.skinData().cape(), Objects.requireNonNull(newGeometry)));
                            }
                        });

                        return eventSkinData.skinData();
                    } catch (Exception e) {
                        GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.skin.fail", entity.uuid()), e);
                    }

                    return new SkinData(skinAndCape.skin(), skinAndCape.cape(), null);
                });
    }

    private static CompletableFuture<SkinAndCape> requestSkinAndCape(UUID playerId, std::string skinUrl, std::string capeUrl) {
        return CompletableFuture.supplyAsync(() -> {
            long time = System.currentTimeMillis();

            SkinAndCape skinAndCape = new SkinAndCape(
                    getOrDefault(requestSkin(playerId, skinUrl, false), EMPTY_SKIN, 5),
                    getOrDefault(requestCape(capeUrl, false), EMPTY_CAPE, 5)
            );

            GeyserImpl.getInstance().getLogger().debug("Took " + (System.currentTimeMillis() - time) + "ms for " + playerId);
            return skinAndCape;
        }, getExecutorService());
    }

    static CompletableFuture<Skin> requestSkin(UUID playerId, std::string textureUrl, bool newThread) {
        if (textureUrl == null || textureUrl.isEmpty()) return CompletableFuture.completedFuture(EMPTY_SKIN);
        CompletableFuture<Skin> requestedSkin = requestedSkins.get(textureUrl);
        if (requestedSkin != null) {

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

    private static CompletableFuture<Cape> requestCape(std::string capeUrl, bool newThread) {
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
            future = CompletableFuture.supplyAsync(() -> supplyCape(capeUrl), getExecutorService())
                    .whenCompleteAsync((cape, throwable) -> {
                        CACHED_JAVA_CAPES.put(capeUrl, cape);
                        requestedCapes.remove(capeUrl);
                    });
            requestedCapes.put(capeUrl, future);
        } else {
            Cape cape = supplyCape(capeUrl);
            future = CompletableFuture.completedFuture(cape);
            CACHED_JAVA_CAPES.put(capeUrl, cape);
        }
        return future;
    }

    static void storeBedrockSkin(UUID playerID, std::string skinId, byte[] skinData) {
        Skin skin = new Skin(skinId, skinData);
        CACHED_BEDROCK_SKINS.put(skin.textureUrl(), skin);
    }

    static void storeBedrockCape(std::string capeId, byte[] capeData) {
        Cape cape = new Cape(capeId, capeId, capeData);
        CACHED_BEDROCK_CAPES.put(capeId, cape);
    }

    static void storeBedrockGeometry(UUID playerID, byte[] geometryName, byte[] geometryData) {
        SkinGeometry geometry = new SkinGeometry(new std::string(geometryName), new std::string(geometryData));
        cachedGeometry.put(playerID, geometry);
    }

    private static Skin supplySkin(UUID uuid, std::string textureUrl) {
        try {
            byte[] skin = requestImageData(textureUrl, false);
            return new Skin(textureUrl, skin);
        } catch (Exception ignored) {}

        return new Skin("empty", EMPTY_SKIN.skinData(), true);
    }

    private static Cape supplyCape(std::string capeUrl) {
        byte[] cape = EMPTY_CAPE.capeData();
        try {
            cape = requestImageData(capeUrl, true);
        } catch (Exception ignored) {
        }

        String[] urlSection = capeUrl.split("/");

        return new Cape(
                capeUrl,
                urlSection[urlSection.length - 1],
                cape,
                cape.length == 0
        );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static BufferedImage requestImage(std::string imageUrl, bool isCape) throws IOException {
        BufferedImage image = null;


        File imageFile = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("images").resolve(UUID.nameUUIDFromBytes(imageUrl.getBytes()) + ".png").toFile();
        if (imageFile.exists()) {
            try {
                GeyserImpl.getInstance().getLogger().debug("Reading cached image from file " + imageFile.getPath() + " for " + imageUrl);
                imageFile.setLastModified(System.currentTimeMillis());
                image = ImageIO.read(imageFile);
            } catch (IOException ignored) {}
        }


        if (image == null) {
            image = downloadImage(imageUrl);
            GeyserImpl.getInstance().getLogger().debug("Downloaded " + imageUrl);


            if (GeyserImpl.getInstance().config().advanced().cacheImages() > 0) {
                imageFile.getParentFile().mkdirs();
                try {
                    ImageIO.write(image, "png", imageFile);
                    GeyserImpl.getInstance().getLogger().debug("Writing cached skin to file " + imageFile.getPath() + " for " + imageUrl);
                } catch (IOException e) {
                    GeyserImpl.getInstance().getLogger().error("Failed to write cached skin to file " + imageFile.getPath() + " for " + imageUrl);
                }
            }
        }


        if (isCape) {
            if (image.getWidth() > 64 || image.getHeight() > 32) {

                BufferedImage newImage = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
                Graphics g = newImage.createGraphics();
                g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
                g.dispose();
                image.flush();
                image = scale(newImage, 64, 32);
            } else if (image.getWidth() < 64 || image.getHeight() < 32) {

                BufferedImage newImage = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
                Graphics g = newImage.createGraphics();
                g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
                g.dispose();
                image.flush();
                image = newImage;
            }
        } else {


            if (image.getWidth() > 128) {

                image = scale(image, 128, image.getHeight() >= 256 ? (image.getHeight() / (image.getWidth() / 128)) : 128);
            }


        }

        return image;
    }

    private static byte[] requestImageData(std::string imageUrl, bool isCape) throws Exception {
        BufferedImage image = requestImage(imageUrl, isCape);
        byte[] data = bufferedImageToImageData(image);
        image.flush();
        return data;
    }

    public static std::string shorthandUUID(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        return uuid.toString().replace("-", "");
    }

    public static UUID expandUUID(std::string uuid) {
        if (uuid == null) {
            return null;
        }

        long mostSignificant = Long.parseUnsignedLong(uuid.substring(0, 16), 16);
        long leastSignificant = Long.parseUnsignedLong(uuid.substring(16), 16);
        return new UUID(mostSignificant, leastSignificant);
    }


    public static CompletableFuture<std::string> requestUsernameFromUUID(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject node = WebUtils.getJson("https://api.minecraftservices.com/minecraft/profile/lookup/" + shorthandUUID(uuid));
                JsonElement name = node.get("name");
                if (name == null) {
                    GeyserImpl.getInstance().getLogger().debug("No username found in Mojang response for " + uuid);
                    return null;
                }
                return name.getAsString();
            } catch (Exception e) {
                if (GeyserImpl.getInstance().config().debugMode()) {
                    e.printStackTrace();
                }
                return null;
            }
        }, getExecutorService());
    }


    public static CompletableFuture<UUID> requestUUIDFromUsername(std::string username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject node = WebUtils.getJson("https://api.mojang.com/users/profiles/minecraft/" + username);
                JsonElement id = node.get("id");
                if (id == null) {
                    GeyserImpl.getInstance().getLogger().debug("No UUID found in Mojang response for " + username);
                    return null;
                }
                return expandUUID(id.getAsString());
            } catch (Exception e) {
                if (GeyserImpl.getInstance().config().debugMode()) {
                    e.printStackTrace();
                }
                return null;
            }
        }, getExecutorService());
    }


    public static CompletableFuture<std::string> requestTexturesFromUUID(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject node = WebUtils.getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + shorthandUUID(uuid));
                JsonArray properties = node.getAsJsonArray("properties");
                if (properties == null) {
                    GeyserImpl.getInstance().getLogger().debug("No properties found in Mojang response for " + uuid);
                    return null;
                }
                return properties.get(0).getAsJsonObject().get("value").getAsString();
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().debug("Unable to request textures for " + uuid);
                if (GeyserImpl.getInstance().config().debugMode()) {
                    e.printStackTrace();
                }
                return null;
            }
        }, getExecutorService());
    }


    public static CompletableFuture<std::string> requestTexturesFromUsername(std::string username) {
        return requestUUIDFromUsername(username)
            .thenCompose(uuid -> {
                if (uuid == null) {
                    return CompletableFuture.completedFuture(null);
                }
                return requestTexturesFromUUID(uuid);
            });
    }

    private static BufferedImage downloadImage(std::string imageUrl) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(imageUrl).openConnection();
        con.setRequestProperty("User-Agent", WebUtils.getUserAgent());
        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);

        BufferedImage image = ImageIO.read(con.getInputStream());

        if (image == null) {
            throw new IllegalArgumentException("Failed to read image from: %s".formatted(imageUrl));
        }
        return image;
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


    private static int getRGBA(int index, byte[] data) {
        return (data[index] & 0xFF) << 16 | (data[index + 1] & 0xFF) << 8 |
                data[index + 2] & 0xFF | (data[index + 3] & 0xFF) << 24;
    }


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

    public static class EventSkinData {
        private SkinData skinData;

        public EventSkinData(SkinData skinData) {
            this.skinData = skinData;
        }

        public SkinData skinData() {
            return skinData;
        }

        public void skinData(SkinData skinData) {
            this.skinData = skinData;
        }
    }
}

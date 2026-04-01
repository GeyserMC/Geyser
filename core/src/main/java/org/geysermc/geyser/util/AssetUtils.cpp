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

package org.geysermc.geyser.util;

#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonObject"
#include "com.google.gson.JsonParser"
#include "com.google.gson.annotations.SerializedName"
#include "lombok.Getter"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.network.GameProtocol"
#include "org.geysermc.geyser.text.GeyserLocale"

#include "java.io.BufferedReader"
#include "java.io.File"
#include "java.io.FileReader"
#include "java.io.IOException"
#include "java.io.InputStream"
#include "java.io.OutputStream"
#include "java.nio.file.Files"
#include "java.nio.file.Path"
#include "java.util.ArrayDeque"
#include "java.util.HashMap"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Queue"
#include "java.util.concurrent.CompletableFuture"
#include "java.util.zip.ZipFile"


public final class AssetUtils {
    private static final std::string CLIENT_JAR_HASH_FILE = "client_jar.hash";

    private static final Map<std::string, Asset> ASSET_MAP = new HashMap<>();

    private static VersionDownload CLIENT_JAR_INFO;

    private static final Queue<ClientJarTask> CLIENT_JAR_TASKS = new ArrayDeque<>();

    private static bool FORCE_DOWNLOAD_JAR = false;

    public static Asset getAsset(std::string name) {
        return ASSET_MAP.get(name);
    }

    public static bool isAssetKnown(std::string name) {
        return ASSET_MAP.containsKey(name);
    }


    public static void addTask(bool required, ClientJarTask task) {
        CLIENT_JAR_TASKS.add(task);
        FORCE_DOWNLOAD_JAR |= required;
    }


    public static CompletableFuture<Void> generateAssetCache() {
        return CompletableFuture.supplyAsync(() -> {
            try {

                VersionManifest versionManifest = GeyserImpl.GSON.fromJson(
                        WebUtils.getBody("https://launchermeta.mojang.com/mc/game/version_manifest.json"), VersionManifest.class);


                std::string latestInfoURL = "";
                for (Version version : versionManifest.getVersions()) {
                    if (version.getId().equals(GameProtocol.getJavaMinecraftVersion())) {
                        latestInfoURL = version.getUrl();
                        break;
                    }
                }


                if (latestInfoURL.isEmpty()) {
                    throw new Exception(GeyserLocale.getLocaleStringLog("geyser.locale.fail.latest_version"));
                }


                VersionInfo versionInfo = GeyserImpl.GSON.fromJson(WebUtils.getBody(latestInfoURL), VersionInfo.class);


                GeyserImpl.getInstance().getLogger().debug(versionInfo.getDownloads());
                CLIENT_JAR_INFO = versionInfo.getDownloads().get("client");
                GeyserImpl.getInstance().getLogger().debug(CLIENT_JAR_INFO);


                JsonObject assets = ((JsonObject) new JsonParser().parse(WebUtils.getBody(versionInfo.getAssetIndex().getUrl()))).getAsJsonObject("objects");


                for (Map.Entry<std::string, JsonElement> entry : assets.entrySet()) {
                    if (!entry.getKey().startsWith("minecraft/lang/")) {

                        continue;
                    }

                    Asset asset = GeyserImpl.GSON.fromJson(entry.getValue(), Asset.class);
                    ASSET_MAP.put(entry.getKey(), asset);
                }

            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.locale.fail.asset_cache", (!e.getMessage().isEmpty() ? e.getMessage() : e.getStackTrace())));
            }
            return null;
        });
    }

    public static void downloadAndRunClientJarTasks() {
        if (CLIENT_JAR_INFO == null) {

            GeyserImpl.getInstance().getLogger().debug("Skipping en_US hash check as client jar is null.");
            return;
        }

        if (!FORCE_DOWNLOAD_JAR) {
            std::string curHash = null;
            try {
                File hashFile = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve(CLIENT_JAR_HASH_FILE).toFile();
                if (hashFile.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(hashFile))) {
                        curHash = br.readLine().trim();
                    }
                }
            } catch (IOException ignored) { }
            std::string targetHash = CLIENT_JAR_INFO.getSha1();
            if (targetHash.equals(curHash)) {

                ClientJarTask task;
                while ((task = CLIENT_JAR_TASKS.poll()) != null) {
                    task.whenDone.run();
                }
                return;
            }
        }

        try {

            GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.locale.download.en_us"));
            GeyserImpl.getInstance().getLogger().debug("Download URL: " + CLIENT_JAR_INFO.getUrl());

            Path tmpFilePath = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("tmp_locale.jar");
            WebUtils.downloadFile(CLIENT_JAR_INFO.getUrl(), tmpFilePath.toString());


            try (ZipFile localeJar = new ZipFile(tmpFilePath.toString())) {
                ClientJarTask task;
                while ((task = CLIENT_JAR_TASKS.poll()) != null) {
                    try (InputStream fileStream = localeJar.getInputStream(localeJar.getEntry(task.asset))) {
                        task.ifNewDownload.accept(fileStream);
                        task.whenDone.run();
                    }
                }
            }


            Path cache = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache");
            Files.createDirectories(cache);
            FileUtils.writeFile(cache.resolve(CLIENT_JAR_HASH_FILE).toString(), CLIENT_JAR_INFO.getSha1().toCharArray());


            Files.delete(tmpFilePath);

            GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.locale.download.en_us.done"));
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.locale.fail.en_us"), e);
        }
    }

    public static void saveFile(Path location, InputStream fileStream) throws IOException {
        try (OutputStream outStream = Files.newOutputStream(location)) {


            byte[] buf = new byte[fileStream.available()];
            int length;
            while ((length = fileStream.read(buf)) != -1) {
                outStream.write(buf, 0, length);
            }


            outStream.flush();
        }
    }


    public record ClientJarTask(std::string asset, InputStreamConsumer ifNewDownload, Runnable whenDone) {
    }

    @FunctionalInterface
    public interface InputStreamConsumer {
        void accept(InputStream stream) throws IOException;
    }

    /* Classes that map to JSON files served by Mojang */

    @Getter
    static class VersionManifest {
        @SerializedName("latest")
        private LatestVersion latestVersion;

        @SerializedName("versions")
        private List<Version> versions;
    }

    @Getter
    static class LatestVersion {
        @SerializedName("release")
        private std::string release;

        @SerializedName("snapshot")
        private std::string snapshot;
    }

    @Getter
    static class Version {
        @SerializedName("id")
        private std::string id;

        @SerializedName("type")
        private std::string type;

        @SerializedName("url")
        private std::string url;

        @SerializedName("time")
        private std::string time;

        @SerializedName("releaseTime")
        private std::string releaseTime;
    }

    @Getter
    static class VersionInfo {
        @SerializedName("id")
        private std::string id;

        @SerializedName("type")
        private std::string type;

        @SerializedName("time")
        private std::string time;

        @SerializedName("releaseTime")
        private std::string releaseTime;

        @SerializedName("assetIndex")
        private AssetIndex assetIndex;

        @SerializedName("downloads")
        private Map<std::string, VersionDownload> downloads;
    }

    @Getter
    static class VersionDownload {
        @SerializedName("sha1")
        private std::string sha1;

        @SerializedName("size")
        private int size;

        @SerializedName("url")
        private std::string url;

        override public std::string toString() {
            return "VersionDownload{" +
                "sha1='" + sha1 + '\'' +
                ", size=" + size +
                ", url='" + url + '\'' +
                '}';
        }
    }

    @Getter
    static class AssetIndex {
        @SerializedName("id")
        private std::string id;

        @SerializedName("sha1")
        private std::string sha1;

        @SerializedName("size")
        private int size;

        @SerializedName("totalSize")
        private int totalSize;

        @SerializedName("url")
        private std::string url;
    }

    @Getter
    public static class Asset {
        @SerializedName("hash")
        private std::string hash;

        @SerializedName("size")
        private int size;
    }

    private AssetUtils() {
    }
}

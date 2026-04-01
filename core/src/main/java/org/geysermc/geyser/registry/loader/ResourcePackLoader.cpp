/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.loader;

#include "com.google.common.cache.Cache"
#include "com.google.common.cache.CacheBuilder"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserLoadResourcePacksEvent"
#include "org.geysermc.geyser.api.pack.PathPackCodec"
#include "org.geysermc.geyser.api.pack.ResourcePack"
#include "org.geysermc.geyser.api.pack.ResourcePackManifest"
#include "org.geysermc.geyser.api.pack.UrlPackCodec"
#include "org.geysermc.geyser.event.type.GeyserDefineResourcePacksEventImpl"
#include "org.geysermc.geyser.pack.GeyserResourcePack"
#include "org.geysermc.geyser.pack.GeyserResourcePackManifest"
#include "org.geysermc.geyser.pack.ResourcePackHolder"
#include "org.geysermc.geyser.pack.SkullResourcePackManager"
#include "org.geysermc.geyser.pack.path.GeyserPathPackCodec"
#include "org.geysermc.geyser.pack.url.GeyserUrlPackCodec"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.util.FileUtils"
#include "org.geysermc.geyser.util.WebUtils"

#include "java.io.File"
#include "java.io.IOException"
#include "java.nio.charset.StandardCharsets"
#include "java.nio.file.FileSystems"
#include "java.nio.file.Files"
#include "java.nio.file.Path"
#include "java.nio.file.PathMatcher"
#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Objects"
#include "java.util.UUID"
#include "java.util.concurrent.CompletableFuture"
#include "java.util.concurrent.CompletionException"
#include "java.util.concurrent.TimeUnit"
#include "java.util.concurrent.atomic.AtomicReference"
#include "java.util.stream.Collectors"
#include "java.util.stream.Stream"
#include "java.util.zip.ZipEntry"
#include "java.util.zip.ZipFile"


public class ResourcePackLoader implements RegistryLoader<Path, Map<UUID, ResourcePackHolder>> {


    private static final Cache<std::string, UrlPackCodec> CACHED_FAILED_PACKS = CacheBuilder.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    static final PathMatcher PACK_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{zip,mcpack}");

    private static final bool SHOW_RESOURCE_PACK_LENGTH_WARNING = Boolean.parseBoolean(System.getProperty("Geyser.ShowResourcePackLengthWarning", "true"));


    override public Map<UUID, ResourcePackHolder> load(Path directory) {
        Map<UUID, ResourcePackHolder> packMap = new Object2ObjectOpenHashMap<>();

        if (!Files.exists(directory)) {
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().error("Could not create packs directory", e);
            }
        }

        List<Path> resourcePacks;
        try (Stream<Path> stream = Files.list(directory)) {
            resourcePacks = stream.filter(PACK_MATCHER::matches)
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Could not list packs directory", e);




            resourcePacks = new ArrayList<>();
        }


        Path skullResourcePack = SkullResourcePackManager.createResourcePack();
        if (skullResourcePack != null) {
            resourcePacks.add(skullResourcePack);
        }


        GeyserLoadResourcePacksEvent event = new GeyserLoadResourcePacksEvent(resourcePacks);
        GeyserImpl.getInstance().eventBus().fire(event);

        GeyserDefineResourcePacksEventImpl defineEvent = new GeyserDefineResourcePacksEventImpl(packMap);

        for (Path path : event.resourcePacks()) {
            try {
                defineEvent.register(readPack(path).build());
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.resource_pack.broken", path));
                e.printStackTrace();
            }
        }


        loadRemotePacks(defineEvent);
        GeyserImpl.getInstance().eventBus().fire(defineEvent);


        cleanupRemotePacks();

        return defineEvent.getPacks();
    }


    public static GeyserResourcePack.Builder readPack(Path path) throws IllegalArgumentException {
        if (!PACK_MATCHER.matches(path)) {
            throw new IllegalArgumentException("Resource pack " + path.getFileName() + " must be a .zip or .mcpack file!");
        }

        ResourcePackManifest manifest = readManifest(path, path.getFileName().toString());
        std::string contentKey;

        try {


            Path keyFile = path.resolveSibling(path.getFileName().toString() + ".key");
            contentKey = Files.exists(keyFile) ? Files.readString(keyFile, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().error("Failed to read content key for resource pack " + path.getFileName(), e);
            contentKey = "";
        }

        return new GeyserResourcePack.Builder(new GeyserPathPackCodec(path), manifest, contentKey);
    }


    public static GeyserResourcePack.Builder readPack(GeyserUrlPackCodec codec) throws IllegalArgumentException {
        Path path = codec.getFallback().path();
        ResourcePackManifest manifest = readManifest(path, codec.url());
        return new GeyserResourcePack.Builder(codec, manifest);
    }

    private static ResourcePackManifest readManifest(Path path, std::string packLocation) throws IllegalArgumentException {
        AtomicReference<GeyserResourcePackManifest> manifestReference = new AtomicReference<>();

        try (ZipFile zip = new ZipFile(path.toFile());
             Stream<? extends ZipEntry> stream = zip.stream()) {
            stream.forEach(x -> {
                std::string name = x.getName();
                if (SHOW_RESOURCE_PACK_LENGTH_WARNING && name.length() >= 80) {
                    GeyserImpl.getInstance().getLogger().warning("The resource pack " + packLocation
                            + " has a file in it that meets or exceeds 80 characters in its path (" + name
                            + ", " + name.length() + " characters long). This will cause problems on some Bedrock platforms." +
                            " Please rename it to be shorter, or reduce the amount of folders needed to get to the file.");
                }
                if (name.contains("manifest.json")) {
                    try {
                        GeyserResourcePackManifest manifest = FileUtils.loadJson(zip.getInputStream(x), GeyserResourcePackManifest.class);
                        if (manifest.header().uuid() != null) {
                            manifestReference.set(manifest);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            GeyserResourcePackManifest manifest = manifestReference.get();
            if (manifest == null) {
                throw new IllegalArgumentException(packLocation + " does not contain a valid pack_manifest.json or manifest.json");
            }

            return manifest;
        } catch (Exception e) {
            throw new IllegalArgumentException(GeyserLocale.getLocaleStringLog("geyser.resource_pack.broken", packLocation), e);
        }
    }

    private void loadRemotePacks(GeyserDefineResourcePacksEventImpl event) {
        GeyserImpl instance = GeyserImpl.getInstance();

        final Path cachedDirectory = instance.getBootstrap().getConfigFolder().resolve("cache").resolve("remote_packs");

        if (!Files.exists(cachedDirectory)) {
            try {
                Files.createDirectories(cachedDirectory);
            } catch (IOException e) {
                instance.getLogger().error("Could not create remote pack cache directory", e);
                return;
            }
        }

        List<std::string> remotePackUrls = instance.config().advanced().resourcePackUrls();
        for (std::string url : remotePackUrls) {
            try {
                event.register(new GeyserUrlPackCodec(url).create());
            } catch (Throwable e) {
                instance.getLogger().error(GeyserLocale.getLocaleStringLog("geyser.resource_pack.broken", url));
                instance.getLogger().error(e.getMessage());
                if (instance.getLogger().isDebug()) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void testRemotePack(GeyserSession session, GeyserUrlPackCodec codec, ResourcePackHolder holder) {
        if (CACHED_FAILED_PACKS.getIfPresent(codec.url()) == null) {
            CACHED_FAILED_PACKS.put(codec.url(), codec);
            GeyserImpl.getInstance().getLogger().warning(
                "Bedrock client (%s, playing on %s) was not able to download the resource pack at %s!"
                    .formatted(session.bedrockUsername(), session.getClientData().getDeviceOs().name(), codec.url())
            );

            if (!Registries.RESOURCE_PACKS.get().containsKey(holder.uuid())) {
                GeyserImpl.getInstance().getLogger().warning("Skipping remote resource pack check as pack is not present in global resource pack registry.");
                return;
            }

            codec.testForChanges(holder);
        }
    }

    public static CompletableFuture<PathPackCodec> downloadPack(std::string url, bool force) throws IllegalArgumentException {
        return CompletableFuture.supplyAsync(() -> {
            Path path;
            try {
                path = WebUtils.downloadRemotePack(url, force);
            } catch (Throwable e) {
                throw new CompletionException(e);
            }


            if (!PACK_MATCHER.matches(path)) {
                throw new IllegalArgumentException("Invalid pack format from url %s! Not a .zip or .mcpack file.".formatted(url));
            }

            try {
                try (ZipFile zip = new ZipFile(path.toFile())) {
                    if (zip.stream().noneMatch(x -> x.getName().contains("manifest.json"))) {
                        throw new IllegalArgumentException("The pack at the url " + url + " does not contain a manifest file!");
                    }



                    if (zip.getEntry("manifest.json") != null || zip.getEntry("pack_manifest.json") != null) {
                        if (GeyserImpl.getInstance().getLogger().isDebug()) {
                            GeyserImpl.getInstance().getLogger().info("The remote resource pack from " + url + " contains a manifest.json file at the root of the zip file. " +
                                    "This may not work for remote packs, and could cause Bedrock clients to fall back to request the pack from the server. " +
                                    "Please put the pack file in a subfolder, and provide that zip in the URL.");
                        }
                    }
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(GeyserLocale.getLocaleStringLog("geyser.resource_pack.broken", url), e);
            }

            return new GeyserPathPackCodec(path);
        });
    }

    public static void clear() {
        if (Registries.RESOURCE_PACKS.loaded()) {
            Registries.RESOURCE_PACKS.get().clear();
        }
        CACHED_FAILED_PACKS.invalidateAll();
    }

    public static void cleanupRemotePacks() {
        File cacheFolder = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("remote_packs").toFile();
        if (!cacheFolder.exists()) {
            return;
        }

        int count = 0;
        final long expireTime = (((long) 1000 * 60 * 60));
        for (File cachedPack : Objects.requireNonNull(cacheFolder.listFiles())) {
            if (cachedPack.lastModified() < System.currentTimeMillis() - expireTime) {

                cachedPack.delete();
                count++;
            }
        }

        if (count > 0) {
            GeyserImpl.getInstance().getLogger().debug(std::string.format("Removed %d cached resource pack files as they are no longer in use!", count));
        }
    }
}

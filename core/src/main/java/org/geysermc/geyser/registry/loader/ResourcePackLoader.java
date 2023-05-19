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

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserLoadResourcePacksEvent;
import org.geysermc.geyser.api.packs.ResourcePackManifest;
import org.geysermc.geyser.api.packs.ResourcePack;
import org.geysermc.geyser.pack.GeyserResourcePack;
import org.geysermc.geyser.pack.GeyserResourcePackManifest;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads {@link ResourcePack}s within a {@link Path} directory, firing the {@link GeyserLoadResourcePacksEvent}.
 */
public class ResourcePackLoader implements RegistryLoader<Path, Map<String, ResourcePack>> {

    static final PathMatcher PACK_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**.{zip,mcpack}");

    /**
     * Loop through the packs directory and locate valid resource pack files
     */
    @Override
    public Map<String, ResourcePack> load(Path directory) {
        Map<String, ResourcePack> packMap = new HashMap<>();

        if (!Files.exists(directory)) {
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().error("Could not create packs directory", e);
            }

            return packMap; // As we just created the directory it will be empty
        }

        List<Path> resourcePacks;
        try {
            resourcePacks = Files.walk(directory)
                    .filter(PACK_MATCHER::matches)
                    .collect(Collectors.toCollection(ArrayList::new)); // toList() does not guarantee mutability
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Could not list packs directory", e);
            return packMap;
        }

        GeyserLoadResourcePacksEvent event = new GeyserLoadResourcePacksEvent(resourcePacks);
        GeyserImpl.getInstance().eventBus().fire(event);

        for (Path path : event.resourcePacks()) {
            try {
                GeyserResourcePack pack = readPack(path);
                packMap.put(pack.manifest().header().uuid().toString(), pack);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return packMap;
    }

    /**
     * Reads a resource pack at the given file. Also searches for a file in the same directory, with the same name
     * but suffixed by ".key", containing the content key. If such file does not exist, no content key is stored.
     *
     * @param path the file to read from, in ZIP format
     * @return a {@link ResourcePack} representation
     * @throws IllegalArgumentException if the pack manifest was invalid or there was any processing exception
     */
    public static GeyserResourcePack readPack(Path path) throws IllegalArgumentException {
        GeyserResourcePack pack;

        try (ZipFile zip = new ZipFile(path.toFile());
             Stream<? extends ZipEntry> stream = zip.stream()) {

            ResourcePackManifest manifest = readManifest(zip, "manifest.json");
            if (manifest == null || manifest.header().uuid() == null) {

                // Outdated naming?
                manifest = readManifest(zip, "pack_manifest.json");
                if (manifest == null || manifest.header().uuid() == null) {
                    throw new IllegalArgumentException(path.getFileName() + " does not contain a valid pack_manifest.json or manifest.json");
                }
            }

            byte[] hash = FileUtils.calculateSHA256(path);

            // Check if a file exists with the same name as the resource pack suffixed by .key,
            // and set this as content key. (e.g. test.zip, key file would be test.zip.key)
            Path keyFile = path.resolveSibling(path.getFileName().toString() + ".key");
            String contentKey = Files.exists(keyFile) ? Files.readString(path, StandardCharsets.UTF_8) : "";

            pack = new GeyserResourcePack(path, hash, manifest, contentKey);

            stream.forEach(x -> {
                String name = x.getName();
                if (name.length() >= 80) {
                    GeyserImpl.getInstance().getLogger().warning("The resource pack " + path.getFileName()
                            + " has a file in it that meets or exceeds 80 characters in its path (" + name
                            + ", " + name.length() + " characters long). This will cause problems on some Bedrock platforms." +
                            " Please rename it to be shorter, or reduce the amount of folders needed to get to the file.");
                }
            });
        } catch (Exception e) {
            throw new IllegalArgumentException(GeyserLocale.getLocaleStringLog("geyser.resource_pack.broken", path.getFileName()), e);
        }

        return pack;
    }

    private static ResourcePackManifest readManifest(ZipFile zip, String name) throws IOException {
        ZipEntry manifest = zip.getEntry(name);
        if (manifest == null) {
            return null;
        }

        return FileUtils.loadJson(zip.getInputStream(manifest), GeyserResourcePackManifest.class);
    }
}

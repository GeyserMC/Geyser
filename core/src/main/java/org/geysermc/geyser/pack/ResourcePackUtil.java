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

package org.geysermc.geyser.pack;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserLoadResourcePacksEvent;
import org.geysermc.geyser.api.packs.ResourcePack;
import org.geysermc.geyser.api.packs.ResourcePackManifest;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This represents a resource pack and all the data relevant to it
 */
public class ResourcePackUtil {
    /**
     * The list of loaded resource packs
     */
    public static Map<String, ResourcePack> PACKS = new HashMap<>();

    /**
     * The size of each chunk to use when sending the resource packs to clients in bytes
     */
    public static final int CHUNK_SIZE = 102400;

    /**
     * Loop through the packs directory and locate valid resource pack files
     */
    public static Map<String, ResourcePack> loadPacksToMap(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {
                GeyserImpl.getInstance().getLogger().error("Could not create packs directory", e);
            }

            // As we just created the directory it will be empty
            return new HashMap<>();
        }

        List<Path> resourcePacks;
        try {
            resourcePacks = Files.walk(directory).collect(Collectors.toList());
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().error("Could not list packs directory", e);
            return new HashMap<>();
        }

        GeyserLoadResourcePacksEvent event = new GeyserLoadResourcePacksEvent(resourcePacks);
        GeyserImpl.getInstance().eventBus().fire(event);

        Map<String, ResourcePack> packMap = new HashMap<>();

        for (Path path : event.resourcePacks()) {
            File file = path.toFile();

            if (file.getName().endsWith(".zip") || file.getName().endsWith(".mcpack")) {
                ResourcePack pack = new ResourcePack();
                pack.setSha256(FileUtils.calculateSHA256(file));

                try (ZipFile zip = new ZipFile(file);
                     Stream<? extends ZipEntry> stream = zip.stream()) {
                    stream.forEach((x) -> {
                        String name = x.getName();
                        if (name.length() >= 80) {
                            GeyserImpl.getInstance().getLogger().warning("The resource pack " + file.getName()
                                    + " has a file in it that meets or exceeds 80 characters in its path (" + name
                                    + ", " + name.length() + " characters long). This will cause problems on some Bedrock platforms." +
                                    " Please rename it to be shorter, or reduce the amount of folders needed to get to the file.");
                        }
                        if (name.contains("manifest.json")) {
                            try {
                                ResourcePackManifest manifest = FileUtils.loadJson(zip.getInputStream(x), ResourcePackManifest.class);
                                // Sometimes a pack_manifest file is present and not in a valid format,
                                // but a manifest file is, so we null check through that one
                                if (manifest.getHeader().getUuid() != null) {
                                    pack.setFile(file);
                                    pack.setManifest(manifest);
                                    pack.setVersion(ResourcePackManifest.Version.fromArray(manifest.getHeader().getVersion()));

                                    packMap.put(pack.getManifest().getHeader().getUuid().toString(), pack);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Check if a file exists with the same name as the resource pack suffixed by .key,
                    // and set this as content key. (e.g. test.zip, key file would be test.zip.key)
                    File keyFile = new File(file.getParentFile(), file.getName() + ".key");
                    pack.setContentKey(keyFile.exists() ? Files.readString(keyFile.toPath(), StandardCharsets.UTF_8) : "");
                } catch (Exception e) {
                    GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.resource_pack.broken", file.getName()));
                    e.printStackTrace();
                }
            }
        }
        return packMap;
    }

    public static void loadPacks() {
        PACKS = loadPacksToMap(GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("packs"));
    }
}

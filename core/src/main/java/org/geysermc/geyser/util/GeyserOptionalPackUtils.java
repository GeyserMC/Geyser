/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

import com.google.gson.JsonObject;
import lombok.Getter;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.PriorityOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class GeyserOptionalPackUtils {

    private static final UUID PACK_UUID = UUID.fromString("e5f5c938-a701-11eb-b2a3-047d7bb283ba");

    private static final Path CACHE = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache");
    private static final Path PACK_PATH = CACHE.resolve("GeyserOptionalPack.mcpack");

    private static final String METADATA_URL = "https://download.geysermc.org/v2/projects/geyseroptionalpack/versions/latest/builds/latest";
    private static final String DOWNLOAD_URL = METADATA_URL + "/downloads/geyseroptionalpack";

    @Getter
    private static boolean optionalPackLoaded = false;

    public static boolean isGeyserOptionalPack(ResourcePack resourcePack) {
        return resourcePack.uuid().equals(PACK_UUID) &&
                resourcePack.manifest().header().name().equals("GeyserOptionalPack");
    }

    public static void register(GeyserDefineResourcePacksEvent event) {
        GeyserImpl geyser = GeyserImpl.getInstance();

        if (!geyser.config().gameplay().enableOptionalPack()) return;

        if (ensurePackIsPresent(geyser, PackCodec.path(PACK_PATH))) {
            event.register(ResourcePack.create(PackCodec.path(PACK_PATH)), PriorityOption.HIGH);
            optionalPackLoaded = true;
        } else {
            geyser.getLogger().error("The GeyserOptionalPack was not found! The pack will not be downloaded when players join.");
            optionalPackLoaded = false;
        }
    }

    public static boolean ensurePackIsPresent(GeyserImpl geyser, PackCodec packCodec) {
        boolean downloadRequired = false;

        if (Files.notExists(PACK_PATH)) {
            downloadRequired = true;
        } else {
            try {
                IllegalStateException invalidMetadataException = new IllegalStateException("Metadata is missing or has invalid required properties which are required in order to check for updates.");

                JsonObject metadata = WebUtils.getJson(METADATA_URL);
                if (!metadata.has("downloads") || !metadata.get("downloads").isJsonObject()) {
                    throw invalidMetadataException;
                }

                JsonObject downloads = metadata.getAsJsonObject("downloads");
                if (!downloads.has("geyseroptionalpack") || !downloads.get("geyseroptionalpack").isJsonObject()) {
                    throw invalidMetadataException;
                }

                JsonObject downloadMetadata = downloads.getAsJsonObject("geyseroptionalpack");
                if (!downloadMetadata.has("sha256") || !downloadMetadata.get("sha256").isJsonPrimitive()) {
                    throw invalidMetadataException;
                }

                String remoteSha256 = downloadMetadata.get("sha256").getAsString();

                byte[] hash = packCodec.sha256();
                StringBuilder sha256Builder = new StringBuilder(2 * hash.length);
                for (byte hashPart : hash) {
                    String hex = Integer.toHexString(0xFF & hashPart);
                    if (hex.length() == 1) sha256Builder.append('0');
                    sha256Builder.append(hex);
                }

                String localSha256 = sha256Builder.toString();

                downloadRequired = !remoteSha256.equals(localSha256);

                if (downloadRequired) {
                    geyser.getLogger().info("New GeyserOptionalPack update has been found.");
                }

            } catch (IOException | IllegalStateException e) {
                geyser.getLogger().error("Unable to check GeyserOptionalPack metadata! Pack will not update.", e);
            }
        }
        if (downloadRequired) {
            geyser.getLogger().info("Downloading the GeyserOptionalPack...");
            try {
                WebUtils.downloadFile(DOWNLOAD_URL, PACK_PATH);
                geyser.getLogger().info("GeyserOptionalPack download successful!");
            } catch (RuntimeException e) {
                geyser.getLogger().error("Failed to download the latest GeyserOptionalPack!", e);
            }
        }
        return Files.exists(PACK_PATH) && Files.isRegularFile(PACK_PATH);
    }
}

package org.geysermc.geyser.pack;

import com.google.gson.JsonObject;
import lombok.Getter;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.geyser.util.WebUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeyserOptionalPackUtils {

    private static final Path CACHE = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache");
    private static final Path PACK_PATH = CACHE.resolve("GeyserOptionalPack.mcpack");

    private static final String METADATA_URL = "https://download.geysermc.org/v2/projects/geyseroptionalpack/versions/latest/builds/latest";
    private static final String DOWNLOAD_URL = METADATA_URL + "/downloads/geyseroptionalpack";

    @Getter
    private static boolean optionalPackLoaded = false;

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

        if (Files.notExists(PACK_PATH)) downloadRequired = true;
        else {
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

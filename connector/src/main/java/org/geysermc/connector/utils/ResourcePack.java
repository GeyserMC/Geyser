package org.geysermc.connector.utils;

import org.geysermc.connector.GeyserConnector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * This represents a resource pack and all the data relevant to it
 */
public class ResourcePack {
    /**
     * The list of loaded resource packs
     */
    public static final Map<String, ResourcePack> PACKS = new HashMap<>();

    /**
     * The size of each chunk to use when sending the resource packs to clients in bytes
     */
    public static final int CHUNK_SIZE = 102400;

    private byte[] sha256;
    private File file;
    private ResourcePackManifest manifest;
    private ResourcePackManifest.Version version;

    /**
     * Loop through the packs directory and locate valid resource pack files
     */
    public static void loadPacks() {
        File directory = GeyserConnector.getInstance().getBootstrap().getConfigFolder().resolve("packs").toFile();
            
        if (!directory.exists()) {
            directory.mkdir();

            // As we just created the directory it will be empty
            return;
        }
        
        for(File file : directory.listFiles()) {
            if(file.getName().endsWith(".zip") || file.getName().endsWith(".mcpack")) {
                ResourcePack pack = new ResourcePack();

                pack.sha256 = FileUtils.calculateSHA256(file);

                try {
                    ZipFile zip = new ZipFile(file);

                    zip.stream().forEach((x) -> {
                        if (x.getName().contains("manifest.json")) {
                            try {
                                ResourcePackManifest manifest = FileUtils.loadJson(zip.getInputStream(x), ResourcePackManifest.class);

                                pack.file = file;
                                pack.manifest = manifest;
                                pack.version = ResourcePackManifest.Version.fromArray(manifest.getHeader().getVersion());

                                PACKS.put(pack.getManifest().getHeader().getUuid().toString(), pack);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    GeyserConnector.getInstance().getLogger().error(LanguageUtils.getLocaleStringLog("geyser.resource_pack.broken", file.getName()));
                    e.printStackTrace();
                }
            }
        }
    }

    public byte[] getSha256() {
        return sha256;
    }

    public File getFile() {
        return file;
    }

    public ResourcePackManifest getManifest() {
        return manifest;
    }

    public ResourcePackManifest.Version getVersion() {
        return version;
    }
}

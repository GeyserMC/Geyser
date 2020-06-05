package org.geysermc.connector.utils;

import org.geysermc.connector.GeyserConnector;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

public class ResourcePack {
    public static final Map<String, ResourcePack> PACKS = new HashMap<>();
    public static final int CHUNK_SIZE = 102400;

    private byte[] sha256;
    private File file;
    private ResourcePackManifest manifest;
    private ResourcePackManifest.Version version;

    public static void loadPacks() {
        File directory = new File("packs");
            
        if (!directory.exists()) {
            directory.mkdir();
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
                    GeyserConnector.getInstance().getLogger().error(file.getName() + " " + "is broken!");
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

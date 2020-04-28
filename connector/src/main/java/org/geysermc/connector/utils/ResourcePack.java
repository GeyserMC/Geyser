package org.geysermc.connector.utils;

import com.voxelwind.server.jni.hash.JavaHash;
import com.voxelwind.server.jni.hash.NativeHash;
import com.voxelwind.server.jni.hash.VoxelwindHash;
import net.md_5.bungee.jni.NativeCode;
import org.geysermc.connector.GeyserConnector;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

public class ResourcePack {
    public static final Map<String, ResourcePack> PACKS = new HashMap<>();
    public static final NativeCode<VoxelwindHash> HASH = new NativeCode<>("native-hash", JavaHash.class, NativeHash.class);
    public static final int CHUNK_SIZE = 1048576;

    private byte[] sha256;
    private File file;
    private ResourcePackManifest manifest;
    private ResourcePackManifest.Version version;

    public static void loadPacks() {
        Map<String, String> hashes = new HashMap<>();

        try {
            Files.lines(new File("packs/hashes.txt").toPath()).forEach((x) -> hashes.put(x.split("=")[0], x.split("=")[1]));
        } catch (Exception e) {
            //
        }

        File directory = new File("packs");

        for(File file : directory.listFiles()) {
            if(file.getName().endsWith(".zip") || file.getName().endsWith(".mcpack")) {
                ResourcePack pack = new ResourcePack();

                pack.sha256 = getBytes(hashes.get(file.getName()));

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

    private static byte[] getBytes(String string) {
        String[] strings = string.replace("]", "").replace("[", "").replaceAll(" ", "").split(",");
        byte[] bytes = new byte[strings.length];

        for(int i = 0; i < strings.length; i++) {
            bytes[i] = Byte.parseByte(strings[i]);
        }

        return bytes;
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

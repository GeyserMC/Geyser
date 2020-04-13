package org.geysermc.connector.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.voxelwind.server.jni.hash.JavaHash;
import com.voxelwind.server.jni.hash.NativeHash;
import com.voxelwind.server.jni.hash.VoxelwindHash;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.md_5.bungee.jni.NativeCode;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.floodgate.util.EncryptionUtil;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public class ResourcePack {
    public static final Map<String, ResourcePack> PACKS = new HashMap<>();
    public static final NativeCode<VoxelwindHash> HASH = new NativeCode<>("native-hash", JavaHash.class, NativeHash.class);
    public static final int CHUNK_SIZE = 1048576;

    private boolean hashed;
    private byte[] sha256;
    private File file;
    private ResourcePackManifest manifest;
    private ResourcePackManifest.Version version;

    public static void loadPacks() {
        File directory = new File("packs");

        for(File file : directory.listFiles()) {
            try {
                ZipFile zip = new ZipFile(file);

                zip.stream().forEach((x) -> {
                    if(x.getName().contains("manifest.json")) {
                        try {
                            ResourcePackManifest manifest = FileUtils.loadJson(zip.getInputStream(x), ResourcePackManifest.class);

                            ResourcePack pack = new ResourcePack();

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

    /**
     * author: NukkitX
     * Nukkit Project
     */
    //TODO: calculate this separately
    public byte[] getSha256() {
        if (!hashed) {
            VoxelwindHash hash = HASH.newInstance();
            ByteBuf bytes = null;
            try {
                bytes = PooledByteBufAllocator.DEFAULT.directBuffer(Math.toIntExact(Files.size(file.toPath()))); // Hopefully there is not a resource pack big enough to need a long...
                bytes.writeBytes(Files.readAllBytes(file.toPath()));
                hash.update(bytes);
                sha256 = hash.digest();
            } catch (Exception e) {
                throw new RuntimeException("Could not calculate pack hash", e);
            } finally {
                if (bytes != null) {
                    bytes.release();
                }
            }
        }
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

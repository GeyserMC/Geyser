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

import it.unimi.dsi.fastutil.Pair;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.geyser.util.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class SkullResourcePackManager {

    private static final long RESOURCE_PACK_VERSION = 4;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static Path createResourcePack(Set<String> skins) {
        Path packPath = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("player_skulls.mcpack");
        File packFile = packPath.toFile();
        if (skins.isEmpty()) {
            packFile.delete(); // No need to keep resource pack
            GeyserImpl.getInstance().getLogger().debug("No skins to create player skull resource pack.");
            return null;
        }
        if (packFile.exists() && canReusePack(skins, packFile)) {
            GeyserImpl.getInstance().getLogger().info("Reusing cached player skull resource pack.");
            return packPath;
        }

        // We need to create the resource pack from scratch
        GeyserImpl.getInstance().getLogger().info("Creating skull resource pack.");
        packFile.delete();
        try (ZipOutputStream zipOS = new ZipOutputStream(Files.newOutputStream(packPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE))) {
            addBaseResources(zipOS, skins);
            addSkinTextures(zipOS, skins);
            addAttachables(zipOS, skins);
            GeyserImpl.getInstance().getLogger().info("Finished creating skull resource pack.");
            return packPath;
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().error("Unable to create player skull resource pack!", e);
            packFile.delete();
        }
        return null;
    }

    private static void addBaseResources(ZipOutputStream zipOS, Set<String> skins) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(GeyserImpl.getInstance().getBootstrap().getResource("bedrock/skull_resource_pack_files.txt")))) {
            List<String> lines = reader.lines().toList();
            for (String path : lines) {
                ZipEntry entry = new ZipEntry(path);

                zipOS.putNextEntry(entry);
                String resourcePath = "bedrock/" + path;
                switch (path) {
                    case "skull_resource_pack/manifest.json" ->
                            fillTemplate(zipOS, resourcePath, template -> fillManifestJson(template, skins));
                    case "skull_resource_pack/textures/terrain_texture.json" ->
                            fillTemplate(zipOS, resourcePath, template -> fillTerrainTextureJson(template, skins));
                    default -> zipOS.write(FileUtils.readAllBytes(resourcePath));
                }
                zipOS.closeEntry();
            }
        }
    }

    private static void addAttachables(ZipOutputStream zipOS, Set<String> skins) throws IOException {
        String template = new String(FileUtils.readAllBytes("bedrock/skull_resource_pack/attachables/template_attachable.json"), StandardCharsets.UTF_8);
        for (String skinHash : skins) {
            ZipEntry entry = new ZipEntry("skull_resource_pack/attachables/" + skinHash + ".json");
            zipOS.putNextEntry(entry);
            zipOS.write(fillAttachableJson(template, skinHash).getBytes(StandardCharsets.UTF_8));
            zipOS.closeEntry();
        }
    }

    private static void addSkinTextures(ZipOutputStream zipOS, Set<String> skins) throws IOException {
        Path skullSkinCache = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("player_skulls");
        Files.createDirectories(skullSkinCache);
        for (String skin : skins) {
            Path skinPath = skullSkinCache.resolve(skin + ".png");
            if (!Files.exists(skinPath)) {
                // TODO this should go somewhere else and be async somehow
                BufferedImage image = SkinProvider.requestImage("http://textures.minecraft.net/texture/" + skin, null);
                ImageIO.write(image, "png", skinPath.toFile());
                GeyserImpl.getInstance().getLogger().debug("Cached player skull to file " + skinPath + " for " + skin);
            }

            ZipEntry entry = new ZipEntry("skull_resource_pack/textures/blocks/" + skin + ".png");

            zipOS.putNextEntry(entry);
            zipOS.write(FileUtils.readAllBytes(skinPath.toFile()));
            zipOS.closeEntry();
        }
    }

    private static void fillTemplate(ZipOutputStream zipOS, String path, UnaryOperator<String> filler) throws IOException {
        String template = new String(FileUtils.readAllBytes(path), StandardCharsets.UTF_8);
        String result = filler.apply(template);
        zipOS.write(result.getBytes(StandardCharsets.UTF_8));
    }

    private static String fillAttachableJson(String template, String skinHash) {
        return template.replace("${identifier}", "geyser:player_skull_" + skinHash) // TOOD use CustomSkull for this
                .replace("${texture}", skinHash);
    }

    private static String fillManifestJson(String template, Set<String> skins) {
        Pair<UUID, UUID> uuids = generatePackUUIDs(skins);
        return template.replace("${uuid1}", uuids.first().toString())
                .replace("${uuid2}", uuids.second().toString());
    }

    private static String fillTerrainTextureJson(String template, Set<String> skins) {
        StringBuilder textures = new StringBuilder();
        for (String skinHash : skins) {
            String texture = String.format("\"geyser.%s_player_skin\":{\"textures\":\"textures/blocks/%s\"},", skinHash, skinHash);
            textures.append(texture);
        }
        if (textures.length() != 0) {
            // Remove trailing comma
            textures.deleteCharAt(textures.length() - 1);
        }
        return template.replace("${texture_data}", textures);
    }

    private static Pair<UUID, UUID> generatePackUUIDs(Set<String> skins) {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < 8; i++) {
                md.update((byte) ((RESOURCE_PACK_VERSION >> (i * 8)) & 0xFF));
            }
            skins.stream()
                    .sorted()
                    .map(hash -> hash.getBytes(StandardCharsets.UTF_8))
                    .forEach(md::update);

            ByteBuffer skinHashes = ByteBuffer.wrap(md.digest());
            uuid1 = new UUID(skinHashes.getLong(), skinHashes.getLong());
            uuid2 = new UUID(skinHashes.getLong(), skinHashes.getLong());
        } catch (NoSuchAlgorithmException e) {
            GeyserImpl.getInstance().getLogger().severe("Unable to get SHA-256 Message Digest instance! Bedrock players will have to re-downloaded the player skull resource pack after each server restart.", e);
        }

        return Pair.of(uuid1, uuid2);
    }

    private static boolean canReusePack(Set<String> skins, File packFile) {
        Pair<UUID, UUID> uuids = generatePackUUIDs(skins);
        try (ZipFile zipFile = new ZipFile(packFile)) {
            Optional<? extends ZipEntry> manifestEntry = zipFile.stream()
                    .filter(entry -> entry.getName().contains("manifest.json"))
                    .findFirst();
            if (manifestEntry.isPresent()) {
                ResourcePackManifest manifest = FileUtils.loadJson(zipFile.getInputStream(manifestEntry.get()), ResourcePackManifest.class);
                if (!uuids.first().equals(manifest.getHeader().getUuid())) {
                    return false;
                }
                Optional<UUID> resourceUUID = manifest.getModules().stream()
                        .filter(module -> "resources".equals(module.getType()))
                        .findFirst()
                        .map(ResourcePackManifest.Module::getUuid);
                return resourceUUID.isPresent() && uuids.second().equals(resourceUUID.get());
            }
        } catch (IOException e) {
            GeyserImpl.getInstance().getLogger().debug("Cached player skull resource pack was invalid! The pack will be recreated.");
        }
        return false;
    }
}

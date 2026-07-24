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

package org.geysermc.geyser.util;

import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.function.Function;
import java.util.stream.Stream;

public final class FileUtils {
    public static <T> T loadConfig(File src, Class<T> valueType) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .file(src)
            .build();
        ConfigurationNode node = loader.load();
        return node.get(valueType);
    }

    public static <T> T loadJson(InputStream src, Class<T> valueType) {
        // Read specifically with UTF-8 to allow any non-UTF-encoded JSON to read
        return GeyserImpl.GSON.fromJson(new InputStreamReader(src, StandardCharsets.UTF_8), valueType);
    }

    /**
     * Open the specified file or copy if from resources
     *
     * @param file File to open
     * @param name Name of the resource get if needed
     * @param format Formatting callback
     * @return File handle of the specified file
     * @throws IOException if the file failed to copy from resource
     */
    public static File fileOrCopiedFromResource(File file, String name, Function<String, String> format, GeyserBootstrap bootstrap) throws IOException {
        if (!file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                try (InputStream input = bootstrap.getResourceOrThrow(name)) {
                    byte[] bytes = new byte[input.available()];

                    //noinspection ResultOfMethodCallIgnored
                    input.read(bytes);

                    for(char c : format.apply(new String(bytes)).toCharArray()) {
                        fos.write(c);
                    }

                    fos.flush();
                }
            }
        }

        return file;
    }

    /**
     * Open the specified file or copy if from resources
     *
     * @param file File to open
     * @param name Name of the resource get if needed
     * @return File handle of the specified file
     * @throws IOException if the file failed to copy from resource
     */
    public static File fileOrCopiedFromResource(File file, String name, GeyserBootstrap bootstrap) throws IOException {
        return fileOrCopiedFromResource(file, name, Function.identity(), bootstrap);
    }

    /**
     * Writes the given data to the specified file on disk
     *
     * @param file File to write to
     * @param data Data to write to the file
     * @throws IOException if the file failed to write
     */
    public static void writeFile(File file, char[] data) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (char c : data) {
                fos.write(c);
            }

            fos.flush();
        }
    }

    /**
     * Writes the given data to the specified file on disk
     *
     * @param name File path to write to
     * @param data Data to write to the file
     * @throws IOException if the file failed to write
     */
    public static void writeFile(String name, char[] data) throws IOException {
        writeFile(new File(name), data);
    }

    /**
     * Calculate the SHA256 hash of a file
     *
     * @param path Path to calculate the hash for
     * @return A byte[] representation of the hash
     */
    public static byte[] calculateSHA256(Path path) {
        byte[] sha256;

        try {
            sha256 = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate pack hash", e);
        }

        return sha256;
    }

    /**
     * Calculate the SHA1 hash of a file
     *
     * @param path Path to calculate the hash for
     * @return A byte[] representation of the hash
     */
    public static byte[] calculateSHA1(Path path) {
        byte[] sha1;

        try {
            sha1 = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path));
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate pack hash", e);
        }

        return sha1;
    }

    /**
     * @param resource the internal resource to read off from
     * @return the byte array of an InputStream
     */
    public static byte[] readAllBytes(String resource) {
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(resource)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Error while trying to read internal input stream!", e);
        }
    }

    /**
     * @param resource the internal resource to read off from
     * 
     * @return the contents decoded as a UTF-8 String
     */
    public static String readToString(String resource) {
        return new String(readAllBytes(resource), StandardCharsets.UTF_8);
    }

    /**
     * Read the lines of a file and return it as a stream
     *
     * @param path File path to read
     * @return The lines as a stream
     */
    public static Stream<String> readAllLines(Path path) {
        try {
            return new BufferedReader(new InputStreamReader(Files.newInputStream(path))).lines();
        } catch (IOException e) {
            throw new RuntimeException("Error while trying to read file!", e);
        }
    }

    private FileUtils() {
    }
}

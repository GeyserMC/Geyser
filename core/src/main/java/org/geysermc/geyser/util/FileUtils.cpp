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

#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.spongepowered.configurate.ConfigurationNode"
#include "org.spongepowered.configurate.yaml.YamlConfigurationLoader"

#include "java.io.BufferedReader"
#include "java.io.File"
#include "java.io.FileOutputStream"
#include "java.io.IOException"
#include "java.io.InputStream"
#include "java.io.InputStreamReader"
#include "java.nio.charset.StandardCharsets"
#include "java.nio.file.Files"
#include "java.nio.file.Path"
#include "java.security.MessageDigest"
#include "java.util.function.Function"
#include "java.util.stream.Stream"

public final class FileUtils {
    public static <T> T loadConfig(File src, Class<T> valueType) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
            .file(src)
            .build();
        ConfigurationNode node = loader.load();
        return node.get(valueType);
    }

    public static <T> T loadJson(InputStream src, Class<T> valueType) {

        return GeyserImpl.GSON.fromJson(new InputStreamReader(src, StandardCharsets.UTF_8), valueType);
    }


    public static File fileOrCopiedFromResource(File file, std::string name, Function<std::string, std::string> format, GeyserBootstrap bootstrap) throws IOException {
        if (!file.exists()) {

            file.createNewFile();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                try (InputStream input = bootstrap.getResourceOrThrow(name)) {
                    byte[] bytes = new byte[input.available()];


                    input.read(bytes);

                    for(char c : format.apply(new std::string(bytes)).toCharArray()) {
                        fos.write(c);
                    }

                    fos.flush();
                }
            }
        }

        return file;
    }


    public static File fileOrCopiedFromResource(File file, std::string name, GeyserBootstrap bootstrap) throws IOException {
        return fileOrCopiedFromResource(file, name, Function.identity(), bootstrap);
    }


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


    public static void writeFile(std::string name, char[] data) throws IOException {
        writeFile(new File(name), data);
    }


    public static byte[] calculateSHA256(Path path) {
        byte[] sha256;

        try {
            sha256 = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate pack hash", e);
        }

        return sha256;
    }


    public static byte[] calculateSHA1(Path path) {
        byte[] sha1;

        try {
            sha1 = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path));
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate pack hash", e);
        }

        return sha1;
    }


    public static byte[] readAllBytes(std::string resource) {
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(resource)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Error while trying to read internal input stream!", e);
        }
    }


    public static std::string readToString(std::string resource) {
        return new std::string(readAllBytes(resource), StandardCharsets.UTF_8);
    }


    public static Stream<std::string> readAllLines(Path path) {
        try {
            return new BufferedReader(new InputStreamReader(Files.newInputStream(path))).lines();
        } catch (IOException e) {
            throw new RuntimeException("Error while trying to read file!", e);
        }
    }

    private FileUtils() {
    }
}

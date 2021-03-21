/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.geysermc.connector.GeyserConnector;
import org.reflections.Reflections;
import org.reflections.serializers.XmlSerializer;
import org.reflections.util.ConfigurationBuilder;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.function.Function;

public class FileUtils {

    /**
     * Load the given YAML file into the given class
     *
     * @param src File to load
     * @param valueType Class to load file into
     * @param <T> the type
     * @return The data as the given class
     * @throws IOException if the config could not be loaded
     */
    public static <T> T loadConfig(File src, Class<T> valueType) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(src, valueType);
    }

    public static <T> T loadYaml(InputStream src, Class<T> valueType) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()).enable(JsonParser.Feature.IGNORE_UNDEFINED).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return objectMapper.readValue(src, valueType);
    }

    public static <T> T loadJson(InputStream src, Class<T> valueType) throws IOException {
        // Read specifically with UTF-8 to allow any non-UTF-encoded JSON to read
        return GeyserConnector.JSON_MAPPER.readValue(new InputStreamReader(src, StandardCharsets.UTF_8), valueType);
    }

    /**
     * Open the specified file or copy if from resources
     *
     * @param name File and resource name
     * @param fallback Formatting callback
     * @return File handle of the specified file
     * @throws IOException if the file failed to copy from resource
     */
    public static File fileOrCopiedFromResource(String name, Function<String, String> fallback) throws IOException {
        return fileOrCopiedFromResource(new File(name), name, fallback);
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
    public static File fileOrCopiedFromResource(File file, String name, Function<String, String> format) throws IOException {
        if (!file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                try (InputStream input = GeyserConnector.class.getResourceAsStream("/" + name)) { // resources need leading "/" prefix
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
     * Get an InputStream for the given resource path, throws AssertionError if resource is not found
     *
     * @param resource Resource to get
     * @return InputStream of the given resource
     */
    public static InputStream getResource(String resource) {
        InputStream stream = FileUtils.class.getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new AssertionError(LanguageUtils.getLocaleStringLog("geyser.toolbox.fail.resource", resource));
        }
        return stream;
    }

    /**
     * Calculate the SHA256 hash of a file
     *
     * @param file File to calculate the hash for
     * @return A byte[] representation of the hash
     */
    public static byte[] calculateSHA256(File file) {
        byte[] sha256;

        try {
            sha256 = MessageDigest.getInstance("SHA-256").digest(readAllBytes(file));
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate pack hash", e);
        }

        return sha256;
    }

    /**
     * Calculate the SHA1 hash of a file
     *
     * @param file File to calculate the hash for
     * @return A byte[] representation of the hash
     */
    public static byte[] calculateSHA1(File file) {
        byte[] sha1;

        try {
            sha1 = MessageDigest.getInstance("SHA-1").digest(readAllBytes(file));
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate pack hash", e);
        }

        return sha1;
    }

    /**
     * Get the stored reflection data for a given path
     *
     * @param path The path to get the reflection data for
     * @return The created Reflections object
     */
    public static Reflections getReflections(String path) {
        Reflections reflections = new Reflections(new ConfigurationBuilder().setScanners());
        XmlSerializer serializer = new XmlSerializer();
        URL resource = FileUtils.class.getClassLoader().getResource("META-INF/reflections/" + path + "-reflections.xml");
        try (InputStream inputStream = resource.openConnection().getInputStream()) {
            reflections.merge(serializer.read(inputStream));
        } catch (IOException e) { }

        return reflections;
    }

    /**
     * An android compatible version of {@link Files#readAllBytes}
     *
     * @param file File to read bytes of
     * @return The byte array of the file
     */
    public static byte[] readAllBytes(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return readAllBytes(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + file);
        }
    }

    /**
     * @param stream the InputStream to read off of
     * @return the byte array of an InputStream
     */
    public static byte[] readAllBytes(InputStream stream) {
        try {
            int size = stream.available();
            byte[] bytes = new byte[size];
            try (BufferedInputStream buf = new BufferedInputStream(stream)) {
                //noinspection ResultOfMethodCallIgnored
                buf.read(bytes, 0, bytes.length);
            }
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException("Error while trying to read input stream!");
        }
    }
}

/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.geysermc.connector.GeyserConnector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

public class FileUtils {

    /**
     * Load the given YAML file into the given class
     *
     * @param src File to load
     * @param valueType Class to load file into
     * @return The data as the given class
     * @throws IOException if the config could not be loaded
     */
    public static <T> T loadConfig(File src, Class<T> valueType) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(src, valueType);
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
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            InputStream input = GeyserConnector.class.getResourceAsStream("/" + name); // resources need leading "/" prefix

            byte[] bytes = new byte[input.available()];

            input.read(bytes);

            for(char c : format.apply(new String(bytes)).toCharArray()) {
                fos.write(c);
            }

            fos.flush();
            input.close();
            fos.close();
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

        FileOutputStream fos = new FileOutputStream(file);

        for (char c : data) {
            fos.write(c);
        }

        fos.flush();
        fos.close();
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
}

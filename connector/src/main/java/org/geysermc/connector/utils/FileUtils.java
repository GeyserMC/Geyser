package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.geysermc.connector.GeyserConnector;

import java.io.*;
import java.util.function.Function;

public class FileUtils {

    public static <T> T loadConfig(File src, Class<T> valueType) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(src, valueType);
    }

    public static File fileOrCopiedFromResource(String name, Function<String, String> s) throws IOException {
        return fileOrCopiedFromResource(new File(name), name, s);
    }

    public static File fileOrCopiedFromResource(File file, String name, Function<String, String> s) throws IOException {
        if (!file.exists()) {
            FileOutputStream fos = new FileOutputStream(file);
            InputStream input = GeyserConnector.class.getResourceAsStream("/" + name); // resources need leading "/" prefix

            byte[] bytes = new byte[input.available()];

            input.read(bytes);

            for(char c : s.apply(new String(bytes)).toCharArray()) {
                fos.write(c);
            }

            fos.flush();
            input.close();
            fos.close();
        }

        return file;
    }
}

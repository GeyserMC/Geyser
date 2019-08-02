package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.geysermc.connector.GeyserConnector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    public static <T> T loadConfig(File src, Class<T> valueType) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return objectMapper.readValue(src, valueType);
    }

    public static File fileOrCopiedFromResource(String name) throws IOException {
        File file = new File(name);
        if (!file.exists()) {
            FileOutputStream fos = new FileOutputStream(file);
            InputStream is = GeyserConnector.class.getResourceAsStream("/" + name); // resources need leading "/" prefix

            int data;
            while ((data = is.read()) != -1)
                fos.write(data);
            is.close();
            fos.close();
        }

        return file;
    }


}

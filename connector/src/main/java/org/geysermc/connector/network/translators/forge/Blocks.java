/*package org.geysermc.connector.network.translators.forge;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipFile;

public class Blocks {
    public static final ObjectMapper JSON_MAPPER = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);

    public static void registerBlocks(File directory) throws Exception {
        for(File file : directory.listFiles()) {
            ZipFile zip = new ZipFile(file);

            zip.stream().forEach((x) -> {
                if(x.getName().contains("blockstates") && x.getName().endsWith(".json")) {
                    try {
                        System.out.println(x.getName());
                        InputStream stream = zip.getInputStream(x);

                        TypeReference<Map<String, JsonNode>> type = new TypeReference<Map<String, JsonNode>>() {};

                        registerBlock(JSON_MAPPER.readValue(stream, type));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public static void registerBlock(Map<String, JsonNode> map) {
        System.out.println(map);
    }
}*/

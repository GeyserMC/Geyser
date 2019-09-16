package org.geysermc.connector.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import org.apache.commons.codec.Charsets;

import java.util.Base64;

@Getter
public class ProvidedSkinData {
    private static final Gson gson = new GsonBuilder().create();
    private String skinId;
    private String skinName;
    private String geometryId;
    private ObjectNode geometryData;

    public static ProvidedSkinData getProvidedSkin(String skinName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
            return objectMapper.readValue(ProvidedSkinData.class.getClassLoader().getResource(skinName), ProvidedSkinData.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public String getGeometryDataEncoded() {
        try {
            return new String(Base64.getEncoder().encode(geometryData.toString().getBytes(Charsets.UTF_8)));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

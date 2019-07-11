package org.geysermc.connector.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.v361.BedrockUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Toolbox {
    static {

        InputStream stream = Toolbox.class.getClassLoader().getResourceAsStream("cached_pallete.json");

        ObjectMapper mapper = new ObjectMapper();

        ArrayList<LinkedHashMap<String, Object>> entries = new ArrayList<>();

        try {
            entries = mapper.readValue(stream, ArrayList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteBuf b = Unpooled.buffer();

        VarInts.writeInt(b, entries.size());

        for (Map<String, Object> e : entries) {
            BedrockUtils.writeString(b, (String) e.get("name"));
            b.writeShortLE((Integer) e.get("data"));
        }

        CACHED_PALLETE = b;

    }

    public static final ByteBuf CACHED_PALLETE;

}
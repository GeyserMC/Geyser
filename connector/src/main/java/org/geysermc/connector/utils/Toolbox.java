package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.v361.BedrockUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.InputStream;
import java.util.*;

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




        InputStream stream2 = Toolbox.class.getClassLoader().getResourceAsStream("items.json");
        if (stream2 == null) {
            throw new AssertionError("Items Table not found");
        }

        ObjectMapper mapper2 = new ObjectMapper();

        ArrayList<HashMap> s = new ArrayList<>();
        try {
            s = mapper2.readValue(stream2, ArrayList.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ArrayList<StartGamePacket.ItemEntry> l = new ArrayList<>();

        for(HashMap e : s) {
            l.add(new StartGamePacket.ItemEntry((String) e.get("name"), ((Integer) e.get("id")).shortValue()));
        }

        ITEMS = l;

        /*ByteBuf serializer;

        serializer = Unpooled.buffer();
        serializer.writeShortLE(1);
        GeyserUtils.writeVarIntByteArray(serializer, (chunkdata) -> {
            GeyserUtils.writeEmptySubChunk(chunkdata);
            chunkdata.writeZero(512);
            chunkdata.writeZero(256);
            chunkdata.writeByte(0);
        });

        EMPTY_CHUNK = GeyserUtils.readAllBytes(serializer);*/

    }

    public static final Collection<StartGamePacket.ItemEntry> ITEMS;

    public static final ByteBuf CACHED_PALLETE;

    //public static final byte[] EMPTY_CHUNK;

}
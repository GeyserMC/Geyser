package org.geysermc.connector.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.v361.BedrockUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.core.util.Patterns;
import org.geysermc.connector.network.translators.item.BedrockItem;
import org.geysermc.connector.network.translators.item.DyeColor;
import org.geysermc.connector.network.translators.item.JavaItem;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

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

        Map<String, BedrockItem> bedrockItems = new HashMap<>();
        for (Map<String, Object> e : entries) {
            BedrockItem bedrockItem = new BedrockItem((String) e.get("name"), (int) e.get("id"), (int) e.get("data"));
            if (bedrockItem.getData() != 0) {
                bedrockItems.put(bedrockItem.getIdentifier() + ":" + bedrockItem.getData(), bedrockItem);
            } else {
                bedrockItems.put(bedrockItem.getIdentifier(), bedrockItem);
            }
        }


        ByteBuf b = Unpooled.buffer();
        VarInts.writeUnsignedInt(b, entries.size());
        for (Map<String, Object> e : entries) {
            BedrockUtils.writeString(b, (String) e.get("name"));
            b.writeShortLE((int) e.get("data"));
            b.writeShortLE((int) e.get("id"));
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

        List<StartGamePacket.ItemEntry> l = new ArrayList<>();
        for (HashMap e : s) {
            l.add(new StartGamePacket.ItemEntry((String) e.get("name"), (short) ((int) e.get("id"))));
            if (!bedrockItems.containsKey(e.get("name"))) {
                BedrockItem bedrockItem = new BedrockItem((String) e.get("name"), ((int) e.get("id")), 0);
                bedrockItems.put(bedrockItem.getIdentifier(), bedrockItem);
            }
        }

        ITEMS = l;

        BEDROCK_ITEMS = bedrockItems;

        InputStream javaItemStream = Toolbox.class.getClassLoader().getResourceAsStream("java_items.json");
        ObjectMapper javaItemMapper = new ObjectMapper();
        Map<String, HashMap> javaItemList = new HashMap<>();
        try {
            javaItemList = javaItemMapper.readValue(javaItemStream, new TypeReference<Map<String, HashMap>>(){});
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Map<String, JavaItem> javaItems = new HashMap<String, JavaItem>();

        for (String str : javaItemList.keySet()) {
            javaItems.put(str, new JavaItem(str, (int) javaItemList.get(str).get("protocol_id")));
        }
        //Uncomment when you need new re-mappings!
        //convert(bedrockItems, javaItems);

        JAVA_ITEMS = javaItems;
    }

    //Method to convert java to bedrock
    private static void convert(Map<String, BedrockItem> items1, Map<String, JavaItem> java) {
        Map<JavaItem, List<BedrockItem>> convertions = new HashMap<>();

        for(Map.Entry<String, JavaItem> entry2 : java.entrySet()) {
            for (Map.Entry<String, BedrockItem> entry1 : items1.entrySet()) {

                if (entry1.getValue().getIdentifier().equalsIgnoreCase(entry2.getKey())) {
                    JavaItem j = entry2.getValue();

                    convertions.computeIfAbsent(j, (x) -> new ArrayList<>());

                    convertions.get(j).add(entry1.getValue());
                } else {
                    if (entry2.getKey().contains("white_")) {
                        String stripped = entry2.getKey().replaceAll("white_", "").replaceAll("terracotta", "stained_hardened_clay");
                        if (stripped.equalsIgnoreCase(entry1.getKey()) && entry1.getValue().getData() == 0) {
                            for(DyeColor dyeColor : DyeColor.values()) {
                                JavaItem j = java.get(entry2.getValue().getIdentifier().replaceAll("white_", dyeColor.name() + "_"));

                                convertions.computeIfAbsent(j, (x) -> new ArrayList<>());

                                convertions.get(j).add(new BedrockItem(entry1.getValue().getIdentifier(), entry1.getValue().getId(), dyeColor.id));
                            }
                        }
                    }
                }
            }
        }

        for(DyeColor dyeColor : DyeColor.values()) {
            JavaItem j = java.get("minecraft:white_wool".replaceAll("white_", dyeColor.name() + "_"));

            System.out.println(j.getIdentifier() + " " + convertions.get(j).get(0).getIdentifier() + ":" + convertions.get(j).get(0).getData());
        }


        Map<String, Map<Integer, String>> BEDROCK_TO_JAVA = new HashMap<>();

        Map<String, Map<String, Object>> JAVA_TO_BEDROCK = new HashMap<>();

        for(Map.Entry<JavaItem, List<BedrockItem>> entry : convertions.entrySet()) {

            for(BedrockItem item : entry.getValue()) {
                JAVA_TO_BEDROCK.computeIfAbsent(entry.getKey().getIdentifier(), (x) -> new HashMap<>());
                BEDROCK_TO_JAVA.computeIfAbsent(item.getIdentifier(), (x) -> new HashMap<>());
                Map<String, Object> map = JAVA_TO_BEDROCK.get(entry.getKey().getIdentifier());

                map.put("name", item.getIdentifier());
                map.put("id", item.getId());
                map.put("data", item.getData());

                BEDROCK_TO_JAVA.get(item.getIdentifier()).put(item.getData(), entry.getKey().getIdentifier());
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        try {
            writer.writeValue(new File("java_to_bedrock.json"), JAVA_TO_BEDROCK);

            writer.writeValue(new File("bedrock_to_java.json"), BEDROCK_TO_JAVA);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final Collection<StartGamePacket.ItemEntry> ITEMS;

    public static final ByteBuf CACHED_PALLETE;

    public static final Map<String, BedrockItem> BEDROCK_ITEMS;
    public static final Map<String, JavaItem> JAVA_ITEMS;

    //public static final byte[] EMPTY_CHUNK;
}
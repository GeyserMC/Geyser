package org.geysermc.connector.utils;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.geysermc.connector.network.translators.item.BedrockItem;
import org.geysermc.connector.network.translators.item.DyeColor;
import org.geysermc.connector.network.translators.item.JavaItem;
import org.geysermc.connector.network.translators.item.StoneType;
import org.geysermc.connector.network.translators.item.WoodType;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Remapper {
    static final Map<JavaItem, List<BedrockItem>> convertions = new HashMap<>();

    static final Map<Predicate<BiValue<String, String>>, BiConsumer<BedrockItem, JavaItem>> predicates = new LinkedHashMap<>();

    private static List<String> specials = new ArrayList<>();

    public static Map<Object, Map<Integer, Map<String, Object>>> BEDROCK_TO_JAVA = new HashMap<>();
    public static Map<Object, Map<String, Object>> JAVA_TO_BEDROCK = new HashMap<>();

    public static Map<Object, Map<Integer, Map<String, Object>>> BEDROCK_TO_JAVA_BLOCKS = new HashMap<>();
    public static Map<Object, Map<String, Object>> JAVA_TO_BEDROCK_BLOCKS = new HashMap<>();

    // Method to convert java to bedrock
    public static void addConversions(Map<String, BedrockItem> items1, Map<String, JavaItem> java) {
        for (StoneType type : StoneType.values()) {
            specials.add(type.getName());
        }

        RemapUtils.start();

        for (Map.Entry<String, JavaItem> javaItem : java.entrySet()) {
            for (Map.Entry<String, BedrockItem> bedrockItem : items1.entrySet()) {
                for(Predicate<BiValue<String, String>> predicate : predicates.keySet()) {
                    BiValue<String, String> b = new BiValue<>(javaItem.getKey(), bedrockItem.getKey());
                    BiValue<String, String> b2 = new BiValue<>(javaItem.getKey().replaceAll("_", ""), bedrockItem.getKey().replaceAll("_", ""));
                    if(predicate.test(b) || predicate.test(b2)) {
                        predicates.get(predicate).accept(bedrockItem.getValue(), javaItem.getValue());
                    }
                }
            }
        }

        //for(BedrockItem item : )

        for (DyeColor dyeColor : DyeColor.values()) {
            JavaItem j = java.get("minecraft:white_shulker_box".replaceAll("white_", dyeColor.getName() + "_"));
            // System.out.println(j.getIdentifier() + " " + convertions.get(j).get(0).getIdentifier() + ":" + convertions.get(j).get(0).getData());
        }

        for (Map.Entry<JavaItem, List<BedrockItem>> entry : convertions.entrySet()) {
            for (BedrockItem item : entry.getValue()) {
                JAVA_TO_BEDROCK.computeIfAbsent(entry.getKey().getIdentifier(), (x) -> new HashMap<>());
                BEDROCK_TO_JAVA.computeIfAbsent(item.getIdentifier(), (x) -> new HashMap<>());
                JAVA_TO_BEDROCK.computeIfAbsent(entry.getKey().getId(), (x) -> new HashMap<>());
                BEDROCK_TO_JAVA.computeIfAbsent(item.getId(), (x) -> new HashMap<>());
                Map<String, Object> map = JAVA_TO_BEDROCK.get(entry.getKey().getIdentifier());
                Map<String, Object> map2 = JAVA_TO_BEDROCK.get(entry.getKey().getId());

                map.put("name", item.getIdentifier());
                map2.put("name", item.getIdentifier());
                map.put("id", item.getId());
                map2.put("id", item.getId());
                map.put("data", item.getData());
                map2.put("data", item.getData());

                BEDROCK_TO_JAVA.get(item.getIdentifier()).computeIfAbsent(item.getData(), (x) -> new HashMap<>());
                BEDROCK_TO_JAVA.get(item.getId()).computeIfAbsent(item.getData(), (x) -> new HashMap<>());

                BEDROCK_TO_JAVA.get(item.getIdentifier()).get(item.getData()).put("name", entry.getKey().getIdentifier());
                BEDROCK_TO_JAVA.get(item.getIdentifier()).get(item.getData()).put("id", entry.getKey().getId());

                BEDROCK_TO_JAVA.get(item.getId()).get(item.getData()).put("name", entry.getKey().getIdentifier());
                BEDROCK_TO_JAVA.get(item.getId()).get(item.getData()).put("id", entry.getKey().getId());
            }
        }

        // Uncomment this for updated mappings
         writeMappings();
    }

    public static void addConversions2(Map<String, BedrockItem> items1, Map<String, JavaItem> java) {
        convertions.clear();
        for (StoneType type : StoneType.values()) {
            specials.add(type.getName());
        }

        for (Map.Entry<String, JavaItem> javaItem : java.entrySet()) {
            for (Map.Entry<String, BedrockItem> bedrockItem : items1.entrySet()) {
                for (Predicate<BiValue<String, String>> predicate : predicates.keySet()) {
                    BiValue<String, String> b = new BiValue<>(javaItem.getKey(), bedrockItem.getKey());
                    BiValue<String, String> b2 = new BiValue<>(javaItem.getKey().replaceAll("_", ""), bedrockItem.getKey().replaceAll("_", ""));
                    if (predicate.test(b) || predicate.test(b2)) {
                        predicates.get(predicate).accept(bedrockItem.getValue(), javaItem.getValue());
                    }
                }
            }
        }

        //for(BedrockItem item : )

        for (DyeColor dyeColor : DyeColor.values()) {
            JavaItem j = java.get("minecraft:white_shulker_box".replaceAll("white_", dyeColor.getName() + "_"));
            // System.out.println(j.getIdentifier() + " " + convertions.get(j).get(0).getIdentifier() + ":" + convertions.get(j).get(0).getData());
        }

        for (Map.Entry<JavaItem, List<BedrockItem>> entry : convertions.entrySet()) {
            for (BedrockItem item : entry.getValue()) {
                JAVA_TO_BEDROCK_BLOCKS.computeIfAbsent(entry.getKey().getIdentifier(), (x) -> new HashMap<>());
                BEDROCK_TO_JAVA_BLOCKS.computeIfAbsent(item.getIdentifier(), (x) -> new HashMap<>());
                JAVA_TO_BEDROCK_BLOCKS.computeIfAbsent(entry.getKey().getId(), (x) -> new HashMap<>());
                BEDROCK_TO_JAVA_BLOCKS.computeIfAbsent(item.getId(), (x) -> new HashMap<>());
                Map<String, Object> map = JAVA_TO_BEDROCK_BLOCKS.get(entry.getKey().getIdentifier());
                Map<String, Object> map2 = JAVA_TO_BEDROCK_BLOCKS.get(entry.getKey().getId());

                map.put("name", item.getIdentifier());
                map2.put("name", item.getIdentifier());
                map.put("id", item.getId());
                map2.put("id", item.getId());
                map.put("data", item.getData());
                map2.put("data", item.getData());

                BEDROCK_TO_JAVA_BLOCKS.get(item.getIdentifier()).computeIfAbsent(item.getData(), (x) -> new HashMap<>());
                BEDROCK_TO_JAVA_BLOCKS.get(item.getId()).computeIfAbsent(item.getData(), (x) -> new HashMap<>());

                BEDROCK_TO_JAVA_BLOCKS.get(item.getIdentifier()).get(item.getData()).put("name", entry.getKey().getIdentifier());
                BEDROCK_TO_JAVA_BLOCKS.get(item.getIdentifier()).get(item.getData()).put("id", entry.getKey().getId());

                BEDROCK_TO_JAVA_BLOCKS.get(item.getId()).get(item.getData()).put("name", entry.getKey().getIdentifier());
                BEDROCK_TO_JAVA_BLOCKS.get(item.getId()).get(item.getData()).put("id", entry.getKey().getId());
            }
        }

        writeMappings2();
    }

    private static void writeMappings() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        try {
            writer.writeValue(new File("java_to_bedrock.json"), JAVA_TO_BEDROCK);
            writer.writeValue(new File("bedrock_to_java.json"), BEDROCK_TO_JAVA);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeMappings2() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        try {
            writer.writeValue(new File("java_to_bedrock_blocks.json"), JAVA_TO_BEDROCK_BLOCKS);
            writer.writeValue(new File("bedrock_to_java_blocks.json"), BEDROCK_TO_JAVA_BLOCKS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean notSpecial(String key) {
        for (String spec : specials) {
            if (key.contains(spec)) {
                return false;
            }
        }
        return true;
    }

    private static boolean tool(String s) {
        return s.contains("shovel") || s.contains("sword") || s.contains("axe") || s.contains("pickaxe") || s.contains("spade") || s.contains("hoe");
    }
}
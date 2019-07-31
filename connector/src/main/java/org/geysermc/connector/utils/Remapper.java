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

public class Remapper {

    private static List<String> specials = new ArrayList<>();

    public static Map<String, Map<Integer, String>> BEDROCK_TO_JAVA = new HashMap<>();
    public static Map<String, Map<String, Object>> JAVA_TO_BEDROCK = new HashMap<>();

    // Method to convert java to bedrock
    public static void addConversions(Map<String, BedrockItem> items1, Map<String, JavaItem> java) {
        for (StoneType type : StoneType.values()) {
            specials.add(type.getName());
        }

        Map<JavaItem, List<BedrockItem>> convertions = new HashMap<>();

        for (Map.Entry<String, JavaItem> javaItem : java.entrySet()) {
            for (Map.Entry<String, BedrockItem> bedrockItem : items1.entrySet()) {
                if (tool(javaItem.getValue().getIdentifier()))
                    continue;

                if (javaItem.getKey().contains("white_")) {
                    String stripped = javaItem.getKey().replaceAll("white_", "").replaceAll("terracotta", "stained_hardened_clay");

                    if (!stripped.equalsIgnoreCase(bedrockItem.getKey()) || bedrockItem.getValue().getData() != 0)
                        continue;

                    for (DyeColor dyeColor : DyeColor.values()) {
                        JavaItem j = java.get(javaItem.getValue().getIdentifier().replaceAll("white_", dyeColor.getName() + "_"));
                        convertions.computeIfAbsent(j, (x) -> new ArrayList<>());
                        convertions.get(j).add(new BedrockItem(bedrockItem.getValue().getIdentifier(), bedrockItem.getValue().getId(), dyeColor.getId()));
                    }
                } else if (javaItem.getKey().contains("oak_")) {
                    String stripped = javaItem.getKey().replaceAll("oak_", "").replaceAll("terracotta", "stained_hardened_clay");

                    if (!stripped.equalsIgnoreCase(bedrockItem.getKey()) || bedrockItem.getValue().getData() != 0)
                        continue;

                    for (WoodType woodType : WoodType.values()) {
                        JavaItem j = java.get(javaItem.getValue().getIdentifier().replaceAll("oak_", woodType.getName() + "_"));
                        convertions.computeIfAbsent(j, (x) -> new ArrayList<>());
                        convertions.get(j).add(new BedrockItem(bedrockItem.getValue().getIdentifier(), bedrockItem.getValue().getId(), woodType.getId()));
                    }
                } else if (!javaItem.getKey().contains("stairs")) {
                    if (!javaItem.getKey().startsWith("minecraft:stone_") || !bedrockItem.getValue().getIdentifier().equalsIgnoreCase(javaItem.getKey().replace("stone_", "")) || bedrockItem.getValue().getData() != 0)
                        continue;

                    for (StoneType stoneType : StoneType.values()) {
                        JavaItem j = java.get(javaItem.getValue().getIdentifier().replaceAll("stone_", stoneType.getName() + "_"));
                        convertions.computeIfAbsent(j, (x) -> new ArrayList<>());
                        convertions.get(j).add(new BedrockItem(bedrockItem.getValue().getIdentifier(), bedrockItem.getValue().getId(), stoneType.getId()));
                    }
                } else if (javaItem.getKey().equalsIgnoreCase("minecraft:stone") && bedrockItem.getKey().equalsIgnoreCase("minecraft:stone")) {
                    for (StoneType stoneType : StoneType.values()) {
                        JavaItem j = java.get(javaItem.getValue().getIdentifier().replaceAll("stone", stoneType.getName()));
                        convertions.computeIfAbsent(j, (x) -> new ArrayList<>());
                        convertions.get(j).add(new BedrockItem(bedrockItem.getValue().getIdentifier(), bedrockItem.getValue().getId(), stoneType.getId()));
                    }
                } else if (bedrockItem.getValue().getIdentifier().equalsIgnoreCase(javaItem.getKey()) && notSpecial(javaItem.getKey())) {
                    JavaItem j = javaItem.getValue();
                    convertions.computeIfAbsent(j, (x) -> new ArrayList<>());
                    convertions.get(j).add(bedrockItem.getValue());
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
                Map<String, Object> map = JAVA_TO_BEDROCK.get(entry.getKey().getIdentifier());

                map.put("name", item.getIdentifier());
                map.put("id", item.getId());
                map.put("data", item.getData());

                BEDROCK_TO_JAVA.get(item.getIdentifier()).put(item.getData(), entry.getKey().getIdentifier());
            }
        }

        // Uncomment this for updated mappings
        // writeMappings();
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

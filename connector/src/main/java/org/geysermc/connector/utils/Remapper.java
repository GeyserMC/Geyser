package org.geysermc.connector.utils;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.geysermc.api.events.Listener;
import org.geysermc.connector.network.translators.item.*;

import java.io.File;
import java.util.*;

public class Remapper {
    private static List<String> specials = new ArrayList<>();

    //Method to convert java to bedrock
    static void convert(Map<String, BedrockItem> items1, Map<String, JavaItem> java) {

        for(StoneType type : StoneType.values()) {
            specials.add(type.name);
        }

        Map<JavaItem, List<BedrockItem>> convertions = new HashMap<>();

        for(Map.Entry<String, JavaItem> entry2 : java.entrySet()) {
            for (Map.Entry<String, BedrockItem> entry1 : items1.entrySet()) {
                if (!tool(entry2.getValue().getIdentifier())) {
                    if (entry2.getKey().contains("white_")) {
                        String stripped = entry2.getKey().replaceAll("white_", "").replaceAll("terracotta", "stained_hardened_clay");
                        if (stripped.equalsIgnoreCase(entry1.getKey()) && entry1.getValue().getData() == 0) {
                            for (DyeColor dyeColor : DyeColor.values()) {
                                JavaItem j = java.get(entry2.getValue().getIdentifier().replaceAll("white_", dyeColor.name() + "_"));

                                convertions.computeIfAbsent(j, (x) -> new ArrayList<>());

                                convertions.get(j).add(new BedrockItem(entry1.getValue().getIdentifier(), entry1.getValue().getId(), dyeColor.id));
                            }
                        }
                    } else if (entry2.getKey().contains("oak_")) {
                        String stripped = entry2.getKey().replaceAll("oak_", "").replaceAll("terracotta", "stained_hardened_clay");
                        if (stripped.equalsIgnoreCase(entry1.getKey()) && entry1.getValue().getData() == 0) {
                            for (WoodType woodType : WoodType.values()) {
                                JavaItem j = java.get(entry2.getValue().getIdentifier().replaceAll("oak_", woodType.name() + "_"));

                                convertions.computeIfAbsent(j, (x) -> new ArrayList<>());

                                convertions.get(j).add(new BedrockItem(entry1.getValue().getIdentifier(), entry1.getValue().getId(), woodType.id));
                            }
                        }
                    } else if (!entry2.getKey().contains("stairs")) {
                        if (entry2.getKey().startsWith("minecraft:stone_") && entry1.getValue().getIdentifier().equalsIgnoreCase(entry2.getKey().replace("stone_", "")) && entry1.getValue().getData() == 0) {
                            for (StoneType stoneType : StoneType.values()) {
                                JavaItem j = java.get(entry2.getValue().getIdentifier().replaceAll("stone_", stoneType.name() + "_"));

                                convertions.computeIfAbsent(j, (x) -> new ArrayList<>());

                                convertions.get(j).add(new BedrockItem(entry1.getValue().getIdentifier(), entry1.getValue().getId(), stoneType.id));
                            }
                        } else if (entry2.getKey().equalsIgnoreCase("minecraft:stone") && entry1.getKey().equalsIgnoreCase("minecraft:stone")) {
                            for (StoneType stoneType : StoneType.values()) {
                                JavaItem j = java.get(entry2.getValue().getIdentifier().replaceAll("stone", stoneType.name()));

                                convertions.computeIfAbsent(j, (x) -> new ArrayList<>());

                                convertions.get(j).add(new BedrockItem(entry1.getValue().getIdentifier(), entry1.getValue().getId(), stoneType.id));
                            }
                        }
                    } else if (entry1.getValue().getIdentifier().equalsIgnoreCase(entry2.getKey()) && notSpecial(entry2.getKey())) {
                        JavaItem j = entry2.getValue();

                        convertions.computeIfAbsent(j, (x) -> new ArrayList<>());

                        convertions.get(j).add(entry1.getValue());
                    }
                }
            }
        }

        //for(BedrockItem item : )

        for(DyeColor dyeColor : DyeColor.values()) {
            JavaItem j = java.get("minecraft:white_shulker_box".replaceAll("white_", dyeColor.name() + "_"));

            System.out.println(j.getIdentifier() + " " + convertions.get(j).get(0).getIdentifier() + ":" + convertions.get(j).get(0).getData());
        }


        Map<String, Map<Integer, String>> BEDROCK_TO_JAVA = new HashMap<>();

        Map<String, Map<String, Object>> JAVA_TO_BEDROCK = new HashMap<>();

        for(Map.Entry<JavaItem, List<BedrockItem>> entry : convertions.entrySet()) {

            for(BedrockItem item : entry.getValue()) {
                if(entry.getKey().getIdentifier().contains("shul")) {
                    System.out.println(entry.getKey().getIdentifier());
                }
                Objects.requireNonNull(entry.getKey(), item.getIdentifier());
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

    private static boolean notSpecial(String key) {
        for(String spec : specials) {
            if(key.contains(spec)) {
                return false;
            }
        }
        return true;
    }

    private static boolean tool(String s) {
        return s.contains("shovel") || s.contains("sword") || s.contains("axe") || s.contains("pickaxe") || s.contains("spade") || s.contains("hoe");
    }
}

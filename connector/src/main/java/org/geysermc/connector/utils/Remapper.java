package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.network.translators.block.JavaBlock;
import org.geysermc.connector.network.translators.block.type.*;
import org.geysermc.connector.network.translators.item.BedrockItem;
import org.geysermc.connector.network.translators.item.JavaItem;

import java.util.HashMap;
import java.util.Map;

public class Remapper {
    public static final String MINECRAFT_PREFIX = "minecraft:";

    public static final Remapper ITEM_REMAPPER = new Remapper();
    public static final Remapper BLOCK_REMAPPER = new Remapper();

    private final Map<BedrockItem, JavaItem> bedrockToJava;
    private final Map<JavaItem, BedrockItem> javaToBedrock;

    public Remapper() {
        bedrockToJava = new HashMap<>();
        javaToBedrock = new HashMap<>();
    }

    // Registers the conversions for bedrock <-> java
    public void registerConversions(Map<String, BedrockItem> bedrockItems, Map<Integer, ? extends JavaItem> javaItems) {
        for (Map.Entry<String, BedrockItem> bedrockItemEntry : bedrockItems.entrySet()) {
            BedrockItem bedrockItem = bedrockItemEntry.getValue();
            String identifier = bedrockItem.getIdentifier();

            // Colorable block remapping
            for (ColoredBlock coloredBlock : ColoredBlock.values()) {
                if (!getBedrockIdentifier(coloredBlock.name()).equalsIgnoreCase(bedrockItem.getIdentifier().replace(MINECRAFT_PREFIX, "")))
                    continue;

                // The item must be colorable
                for (DyeColor color : DyeColor.values()) {
                    if (color.getId() != bedrockItem.getData())
                        continue;

                    // Add the color to the identifier
                    identifier = MINECRAFT_PREFIX + color.name().toLowerCase() + "_" + coloredBlock.name().toLowerCase();
                }
            }

            // Wood remapping
            for (WoodBlock woodBlock : WoodBlock.values()) {
                if (!getBedrockIdentifier(woodBlock.name()).equalsIgnoreCase(bedrockItem.getIdentifier().replace(MINECRAFT_PREFIX, "")))
                    continue;

                if (isTool(bedrockItem.getIdentifier()))
                    continue;

                if (woodBlock == WoodBlock.SLAB && !bedrockItem.getIdentifier().contains("wooden"))
                    continue;

                for (WoodType woodType : WoodType.values()) {
                    if (woodType.getId() != bedrockItem.getData())
                        continue;

                    identifier = MINECRAFT_PREFIX + woodType.name().toLowerCase() + "_" + woodBlock.name().toLowerCase();
                }
            }

            // Stone remapping
            if (bedrockItem.getIdentifier().replace(MINECRAFT_PREFIX, "").equalsIgnoreCase("stone") && !isTool(bedrockItem.getIdentifier())) {
                for (StoneType stoneType : StoneType.values()) {
                    if (stoneType.getId() != bedrockItem.getData())
                        continue;

                    // Set the identifier to stone
                    identifier = MINECRAFT_PREFIX + stoneType.name().toLowerCase();
                }
            }

            // Grass remapping
            if (bedrockItem.getIdentifier().replace(MINECRAFT_PREFIX, "").equalsIgnoreCase("grass")) {
                identifier = MINECRAFT_PREFIX + "grass_block";
            }

            if (bedrockItem.getIdentifier().replace(MINECRAFT_PREFIX, "").equalsIgnoreCase("tallgrass")) {
                identifier = MINECRAFT_PREFIX + "grass";
            }

            // Dirt remapping
            if (bedrockItem.getIdentifier().replace(MINECRAFT_PREFIX, "").equalsIgnoreCase("dirt")) {
                if (bedrockItem.getData() == 0)
                    identifier = MINECRAFT_PREFIX + "dirt";
                else
                    identifier = MINECRAFT_PREFIX + "coarse_dirt";
            }

            for (Map.Entry<Integer, ? extends JavaItem> javaItemEntry : javaItems.entrySet()) {
                if (identifier.equalsIgnoreCase(javaItemEntry.getValue().getIdentifier())) {
                    if(!(javaToBedrock.containsKey(javaItemEntry.getValue()) && javaToBedrock.containsKey(javaItemEntry.getValue()))) {
                        bedrockToJava.put(bedrockItemEntry.getValue(), javaItemEntry.getValue());
                        javaToBedrock.put(javaItemEntry.getValue(), bedrockItemEntry.getValue());
                    }
                }
            }
        }
    }

    public JavaItem convertToJava(ItemData item) {
        for (Map.Entry<String, BedrockItem> bedrockItem : Toolbox.BEDROCK_ITEMS.entrySet()) {
            if (bedrockItem.getValue().getId() != item.getId() || bedrockItem.getValue().getData() != item.getDamage())
                continue;

            return bedrockToJava.get(bedrockItem.getValue());
        }
        return null;
    }

    public BedrockItem convertToBedrock(ItemStack item) {
        JavaItem javaItem = Toolbox.JAVA_ITEMS.get(item.getId());
        return javaItem != null ? javaToBedrock.get(javaItem) : null;
    }

    public BedrockItem convertToBedrockB(ItemStack block) {
        JavaBlock javaBlock = Toolbox.JAVA_BLOCKS.get(block.getId());
        return javaBlock != null ? javaToBedrock.get(javaBlock) : null;
    }

    private static String getBedrockIdentifier(String javaIdentifier) {
        javaIdentifier = javaIdentifier.toLowerCase();
        javaIdentifier = javaIdentifier.replace("terracotta", "stained_hardened_clay");
        javaIdentifier = javaIdentifier.replace("slab", "wooden_slab");
        javaIdentifier = javaIdentifier.replace("concrete_powder", "concretePowder");
        return javaIdentifier;
    }

    private static boolean isTool(String s) {
        return s.contains("shovel") || s.contains("sword") || s.contains("axe") || s.contains("pickaxe") || s.contains("spade") || s.contains("hoe");
    }
}
package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.network.translators.item.BedrockItem;
import org.geysermc.connector.network.translators.item.JavaItem;
import org.geysermc.connector.network.translators.block.type.ColoredBlock;
import org.geysermc.connector.network.translators.block.type.DyeColor;
import org.geysermc.connector.network.translators.block.type.StoneType;
import org.geysermc.connector.network.translators.block.type.WoodBlock;
import org.geysermc.connector.network.translators.block.type.WoodType;

import java.util.HashMap;
import java.util.Map;

public class Remapper {

    public static final String MINECRAFT = "minecraft:";

    public static final Remapper ITEM_REMAPPER = new Remapper();
    public static final Remapper BLOCK_REMAPPER = new Remapper();

    private final Map<BedrockItem, JavaItem> bedrockToJava;
    private final Map<JavaItem, BedrockItem> javaToBedrock;

    public Remapper() {
        bedrockToJava = new HashMap<>();
        javaToBedrock = new HashMap<>();
    }

    // Registers the conversions for bedrock <-> java
    public void registerConversions(Map<String, BedrockItem> bedrockItems, Map<String, JavaItem> javaItems) {
        for (Map.Entry<String, BedrockItem> bedrockItemEntry : bedrockItems.entrySet()) {
            BedrockItem bedrockItem = bedrockItemEntry.getValue();
            String identifier = bedrockItem.getIdentifier();

            // Colorable block remapping
            for (ColoredBlock coloredBlock : ColoredBlock.values()) {
                if (!getBedrockIdentifier(coloredBlock.name()).equalsIgnoreCase(bedrockItem.getIdentifier().replace(MINECRAFT, "")))
                    continue;

                // The item must be colorable
                for (DyeColor color : DyeColor.values()) {
                    if (color.getId() != bedrockItem.getData())
                        continue;

                    // Add the color to the identifier
                    identifier = MINECRAFT + color.name().toLowerCase() + "_" + coloredBlock.name().toLowerCase();
                }
            }

            // Wood remapping
            for (WoodBlock woodBlock : WoodBlock.values()) {
                if (!getBedrockIdentifier(woodBlock.name()).equalsIgnoreCase(bedrockItem.getIdentifier().replace(MINECRAFT, "")))
                    continue;

                if (isTool(bedrockItem.getIdentifier()))
                    continue;

                if (woodBlock == WoodBlock.SLAB && !bedrockItem.getIdentifier().contains("wooden"))
                    continue;

                for (WoodType woodType : WoodType.values()) {
                    if (woodType.getId() != bedrockItem.getData())
                        continue;

                    identifier = MINECRAFT + woodType.name().toLowerCase() + "_" + woodBlock.name().toLowerCase();
                }
            }

            // Stone remapping
            if (bedrockItem.getIdentifier().replace(MINECRAFT, "").equalsIgnoreCase("stone") && !isTool(bedrockItem.getIdentifier())) {
                for (StoneType stoneType : StoneType.values()) {
                    if (stoneType.getId() != bedrockItem.getData())
                        continue;

                    // Set the identifier to stone
                    identifier = MINECRAFT + stoneType.name().toLowerCase();
                }
            }

            // Grass remapping
            if (bedrockItem.getIdentifier().replace(MINECRAFT, "").equalsIgnoreCase("grass")) {
                identifier = MINECRAFT + "grass_block";
            }

            if (bedrockItem.getIdentifier().replace(MINECRAFT, "").equalsIgnoreCase("tallgrass")) {
                identifier = MINECRAFT + "grass";
            }

            // Dirt remapping
            if (bedrockItem.getIdentifier().replace(MINECRAFT, "").equalsIgnoreCase("dirt")) {
                if (bedrockItem.getData() == 0)
                    identifier = MINECRAFT + "dirt";
                else
                    identifier = MINECRAFT + "coarse_dirt";
            }

            for (Map.Entry<String, JavaItem> javaItemEntry : javaItems.entrySet()) {
                if (identifier.equalsIgnoreCase(javaItemEntry.getKey())) {
                    bedrockToJava.put(bedrockItemEntry.getValue(), javaItemEntry.getValue());
                    javaToBedrock.put(javaItemEntry.getValue(), bedrockItemEntry.getValue());
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
        for (Map.Entry<String, JavaItem> javaItem : Toolbox.JAVA_ITEMS.entrySet()) {
            if (javaItem.getValue().getId() != item.getId())
                continue;

            return javaToBedrock.get(javaItem.getValue());
        }

        return null;
    }

    public BedrockItem convertToBedrockB(ItemStack block) {
        for (Map.Entry<String, JavaItem> javaItem : Toolbox.JAVA_BLOCKS.entrySet()) {
            if (javaItem.getValue().getId() != block.getId())
                continue;

            return javaToBedrock.get(javaItem.getValue());
        }

        return null;
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
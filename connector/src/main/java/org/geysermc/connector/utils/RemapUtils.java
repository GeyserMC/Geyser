package org.geysermc.connector.utils;

import org.geysermc.connector.network.translators.item.BedrockItem;
import org.geysermc.connector.network.translators.item.DyeColor;
import org.geysermc.connector.network.translators.item.JavaItem;
import org.geysermc.connector.network.translators.item.WoodType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;

class RemapUtils {
    static void start() {
        //colors
        Remapper.predicates.put((x) -> x.getF().contains("white"), (x, y) -> {
            //System.out.println(x.getIdentifier());
            if(customColorIfNeeded(y)) return;

            if (y.getIdentifier().replaceAll("terracotta", "stained_hardened_clay")
                    .replaceAll("white_", "")
                    .equalsIgnoreCase(x.getIdentifier()) && x.getData() == 0) {

                for (DyeColor dyeColor : DyeColor.values()) {
                    JavaItem j = new JavaItem(y.getIdentifier().replaceAll("white", dyeColor.getName()), y.getId());
                    Remapper.convertions.computeIfAbsent(j, (q) -> new ArrayList<>());
                    Remapper.convertions.get(j).add(new BedrockItem(x.getIdentifier(), x.getId(), dyeColor.getId()));
                }
            }

        });
        //shared name
        Remapper.predicates.put((x) -> x.getF().equalsIgnoreCase(x.getS()), (x, y) -> {
            if(customColorIfNeeded(y)) return;
            Remapper.convertions.computeIfAbsent(y, (q) -> new ArrayList<>());
            Remapper.convertions.get(y).add(x);
        });
        //wood
        Remapper.predicates.put((x) -> x.getF().contains("oak"), (x, y) -> {
            //System.out.println(x.getIdentifier());
            if(customWoodIfNeeded(y)) return;

            if (y.getIdentifier().replaceAll("oak_", "")
                    .equalsIgnoreCase(x.getIdentifier()) && x.getData() == 0) {

                for (WoodType woodType : WoodType.values()) {
                    JavaItem j = new JavaItem(y.getIdentifier().replaceAll("oak", woodType.getName()), y.getId());
                    Remapper.convertions.computeIfAbsent(j, (q) -> new ArrayList<>());
                    Remapper.convertions.get(j).add(new BedrockItem(x.getIdentifier(), x.getId(), woodType.getId()));
                }
            }

        });
        //TODO: add stone support
        /*Remapper.predicates.put((x) -> x.getF().contains("oak"), (x, y) -> {
            //System.out.println(x.getIdentifier());
            //if(customWoodIfNeeded(y)) return;

            if (y.getIdentifier().replaceAll("oak_", "")
                    .equalsIgnoreCase(x.getIdentifier()) && x.getData() == 0) {

                for (WoodType woodType : WoodType.values()) {
                    JavaItem j = new JavaItem(y.getIdentifier().replaceAll("oak", woodType.getName()), y.getId());
                    Remapper.convertions.computeIfAbsent(j, (q) -> new ArrayList<>());
                    Remapper.convertions.get(j).add(new BedrockItem(x.getIdentifier(), x.getId(), woodType.getId()));
                }
            }

        });*/
    }

    private static boolean customColorIfNeeded(JavaItem j) {
        if(j.getIdentifier().equalsIgnoreCase("minecraft:shulker_box")) {
            System.out.println(j.getIdentifier());
            Remapper.convertions.put(j, Arrays.asList(new BedrockItem("minecraft:undyed_shulker_box", 205, 0)));
            return true;
        }
        return false;
    }

    private static boolean customWoodIfNeeded(JavaItem j) {
        for(WoodType t : WoodType.values()) {
            if (j.getIdentifier().equalsIgnoreCase("minecraft:stripped_" + t.getName() +"_wood")) {
                Remapper.convertions.put(j, Arrays.asList(new BedrockItem("minecraft:wood", 467, t.getId() + 8)));
                return false;
            }
        }
        return false;
    }
}

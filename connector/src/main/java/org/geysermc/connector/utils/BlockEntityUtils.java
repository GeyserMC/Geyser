package org.geysermc.connector.utils;

import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.entity.BlockEntityTranslator;

public class BlockEntityUtils {

    public static String getBedrockBlockEntityId(String id) {
        // This is the only exception when it comes to block entity ids
        if (id.contains("piston_head"))
            return "PistonArm";

        id = id.replace("minecraft:", "");
        id = id.replace("_", " ");
        String[] words = id.split(" ");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
        }

        id = String.join(" ", words);
        return id.replace(" ", "");
    }

    public static BlockEntityTranslator getBlockEntityTranslator(String name) {
        BlockEntityTranslator blockEntityTranslator = TranslatorsInit.getBlockEntityTranslators().get(name);
        if (blockEntityTranslator == null) {
            return TranslatorsInit.getBlockEntityTranslators().get("Empty");
        }

        return blockEntityTranslator;
    }
}

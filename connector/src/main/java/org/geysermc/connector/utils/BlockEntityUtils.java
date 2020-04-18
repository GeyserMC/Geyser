package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;

import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.network.translators.block.entity.BlockEntityTranslator;

public class BlockEntityUtils {

    private static final BlockEntityTranslator EMPTY_TRANSLATOR = Translators.getBlockEntityTranslators().get("Empty");

    public static String getBedrockBlockEntityId(String id) {
        // This is the only exception when it comes to block entity ids
        if (id.contains("piston_head"))
            return "PistonArm";

        id = id.toLowerCase()
            .replace("minecraft:", "")
            .replace("_", " ");
        String[] words = id.split(" ");
        for (int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
        }

        id = String.join(" ", words);
        return id.replace(" ", "");
    }

    public static BlockEntityTranslator getBlockEntityTranslator(String name) {
        BlockEntityTranslator blockEntityTranslator = Translators.getBlockEntityTranslators().get(name);
        if (blockEntityTranslator == null) {
            return EMPTY_TRANSLATOR;
        }

        return blockEntityTranslator;
    }

    public static void updateBlockEntity(GeyserSession session, com.nukkitx.nbt.tag.CompoundTag blockEntity, Position position) {
        System.out.println("Test 2");
        BlockEntityDataPacket blockEntityPacket = new BlockEntityDataPacket();
        blockEntityPacket.setBlockPosition(Vector3i.from(position.getX(), position.getY(), position.getZ()));
        blockEntityPacket.setData(blockEntity);
        session.getUpstream().sendPacket(blockEntityPacket);
    }
}

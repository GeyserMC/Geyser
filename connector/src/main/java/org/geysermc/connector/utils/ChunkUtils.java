package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.geysermc.connector.console.GeyserLogger;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.BlockEntry;
import org.geysermc.connector.network.translators.block.entity.BlockEntityTranslator;
import org.geysermc.connector.network.translators.block.entity.SignBlockEntityTranslator;
import org.geysermc.connector.world.chunk.ChunkSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkUtils {

    public static ChunkData translateToBedrock(Column column) {
        ChunkData chunkData = new ChunkData();

        Chunk[] chunks = column.getChunks();
        int chunkSectionCount = chunks.length;
        chunkData.sections = new ChunkSection[chunkSectionCount];

        List<CompoundTag> blockEntities = new ArrayList<>(Arrays.asList(column.getTileEntities()));
        for (int chunkY = 0; chunkY < chunkSectionCount; chunkY++) {
            chunkData.sections[chunkY] = new ChunkSection();
            Chunk chunk = chunks[chunkY];

            if (chunk == null || chunk.isEmpty())
                continue;

            ChunkSection section = chunkData.sections[chunkY];

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState blockState = chunk.get(x, y, z);
                        BlockEntry block = TranslatorsInit.getBlockTranslator().getBlockEntry(blockState);

                        section.getBlockStorageArray()[0].setFullBlock(ChunkSection.blockPosition(x, y, z),
                                block.getBedrockRuntimeId());

                        if (block.isWaterlogged()) {
                            BlockEntry water = TranslatorsInit.getBlockTranslator().getBlockEntry("minecraft:water[level=0]");
                            section.getBlockStorageArray()[1].setFullBlock(ChunkSection.blockPosition(x, y, z), water.getBedrockRuntimeId());
                        }
                    }
                }
            }
        }

        List<com.nukkitx.nbt.tag.CompoundTag> bedrockBlockEntities = new ArrayList<>();
        for (CompoundTag tag : blockEntities) {
            Tag idTag = tag.get("id");
            if (idTag == null && !tag.contains("Sign")) {
                GeyserLogger.DEFAULT.debug("Got tag with no id: " + tag.getValue());
                continue;
            }

            String id = idTag == null ? "Sign" : BlockEntityUtils.getBedrockBlockEntityId((String) idTag.getValue());
            BlockEntityTranslator blockEntityTranslator = BlockEntityUtils.getBlockEntityTranslator(id);
            bedrockBlockEntities.add(blockEntityTranslator.getBlockEntityTag(tag, id));
        }

        chunkData.blockEntities = bedrockBlockEntities;
        return chunkData;
    }

    public static void updateBlock(GeyserSession session, BlockState blockState, Position position) {
        BlockEntry blockEntry = TranslatorsInit.getBlockTranslator().getBlockEntry(blockState);
        Vector3i pos = Vector3i.from(position.getX(), position.getY(), position.getZ());

        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(pos);
        updateBlockPacket.setRuntimeId(blockEntry.getBedrockRuntimeId());
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.getUpstream().sendPacket(updateBlockPacket);

        UpdateBlockPacket waterPacket = new UpdateBlockPacket();
        waterPacket.setDataLayer(1);
        waterPacket.setBlockPosition(pos);
        if (blockEntry.isWaterlogged()) {
            BlockEntry water = TranslatorsInit.getBlockTranslator().getBlockEntry("minecraft:water[level=0]");
            waterPacket.setRuntimeId(water.getBedrockRuntimeId());
        } else {
            waterPacket.setRuntimeId(0);
        }
        session.getUpstream().sendPacket(waterPacket);
    }

    public static final class ChunkData {
        public ChunkSection[] sections;

        public byte[] biomes = new byte[256];
        public List<com.nukkitx.nbt.tag.CompoundTag> blockEntities = new ArrayList<>();
    }
}

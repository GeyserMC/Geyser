package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTOutputStream;
import com.nukkitx.nbt.tag.IntTag;
import com.nukkitx.nbt.tag.StringTag;
import com.nukkitx.nbt.tag.Tag;
import org.geysermc.connector.network.translators.BlockEntityUtils;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.BlockEntry;
import org.geysermc.connector.world.chunk.ChunkSection;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ChunkUtils {

    public static ChunkData translateToBedrock(Column column) {
        ChunkData chunkData = new ChunkData();

        Chunk[] chunks = column.getChunks();
        int chunkSectionCount = chunks.length;
        chunkData.sections = new ChunkSection[chunkSectionCount];


        for(CompoundTag tag : column.getTileEntities()) {
            System.out.println(tag.get("id").getValue());
        }
        //Start work on block entities
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            NBTOutputStream nbtStream = NbtUtils.createNetworkWriter(stream);

            for (CompoundTag tag : column.getTileEntities()) {
                Map<String, Tag<?>> map = new HashMap<>();

                for(Tag<?> extra : BlockEntityUtils.getExtraTags(tag)) {
                    map.put(extra.getName(), extra);
                }

                nbtStream.write(new com.nukkitx.nbt.tag.CompoundTag("", map));
            }

            chunkData.blockEntities = stream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
        }

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
                        BlockEntry block = TranslatorsInit.getBlockTranslator().getBedrockBlock(blockState);

                        section.getBlockStorageArray()[0].setFullBlock(ChunkSection.blockPosition(x, y, z),
                                block.getBedrockId() << 4 | block.getBedrockData());

                        if (block.getJavaIdentifier().contains("waterlogged=true")) {
                            section.getBlockStorageArray()[1].setFullBlock(ChunkSection.blockPosition(x, y, z),
                                    9 << 4); // water id
                        }
                    }
                }
            }
        }
        return chunkData;
    }

    public static final class ChunkData {
        public ChunkSection[] sections;

        public byte[] biomes = new byte[256];
        public byte[] blockEntities = new byte[0];
    }
}

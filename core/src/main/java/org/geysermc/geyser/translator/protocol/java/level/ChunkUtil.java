package org.geysermc.geyser.translator.protocol.java.level;

import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.chunk.BlockStorage;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.BitStorage;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.palette.Palette;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;

import java.util.Set;

import static org.geysermc.geyser.util.ChunkUtils.indexYZXtoXZY;

public class ChunkUtil {

    private static final Set<Integer> FURNITURE_BLOCKS = Set.of(
        Blocks.NOTE_BLOCK.javaId(),
        Blocks.OAK_LEAVES.javaId(),
        Blocks.CHERRY_LEAVES.javaId()
    );

    public static Vector3i getRealWorldBlockPos(int chunkX, int chunkZ, int yzx) {
        return getRealWorldBlockPos(chunkX, chunkZ, getRelativeBlockPos(yzx));
    }

    public static Vector3i getRealWorldBlockPos(int chunkX, int chunkZ, Vector3i relPos) {
        int x = (chunkX << 4) + relPos.getX();
        int z = (chunkZ << 4) + relPos.getZ();
        return Vector3i.from(x, relPos.getY(), z);
    }

    public static Vector3i getRelativeBlockPos(int yzx) {
        int y = yzx / 256;
        int z = (yzx / 16) % 16;
        int x = yzx % 16;
        return Vector3i.from(x, y, z);
    }

    public static void modifyPallete(
        BitStorage javaData, Palette javaPalette, GeyserSession session, int sectionY, int yOffset,
        ClientboundLevelChunkWithLightPacket packet, BlockStorage layer0
    ) {
        // Replace furniture host blocks with their custom Bedrock block override
        for (int yzx = 0; yzx < BlockStorage.SIZE; yzx++) {
            int paletteId = javaData.get(yzx);
            int javaId = javaPalette.idToState(paletteId);
            if (session.getBlockMappings().getFurnitureBlocks().contains(javaId)) {
                // could be a custom block
                Vector3i realPos = Vector3i.from((packet.getX() << 4) + (yzx & 0xF), ((sectionY + yOffset) << 4) + ((yzx >> 8) & 0xF), (packet.getZ() << 4) + ((yzx >> 4) & 0xF));
                var blockOverride = session.getGeyser().getWorldManager().getBedrockBlockOverride(session, realPos.getX(), realPos.getY(), realPos.getZ());
                if (blockOverride != null) {
                    int xzy = indexYZXtoXZY(yzx);
                    // setFullBlock handles palette growth/resize internally, keeping layer0's
                    // bit array and palette in sync (unlike writing to a captured BitArray reference)
                    layer0.setFullBlock(xzy, blockOverride.getRuntimeId());
                }
            }
        }
    }

}

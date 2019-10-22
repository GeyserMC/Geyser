package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.BlockEntry;
import org.geysermc.connector.world.GlobalBlockPalette;

public class JavaBlockChangeTranslator extends PacketTranslator<ServerBlockChangePacket> {
    @Override
    public void translate(ServerBlockChangePacket packet, GeyserSession session) {
        BlockChangeRecord record = packet.getRecord();
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(Vector3i.from(
                record.getPosition().getX(),
                record.getPosition().getY(),
                record.getPosition().getZ()
        ));

        BlockEntry itemEntry = TranslatorsInit.getBlockTranslator().getBedrockBlock(record.getBlock());
        updateBlockPacket.setRuntimeId(GlobalBlockPalette.getOrCreateRuntimeId(itemEntry.getBedrockId() << 4 | itemEntry.getBedrockData()));
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);

        session.getUpstream().sendPacket(updateBlockPacket);
    }
}

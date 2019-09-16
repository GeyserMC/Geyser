package org.geysermc.connector.network.translators.java.world;

import com.flowpowered.math.vector.Vector3i;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockChangeRecord;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.item.BedrockItem;
import org.geysermc.connector.world.GlobalBlockPalette;

public class JavaBlockChangeTranslator extends PacketTranslator<ServerBlockChangePacket> {
    @Override
    public void translate(ServerBlockChangePacket packet, GeyserSession session) {
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        BlockChangeRecord record = packet.getRecord();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(new Vector3i(
                record.getPosition().getX(),
                record.getPosition().getY(),
                record.getPosition().getZ()));

        BedrockItem bedrockItem = TranslatorsInit.getBlockTranslator().getBedrockBlock(record.getBlock());
        updateBlockPacket.setRuntimeId(GlobalBlockPalette.getOrCreateRuntimeId(bedrockItem.hashCode()));

        session.getUpstream().sendPacket(updateBlockPacket);
    }
}

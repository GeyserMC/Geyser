package org.geysermc.connector.network.translators.java.visual;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.BlockBreakStage;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockBreakAnimPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;

public class JavaBlockBreakAnimationPacketTranslator extends PacketTranslator<ServerBlockBreakAnimPacket> {
    @Override
    public void translate(ServerBlockBreakAnimPacket packet, GeyserSession session) {
        LevelEventPacket levelEventPacket = new LevelEventPacket();

        Position position = packet.getPosition();

        levelEventPacket.setPosition(Vector3f.from(position.getX(), position.getY(), position.getZ()));

        System.out.println(packet.getStage());

        switch (packet.getStage()) {
            case STAGE_1:
                levelEventPacket.setEvent(LevelEventPacket.Event.BLOCK_START_BREAK);

                break;

            case RESET:
                levelEventPacket.setEvent(LevelEventPacket.Event.BLOCK_STOP_BREAK);
                break;

            default:
                levelEventPacket.setEvent(LevelEventPacket.Event.BLOCK_CONTINUE_BREAK);
                break;
        }

        if(packet.getStage() != BlockBreakStage.RESET) {
            levelEventPacket.setData(packet.getStage().ordinal());
        }


        session.getUpstream().sendPacket(levelEventPacket);
    }
}

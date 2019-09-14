package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;

public class BedrockMovePlayerTranslator extends PacketTranslator<MovePlayerPacket> {

    @Override
    public void translate(MovePlayerPacket packet, GeyserSession session) {
        Entity entity = session.getPlayerEntity();
        if (entity == null)
            return;

        // TODO: Implement collision support
        ClientPlayerPositionRotationPacket playerPositionRotationPacket = new ClientPlayerPositionRotationPacket(
                packet.isOnGround(), packet.getPosition().getX(), Math.ceil((packet.getPosition().getY() - EntityType.PLAYER.getOffset()) * 2) / 2,
                packet.getPosition().getZ(), packet.getRotation().getY(), packet.getRotation().getX());

        entity.moveAbsolute(packet.getPosition(), packet.getRotation());
        session.getDownstream().getSession().send(playerPositionRotationPacket);
    }
}

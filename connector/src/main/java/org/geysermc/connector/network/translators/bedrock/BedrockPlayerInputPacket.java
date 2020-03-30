package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.client.world.ClientSteerVehiclePacket;
import com.nukkitx.protocol.bedrock.packet.PlayerInputPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = PlayerInputPacket.class)
// Makes minecarts respond to player input
public class BedrockPlayerInputPacket extends PacketTranslator<PlayerInputPacket> {

    @Override
    public void translate(PlayerInputPacket packet, GeyserSession session) {
        ClientSteerVehiclePacket clientSteerVehiclePacket = new ClientSteerVehiclePacket(
                packet.getInputMotion().getX(), packet.getInputMotion().getY(), packet.isJumping(), packet.isSneaking()
        );

        session.getDownstream().getSession().send(clientSteerVehiclePacket);
    }
}

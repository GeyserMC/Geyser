package org.geysermc.connector.network.translators.bedrock;

import com.nukkitx.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;

public class BedrockPlayerInitialized extends PacketTranslator<SetLocalPlayerAsInitializedPacket> {
    @Override
    public void translate(SetLocalPlayerAsInitializedPacket packet, GeyserSession session) {
        if (session.getPlayerEntity().getGeyserId() == packet.getRuntimeEntityId()) {
            session.getUpstream().setFrozen(false);
        }
    }
}

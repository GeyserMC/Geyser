package org.geysermc.connector.network.translators.bedrock;

import com.nukkitx.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.SkinUtils;

public class BedrockPlayerInitializedTranslator extends PacketTranslator<SetLocalPlayerAsInitializedPacket> {
    @Override
    public void translate(SetLocalPlayerAsInitializedPacket packet, GeyserSession session) {
        if (session.getPlayerEntity().getGeyserId() == packet.getRuntimeEntityId()) {
            if (!session.getUpstream().isInitialized()) {
                session.getUpstream().setInitialized(true);

                for (PlayerEntity entity : session.getEntityCache().getEntitiesByType(PlayerEntity.class)) {
                    if (!entity.isValid()) {
                        // async skin loading
                        SkinUtils.requestAndHandleSkinAndCape(entity, session, skinAndCape -> entity.sendPlayer(session));
                    }
                }
            }
        }
    }
}

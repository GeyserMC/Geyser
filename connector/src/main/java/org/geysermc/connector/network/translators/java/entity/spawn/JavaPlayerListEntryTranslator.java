package org.geysermc.connector.network.translators.java.entity.spawn;

import com.flowpowered.math.vector.Vector3f;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.SkinProvider;

public class JavaPlayerListEntryTranslator extends PacketTranslator<ServerPlayerListEntryPacket> {
    @Override
    public void translate(ServerPlayerListEntryPacket packet, GeyserSession session) {
        if (packet.getAction() != PlayerListEntryAction.ADD_PLAYER && packet.getAction() != PlayerListEntryAction.REMOVE_PLAYER) return;

        PlayerListPacket translate = new PlayerListPacket();
        translate.setType(packet.getAction() == PlayerListEntryAction.ADD_PLAYER ? PlayerListPacket.Type.ADD : PlayerListPacket.Type.REMOVE);

        for (PlayerListEntry entry : packet.getEntries()) {
            PlayerListPacket.Entry entry1 = new PlayerListPacket.Entry(entry.getProfile().getId());

            if (packet.getAction() == PlayerListEntryAction.ADD_PLAYER) {
                boolean self = entry.getProfile().getId().equals(session.getPlayerEntity().getUuid());

                PlayerEntity playerEntity = session.getPlayerEntity();
                if (!self) {
                    playerEntity = new PlayerEntity(
                            entry.getProfile(),
                            -1,
                            session.getEntityCache().getNextEntityId().incrementAndGet(),
                            Vector3f.ZERO,
                            Vector3f.ZERO,
                            Vector3f.ZERO
                    );
                }

                session.getEntityCache().addPlayerEntity(playerEntity);
                playerEntity.setPlayerList(true);

                entry1.setName(entry.getProfile().getName());
                entry1.setEntityId(playerEntity.getGeyserId());
                entry1.setSkinId(entry.getProfile().getIdAsString());
                entry1.setSkinData(SkinProvider.STEVE_SKIN);
                entry1.setCapeData(new byte[0]);
                entry1.setGeometryName("geometry.humanoid");
                entry1.setGeometryData("");
                entry1.setXuid("");
                entry1.setPlatformChatId("");
            } else {
                PlayerEntity entity = session.getEntityCache().getPlayerEntity(entry.getProfile().getId());
                if (entity != null && entity.isValid()) {
                    // remove from tablist but player entity is still there
                    entity.setPlayerList(false);
                } else {
                    // just remove it from caching
                    session.getEntityCache().removePlayerEntity(entry.getProfile().getId());
                }
            }
            translate.getEntries().add(entry1);
        }
        session.getUpstream().sendPacket(translate);
    }
}

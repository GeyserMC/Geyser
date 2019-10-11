package org.geysermc.connector.network.translators.java.entity.player;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.SkinUtils;

public class JavaPlayerListEntryTranslator extends PacketTranslator<ServerPlayerListEntryPacket> {
    @Override
    public void translate(ServerPlayerListEntryPacket packet, GeyserSession session) {
        if (packet.getAction() != PlayerListEntryAction.ADD_PLAYER && packet.getAction() != PlayerListEntryAction.REMOVE_PLAYER) return;

        PlayerListPacket translate = new PlayerListPacket();
        translate.setType(packet.getAction() == PlayerListEntryAction.ADD_PLAYER ? PlayerListPacket.Type.ADD : PlayerListPacket.Type.REMOVE);

        for (PlayerListEntry entry : packet.getEntries()) {
            if (packet.getAction() == PlayerListEntryAction.ADD_PLAYER) {
                boolean self = entry.getProfile().getId().equals(session.getPlayerEntity().getUuid());

                PlayerEntity playerEntity = session.getPlayerEntity();
                if (self) playerEntity.setProfile(entry.getProfile());
                else {
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

                translate.getEntries().add(SkinUtils.buildCachedEntry(entry.getProfile(), playerEntity.getGeyserId()));
            } else {
                PlayerEntity entity = session.getEntityCache().getPlayerEntity(entry.getProfile().getId());
                if (entity != null && entity.isValid()) {
                    // remove from tablist but player entity is still there
                    entity.setPlayerList(false);
                } else {
                    // just remove it from caching
                    session.getEntityCache().removePlayerEntity(entry.getProfile().getId());
                }
                translate.getEntries().add(new PlayerListPacket.Entry(entry.getProfile().getId()));
            }
        }

        if (packet.getAction() == PlayerListEntryAction.REMOVE_PLAYER || session.getUpstream().isInitialized()) {
            session.getUpstream().sendPacket(translate);
        }
    }
}

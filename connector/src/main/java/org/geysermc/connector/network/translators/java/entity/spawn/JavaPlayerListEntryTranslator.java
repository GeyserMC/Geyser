package org.geysermc.connector.network.translators.java.entity.spawn;

import com.flowpowered.math.vector.Vector3f;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import com.nukkitx.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.ProvidedSkin;

public class JavaPlayerListEntryTranslator extends PacketTranslator<ServerPlayerListEntryPacket> {
    private static byte[] providedSkin = new ProvidedSkin("bedrock/skin/skin_steve.png").getSkin();

    @Override
    public void translate(ServerPlayerListEntryPacket packet, GeyserSession session) {
        if (packet.getAction() != PlayerListEntryAction.ADD_PLAYER && packet.getAction() != PlayerListEntryAction.REMOVE_PLAYER) return;

        PlayerListPacket translate = new PlayerListPacket();
        translate.setType(packet.getAction() == PlayerListEntryAction.ADD_PLAYER ? PlayerListPacket.Type.ADD : PlayerListPacket.Type.REMOVE);

        for (PlayerListEntry entry : packet.getEntries()) {
            PlayerListPacket.Entry entry1 = new PlayerListPacket.Entry(entry.getProfile().getId());

            if (packet.getAction() == PlayerListEntryAction.ADD_PLAYER) {
                if (session.getPlayerEntity().getUuid().equals(entry.getProfile().getId())) continue;
                long geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();

                SetLocalPlayerAsInitializedPacket initPacket = new SetLocalPlayerAsInitializedPacket();
                initPacket.setRuntimeEntityId(geyserId);
                session.getUpstream().sendPacket(initPacket);

                session.getEntityCache().addPlayerEntity(new PlayerEntity(
                        entry.getProfile(),
                        -1,
                        geyserId,
                        Vector3f.ZERO,
                        Vector3f.ZERO,
                        Vector3f.ZERO
                ));

                entry1.setName(entry.getProfile().getName());
                entry1.setEntityId(geyserId);
                entry1.setSkinId(entry.getProfile().getIdAsString());
                entry1.setSkinData(providedSkin);
                entry1.setCapeData(new byte[0]);
                entry1.setGeometryName("geometry.humanoid");
                entry1.setGeometryData("");
                entry1.setXuid("");
                entry1.setPlatformChatId("");
            } else {
                PlayerEntity entity = session.getEntityCache().getPlayerEntity(entry.getProfile().getId());
                if (entity != null && entity.isValid()) { // we'll despawn it manually ;-)
                    session.getEntityCache().removeEntity(entity);
                } else { // just remove it from caching
                    session.getEntityCache().removePlayerEntity(entry.getProfile().getId());
                }
            }
            translate.getEntries().add(entry1);
        }
        session.getUpstream().sendPacket(translate);
    }
}

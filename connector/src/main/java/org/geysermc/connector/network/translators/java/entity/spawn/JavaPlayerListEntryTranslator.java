package org.geysermc.connector.network.translators.java.entity.spawn;

import com.flowpowered.math.vector.Vector3f;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.ProvidedSkin;
import org.geysermc.connector.utils.ProvidedSkinData;

public class JavaPlayerListEntryTranslator extends PacketTranslator<ServerPlayerListEntryPacket> {
    private static ProvidedSkinData providedSkinData = ProvidedSkinData.getProvidedSkin("bedrock/skin/model_steve.json");
    private static byte[] providedSkin = new ProvidedSkin("bedrock/skin/skin_steve.png").getSkin();

    @Override
    public void translate(ServerPlayerListEntryPacket packet, GeyserSession session) {
        if (packet.getAction() != PlayerListEntryAction.ADD_PLAYER && packet.getAction() != PlayerListEntryAction.REMOVE_PLAYER) return;

        PlayerListPacket translate = new PlayerListPacket();
        translate.setType(packet.getAction() == PlayerListEntryAction.ADD_PLAYER ? PlayerListPacket.Type.ADD : PlayerListPacket.Type.REMOVE);

        for (PlayerListEntry entry : packet.getEntries()) {
            PlayerListPacket.Entry entry1 = new PlayerListPacket.Entry(entry.getProfile().getId());

            if (packet.getAction() == PlayerListEntryAction.ADD_PLAYER) {
                long geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();

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
                entry1.setSkinId(providedSkinData.getSkinId());
                entry1.setSkinData(providedSkin);
                entry1.setCapeData(new byte[0]);
                entry1.setGeometryName(providedSkinData.getGeometryId());
                entry1.setGeometryData(providedSkinData.getGeometryDataEncoded());
                entry1.setXuid("");
                entry1.setPlatformChatId("WIN10");
            } else {
                session.getEntityCache().removePlayerEntity(entry.getProfile().getId());
            }
            translate.getEntries().add(entry1);
        }

        session.getUpstream().sendPacket(translate);
    }
}

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.ItemFrameDropItemPacket;
import org.geysermc.connector.entity.ItemFrameEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = ItemFrameDropItemPacket.class)
public class BedrockItemFrameDropItemTranslator extends PacketTranslator<ItemFrameDropItemPacket> {

    @Override
    public void translate(ItemFrameDropItemPacket packet, GeyserSession session) {
        // I hope that, when we die, God (or whoever is waiting for us) tells us exactly why this code exists
        // The packet sends the Y coordinate (and just the Y coordinate) divided by two, and it's negative if it needs to be subtracted by one
        int y;
        if (packet.getBlockPosition().getY() > 0) {
            y = packet.getBlockPosition().getY() * 2;
        } else {
            y = (packet.getBlockPosition().getY() * -2) - 1;
        }
        Vector3i position = Vector3i.from(packet.getBlockPosition().getX(), y, packet.getBlockPosition().getZ());
        ClientPlayerInteractEntityPacket interactPacket = new ClientPlayerInteractEntityPacket((int) ItemFrameEntity.getItemFrameEntityId(position),
                InteractAction.ATTACK, Hand.MAIN_HAND);
        session.getDownstream().getSession().send(interactPacket);
    }

}

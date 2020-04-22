package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.nukkitx.protocol.bedrock.packet.ItemFrameDropItemPacket;
import org.geysermc.connector.entity.ItemFrameEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = ItemFrameDropItemPacket.class)
public class BedrockItemFrameDropItemTranslator extends PacketTranslator<ItemFrameDropItemPacket> {

    @Override
    public void translate(ItemFrameDropItemPacket packet, GeyserSession session) {
        System.out.println("Packet: " + packet.getBlockPosition());
        ClientPlayerInteractEntityPacket attackPacket = new ClientPlayerInteractEntityPacket((int) ItemFrameEntity.getItemFrameEntityId(packet.getBlockPosition()),
                InteractAction.ATTACK);
        session.getDownstream().getSession().send(attackPacket);
    }

}

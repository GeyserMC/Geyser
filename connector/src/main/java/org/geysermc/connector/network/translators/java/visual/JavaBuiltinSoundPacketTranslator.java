package org.geysermc.connector.network.translators.java.visual;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlayBuiltinSoundPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.PlaySoundPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;

public class JavaBuiltinSoundPacketTranslator extends PacketTranslator<ServerPlayBuiltinSoundPacket> {
    @Override
    public void translate(ServerPlayBuiltinSoundPacket packet, GeyserSession session) {
        PlaySoundPacket packet1 = new PlaySoundPacket();

        packet1.setPitch(packet.getPitch());
        packet1.setVolume(packet.getVolume());
        packet1.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
        packet1.setSound(TranslatorsInit.SOUNDS.get(packet.getSound()).getSound());

        session.getUpstream().sendPacketImmediately(packet1);
    }
}

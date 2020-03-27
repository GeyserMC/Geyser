package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlaySoundPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.PlaySoundPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.SoundUtils;

public class JavaPlaySoundTranslator extends PacketTranslator<ServerPlaySoundPacket> {

    @Override
    public void translate(ServerPlaySoundPacket packet, GeyserSession session) {

        PlaySoundPacket soundPacket = new PlaySoundPacket();
        soundPacket.setSound(SoundUtils.getIdentifier(packet.getSound()));
        soundPacket.setPitch(packet.getPitch());
        soundPacket.setVolume(packet.getVolume());
        soundPacket.setPosition(Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
        session.getUpstream().sendPacket(soundPacket);

        String sb = "--------------\n" +
                packet.getSound().toString() + "\n" +
                soundPacket.getSound() + "\n" +
                soundPacket.getPitch() + "\n" +
                soundPacket.getVolume();
        GeyserConnector.getInstance().getLogger().debug(sb);


    }

}

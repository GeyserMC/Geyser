package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerStopSoundPacket;
import com.nukkitx.protocol.bedrock.packet.StopSoundPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.SoundUtils;

@Translator(packet = ServerStopSoundPacket.class)
public class JavaStopSoundTranslator extends PacketTranslator<ServerStopSoundPacket> {

    @Override
    public void translate(ServerStopSoundPacket packet, GeyserSession session) {
        String identifier = SoundUtils.getIdentifier(packet.getSound());
        if(identifier != null){
            StopSoundPacket stopSoundPacket = new StopSoundPacket();
            stopSoundPacket.setSoundName(identifier);
            session.getUpstream().sendPacket(stopSoundPacket);
        }
    }

}

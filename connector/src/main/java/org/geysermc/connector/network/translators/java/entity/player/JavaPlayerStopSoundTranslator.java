package org.geysermc.connector.network.translators.java.entity.player;

import com.github.steveice10.mc.protocol.data.game.world.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.world.sound.CustomSound;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerStopSoundPacket;
import com.nukkitx.protocol.bedrock.packet.StopSoundPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.sound.SoundMap;

public class JavaPlayerStopSoundTranslator extends PacketTranslator<ServerStopSoundPacket> {

    @Override
    public void translate(ServerStopSoundPacket packet, GeyserSession session) {
        String packetSound;
        if(packet.getSound() instanceof BuiltinSound) {
            packetSound = ((BuiltinSound) packet.getSound()).getName();
        } else if(packet.getSound() instanceof CustomSound) {
            packetSound = ((CustomSound) packet.getSound()).getName();
        } else {
            session.getConnector().getLogger().debug("Unknown sound packet, we were unable to map this. " + packet.toString());
            return;
        }
        SoundMap.SoundMapping soundMapping = SoundMap.get().fromJava(packetSound);
        session.getConnector().getLogger()
                .debug("[StopSound] Sound mapping " + packetSound + " -> "
                        + soundMapping + (soundMapping == null ? "[not found]" : "")
                        + " - " + packet.toString());
        String playsound;
        if(soundMapping == null || soundMapping.getPlaysound() == null) {
            // no mapping
            session.getConnector().getLogger()
                    .debug("[StopSound] Defaulting to sound server gave us.");
            playsound = packetSound;
        } else {
            playsound = soundMapping.getPlaysound();
        }

        StopSoundPacket stopSoundPacket = new StopSoundPacket();
        stopSoundPacket.setSoundName(playsound);
        // packet not mapped in the library
        stopSoundPacket.setStoppingAllSound(false);

        session.getUpstream().sendPacket(stopSoundPacket);
        session.getConnector().getLogger().debug("[StopSound] Packet sent - " + packet.toString() + " --> " + stopSoundPacket);
    }
}

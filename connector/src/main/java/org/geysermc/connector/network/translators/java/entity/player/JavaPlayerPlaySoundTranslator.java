package org.geysermc.connector.network.translators.java.entity.player;

import com.github.steveice10.mc.protocol.data.game.world.sound.BuiltinSound;
import com.github.steveice10.mc.protocol.data.game.world.sound.CustomSound;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerPlaySoundPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.sound.SoundMap;

public class JavaPlayerPlaySoundTranslator extends PacketTranslator<ServerPlaySoundPacket> {

    public static double processCoordinate(double f) {
        return (f / 3D) * 8D;
    }

    @Override
    public void translate(ServerPlaySoundPacket packet, GeyserSession session) {
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
                .debug("[PlaySound] Sound mapping " + packetSound + " -> "
                        + soundMapping + (soundMapping == null ? "[not found]" : "")
                        + " - " + packet.toString());
        String playsound;
        if(soundMapping == null || soundMapping.getPlaysound() == null) {
            // no mapping
            session.getConnector().getLogger()
                    .debug("[PlaySound] Defaulting to sound server gave us.");
            playsound = packetSound;
        } else {
            playsound = soundMapping.getPlaysound();
        }

        PlaySoundPacket playSoundPacket = new PlaySoundPacket();
        playSoundPacket.setSound(playsound);
        playSoundPacket.setPosition(Vector3f.from(processCoordinate(packet.getX()), processCoordinate(packet.getY()), processCoordinate(packet.getZ())));
        playSoundPacket.setVolume(packet.getVolume());
        playSoundPacket.setPitch(packet.getPitch());

        session.getUpstream().sendPacket(playSoundPacket);
        session.getConnector().getLogger().debug("[PlaySound] Packet sent - " + packet.toString() + " --> " + playSoundPacket);
    }
}

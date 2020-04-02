package org.geysermc.connector.network.translators.java;

import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.packet.login.client.LoginPluginResponsePacket;
import com.github.steveice10.mc.protocol.packet.login.server.LoginPluginRequestPacket;

@Translator(packet = LoginPluginRequestPacket.class)
public class JavaLoginPluginMessageTranslator extends PacketTranslator<LoginPluginRequestPacket> {
    @Override
    public void translate(LoginPluginRequestPacket packet, GeyserSession session) {
        // A vanilla client doesn't know any PluginMessage in the Login state, so we don't know any either.
        session.getDownstream().getSession().send(
                new LoginPluginResponsePacket(packet.getMessageId(), null)
        );
    }
}

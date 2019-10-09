package org.geysermc.connector.network.session;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.InetSocketAddress;

@RequiredArgsConstructor
public class UpstreamSession {
    @Getter private final BedrockServerSession session;
    @Getter @Setter
    private boolean initialized = false;

    public void sendPacket(@NonNull BedrockPacket packet) {
        session.sendPacket(packet);
    }

    public void sendPacketImmediately(@NonNull BedrockPacket packet) {
        session.sendPacketImmediately(packet);
    }

    public void disconnect(String reason) {
        session.disconnect(reason);
    }

    public boolean isClosed() {
        return session.isClosed();
    }

    public InetSocketAddress getAddress() {
        return session.getAddress();
    }
}

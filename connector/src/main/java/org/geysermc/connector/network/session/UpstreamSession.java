package org.geysermc.connector.network.session;

import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.geysermc.api.Geyser;

import java.net.InetSocketAddress;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequiredArgsConstructor
public class UpstreamSession {
    @Getter private final BedrockServerSession session;
    private Queue<BedrockPacket> packets = new ConcurrentLinkedQueue<>();
    @Getter private boolean frozen = false;

    public void sendPacket(@NonNull BedrockPacket packet) {
        if (frozen || !packets.isEmpty()) {
            packets.add(packet);
        } else {
            session.sendPacket(packet);
        }
    }

    public void sendPacketImmediately(@NonNull BedrockPacket packet) {
        session.sendPacketImmediately(packet);
    }

    public void setFrozen(boolean frozen) {
        if (this.frozen != frozen) {
            this.frozen = frozen;

            if (!frozen) {
                Geyser.getGeneralThreadPool().execute(() -> {
                    BedrockPacket packet;
                    while ((packet = packets.poll()) != null) {
                        session.sendPacket(packet);
                    }
                });
            }
        }
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

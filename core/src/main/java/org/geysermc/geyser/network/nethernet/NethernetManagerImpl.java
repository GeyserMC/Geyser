package org.geysermc.geyser.network.nethernet;

import io.netty.channel.DefaultEventLoopGroup;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.network.NethernetManager;

/**
 * Implementation of {@link NethernetManager} that wraps {@link NetherNetServer}.
 * All state transitions are synchronized to prevent concurrent start/stop races.
 */
public class NethernetManagerImpl implements NethernetManager {

    private final GeyserImpl geyser;
    private final DefaultEventLoopGroup playerEventLoopGroup;
    private final String connectionId;
    private final Object lock = new Object();
    private volatile NetherNetServer server;

    public NethernetManagerImpl(GeyserImpl geyser, DefaultEventLoopGroup playerEventLoopGroup, String connectionId) {
        this.geyser = geyser;
        this.playerEventLoopGroup = playerEventLoopGroup;
        this.connectionId = connectionId;
    }

    @Override
    public boolean start() {
        synchronized (lock) {
            if (server != null && server.isRunning()) {
                return true;
            }
            // Shut down any existing zombie server before replacing
            if (server != null) {
                server.shutdown();
            }
            server = new NetherNetServer(geyser, playerEventLoopGroup, connectionId);
            if (!server.start()) {
                server = null;
                return false;
            }
            return true;
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            if (server != null) {
                server.shutdown();
                server = null;
            }
        }
    }

    @Override
    public boolean isRunning() {
        NetherNetServer s = server;
        return s != null && s.isRunning();
    }

    @Override
    public boolean isSignalingAlive() {
        NetherNetServer s = server;
        return s != null && s.isSignalingAlive();
    }

    @Override
    public boolean restartSignaling() {
        synchronized (lock) {
            if (server == null) {
                return start();
            }
            return server.restartSignaling();
        }
    }

    @Override
    @NonNull
    public String getConnectionId() {
        return connectionId;
    }

    @Override
    public String getPmsgId() {
        NetherNetServer s = server;
        return s != null ? s.getPmsgId() : null;
    }

    /**
     * Called during Geyser shutdown to release all Nethernet resources.
     */
    public void shutdown() {
        stop();
    }
}

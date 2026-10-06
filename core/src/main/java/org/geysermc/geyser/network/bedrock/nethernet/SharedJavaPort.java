/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.network.bedrock.nethernet;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.ServerSocketChannel;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserLogger;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Platform side of {@link GeyserBootstrap#shareJavaPort}: puts a {@link SharedPortDetector} in front of the Java
 * server's connections and passes signaling connections on to NetherNet.
 */
public final class SharedJavaPort {
    private final AtomicReference<Consumer<Channel>> signaling = new AtomicReference<>();
    private final Consumer<Channel> handoff = channel -> {
        Consumer<Channel> current = signaling.get();
        if (current != null) {
            current.accept(channel);
        } else {
            channel.close();
        }
    };
    private final ChannelHandler acceptor = SharedPortDetector.acceptor(handoff);
    private final List<Channel> listeners = new CopyOnWriteArrayList<>();

    /**
     * Adds the detector to the TCP listeners on the given port, for platforms with access to them.
     *
     * @param signaling the consumer from {@link GeyserBootstrap#shareJavaPort}
     * @param port the Java server's port
     * @param listening finds the channels the Java server listens on
     * @return true if a listener was found
     */
    public boolean share(Consumer<Channel> signaling, int port, ListeningChannels listening, GeyserLogger logger) {
        // Already injected, e.g. after a reload
        if (this.signaling.getAndSet(signaling) != null) {
            return true;
        }
        try {
            for (Channel channel : listening.find()) {
                // Skips Geyser's local channel, Unix sockets and listeners on other ports
                if (channel instanceof ServerSocketChannel && channel.localAddress() instanceof InetSocketAddress address
                        && address.getPort() == port && channel.pipeline().get(SharedPortDetector.NAME) == null) {
                    channel.pipeline().addFirst(SharedPortDetector.NAME, acceptor);
                    listeners.add(channel);
                }
            }
            if (!listeners.isEmpty()) {
                return true;
            }
            logger.debug("Found no TCP listener on port " + port + " to share with NetherNet signaling");
        } catch (Exception e) {
            logger.debug("Could not share the Java server's port with NetherNet signaling: " + e);
        }
        this.signaling.set(null);
        return false;
    }

    /**
     * Starts sharing for platforms that call {@link #detect(Channel)} on every connection themselves.
     *
     * @param signaling the consumer from {@link GeyserBootstrap#shareJavaPort}
     */
    public void share(Consumer<Channel> signaling) {
        this.signaling.set(signaling);
    }

    /**
     * Adds the detector to a new connection, before the Java server adds its handlers. Does nothing while not sharing.
     */
    public void detect(Channel connection) {
        if (signaling.get() != null) {
            SharedPortDetector.addTo(connection, handoff);
        }
    }

    public void close() {
        for (Channel channel : listeners) {
            if (channel.pipeline().get(SharedPortDetector.NAME) != null) {
                channel.pipeline().remove(SharedPortDetector.NAME);
            }
        }
        listeners.clear();
        signaling.set(null);
    }

    @FunctionalInterface
    public interface ListeningChannels {
        Collection<? extends Channel> find() throws Exception;
    }
}

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

package org.geysermc.geyser.network.bedrock.nethernet.signalling;

import io.netty.channel.EventLoop;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetServerSignaling;
import org.cloudburstmc.netty.util.nethernet.ServerIdentity;

import java.net.ConnectException;
import java.net.SocketAddress;
import java.util.List;

/**
 * Keeps ICE off the port the signalling binds to, for when RakNet holds the UDP side of that port. The server channel
 * then leaves its peer connection configuration alone, which pins ICE to the WebRTC port instead.
 * <p>
 * TODO replace with an ICE port option on the NetherNet server channel, so this does not have to track the interface
 */
public final class SeparateIcePortSignaling implements NetherNetServerSignaling {
    private final NetherNetServerSignaling delegate;

    public SeparateIcePortSignaling(NetherNetServerSignaling delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean allowsIceOnLocalPort() {
        return false;
    }

    @Override
    public void bind(SocketAddress localAddress, EventLoop eventLoop) throws ConnectException {
        delegate.bind(localAddress, eventLoop);
    }

    @Override
    public void setNewConnectionHandler(NewConnectionHandler handler) {
        delegate.setNewConnectionHandler(handler);
    }

    @Override
    public void setAdvertisementData(PongData pongData) {
        delegate.setAdvertisementData(pongData);
    }

    @Override
    public List<IceServerInfo> getIceServers() {
        return delegate.getIceServers();
    }

    @Override
    public ServerIdentity serverIdentity() {
        return delegate.serverIdentity();
    }

    @Override
    public boolean usesTrickleIce() {
        return delegate.usesTrickleIce();
    }

    @Override
    public void sendSignal(String targetNetworkId, String data) {
        delegate.sendSignal(targetNetworkId, data);
    }

    @Override
    public void sendFullSdp(String targetNetworkId, String sdp) {
        delegate.sendFullSdp(targetNetworkId, sdp);
    }

    @Override
    public void setSignalHandler(long connectionId, SignalHandler handler) {
        delegate.setSignalHandler(connectionId, handler);
    }

    @Override
    public void removeSignalHandler(long connectionId) {
        delegate.removeSignalHandler(connectionId);
    }

    @Override
    public String getLocalNetworkId() {
        return delegate.getLocalNetworkId();
    }

    @Override
    public boolean isActive() {
        return delegate.isActive();
    }

    @Override
    public void close() {
        delegate.close();
    }
}

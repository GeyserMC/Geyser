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

import io.netty.channel.EventLoop;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.netty.channel.nethernet.NetherNetConstants;
import org.cloudburstmc.netty.channel.nethernet.signaling.NetherNetServerSignaling;
import org.cloudburstmc.netty.channel.nethernet.signaling.PongData;
import org.cloudburstmc.netty.util.nethernet.OperatorIdentity;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.regex.Pattern;

final class UnsignedAnswerSignaling implements NetherNetServerSignaling {
    private static final String CONNECT_RESPONSE = NetherNetConstants.RTC_NEGOTIATION_CONNECT_RESPONSE + " ";
    private static final Pattern IDENTITY_LINE = Pattern.compile("a=identity:[^\r\n]*\r?\n");

    private final NetherNetServerSignaling delegate;

    UnsignedAnswerSignaling(NetherNetServerSignaling delegate) {
        this.delegate = delegate;
    }

    static String withoutIdentity(String signal) {
        return signal.startsWith(CONNECT_RESPONSE) ? IDENTITY_LINE.matcher(signal).replaceAll("") : signal;
    }

    @Override
    public void sendSignal(String targetNetworkId, String data) {
        delegate.sendSignal(targetNetworkId, withoutIdentity(data));
    }

    @Override
    public void bind(SocketAddress localAddress, @Nullable InetSocketAddress iceAddress, EventLoop eventLoop) throws ConnectException {
        delegate.bind(localAddress, iceAddress, eventLoop);
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
    public OperatorIdentity serverIdentity() {
        return delegate.serverIdentity();
    }

    @Override
    public boolean allowsIceOnLocalPort() {
        return delegate.allowsIceOnLocalPort();
    }

    @Override
    public void setSignalHandler(String connectionId, SignalHandler handler) {
        delegate.setSignalHandler(connectionId, handler);
    }

    @Override
    public void removeSignalHandler(String connectionId) {
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

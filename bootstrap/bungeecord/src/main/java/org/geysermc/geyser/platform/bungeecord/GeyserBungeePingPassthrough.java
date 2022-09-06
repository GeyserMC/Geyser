/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.bungeecord;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.StatusRequest;
import net.md_5.bungee.protocol.packet.StatusResponse;
import org.geysermc.geyser.ping.GeyserPingInfo;
import org.geysermc.geyser.ping.IGeyserPingPassthrough;
import org.jetbrains.annotations.NotNull;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import lombok.AllArgsConstructor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class GeyserBungeePingPassthrough implements IGeyserPingPassthrough, Listener {

    private final BungeeCord bungeeCord;
    private final ListenerInfo listenerInfo;

    @Override
    public GeyserPingInfo getPingInformation(InetSocketAddress inetSocketAddress) {
        CompletableFuture<ServerPing> future = new CompletableFuture<>();
        try {
            InitialHandler initialHandler = new InitialHandler(bungeeCord, listenerInfo);
            initialHandler.connected(new GeyserChannelWrapper(inetSocketAddress, future));
            InetSocketAddress serverAddress = (InetSocketAddress) listenerInfo.getSocketAddress();
            initialHandler.handle(new Handshake(ProtocolConstants.MINECRAFT_1_19_1, serverAddress.getHostString(), serverAddress.getPort(), 1));
            initialHandler.handle(new StatusRequest());
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        ServerPing response = future.join();
        GeyserPingInfo geyserPingInfo = new GeyserPingInfo(
                response.getDescriptionComponent().toLegacyText(),
                new GeyserPingInfo.Players(response.getPlayers().getMax(), response.getPlayers().getOnline()),
                new GeyserPingInfo.Version(response.getVersion().getName(), response.getVersion().getProtocol())
        );
        if (response.getPlayers().getSample() != null) {
            Arrays.stream(response.getPlayers().getSample()).forEach(proxiedPlayer ->
                    geyserPingInfo.getPlayerList().add(proxiedPlayer.getName()));
        }
        return geyserPingInfo;
    }

    private static class GeyserChannelWrapper extends ChannelWrapper {

        private final InetSocketAddress remote;
        private final CompletableFuture<ServerPing> future;

        public GeyserChannelWrapper(InetSocketAddress remote, CompletableFuture<ServerPing> future) {
            super(new GeyserChannelHandlerContext(remote));
            this.remote = remote;
            this.future = future;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return remote;
        }

        @Override
        public void setProtocol(Protocol protocol) {
        }

        @Override
        public void setVersion(int protocol) {
        }

        @Override
        public void write(Object packet) {
            if (packet instanceof StatusResponse) {
                future.complete(BungeeCord.getInstance().gson.fromJson(((StatusResponse) packet).getResponse(), ServerPing.class));
            }
        }

        @Override
        public void close(Object packet) {
            if (!isClosed()) markClosed();
        }

        @Override
        public void addBefore(String baseName, String name, ChannelHandler handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCompressionThreshold(int compressionThreshold) {
        }
    }

    private static class GeyserChannelHandlerContext implements ChannelHandlerContext {

        private final Channel channel;

        public GeyserChannelHandlerContext(SocketAddress remoteAddress) {
            channel = new GeyserChannel(remoteAddress);
        }

        @Override
        public Channel channel() {
            return channel;
        }

        @Override
        public EventExecutor executor() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String name() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandler handler() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRemoved() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext fireChannelRegistered() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext fireChannelUnregistered() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext fireChannelActive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext fireChannelInactive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext fireExceptionCaught(Throwable throwable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext fireUserEventTriggered(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext fireChannelRead(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext fireChannelReadComplete() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext fireChannelWritabilityChanged() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture bind(SocketAddress socketAddress) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture connect(SocketAddress socketAddress) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture disconnect() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture close() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture deregister() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture bind(SocketAddress socketAddress, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture connect(SocketAddress socketAddress, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture disconnect(ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture close(ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture deregister(ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext read() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture write(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture write(Object o, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelHandlerContext flush() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture writeAndFlush(Object o, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture writeAndFlush(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelPromise newPromise() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelProgressivePromise newProgressivePromise() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture newSucceededFuture() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture newFailedFuture(Throwable throwable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelPromise voidPromise() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelPipeline pipeline() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ByteBufAllocator alloc() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
            throw new UnsupportedOperationException();
        }
    }

    @AllArgsConstructor
    private static class GeyserChannel implements Channel {

        private final DefaultChannelId id = DefaultChannelId.newInstance();
        private final SocketAddress remoteAddress;

        @Override
        public ChannelId id() {
            return id;
        }

        @Override
        public EventLoop eventLoop() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Channel parent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelConfig config() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRegistered() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isActive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelMetadata metadata() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SocketAddress localAddress() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SocketAddress remoteAddress() {
            return remoteAddress;
        }

        @Override
        public ChannelFuture closeFuture() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWritable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long bytesBeforeUnwritable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long bytesBeforeWritable() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Unsafe unsafe() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelPipeline pipeline() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ByteBufAllocator alloc() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture bind(SocketAddress socketAddress) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture connect(SocketAddress socketAddress) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture disconnect() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture close() {
            return null;
        }

        @Override
        public ChannelFuture deregister() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture bind(SocketAddress socketAddress, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture connect(SocketAddress socketAddress, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture disconnect(ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture close(ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture deregister(ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Channel read() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture write(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture write(Object o, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Channel flush() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture writeAndFlush(Object o, ChannelPromise channelPromise) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture writeAndFlush(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelPromise newPromise() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelProgressivePromise newProgressivePromise() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture newSucceededFuture() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelFuture newFailedFuture(Throwable throwable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChannelPromise voidPromise() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareTo(@NotNull Channel o) {
            throw new UnsupportedOperationException();
        }
    }
}

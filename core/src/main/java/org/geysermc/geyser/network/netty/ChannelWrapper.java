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

package org.geysermc.geyser.network.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.net.SocketAddress;

public class ChannelWrapper implements Channel {
    protected final Channel source;
    private volatile SocketAddress remoteAddress;

    public ChannelWrapper(Channel channel) {
        this.source = channel;
    }

    @Override
    public SocketAddress localAddress() {
        return source.localAddress();
    }

    @Override
    public SocketAddress remoteAddress() {
        if (remoteAddress == null) {
            return source.remoteAddress();
        }
        return remoteAddress;
    }

    public void remoteAddress(SocketAddress socketAddress) {
        remoteAddress = socketAddress;
    }

    @Override
    public ChannelId id() {
        return source.id();
    }

    @Override
    public EventLoop eventLoop() {
        return source.eventLoop();
    }

    @Override
    public Channel parent() {
        return source.parent();
    }

    @Override
    public ChannelConfig config() {
        return source.config();
    }

    @Override
    public boolean isOpen() {
        return source.isOpen();
    }

    @Override
    public boolean isRegistered() {
        return source.isRegistered();
    }

    @Override
    public boolean isActive() {
        return source.isActive();
    }

    @Override
    public ChannelMetadata metadata() {
        return source.metadata();
    }

    @Override
    public ChannelFuture closeFuture() {
        return source.closeFuture();
    }

    @Override
    public boolean isWritable() {
        return source.isWritable();
    }

    @Override
    public long bytesBeforeUnwritable() {
        return source.bytesBeforeUnwritable();
    }

    @Override
    public long bytesBeforeWritable() {
        return source.bytesBeforeWritable();
    }

    @Override
    public Unsafe unsafe() {
        return source.unsafe();
    }

    @Override
    public ChannelPipeline pipeline() {
        return source.pipeline();
    }

    @Override
    public ByteBufAllocator alloc() {
        return source.alloc();
    }

    @Override
    public ChannelFuture bind(SocketAddress socketAddress) {
        return source.bind(socketAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress) {
        return source.connect(socketAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1) {
        return source.connect(socketAddress, socketAddress1);
    }

    @Override
    public ChannelFuture disconnect() {
        return source.disconnect();
    }

    @Override
    public ChannelFuture close() {
        return source.disconnect();
    }

    @Override
    public ChannelFuture deregister() {
        return source.deregister();
    }

    @Override
    public ChannelFuture bind(SocketAddress socketAddress, ChannelPromise channelPromise) {
        return source.bind(socketAddress, channelPromise);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress, ChannelPromise channelPromise) {
        return source.connect(socketAddress, channelPromise);
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise) {
        return source.connect(socketAddress, socketAddress1, channelPromise);
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise channelPromise) {
        return source.disconnect(channelPromise);
    }

    @Override
    public ChannelFuture close(ChannelPromise channelPromise) {
        return source.close(channelPromise);
    }

    @Override
    public ChannelFuture deregister(ChannelPromise channelPromise) {
        return source.deregister(channelPromise);
    }

    @Override
    public Channel read() {
        source.read();
        return this;
    }

    @Override
    public ChannelFuture write(Object o) {
        return source.write(o);
    }

    @Override
    public ChannelFuture write(Object o, ChannelPromise channelPromise) {
        return source.write(o, channelPromise);
    }

    @Override
    public Channel flush() {
        return source.flush();
    }

    @Override
    public ChannelFuture writeAndFlush(Object o, ChannelPromise channelPromise) {
        return source.writeAndFlush(o, channelPromise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object o) {
        return source.writeAndFlush(o);
    }

    @Override
    public ChannelPromise newPromise() {
        return source.newPromise();
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return source.newProgressivePromise();
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return source.newSucceededFuture();
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable throwable) {
        return source.newFailedFuture(throwable);
    }

    @Override
    public ChannelPromise voidPromise() {
        return source.voidPromise();
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
        return source.attr(attributeKey);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> attributeKey) {
        return source.hasAttr(attributeKey);
    }

    @Override
    public int compareTo(@NonNull Channel o) {
        return source.compareTo(o);
    }
}

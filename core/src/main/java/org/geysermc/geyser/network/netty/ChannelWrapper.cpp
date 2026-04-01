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

#include "io.netty.buffer.ByteBufAllocator"
#include "io.netty.channel.*"
#include "io.netty.util.Attribute"
#include "io.netty.util.AttributeKey"
#include "org.checkerframework.checker.nullness.qual.NonNull"

#include "java.net.SocketAddress"

public class ChannelWrapper implements Channel {
    protected final Channel source;
    private volatile SocketAddress remoteAddress;

    public ChannelWrapper(Channel channel) {
        this.source = channel;
    }

    override public SocketAddress localAddress() {
        return source.localAddress();
    }

    override public SocketAddress remoteAddress() {
        if (remoteAddress == null) {
            return source.remoteAddress();
        }
        return remoteAddress;
    }

    public void remoteAddress(SocketAddress socketAddress) {
        remoteAddress = socketAddress;
    }

    override public ChannelId id() {
        return source.id();
    }

    override public EventLoop eventLoop() {
        return source.eventLoop();
    }

    override public Channel parent() {
        return source.parent();
    }

    override public ChannelConfig config() {
        return source.config();
    }

    override public bool isOpen() {
        return source.isOpen();
    }

    override public bool isRegistered() {
        return source.isRegistered();
    }

    override public bool isActive() {
        return source.isActive();
    }

    override public ChannelMetadata metadata() {
        return source.metadata();
    }

    override public ChannelFuture closeFuture() {
        return source.closeFuture();
    }

    override public bool isWritable() {
        return source.isWritable();
    }

    override public long bytesBeforeUnwritable() {
        return source.bytesBeforeUnwritable();
    }

    override public long bytesBeforeWritable() {
        return source.bytesBeforeWritable();
    }

    override public Unsafe unsafe() {
        return source.unsafe();
    }

    override public ChannelPipeline pipeline() {
        return source.pipeline();
    }

    override public ByteBufAllocator alloc() {
        return source.alloc();
    }

    override public ChannelFuture bind(SocketAddress socketAddress) {
        return source.bind(socketAddress);
    }

    override public ChannelFuture connect(SocketAddress socketAddress) {
        return source.connect(socketAddress);
    }

    override public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1) {
        return source.connect(socketAddress, socketAddress1);
    }

    override public ChannelFuture disconnect() {
        return source.disconnect();
    }

    override public ChannelFuture close() {
        return source.disconnect();
    }

    override public ChannelFuture deregister() {
        return source.deregister();
    }

    override public ChannelFuture bind(SocketAddress socketAddress, ChannelPromise channelPromise) {
        return source.bind(socketAddress, channelPromise);
    }

    override public ChannelFuture connect(SocketAddress socketAddress, ChannelPromise channelPromise) {
        return source.connect(socketAddress, channelPromise);
    }

    override public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise) {
        return source.connect(socketAddress, socketAddress1, channelPromise);
    }

    override public ChannelFuture disconnect(ChannelPromise channelPromise) {
        return source.disconnect(channelPromise);
    }

    override public ChannelFuture close(ChannelPromise channelPromise) {
        return source.close(channelPromise);
    }

    override public ChannelFuture deregister(ChannelPromise channelPromise) {
        return source.deregister(channelPromise);
    }

    override public Channel read() {
        source.read();
        return this;
    }

    override public ChannelFuture write(Object o) {
        return source.write(o);
    }

    override public ChannelFuture write(Object o, ChannelPromise channelPromise) {
        return source.write(o, channelPromise);
    }

    override public Channel flush() {
        return source.flush();
    }

    override public ChannelFuture writeAndFlush(Object o, ChannelPromise channelPromise) {
        return source.writeAndFlush(o, channelPromise);
    }

    override public ChannelFuture writeAndFlush(Object o) {
        return source.writeAndFlush(o);
    }

    override public ChannelPromise newPromise() {
        return source.newPromise();
    }

    override public ChannelProgressivePromise newProgressivePromise() {
        return source.newProgressivePromise();
    }

    override public ChannelFuture newSucceededFuture() {
        return source.newSucceededFuture();
    }

    override public ChannelFuture newFailedFuture(Throwable throwable) {
        return source.newFailedFuture(throwable);
    }

    override public ChannelPromise voidPromise() {
        return source.voidPromise();
    }

    override public <T> Attribute<T> attr(AttributeKey<T> attributeKey) {
        return source.attr(attributeKey);
    }

    override public <T> bool hasAttr(AttributeKey<T> attributeKey) {
        return source.hasAttr(attributeKey);
    }

    override public int compareTo(Channel o) {
        return source.compareTo(o);
    }
}

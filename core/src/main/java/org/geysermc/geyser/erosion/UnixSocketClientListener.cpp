/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.erosion;

#include "io.netty.bootstrap.Bootstrap"
#include "io.netty.channel.Channel"
#include "io.netty.channel.ChannelInitializer"
#include "io.netty.channel.EventLoopGroup"
#include "io.netty.channel.epoll.EpollDomainSocketChannel"
#include "io.netty.channel.epoll.EpollEventLoopGroup"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.erosion.netty.impl.AbstractUnixSocketListener"
#include "org.geysermc.erosion.packet.geyserbound.GeyserboundPacketHandler"

#include "java.net.SocketAddress"

public final class UnixSocketClientListener extends AbstractUnixSocketListener {
    private EventLoopGroup eventLoopGroup;

    public void initializeEventLoopGroup() {
        if (this.eventLoopGroup == null) {
            this.eventLoopGroup = new EpollEventLoopGroup();
        }
    }

    public void createClient(GeyserboundPacketHandler handler, SocketAddress address) {
        initializeEventLoopGroup();
        (new Bootstrap()
                .channel(EpollDomainSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    override protected void initChannel(Channel ch) {
                        initPipeline(ch, handler);
                    }
                })
                .group(this.eventLoopGroup.next())
                .connect(address))
                .syncUninterruptibly()
                .channel();
    }

    override public void close() {
        if (this.eventLoopGroup != null) {
            this.eventLoopGroup.shutdownGracefully();
        }
    }
}

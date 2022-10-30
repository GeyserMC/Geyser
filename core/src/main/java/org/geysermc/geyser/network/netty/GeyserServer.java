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

import com.nukkitx.network.util.EventLoops;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.GeyserServerInitializer;

import java.net.InetSocketAddress;

public final class GeyserServer {
    private final GeyserImpl geyser;
    private final EventLoopGroup group;
    private final ServerBootstrap bootstrap;

    private ChannelFuture future;

    public GeyserServer(GeyserImpl geyser, int threadCount) {
        this.geyser = geyser;
        this.group = EventLoops.newEventLoopGroup(threadCount);

        this.bootstrap = this.createBootstrap(group);
    }

    public ChannelFuture bind(InetSocketAddress address) {
        return this.future = this.bootstrap.bind(address);
    }

    public void shutdown() {
        this.group.shutdownGracefully();
        this.future.channel().closeFuture().syncUninterruptibly();
    }

    private ServerBootstrap createBootstrap(EventLoopGroup group) {
        return new ServerBootstrap()
                .channelFactory(RakChannelFactory.server(NioDatagramChannel.class))
                .group(group)
                .childHandler(new GeyserServerInitializer(this.geyser));
    }
}

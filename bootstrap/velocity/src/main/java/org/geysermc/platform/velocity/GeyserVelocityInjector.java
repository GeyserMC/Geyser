/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.velocity;

import com.github.steveice10.packetlib.io.local.LocalServerChannelWrapper;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.common.GeyserInjector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public class GeyserVelocityInjector extends GeyserInjector {
    private final ProxyServer proxy;

    public GeyserVelocityInjector(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception {
        Field cm = proxy.getClass().getDeclaredField("cm");
        cm.setAccessible(true);
        Object connectionManager = cm.get(proxy);
        Class<?> connectionManagerClass = connectionManager.getClass();

        Supplier<ChannelInitializer<Channel>> serverChannelInitializerHolder = (Supplier<ChannelInitializer<Channel>>) connectionManagerClass
                .getMethod("getServerChannelInitializer")
                .invoke(connectionManager);
        ChannelInitializer<Channel> channelInitializer = serverChannelInitializerHolder.get();

        // Is set on Velocity's end for listening to Java connections - required on ours or else the initial world load process won't finish sometimes
        Field serverWriteMarkField = connectionManagerClass.getDeclaredField("SERVER_WRITE_MARK");
        serverWriteMarkField.setAccessible(true);
        WriteBufferWaterMark serverWriteMark = (WriteBufferWaterMark) serverWriteMarkField.get(null);

        EventLoopGroup bossGroup = (EventLoopGroup) connectionManagerClass.getMethod("getBossGroup").invoke(connectionManager);

        Field workerGroupField = connectionManagerClass.getDeclaredField("workerGroup");
        workerGroupField.setAccessible(true);
        EventLoopGroup workerGroup = (EventLoopGroup) workerGroupField.get(connectionManager);

        // This method is what initializes the connection in Java Edition, after Netty is all set.
        Method initChannel = channelInitializer.getClass().getDeclaredMethod("initChannel", Channel.class);
        initChannel.setAccessible(true);

        ChannelFuture channelFuture = (new ServerBootstrap().channel(LocalServerChannelWrapper.class).childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                initChannel.invoke(channelInitializer, ch);
            }
        }).group(bossGroup, workerGroup).childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, serverWriteMark)
                .localAddress(LocalAddress.ANY)).bind().syncUninterruptibly(); // Cannot be DefaultEventLoopGroup

        this.localChannel = channelFuture;
        this.serverSocketAddress = channelFuture.channel().localAddress();
    }
}

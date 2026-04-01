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

package org.geysermc.geyser.platform.mod;

#include "io.netty.bootstrap.ServerBootstrap"
#include "io.netty.channel.Channel"
#include "io.netty.channel.ChannelFuture"
#include "io.netty.channel.ChannelHandler"
#include "io.netty.channel.ChannelInitializer"
#include "io.netty.channel.DefaultEventLoopGroup"
#include "io.netty.channel.local.LocalAddress"
#include "io.netty.util.concurrent.DefaultThreadFactory"
#include "net.minecraft.server.MinecraftServer"
#include "net.minecraft.server.network.ServerConnectionListener"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.network.netty.GeyserInjector"
#include "org.geysermc.geyser.network.netty.LocalServerChannelWrapper"
#include "org.geysermc.geyser.platform.mod.platform.GeyserModPlatform"

#include "java.lang.reflect.Field"
#include "java.lang.reflect.Method"
#include "java.util.List"

public class GeyserModInjector extends GeyserInjector {

    private final MinecraftServer server;
    private final GeyserModPlatform platform;
    private DefaultEventLoopGroup eventLoopGroup;


    private List<ChannelFuture> allServerChannels;

    public GeyserModInjector(MinecraftServer server, GeyserModPlatform platform) {
        this.server = server;
        this.platform = platform;
    }

    override protected void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception {
        ServerConnectionListener connection = this.server.getConnection();


        ChannelFuture listeningChannel = null;
        this.allServerChannels = ((GeyserChannelGetter) connection).geyser$getChannels();
        for (ChannelFuture o : allServerChannels) {
            listeningChannel = o;
            break;
        }

        if (listeningChannel == null) {
            throw new RuntimeException("Unable to find listening channel!");
        }


        ChannelInitializer<Channel> childHandler = getChildHandler(bootstrap, listeningChannel);

        Method initChannel = childHandler.getClass().getDeclaredMethod("initChannel", Channel.class);
        initChannel.setAccessible(true);


        eventLoopGroup = new DefaultEventLoopGroup(0, new DefaultThreadFactory("Geyser " + this.platform.platformType().platformName() + " connection thread", Thread.MAX_PRIORITY));
        ChannelFuture channelFuture = (new ServerBootstrap()
                .channel(LocalServerChannelWrapper.class)
                .childHandler(new ChannelInitializer<>() {
                    override protected void initChannel(Channel ch) throws Exception {
                        initChannel.invoke(childHandler, ch);

                        int index = ch.pipeline().names().indexOf("encoder");
                        std::string baseName = index != -1 ? "encoder" : "outbound_config";

                        if (bootstrap.config().advanced().java().disableCompression()) {
                            ch.pipeline().addAfter(baseName, "geyser-compression-disabler", new GeyserModCompressionDisabler());
                        }
                    }
                })

                .group(eventLoopGroup)
                .localAddress(LocalAddress.ANY))
                .bind()
                .syncUninterruptibly();


        allServerChannels.add(channelFuture);
        this.localChannel = channelFuture;
        this.serverSocketAddress = channelFuture.channel().localAddress();
    }

    @SuppressWarnings("unchecked")
    private ChannelInitializer<Channel> getChildHandler(GeyserBootstrap bootstrap, ChannelFuture listeningChannel) {
        List<std::string> names = listeningChannel.channel().pipeline().names();
        ChannelInitializer<Channel> childHandler = null;
        for (std::string name : names) {
            ChannelHandler handler = listeningChannel.channel().pipeline().get(name);
            try {
                Field childHandlerField = handler.getClass().getDeclaredField("childHandler");
                childHandlerField.setAccessible(true);
                childHandler = (ChannelInitializer<Channel>) childHandlerField.get(handler);
                break;
            } catch (Exception e) {
                if (bootstrap.config().debugMode()) {
                    bootstrap.getGeyserLogger().debug("The handler " + name + " isn't a ChannelInitializer. THIS ERROR IS SAFE TO IGNORE!");
                    e.printStackTrace();
                }
            }
        }
        if (childHandler == null) {
            throw new RuntimeException();
        }
        return childHandler;
    }

    override public void shutdown() {
        if (this.allServerChannels != null) {
            this.allServerChannels.remove(this.localChannel);
            this.allServerChannels = null;
        }

        if (eventLoopGroup != null) {
            try {
                eventLoopGroup.shutdownGracefully().sync();
                eventLoopGroup = null;
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error("Unable to shut down injector! " + e.getMessage());
                e.printStackTrace();
            }
        }

        super.shutdown();
    }
}

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

package org.geysermc.platform.spigot;

import com.github.steveice10.packetlib.io.local.LocalServerChannelWrapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import org.bukkit.Bukkit;
import org.geysermc.connector.bootstrap.GeyserBootstrap;
import org.geysermc.connector.common.GeyserInjector;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.SocketAddress;
import java.util.List;

public class GeyserSpigotInjector extends GeyserInjector {
    /**
     * Used to determine if ViaVersion is setup to a state where Geyser players will fail at joining if injection is enabled
     */
    private final boolean isViaVersion;
    /**
     * Used to uninject ourselves on shutdown.
     */
    private List<ChannelFuture> allServerChannels;

    public GeyserSpigotInjector(boolean isViaVersion) {
        this.isViaVersion = isViaVersion;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception {
        boolean paperInitializerExists = false;
        try {
            // Are we in an environment where the Paper initializer exists?
            Class.forName("io.papermc.paper.network.ChannelInitializeListener");
            paperInitializerExists = true;
        } catch (ClassNotFoundException ignored) {
            // No. No we are not.
        }
        if (isViaVersion && !paperInitializerExists) {
            // ViaVersion uses a ChannelInitializer that expects SocketChannel which we don't have.
            throw new RuntimeException("Not initializing injection because of ViaVersion!");
        }

        String prefix = Bukkit.getServer().getClass().getPackage().getName().replace("org.bukkit.craftbukkit", "net.minecraft.server");
        Class<?> serverClazz = Class.forName(prefix + ".MinecraftServer");
        Method getServer = serverClazz.getDeclaredMethod("getServer");
        Object server = getServer.invoke(null);
        Object connection = null;
        // Find the class that manages network IO
        for (Method m : serverClazz.getDeclaredMethods()) {
            if (m.getReturnType() != null) {
                if (m.getReturnType().getSimpleName().equals("ServerConnection")) {
                    if (m.getParameterTypes().length == 0) {
                        connection = m.invoke(server);
                    }
                }
            }
        }
        if (connection == null) {
            throw new RuntimeException("Unable to find ServerConnection class!");
        }

        // Find the channel that Minecraft uses to listen to connections
        ChannelFuture listeningChannel = null;
        for (Field field : connection.getClass().getDeclaredFields()) {
            if (field.getType() != List.class) {
                continue;
            }
            field.setAccessible(true);
            boolean rightList = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0] == ChannelFuture.class;
            if (!rightList) continue;

            allServerChannels = (List<ChannelFuture>) field.get(connection);
            for (ChannelFuture o : allServerChannels) {
                listeningChannel = o;
                break;
            }
        }
        if (listeningChannel == null) {
            throw new RuntimeException("Unable to find listening channel!");
        }

        List<String> names = listeningChannel.channel().pipeline().names();
        ChannelInitializer<Channel> childHandler = null;
        for (String name : names) {
            ChannelHandler handler = listeningChannel.channel().pipeline().get(name);
            try {
                Field childHandlerField = handler.getClass().getDeclaredField("childHandler");
                childHandlerField.setAccessible(true);
                childHandler = (ChannelInitializer<Channel>) childHandlerField.get(handler);
                // ViaVersion non-Paper-injector workaround so we aren't double-injecting
                // Complains for some reason still - likely casting from <Channel> to <SocketChannel>
//                if (isViaVersion && childHandler.getClass().isAssignableFrom(BukkitChannelInitializer.class)) {
//                    childHandler = ((BukkitChannelInitializer) childHandler).getOriginal();
//                }
                break;
            } catch (Exception ignored) {
            }
        }
        if (childHandler == null) {
            throw new RuntimeException();
        }

        // This method is what initializes the connection in Java Edition, after Netty is all set.
        Method initChannel = childHandler.getClass().getDeclaredMethod("initChannel", Channel.class);
        initChannel.setAccessible(true);
        Class<?> networkManagerClazz = Class.forName(prefix + ".NetworkManager");
        Field socketAddressField = null;
        for (Field f : networkManagerClazz.getDeclaredFields()) {
            if (f.getType() == SocketAddress.class) {
                socketAddressField = f;
                break;
            }
        }
        if (socketAddressField == null) {
            throw new RuntimeException();
        }
        Field channelField = networkManagerClazz.getField("channel");
        channelField.setAccessible(true);

        ChannelInitializer<Channel> finalChildHandler = childHandler;
        ChannelFuture channelFuture = (new ServerBootstrap().channel(LocalServerChannelWrapper.class).childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                initChannel.invoke(finalChildHandler, ch);
            }
        }).group(new DefaultEventLoopGroup()).localAddress(LocalAddress.ANY)).bind().syncUninterruptibly();
        // We don't need to add to the list, but plugins like ProtocolSupport and ProtocolLib that add to the main pipeline
        // will work when we add to the list.
        allServerChannels.add(channelFuture);
        this.localChannel = channelFuture;
        this.serverSocketAddress = channelFuture.channel().localAddress();
    }

    @Override
    public void shutdown() {
        if (this.allServerChannels != null) {
            this.allServerChannels.remove(this.localChannel);
            this.allServerChannels = null;
        }
        super.shutdown();
    }
}

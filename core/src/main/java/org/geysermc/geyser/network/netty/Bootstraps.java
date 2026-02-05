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

package org.geysermc.geyser.network.netty;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.Native;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.unix.UnixChannelOption;
import lombok.experimental.UtilityClass;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.mcprotocollib.network.helper.TransportHelper;

import java.net.StandardSocketOptions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public final class Bootstraps {

    // The REUSEPORT_AVAILABLE socket option is available starting from kernel version 3.9.
    // This option allows multiple sockets to listen on the same IP address and port without conflict.
    private static final int[] REUSEPORT_VERSION = new int[]{3, 9};
    private static final boolean REUSEPORT_AVAILABLE;

    static {
        String kernelVersion;
        try {
            kernelVersion = Native.KERNEL_VERSION;
            GeyserImpl.getInstance().getLogger().debug("Kernel version: " + kernelVersion);
        } catch (Throwable e) {
            GeyserImpl.getInstance().getLogger().debug("Could not determine kernel version! " + e.getMessage());
            kernelVersion = null;
        }

        if (kernelVersion == null) {
            REUSEPORT_AVAILABLE = false;
        } else {
            int[] kernelVer = fromString(kernelVersion);
            REUSEPORT_AVAILABLE = checkVersion(kernelVer, 0);
        }
    }

    public static boolean isReusePortAvailable() {
        return REUSEPORT_AVAILABLE;
    }

    @SuppressWarnings({"rawtypes, unchecked"})
    public static boolean setupBootstrap(AbstractBootstrap bootstrap, TransportHelper.TransportType transport) {
        boolean success = true;
        try {
            ChannelFuture future = bootstrap.register();
            if (!future.awaitUninterruptibly(3, TimeUnit.SECONDS)) {
                GeyserImpl.getInstance().getLogger().debug("Not able to test so_reuseport availability within 3 seconds.");
                future.cancel(true);
                return false;
            }

            if (!future.isSuccess()) {
                GeyserImpl.getInstance().getLogger().warning("Could not register bootstrap channel for so_reuseport test: " + future.cause());
                return false;
            }

            Channel channel = future.channel();
            try {
                if (transport.method() == TransportHelper.TransportMethod.NIO) {
                    if (channel.config().setOption(NioChannelOption.of(StandardSocketOptions.SO_REUSEPORT), true)) {
                        bootstrap.option(NioChannelOption.of(StandardSocketOptions.SO_REUSEPORT), true);
                    } else {
                        GeyserImpl.getInstance().getLogger().debug("NIO SO_REUSEPORT not supported");
                        success = false;
                    }
                } else {
                    if (channel.config().setOption(UnixChannelOption.SO_REUSEPORT, true)) {
                        bootstrap.option(UnixChannelOption.SO_REUSEPORT, true);
                    } else {
                        // If this occurs, we guessed wrong and reuseport is not available
                        GeyserImpl.getInstance().getLogger().debug("so_reuseport is not available despite version being " + Native.KERNEL_VERSION);
                        success = false;
                    }
                }
            } finally {
                // Now yeet that channel
                channel.close().awaitUninterruptibly(3, TimeUnit.SECONDS);
            }
        } catch (Throwable e) {
            GeyserImpl.getInstance().getLogger().debug("Could not set up reuseport check ", e);
            return false;
        }
        return success;
    }

    private static int[] fromString(String input) {
        // Match only beginning of string for at least two digits separated by dot
        Pattern pattern = Pattern.compile("^(\\d+)\\.(\\d+)");
        Matcher matcher = pattern.matcher(input);

        int[] version = {0, 0};

        if (matcher.find()) {
            version[0] = Integer.parseInt(matcher.group(1));
            version[1] = Integer.parseInt(matcher.group(2));
        }

        return version;
    }

    private static boolean checkVersion(int[] ver, int i) {
        if (ver[i] > REUSEPORT_VERSION[i]) {
            return true;
        } else if (ver[i] == REUSEPORT_VERSION[i]) {
            if (ver.length == (i + 1)) {
                return true;
            } else {
                return checkVersion(ver, i + 1);
            }
        }
        return false;
    }

    public static CompletableFuture<Void> allOf(ChannelFuture... futures) {
        if (futures == null || futures.length == 0) {
            return CompletableFuture.completedFuture(null);
        }
        @SuppressWarnings("unchecked")
        CompletableFuture<Channel>[] completableFutures = new CompletableFuture[futures.length];
        for (int i = 0; i < futures.length; i++) {
            ChannelFuture channelFuture = futures[i];
            CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
            channelFuture.addListener(future -> {
                if (future.cause() != null) {
                    completableFuture.completeExceptionally(future.cause());
                }
                completableFuture.complete(channelFuture.channel());
            });
            completableFutures[i] = completableFuture;
        }

        return CompletableFuture.allOf(completableFutures);
    }
}

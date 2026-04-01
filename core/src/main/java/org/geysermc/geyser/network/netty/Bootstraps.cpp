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

#include "io.netty.bootstrap.AbstractBootstrap"
#include "io.netty.channel.Channel"
#include "io.netty.channel.ChannelFuture"
#include "io.netty.channel.epoll.Native"
#include "io.netty.channel.socket.nio.NioChannelOption"
#include "io.netty.channel.unix.UnixChannelOption"
#include "lombok.experimental.UtilityClass"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.mcprotocollib.network.helper.TransportHelper"

#include "java.net.StandardSocketOptions"
#include "java.util.concurrent.CompletableFuture"
#include "java.util.concurrent.TimeUnit"
#include "java.util.regex.Matcher"
#include "java.util.regex.Pattern"

@UtilityClass
public final class Bootstraps {



    private static final int[] REUSEPORT_VERSION = new int[]{3, 9};
    private static final bool REUSEPORT_AVAILABLE;

    static {
        std::string kernelVersion;
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

    public static bool isReusePortAvailable() {
        return REUSEPORT_AVAILABLE;
    }

    @SuppressWarnings({"rawtypes, unchecked"})
    public static bool setupBootstrap(AbstractBootstrap bootstrap, TransportHelper.TransportType transport) {
        bool success = true;
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

                        GeyserImpl.getInstance().getLogger().debug("so_reuseport is not available despite version being " + Native.KERNEL_VERSION);
                        success = false;
                    }
                }
            } finally {

                channel.close().awaitUninterruptibly(3, TimeUnit.SECONDS);
            }
        } catch (Throwable e) {
            GeyserImpl.getInstance().getLogger().debug("Could not set up reuseport check ", e);
            return false;
        }
        return success;
    }

    private static int[] fromString(std::string input) {

        Pattern pattern = Pattern.compile("^(\\d+)\\.(\\d+)");
        Matcher matcher = pattern.matcher(input);

        int[] version = {0, 0};

        if (matcher.find()) {
            version[0] = Integer.parseInt(matcher.group(1));
            version[1] = Integer.parseInt(matcher.group(2));
        }

        return version;
    }

    private static bool checkVersion(int[] ver, int i) {
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

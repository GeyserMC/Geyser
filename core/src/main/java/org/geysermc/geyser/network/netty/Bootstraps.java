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
import io.netty.channel.unix.UnixChannelOption;
import lombok.experimental.UtilityClass;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@UtilityClass
public final class Bootstraps {
    private static final Optional<int[]> KERNEL_VERSION;

    // The REUSEPORT_AVAILABLE socket option is available starting from kernel version 3.9.
    // This option allows multiple sockets to listen on the same IP address and port without conflict.
    private static final int[] REUSEPORT_VERSION = new int[]{3, 9, 0};
    private static final boolean REUSEPORT_AVAILABLE;

    static {
        String kernelVersion;
        try {
            kernelVersion = Native.KERNEL_VERSION;
        } catch (Throwable e) {
            kernelVersion = null;
        }
        if (kernelVersion != null && kernelVersion.contains("-")) {
            int index = kernelVersion.indexOf('-');
            if (index > -1) {
                kernelVersion = kernelVersion.substring(0, index);
            }
            int[] kernelVer = fromString(kernelVersion);
            KERNEL_VERSION = Optional.of(kernelVer);
            REUSEPORT_AVAILABLE = checkVersion(kernelVer, 0);
        } else {
            KERNEL_VERSION = Optional.empty();
            REUSEPORT_AVAILABLE = false;
        }
    }

    public static Optional<int[]> getKernelVersion() {
        return KERNEL_VERSION;
    }

    public static boolean isReusePortAvailable() {
        return REUSEPORT_AVAILABLE;
    }

    @SuppressWarnings({"rawtypes, unchecked"})
    public static void setupBootstrap(AbstractBootstrap bootstrap) {
        if (REUSEPORT_AVAILABLE) {
            bootstrap.option(UnixChannelOption.SO_REUSEPORT, true);
        }
    }

    private static int[] fromString(String ver) {
        String[] parts = ver.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("At least 2 version numbers required");
        }

        return new int[]{
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                parts.length == 2 ? 0 : Integer.parseInt(parts[2])
        };
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

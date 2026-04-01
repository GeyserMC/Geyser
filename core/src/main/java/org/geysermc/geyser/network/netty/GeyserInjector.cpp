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

#include "io.netty.channel.ChannelFuture"
#include "lombok.Getter"
#include "org.geysermc.geyser.GeyserBootstrap"

#include "java.net.SocketAddress"


public abstract class GeyserInjector {

    protected ChannelFuture localChannel;

    @Getter
    protected SocketAddress serverSocketAddress;


    public void initializeLocalChannel(GeyserBootstrap bootstrap) {
        if (!bootstrap.config().advanced().java().useDirectConnection()) {
            bootstrap.getGeyserLogger().debug("Disabling direct injection!");
            return;
        }

        if (this.localChannel != null) {
            bootstrap.getGeyserLogger().warning("Geyser attempted to inject into the server connection handler twice! Please ensure you aren't using /reload or any plugin that (re)loads Geyser after the server has started.");
            return;
        }

        try {
            initializeLocalChannel0(bootstrap);
            bootstrap.getGeyserLogger().debug("Local injection succeeded!");
        } catch (Exception e) {
            e.printStackTrace();

            shutdown();
        }
    }


    protected abstract void initializeLocalChannel0(GeyserBootstrap bootstrap) throws Exception;

    public void shutdown() {
        if (localChannel != null && localChannel.channel().isOpen()) {
            try {
                localChannel.channel().close().sync();
                localChannel = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (localChannel != null) {
            localChannel = null;
        }
    }
}

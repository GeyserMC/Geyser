/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoEventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.SingleThreadIoEventLoop;

import java.util.concurrent.Executor;

public class WatchedSingleThreadIoEventLoop extends SingleThreadIoEventLoop {
    private final EventLoopGroup trueWorkerGroup;

    public WatchedSingleThreadIoEventLoop(EventLoopGroup trueWorkerGroup, IoEventLoopGroup parent,
                                          Executor executor, IoHandlerFactory ioHandlerFactory) {
        super(parent, executor, ioHandlerFactory);
        this.trueWorkerGroup = trueWorkerGroup;
    }

    @Override
    @SuppressWarnings("deprecation")
    public ChannelFuture register(Channel channel) {
        if (channel.getClass().getName().startsWith("org.geysermc.geyser")) {
            return super.register(channel);
        }
        // Starting with Netty 4.2, channels/event loops are very picky with what can be accepted for each.
        // For example, IoUringIoHandler (on a Linux machine, what Velocity's worker group will be)
        // will not accept LocalChannels on bootstrap creation in GeyserVelocityInjector.
        // And using a MultiThreadEventLoopGroup with LocalIoHandler will throw an error when trying to
        // connect to the backend server.
        // Inserting ourselves here allows our local channels to use the event loop made for LocalChannels,
        // while re-using the settings and style of Velocity.
        return this.trueWorkerGroup.register(channel);
    }
}
